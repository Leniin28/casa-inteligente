"""Telemetría normalizada del ESP32: ingesta y lectura (modo "sensors").

El backend no sabe qué sensor midió cada valor. Los contadores acumulados (`energy_kwh`,
`total_liters`) son opcionales: si llegan se usan sus diferencias (tolerando reinicios a 0);
si no, se integra la potencia/caudal de la lectura anterior en el tiempo transcurrido.
"""

from datetime import UTC, date, datetime, time, timedelta, tzinfo

from sqlalchemy import Engine, func
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import InstrumentedAttribute
from sqlmodel import Session, select

from app.models import TelemetryReading
from app.schemas import TelemetryIn

MAX_FUTURE_SKEW = timedelta(minutes=5)
MAX_AGE = timedelta(days=30)
# Por encima de este hueco entre paquetes no se inventa consumo integrando la potencia.
MAX_INTEGRATION_GAP = timedelta(minutes=5)

WATT_SECONDS_PER_KWH = 3_600_000.0
SECONDS_PER_MINUTE = 60.0


class TelemetryRejected(ValueError):
    """Paquete bien formado pero no aceptable (p. ej. timestamp fuera de rango)."""


def to_naive_utc(at: datetime) -> datetime:
    return at.astimezone(UTC).replace(tzinfo=None)


def to_aware_utc(at: datetime) -> datetime:
    return at.replace(tzinfo=UTC)


# ---------------------------------------------------------------- Ingesta


def ingest(session: Session, packet: TelemetryIn, received_at: datetime) -> tuple[TelemetryReading, bool]:
    """Guarda un paquete. Devuelve (lectura, duplicado). Reenviar el mismo paquete es idempotente."""
    sample_at = packet.timestamp or received_at
    if sample_at > received_at + MAX_FUTURE_SKEW:
        raise TelemetryRejected("timestamp está en el futuro (revisa el reloj/NTP del dispositivo)")
    if sample_at < received_at - MAX_AGE:
        raise TelemetryRejected(f"timestamp demasiado antiguo (máximo {MAX_AGE.days} días)")

    ts = to_naive_utc(sample_at)
    existing = _find(session, packet.device_id, ts)
    if existing is not None:
        return existing, True

    reading = TelemetryReading(device_id=packet.device_id, timestamp=ts, received_at=to_naive_utc(received_at))
    if packet.electricity is not None:
        e = packet.electricity
        reading.voltage, reading.current, reading.power, reading.energy_kwh = e.voltage, e.current, e.power, e.energy_kwh
        reading.energy_delta_kwh = _increment(
            session, packet.device_id, ts, TelemetryReading.power, TelemetryReading.energy_kwh,
            e.energy_kwh, WATT_SECONDS_PER_KWH,
        )
    if packet.water is not None:
        w = packet.water
        reading.flow_liters_per_minute, reading.total_liters = w.flow_liters_per_minute, w.total_liters
        reading.liters_delta = _increment(
            session, packet.device_id, ts, TelemetryReading.flow_liters_per_minute, TelemetryReading.total_liters,
            w.total_liters, SECONDS_PER_MINUTE,
        )

    session.add(reading)
    try:
        session.commit()
    except IntegrityError:  # el mismo paquete llegó dos veces a la vez
        session.rollback()
        return _find(session, packet.device_id, ts), True
    session.refresh(reading)
    return reading, False


def _find(session: Session, device_id: str, ts: datetime) -> TelemetryReading | None:
    query = select(TelemetryReading).where(TelemetryReading.device_id == device_id, TelemetryReading.timestamp == ts)
    return session.exec(query).first()


def _increment(
    session: Session,
    device_id: str,
    ts: datetime,
    rate: InstrumentedAttribute,
    counter: InstrumentedAttribute,
    counter_value: float | None,
    rate_seconds_per_unit: float,
) -> float:
    same_device = (TelemetryReading.device_id == device_id, rate.is_not(None))
    newer = select(TelemetryReading.id).where(*same_device, TelemetryReading.timestamp > ts).limit(1)
    if session.exec(newer).first() is not None:
        return 0.0  # llegó fuera de orden: ese intervalo ya lo cubre la lectura siguiente
    previous = session.exec(
        select(TelemetryReading).where(*same_device, TelemetryReading.timestamp < ts)
        .order_by(TelemetryReading.timestamp.desc()).limit(1)
    ).first()
    if previous is None:
        return 0.0

    previous_counter = getattr(previous, counter.key)
    if counter_value is not None and previous_counter is not None:
        # Si el contador bajó, el dispositivo se reinició y empezó de nuevo desde 0.
        return counter_value - previous_counter if counter_value >= previous_counter else counter_value
    gap = ts - previous.timestamp
    if gap > MAX_INTEGRATION_GAP:
        return 0.0
    return getattr(previous, rate.key) * gap.total_seconds() / rate_seconds_per_unit


# ---------------------------------------------------------------- Lectura (DataProvider)


