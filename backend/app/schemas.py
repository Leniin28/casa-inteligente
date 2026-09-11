"""Modelos de entrada/salida de la API (contrato con Android, ver docs/API.md).

JSON en snake_case. Fechas ISO-8601 en UTC. Enums en minúsculas.
"""

from datetime import date, datetime
from typing import Literal

from pydantic import BaseModel, Field

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


class DashboardOut(BaseModel):
    electricity: ElectricalReadingOut
    water: WaterReadingOut
    active_devices: int
    unread_alerts: int
    budgets: list[BudgetOut]
    system_status: SystemStatusOut


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
