"""Dependencias comunes de FastAPI (sesión de BD, reloj, proveedor de datos, usuario actual)."""

from collections.abc import Callable, Iterator
from datetime import datetime
from typing import Annotated

from fastapi import Depends, HTTPException, Request, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlmodel import Session

from app.config import Settings
from app.models import SessionToken, User, utcnow_naive
from app.security import hash_token
from app.services.providers import DataProvider

bearer_scheme = HTTPBearer(auto_error=False, description="Token devuelto por /api/auth/login")


def get_settings(request: Request) -> Settings:
    return request.app.state.settings


def get_session(request: Request) -> Iterator[Session]:
    with Session(request.app.state.engine) as session:
        yield session


def get_now(request: Request) -> datetime:
    """Hora actual (con zona). En los tests se sustituye por una hora fija."""
    clock: Callable[[], datetime] = request.app.state.clock
    return clock()


def get_provider(request: Request) -> DataProvider:
    return request.app.state.provider


SessionDep = Annotated[Session, Depends(get_session)]
SettingsDep = Annotated[Settings, Depends(get_settings)]
NowDep = Annotated[datetime, Depends(get_now)]
ProviderDep = Annotated[DataProvider, Depends(get_provider)]


def get_current_user(
    session: SessionDep,
    credentials: Annotated[HTTPAuthorizationCredentials | None, Depends(bearer_scheme)],
) -> User:
    unauthorized = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Sesión no válida",
        headers={"WWW-Authenticate": "Bearer"},
    )
    if credentials is None:
        raise unauthorized
    token = session.get(SessionToken, hash_token(credentials.credentials))
    if token is None or token.expires_at < utcnow_naive():
        raise unauthorized
    user = session.get(User, token.user_id)
    if user is None:
        raise unauthorized
    return user


CurrentUser = Annotated[User, Depends(get_current_user)]