class TelemetryDataProvider:
    """Sirve el contrato de Android a partir de la telemetría guardada en la BD."""

    mode = "sensors"

    def __init__(self, engine: Engine, tz: tzinfo, electricity_price: float, water_price: float, timeout_seconds: int):
        self.engine = engine
        self.tz = tz
        self.electricity_price = electricity_price
        self.water_price = water_price
        self.timeout = timedelta(seconds=timeout_seconds)

    # ------------------------------------------------------------ Lecturas actuales

    def electrical_reading(self, now: datetime) -> dict:
        with Session(self.engine) as session:
            latest = self._latest(session, TelemetryReading.power)
            energy_today = self._sum(session, TelemetryReading.energy_delta_kwh, self._start_of_day(now))
        if latest is None:
            return {"timestamp": now.astimezone(UTC), "voltage": 0.0, "current": 0.0, "power": 0.0,
                    "energy_today_kwh": 0.0}
        return {
            "timestamp": to_aware_utc(latest.timestamp),
            "voltage": round(latest.voltage, 2),
            "current": round(latest.current, 3),
            "power": round(latest.power, 2),
            "energy_today_kwh": round(energy_today, 3),
        }

    def water_reading(self, now: datetime) -> dict:
        with Session(self.engine) as session:
            latest = self._latest(session, TelemetryReading.flow_liters_per_minute)
            liters_today = self._sum(session, TelemetryReading.liters_delta, self._start_of_day(now))
        return {
            "timestamp": to_aware_utc(latest.timestamp) if latest else now.astimezone(UTC),
            "flow_liters_per_minute": round(latest.flow_liters_per_minute, 2) if latest else 0.0,
            "liters_today": round(liters_today, 1),
        }

    # ------------------------------------------------------------ Historial

    def electrical_history(self, period: str, now: datetime) -> list[dict]:
        result = []
        with Session(self.engine) as session:
            for start, end in self._buckets(period, now):
                voltage, current, power = session.exec(
                    self._in_range(
                        select(func.avg(TelemetryReading.voltage), func.avg(TelemetryReading.current),
                               func.avg(TelemetryReading.power)),
                        start, end,
                    )
                ).one()
                energy = self._sum(session, TelemetryReading.energy_delta_kwh, self._start_of_day(start), end)
                result.append({
                    "timestamp": start.astimezone(UTC),
                    "voltage": round(voltage or 0.0, 2),
                    "current": round(current or 0.0, 3),
                    "power": round(power or 0.0, 2),
                    "energy_today_kwh": round(energy, 3),
                })
        return result

    def water_history(self, period: str, now: datetime) -> list[dict]:
        result = []
        with Session(self.engine) as session:
            for start, end in self._buckets(period, now):
                flow = session.exec(
                    self._in_range(select(func.avg(TelemetryReading.flow_liters_per_minute)), start, end)
                ).one()
                liters = self._sum(session, TelemetryReading.liters_delta, self._start_of_day(start), end)
                result.append({
                    "timestamp": start.astimezone(UTC),
                    "flow_liters_per_minute": round(flow or 0.0, 2),
                    "liters_today": round(liters, 1),
                })
        return result

    def history(self, period: str, now: datetime) -> list[dict]:
        result = []
        with Session(self.engine) as session:
            for start, end in self._buckets(period, now):
                kwh = self._sum(session, TelemetryReading.energy_delta_kwh, start, end)
                liters = self._sum(session, TelemetryReading.liters_delta, start, end)
                result.append({
                    "timestamp": start.astimezone(UTC),
                    "electricity_kwh": round(kwh, 3),
                    "water_liters": round(liters, 1),
                    "estimated_cost": round(kwh * self.electricity_price + liters * self.water_price, 2),
                })
        return result

    def month_usage(self, resource_type: str, now: datetime) -> float:
        start = self._day_start(now.astimezone(self.tz).date().replace(day=1))
        with Session(self.engine) as session:
            if resource_type == "electricity":
                return round(self._sum(session, TelemetryReading.energy_delta_kwh, start), 3)
            return round(self._sum(session, TelemetryReading.liters_delta, start), 1)

    # ------------------------------------------------------------ Sin fuente real todavía

    def devices(self, now: datetime) -> list[dict]:
        return []  # la detección de dispositivos (NILM) llegará en otra fase

    def alerts(self, now: datetime) -> list[dict]:
        return []

    # ------------------------------------------------------------ Estado

    def connection(self, now: datetime) -> dict:
        with Session(self.engine) as session:
            last = session.exec(
                select(TelemetryReading).order_by(TelemetryReading.received_at.desc()).limit(1)
            ).first()
        if last is None:
            return {"esp32_connected": False, "last_update": None, "device_id": None}
        received_at = to_aware_utc(last.received_at)
        return {
            "esp32_connected": now - received_at <= self.timeout,
            "last_update": received_at,
            "device_id": last.device_id,
        }

    # ------------------------------------------------------------ Utilidades

    def _latest(self, session: Session, column: InstrumentedAttribute) -> TelemetryReading | None:
        query = select(TelemetryReading).where(column.is_not(None)).order_by(TelemetryReading.timestamp.desc())
        return session.exec(query.limit(1)).first()

    @staticmethod
    def _in_range(query, start: datetime, end: datetime | None):
        query = query.where(TelemetryReading.timestamp >= to_naive_utc(start))
        return query.where(TelemetryReading.timestamp < to_naive_utc(end)) if end is not None else query

    def _sum(self, session: Session, column: InstrumentedAttribute, start: datetime, end: datetime | None = None) -> float:
        return session.exec(self._in_range(select(func.coalesce(func.sum(column), 0.0)), start, end)).one()

    def _buckets(self, period: str, now: datetime) -> list[tuple[datetime, datetime]]:
        """Mismos intervalos que el modo demo: 24 horas o 7/30 días locales."""
        if period == "day":
            current_hour = now.astimezone(UTC).replace(minute=0, second=0, microsecond=0)
            starts = [current_hour - timedelta(hours=h) for h in range(23, -1, -1)]
            return [(s, s + timedelta(hours=1)) for s in starts]
        days = 7 if period == "week" else 30
        today = now.astimezone(self.tz).date()
        dates = [today - timedelta(days=n) for n in range(days - 1, -1, -1)]
        return [(self._day_start(d), self._day_start(d + timedelta(days=1))) for d in dates]

    def _start_of_day(self, at: datetime) -> datetime:
        return self._day_start(at.astimezone(self.tz).date())

    def _day_start(self, day: date) -> datetime:
        return datetime.combine(day, time.min, tzinfo=self.tz)
