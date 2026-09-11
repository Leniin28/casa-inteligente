"""Tablas de la base de datos (SQLModel sobre SQLite).

Las fechas se guardan como UTC sin zona horaria (SQLite no guarda tzinfo).
"""

from datetime import UTC, datetime

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
