"""Proveedores de datos: la capa que adapta el hardware al contrato de la API.

Android nunca ve sensores concretos (INA226, PZEM, YF-S401...). SMARTHOME_DATA_MODE elige:

* `demo`: `DemoDataProvider`, datos simulados deterministas (sin ESP32).
* `sensors`: `TelemetryDataProvider`, telemetría normalizada que el ESP32 envía a
  POST /api/telemetry y se guarda en la BD.

Los routers y la app Android no cambian entre modos.
"""

from datetime import UTC, datetime
from typing import Protocol

from sqlalchemy import Engine

from app.config import Settings
from app.services.simulator import DemoSimulator
from app.services.telemetry import TelemetryDataProvider


class DataProvider(Protocol):
    mode: str

    def electrical_reading(self, now: datetime) -> dict: ...
    def electrical_history(self, period: str, now: datetime) -> list[dict]: ...
    def water_reading(self, now: datetime) -> dict: ...
    def water_history(self, period: str, now: datetime) -> list[dict]: ...
    def devices(self, now: datetime) -> list[dict]: ...
    def alerts(self, now: datetime) -> list[dict]: ...
    def history(self, period: str, now: datetime) -> list[dict]: ...
    def month_usage(self, resource_type: str, now: datetime) -> float: ...
    # {"esp32_connected": bool, "last_update": datetime | None, "device_id": str | None}
    def connection(self, now: datetime) -> dict: ...


class DemoDataProvider:
    """Datos simulados coherentes para trabajar sin ESP32."""

    mode = "demo"

    def __init__(self, simulator: DemoSimulator):
        self.sim = simulator

    def electrical_reading(self, now: datetime) -> dict:
        return self.sim.electrical_reading(now)

    def electrical_history(self, period: str, now: datetime) -> list[dict]:
        return self.sim.electrical_history(period, now)

    def water_reading(self, now: datetime) -> dict:
        return self.sim.water_reading(now)

    def water_history(self, period: str, now: datetime) -> list[dict]:
        return self.sim.water_history(period, now)

    def devices(self, now: datetime) -> list[dict]:
        return self.sim.devices(now)

    def alerts(self, now: datetime) -> list[dict]:
        return self.sim.alerts(now)

    def history(self, period: str, now: datetime) -> list[dict]:
        return self.sim.history(period, now)

    def month_usage(self, resource_type: str, now: datetime) -> float:
        return self.sim.month_usage(resource_type, now)

    def connection(self, now: datetime) -> dict:
        return {"esp32_connected": True, "last_update": now.astimezone(UTC), "device_id": None}  # simulado


def build_provider(settings: Settings, engine: Engine) -> DataProvider:
    if settings.data_mode == "sensors":
        return TelemetryDataProvider(
            engine,
            tz=settings.tz(),
            electricity_price=settings.electricity_price_per_kwh,
            water_price=settings.water_price_per_liter,
            timeout_seconds=settings.esp32_timeout_seconds,
        )
    simulator = DemoSimulator(
        tz=settings.tz(),
        electricity_price=settings.electricity_price_per_kwh,
        water_price=settings.water_price_per_liter,
    )
    return DemoDataProvider(simulator)
