"""Proveedores de datos: la capa que adapta el hardware al contrato de la API.

Android nunca ve sensores concretos (INA226, PZEM, YF-S401...). Para conectar hardware
real se añade otra clase que cumpla `DataProvider` (p. ej. `SensorDataProvider`, que lea
las lecturas que el ESP32 guarde en la BD) y se selecciona con SMARTHOME_DATA_MODE.
Los routers y la app Android no cambian.
"""

from datetime import datetime
from typing import Protocol

from app.config import Settings
from app.services.simulator import DemoSimulator


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
    def esp32_connected(self, now: datetime) -> bool: ...


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

    def esp32_connected(self, now: datetime) -> bool:
        return True  # simulado


def build_provider(settings: Settings) -> DataProvider:
    simulator = DemoSimulator(
        tz=settings.tz(),
        electricity_price=settings.electricity_price_per_kwh,
        water_price=settings.water_price_per_liter,
    )
    return DemoDataProvider(simulator)
