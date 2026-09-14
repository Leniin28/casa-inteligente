"""Modelos de entrada/salida de la API (contrato con Android, ver docs/API.md).

JSON en snake_case. Fechas ISO-8601 en UTC. Enums en minúsculas.
"""

from datetime import UTC, date, datetime
from typing import Literal

from pydantic import AliasChoices, BaseModel, ConfigDict, Field, field_validator, model_validator

ResourceType = Literal["electricity", "water"]
Period = Literal["day", "week", "month"]

EMAIL_PATTERN = r"^[^@\s]+@[^@\s]+\.[^@\s]+$"


class HealthOut(BaseModel):
    status: Literal["ok"]
    app: str
    version: str
    time: datetime
    data_mode: str
    database: str


# ---------------------------------------------------------------- Auth


class UserOut(BaseModel):
    id: str
    name: str
    email: str


class LoginIn(BaseModel):
    email: str
    password: str


class RegisterIn(BaseModel):
    name: str = Field(min_length=1, max_length=80)
    email: str = Field(pattern=EMAIL_PATTERN, max_length=254)
    password: str = Field(min_length=6, max_length=128)


class PasswordResetIn(BaseModel):
    email: str


class AuthOut(BaseModel):
    token: str
    user: UserOut


class MessageOut(BaseModel):
    message: str


# ---------------------------------------------------------------- Lecturas


class ElectricalReadingOut(BaseModel):
    timestamp: datetime
    voltage: float
    current: float
    power: float
    energy_today_kwh: float


class WaterReadingOut(BaseModel):
    timestamp: datetime
    flow_liters_per_minute: float
    liters_today: float


class DeviceOut(BaseModel):
    id: str
    name: str
    type: str
    estimated_power_watts: float
    status: Literal["active", "probably_active", "inactive", "unknown"]
    confidence: float | None = None
    controllable: bool = False


class AlertOut(BaseModel):
    id: str
    type: str
    severity: Literal["info", "warning", "critical"]
    title: str
    message: str
    timestamp: datetime
    read: bool


class BudgetOut(BaseModel):
    id: str
    resource_type: ResourceType
    limit: float
    current_usage: float
    estimated_final_usage: float
    estimated_limit_date: date | None = None
    period_start: date
    period_end: date


class BudgetIn(BaseModel):
    resource_type: ResourceType
    limit: float = Field(gt=0)


class HistoryRecordOut(BaseModel):
    timestamp: datetime
    electricity_kwh: float
    water_liters: float
    estimated_cost: float | None = None


class SystemStatusOut(BaseModel):
    backend_connected: bool
    esp32_connected: bool
    last_update: datetime | None
    data_source: str
    device_id: str | None = None  # último dispositivo que envió telemetría (modo sensors)


class DashboardOut(BaseModel):
    electricity: ElectricalReadingOut
    water: WaterReadingOut
    active_devices: int
    unread_alerts: int
    budgets: list[BudgetOut]
    system_status: SystemStatusOut


# ---------------------------------------------------------------- Telemetría (ESP32 → backend)
#
# Contrato normalizado, independiente del sensor físico (ver docs/TELEMETRY-CONTRACT.md).
# Se aceptan claves en snake_case (estilo de la API) y camelCase. Los rangos solo descartan
# valores físicamente absurdos; no son reglas de la maqueta.

DEVICE_ID_PATTERN = r"^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$"


class _TelemetryBlock(BaseModel):
    model_config = ConfigDict(extra="forbid", allow_inf_nan=False)


class ElectricityTelemetryIn(_TelemetryBlock):
    voltage: float = Field(strict=True, ge=0, le=1_000, description="Tensión (V)")
    current: float = Field(strict=True, ge=0, le=500, description="Corriente (A)")
    power: float = Field(strict=True, ge=0, le=100_000, description="Potencia activa (W)")
    energy_kwh: float | None = Field(
        default=None,
        strict=True,
        ge=0,
        le=10_000_000,
        validation_alias=AliasChoices("energy_kwh", "energyKwh", "energy"),
        description="Contador acumulado de energía del dispositivo (kWh). Puede reiniciarse a 0.",
    )


class WaterTelemetryIn(_TelemetryBlock):
    flow_liters_per_minute: float = Field(
        strict=True,
        ge=0,
        le=1_000,
        validation_alias=AliasChoices("flow_liters_per_minute", "flowLitersPerMinute"),
        description="Caudal instantáneo (L/min)",
    )
    total_liters: float | None = Field(
        default=None,
        strict=True,
        ge=0,
        le=1_000_000_000,
        validation_alias=AliasChoices("total_liters", "totalLiters"),
        description="Contador acumulado de litros del dispositivo. Puede reiniciarse a 0.",
    )


class TelemetryIn(_TelemetryBlock):
    device_id: str = Field(pattern=DEVICE_ID_PATTERN, validation_alias=AliasChoices("device_id", "deviceId"))
    timestamp: datetime | None = Field(
        default=None, description="Instante de la medida con zona horaria. Si falta, se usa la hora de recepción."
    )
    electricity: ElectricityTelemetryIn | None = None
    water: WaterTelemetryIn | None = None

    @field_validator("timestamp")
    @classmethod
    def _timestamp_with_zone(cls, value: datetime | None) -> datetime | None:
        if value is None:
            return None
        if value.utcoffset() is None:
            raise ValueError("timestamp debe incluir zona horaria (p. ej. '2026-09-11T18:30:00Z')")
        return value.astimezone(UTC)

    @model_validator(mode="after")
    def _not_empty(self) -> "TelemetryIn":
        if self.electricity is None and self.water is None:
            raise ValueError("el paquete debe incluir al menos 'electricity' o 'water'")
        return self


class TelemetryAckOut(BaseModel):
    id: int
    device_id: str
    timestamp: datetime
    received_at: datetime
    duplicate: bool = False


# ---------------------------------------------------------------- Asistente


class ChatMessageIn(BaseModel):
    role: Literal["user", "assistant"]
    text: str


class ChatIn(BaseModel):
    message: str = Field(min_length=1, max_length=2000)
    history: list[ChatMessageIn] = Field(default_factory=list)


class AssistantMessageOut(BaseModel):
    id: str
    role: Literal["user", "assistant"]
    text: str
    timestamp: datetime


class ChatOut(BaseModel):
    reply: AssistantMessageOut
