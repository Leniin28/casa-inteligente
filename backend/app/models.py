"""Tablas de la base de datos (SQLModel sobre SQLite).

Las fechas se guardan como UTC sin zona horaria (SQLite no guarda tzinfo).
"""

from datetime import UTC, datetime

from sqlalchemy import UniqueConstraint
from sqlmodel import Field, SQLModel


def utcnow_naive() -> datetime:
    return datetime.now(UTC).replace(tzinfo=None)


class User(SQLModel, table=True):
    id: int | None = Field(default=None, primary_key=True)
    name: str
    email: str = Field(index=True, unique=True)
    # Formato "pbkdf2_sha256$<iteraciones>$<salt>$<hash>". Nunca la contraseña en claro.
    password_hash: str
    created_at: datetime = Field(default_factory=utcnow_naive)


class SessionToken(SQLModel, table=True):
    __tablename__ = "session_tokens"

    # Se guarda el SHA-256 del token, no el token: si se filtra la BD no sirve para entrar.
    token_hash: str = Field(primary_key=True)
    user_id: int = Field(foreign_key="user.id", index=True)
    created_at: datetime = Field(default_factory=utcnow_naive)
    expires_at: datetime


class BudgetLimit(SQLModel, table=True):
    __tablename__ = "budget_limits"

    resource_type: str = Field(primary_key=True)  # "electricity" | "water"
    limit: float


class AlertReadState(SQLModel, table=True):
    __tablename__ = "alert_read_state"

    alert_id: str = Field(primary_key=True)
    read_at: datetime = Field(default_factory=utcnow_naive)


class TelemetryReading(SQLModel, table=True):
    """Un paquete de telemetría normalizada (POST /api/telemetry). Sin nombres de sensores físicos.

    Las columnas de un bloque ausente quedan a NULL. Los `*_delta` los calcula el backend al
    ingerir (incremento desde la lectura anterior del mismo dispositivo) para que los totales
    diarios, el historial y los presupuestos sean simples SUM.
    """

    __tablename__ = "telemetry_readings"
    __table_args__ = (UniqueConstraint("device_id", "timestamp"),)

    id: int | None = Field(default=None, primary_key=True)
    device_id: str = Field(index=True)
    timestamp: datetime = Field(index=True)  # instante de la medida
    received_at: datetime = Field(index=True)  # instante en que llegó al backend

    voltage: float | None = None  # V
    current: float | None = None  # A
    power: float | None = None  # W
    energy_kwh: float | None = None  # contador acumulado del dispositivo
    energy_delta_kwh: float | None = None

    flow_liters_per_minute: float | None = None
    total_liters: float | None = None  # contador acumulado del dispositivo
    liters_delta: float | None = None
