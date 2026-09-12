"""Configuración leída de variables de entorno / fichero .env (prefijo SMARTHOME_)."""

from datetime import datetime, timezone, tzinfo
from functools import lru_cache
from typing import Literal
from zoneinfo import ZoneInfo

from pydantic import Field, SecretStr, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict

MIN_DEVICE_KEY_LENGTH = 16


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_prefix="SMARTHOME_", extra="ignore")

    app_name: str = "Casa Inteligente API"
    database_url: str = "sqlite:///./smarthome.db"

    # "demo" = datos simulados. "sensors" = telemetría real recibida en POST /api/telemetry.
    data_mode: Literal["demo", "sensors"] = "demo"

    # Clave que el ESP32 envía en la cabecera X-Device-Key. Vacía = ingesta deshabilitada (503).
    device_api_key: SecretStr | None = None
    # Sin paquetes durante este tiempo, el ESP32 se considera desconectado.
    esp32_timeout_seconds: int = Field(default=60, gt=0)

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

    @field_validator("device_api_key", mode="before")
    @classmethod
    def _validate_device_key(cls, value: object) -> object:
        raw = value.get_secret_value() if isinstance(value, SecretStr) else value
        if raw is None or (isinstance(raw, str) and not raw.strip()):
            return None
        if isinstance(raw, str) and len(raw.strip()) < MIN_DEVICE_KEY_LENGTH:
            raise ValueError(f"SMARTHOME_DEVICE_API_KEY debe tener al menos {MIN_DEVICE_KEY_LENGTH} caracteres")
        return raw.strip() if isinstance(raw, str) else raw

    def tz(self) -> tzinfo:
        if not self.timezone:
            return datetime.now().astimezone().tzinfo or timezone.utc
        if self.timezone.upper() == "UTC":
            return timezone.utc
        return ZoneInfo(self.timezone)


@lru_cache
def get_settings() -> Settings:
    return Settings()
