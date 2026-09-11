"""Simulación determinista de la maqueta (12 V DC). Equivale a DemoSimulator.kt en Android.

El mismo instante produce siempre los mismos valores, así que los datos son
reproducibles y coherentes entre endpoints.
"""

from dataclasses import dataclass
from datetime import UTC, date, datetime, time, timedelta, tzinfo

MASK64 = (1 << 64) - 1
SEED = 42
IDLE_WATTS = 0.3
ENERGY_STEP_SECONDS = 300
DEVICE_SLOT_SECONDS = 1200
WATER_SLOT_SECONDS = 600

DEFAULT_LIMITS = {"electricity": 5.0, "water": 7000.0}


@dataclass(frozen=True)
class DemoDevice:
    id: str
    name: str
    type: str
    watts: float


CATALOG = [
    DemoDevice("router", "Router + ESP32", "electronics", 2.5),
    DemoDevice("lights", "Luces LED sala", "lighting", 4.8),
    DemoDevice("fan", "Ventilador", "fan", 12.0),
    DemoDevice("charger", "Cargador USB", "electronics", 7.5),
    DemoDevice("pump", "Bomba de agua", "pump", 6.0),
]


def _java_hash(text: str) -> int:
    """String.hashCode() de Java, para obtener los mismos valores que la app Android."""
    h = 0
    for char in text:
        h = (31 * h + ord(char)) & 0xFFFFFFFF
    return h - (1 << 32) if h >= (1 << 31) else h


def _noise(a: int, b: int) -> float:
    """Pseudoaleatorio determinista en [0, 1) (SplitMix64)."""
    z = (a * 0x9E3779B97F4A7C15 + b + SEED) & MASK64
    z = ((z ^ (z >> 30)) * 0xBF58476D1CE4E5B9) & MASK64
    z = ((z ^ (z >> 27)) * 0x94D049BB133111EB) & MASK64
    z ^= z >> 31
    return (z >> 11) / float(1 << 53)


def _epoch(at: datetime) -> int:
    return int(at.timestamp())


