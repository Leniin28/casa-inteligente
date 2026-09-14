"""Punto de entrada: `uvicorn app.main:app --reload --host 0.0.0.0 --port 8000`."""

from collections.abc import Callable
from contextlib import asynccontextmanager
from datetime import UTC, datetime

import math

from fastapi import FastAPI, Request
from fastapi.encoders import jsonable_encoder
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from sqlalchemy import Engine
from sqlmodel import Session, select

from app import __version__
from app.config import Settings, get_settings
from app.database import create_db_engine, init_db
from app.models import User
from app.routers import assistant, auth, data, system, telemetry
from app.security import hash_password
from app.services.providers import build_provider

DESCRIPTION = """
API local de la Casa Inteligente. La consume la app Android.

* Modos (`SMARTHOME_DATA_MODE`): **demo** (simulado) o **sensors** (telemetría del ESP32).
* Autenticación: `POST /api/auth/login` devuelve un token; úsalo en **Authorize** como Bearer.
* El ESP32 envía telemetría a `POST /api/telemetry` con la cabecera `X-Device-Key`.
* Usuario demo: `demo@casa.local` / `demo1234`.
"""


def seed_demo_user(engine: Engine, settings: Settings) -> None:
    if not settings.seed_demo_user:
        return
    email = settings.demo_user_email.lower()
    with Session(engine) as session:
        if session.exec(select(User).where(User.email == email)).first() is None:
            session.add(
                User(
                    name=settings.demo_user_name,
                    email=email,
                    password_hash=hash_password(settings.demo_user_password, settings.password_hash_iterations),
                )
            )
            session.commit()


def _json_safe(value):
    """NaN/Infinity no son JSON válido: se devuelven como texto al repetir la entrada en un 422."""
    if isinstance(value, float) and not math.isfinite(value):
        return str(value)
    if isinstance(value, dict):
        return {k: _json_safe(v) for k, v in value.items()}
    if isinstance(value, list):
        return [_json_safe(v) for v in value]
    return value


async def validation_error_handler(_: Request, exc: RequestValidationError) -> JSONResponse:
    return JSONResponse(status_code=422, content={"detail": _json_safe(jsonable_encoder(exc.errors()))})


def create_app(settings: Settings | None = None, clock: Callable[[], datetime] | None = None) -> FastAPI:
    settings = settings or get_settings()
    engine = create_db_engine(settings.database_url)

    @asynccontextmanager
    async def lifespan(_: FastAPI):
        init_db(engine)
        seed_demo_user(engine, settings)
        yield
        engine.dispose()

    app = FastAPI(title=settings.app_name, version=__version__, description=DESCRIPTION, lifespan=lifespan)
    app.state.settings = settings
    app.state.engine = engine
    app.state.clock = clock or (lambda: datetime.now(UTC))
    app.state.provider = build_provider(settings, engine)
    app.add_exception_handler(RequestValidationError, validation_error_handler)

    app.include_router(system.router)
    app.include_router(auth.router)
    app.include_router(data.router)
    app.include_router(telemetry.router)
    app.include_router(assistant.router)
    return app


app = create_app()
