"""Punto de entrada: `uvicorn app.main:app --reload --host 0.0.0.0 --port 8000`."""

from collections.abc import Callable
from contextlib import asynccontextmanager
from datetime import UTC, datetime

from fastapi import FastAPI
from sqlalchemy import Engine
from sqlmodel import Session, select

from app import __version__
from app.config import Settings, get_settings
from app.database import create_db_engine, init_db
from app.models import User
from app.routers import assistant, auth, data, system
from app.security import hash_password
from app.services.providers import build_provider

DESCRIPTION = """
API local de la Casa Inteligente. La consume la app Android.

* Modo actual: datos **demo** simulados (sin ESP32).
* Autenticación: `POST /api/auth/login` devuelve un token; úsalo en **Authorize** como Bearer.
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
    app.state.provider = build_provider(settings)

    app.include_router(system.router)
    app.include_router(auth.router)
    app.include_router(data.router)
    app.include_router(assistant.router)
    return app


app = create_app()