class DemoSimulator:
    def __init__(self, tz: tzinfo, electricity_price: float, water_price: float):
        self.tz = tz
        self.electricity_price = electricity_price
        self.water_price = water_price

    # ------------------------------------------------------------ Electricidad

    def power(self, at: datetime) -> float:
        devices_power = sum(d.watts for d in CATALOG if self._is_on(d, at))
        jitter = 1.0 + (_noise(_epoch(at) // 60, 7) - 0.5) * 0.06
        return IDLE_WATTS + devices_power * jitter

    def voltage_for(self, power: float, at: datetime) -> float:
        return 12.2 - power * 0.004 + (_noise(_epoch(at) // 60, 11) - 0.5) * 0.04

    def energy_kwh(self, start: datetime, end: datetime) -> float:
        watt_hours = 0.0
        t = start
        while t < end:
            nxt = min(t + timedelta(seconds=ENERGY_STEP_SECONDS), end)
            watt_hours += self.power(t) * (nxt - t).total_seconds() / 3600.0
            t = nxt
        return watt_hours / 1000.0

    def electrical_reading(self, at: datetime) -> dict:
        power = self.power(at)
        voltage = self.voltage_for(power, at)
        return {
            "timestamp": at.astimezone(UTC),
            "voltage": round(voltage, 2),
            "current": round(power / voltage, 3),
            "power": round(power, 2),
            "energy_today_kwh": round(self.energy_kwh(self.start_of_day(at), at), 3),
        }

    # ------------------------------------------------------------ Agua

    def water_flow(self, at: datetime) -> float:
        slot = _epoch(at) // WATER_SLOT_SECONDS
        hour = at.astimezone(self.tz).hour
        if 6 <= hour <= 8:
            probability = 0.35
        elif 12 <= hour <= 13:
            probability = 0.25
        elif 19 <= hour <= 21:
            probability = 0.30
        elif 0 <= hour <= 4:
            probability = 0.02
        else:
            probability = 0.08
        return 0.6 + _noise(slot, 202) * 1.4 if _noise(slot, 101) < probability else 0.0

    def liters(self, start: datetime, end: datetime) -> float:
        total = 0.0
        t = start
        while t < end:
            slot_end = datetime.fromtimestamp((_epoch(t) // WATER_SLOT_SECONDS + 1) * WATER_SLOT_SECONDS, UTC)
            nxt = min(slot_end, end)
            total += self.water_flow(t) * (nxt - t).total_seconds() / 60.0
            t = nxt
        return total

    def water_reading(self, at: datetime) -> dict:
        return {
            "timestamp": at.astimezone(UTC),
            "flow_liters_per_minute": round(self.water_flow(at), 2),
            "liters_today": round(self.liters(self.start_of_day(at), at), 1),
        }

    # ------------------------------------------------------------ Dispositivos

    def _is_on(self, device: DemoDevice, at: datetime) -> bool:
        hour = at.astimezone(self.tz).hour
        n = _noise(_epoch(at) // DEVICE_SLOT_SECONDS, _java_hash(device.id))
        if device.id == "router":
            return True
        if device.id == "lights":
            return (18 <= hour <= 22 or 6 <= hour <= 7) and n < 0.9
        if device.id == "fan":
            return 12 <= hour <= 17 and n < 0.7
        if device.id == "charger":
            return (hour >= 21 or hour <= 1) and n < 0.8
        if device.id == "pump":
            return self.water_flow(at) > 0.0
        return False

    def devices(self, at: datetime) -> list[dict]:
        result = []
        for device in CATALOG:
            on = self._is_on(device, at)
            if device.id == "router":
                device_status = "active"
            elif on:
                device_status = "probably_active"
            else:
                device_status = "inactive"
            result.append(
                {
                    "id": device.id,
                    "name": device.name,
                    "type": device.type,
                    "estimated_power_watts": device.watts,
                    "status": device_status,
                    "confidence": round(0.7 + _noise(_java_hash(device.id), 303) * 0.25, 2),
                    "controllable": False,
                }
            )
        return result

    # ------------------------------------------------------------ Historial

    def history(self, period: str, now: datetime) -> list[dict]:
        if period == "day":
            current_hour = now.replace(minute=0, second=0, microsecond=0)
            starts = [current_hour - timedelta(hours=h) for h in range(23, -1, -1)]
            return [self._record(s, min(s + timedelta(hours=1), now)) for s in starts]
        days = 7 if period == "week" else 30
        today = self.today(now)
        records = []
        for days_ago in range(days - 1, -1, -1):
            day = today - timedelta(days=days_ago)
            start = self._day_start(day)
            end = min(self._day_start(day + timedelta(days=1)), now)
            records.append(self._record(start, end))
        return records

    def _record(self, start: datetime, end: datetime) -> dict:
        kwh = self.energy_kwh(start, end)
        liters = self.liters(start, end)
        return {
            "timestamp": start.astimezone(UTC),
            "electricity_kwh": round(kwh, 3),
            "water_liters": round(liters, 1),
            "estimated_cost": round(kwh * self.electricity_price + liters * self.water_price, 2),
        }

    def electrical_history(self, period: str, now: datetime) -> list[dict]:
        return [self.electrical_reading(r["timestamp"]) for r in self.history(period, now)]

    def water_history(self, period: str, now: datetime) -> list[dict]:
        return [self.water_reading(r["timestamp"]) for r in self.history(period, now)]

    # ------------------------------------------------------------ Presupuestos

    def month_usage(self, resource_type: str, now: datetime) -> float:
        start = self._day_start(self.today(now).replace(day=1))
        if resource_type == "electricity":
            return round(self.energy_kwh(start, now), 3)
        return round(self.liters(start, now), 1)

    # ------------------------------------------------------------ Alertas

    def alerts(self, now: datetime) -> list[dict]:
        hour = now.replace(minute=0, second=0, microsecond=0)

        def alert(alert_id, alert_type, severity, title, message, hours_ago):
            return {
                "id": alert_id,
                "type": alert_type,
                "severity": severity,
                "title": title,
                "message": message,
                "timestamp": (hour - timedelta(hours=hours_ago)).astimezone(UTC),
            }

        return [
            alert("alert-high-power", "high_consumption", "warning", "Consumo elevado",
                  "La potencia superó 25 W durante más de 10 minutos.", 1),
            alert("alert-water-night", "water_leak", "critical", "Posible fuga de agua",
                  "Se detectó flujo de agua continuo durante la madrugada.", 5),
            alert("alert-budget-electricity", "budget", "warning", "Presupuesto eléctrico",
                  "Al ritmo actual superarás el límite mensual de electricidad.", 26),
            alert("alert-device-new", "device", "info", "Nuevo dispositivo detectado",
                  "Se detectó un patrón compatible con un cargador USB.", 50),
            alert("alert-system-reconnected", "system", "info", "ESP32 reconectado",
                  "El nodo de sensores volvió a enviar datos.", 74),
        ]

    # ------------------------------------------------------------ Utilidades

    def today(self, now: datetime) -> date:
        return now.astimezone(self.tz).date()

    def start_of_day(self, at: datetime) -> datetime:
        return self._day_start(self.today(at))

    def _day_start(self, day: date) -> datetime:
        return datetime.combine(day, time.min, tzinfo=self.tz)
