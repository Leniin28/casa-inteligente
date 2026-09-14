from datetime import UTC, datetime

from fastapi import APIRouter
from sqlalchemy import text

from app import __version__
from app.deps import NowDep, ProviderDep, SessionDep, SettingsDep
from app.schemas import HealthOut, SystemStatusOut
from app.services.providers import DataProvider

router = APIRouter()


def build_status(provider: DataProvider, now: datetime) -> SystemStatusOut:
    return SystemStatusOut(backend_connected=True, data_source=provider.mode, **provider.connection(now))


@router.get("/health", response_model=HealthOut, tags=["system"])
def health(session: SessionDep, settings: SettingsDep, provider: ProviderDep, now: NowDep) -> HealthOut:
    """Comprueba que el backend y la base de datos responden. No requiere sesión."""
    session.exec(text("SELECT 1"))
    return HealthOut(
        status="ok",
        app=settings.app_name,
        version=__version__,
        time=now.astimezone(UTC),
        data_mode=provider.mode,
        database="ok",
    )


@router.get("/api/system/status", response_model=SystemStatusOut, tags=["system"])
def system_status(provider: ProviderDep, now: NowDep) -> SystemStatusOut:
    """Estado de las conexiones. Público para poder diagnosticar sin iniciar sesión."""
    return build_status(provider, now)
