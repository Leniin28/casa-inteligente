"""Configuración leída de variables de entorno / fichero .env (prefijo SMARTHOME_)."""

from datetime import datetime, timezone, tzinfo
from functools import lru_cache
from typing import Literal
from zoneinfo import ZoneInfo

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_prefix="SMARTHOME_", extra="ignore")

    app_name: str = "Casa Inteligente API"
    database_url: str = "sqlite:///./smarthome.db"

    # "demo" = datos simulados. En el futuro: "sensors" (lecturas reales enviadas por el ESP32).
    data_mode: Literal["demo"] = "demo"

    # Zona horaria para calcular "hoy" y los cortes diarios. Vacío = zona del sistema.
    timezone: str | None = None

    session_ttl_hours: int = 72
    password_hash_iterations: int = 600_000

    # Usuario de demostración creado al arrancar (no es un secreto: está documentado).
    seed_demo_user: bool = True
    demo_user_name: str = "Usuario demo"
    demo_user_email: str = "demo@casa.local"
    demo_user_password: str = "demo1234"

    electricity_price_per_kwh: float = 0.15
    water_price_per_liter: float = 0.002

    def tz(self) -> tzinfo:
        if not self.timezone:
            return datetime.now().astimezone().tzinfo or timezone.utc
        if self.timezone.upper() == "UTC":
            return timezone.utc
        return ZoneInfo(self.timezone)


@lru_cache
def get_settings() -> Settings:
    return Settings()
