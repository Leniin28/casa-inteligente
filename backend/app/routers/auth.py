from datetime import timedelta

from fastapi import APIRouter, Depends, HTTPException, Response, status
from fastapi.security import HTTPAuthorizationCredentials
from sqlmodel import Session, select

from app.config import Settings
from app.deps import CurrentUser, SessionDep, SettingsDep, bearer_scheme
from app.models import SessionToken, User, utcnow_naive
from app.schemas import AuthOut, LoginIn, MessageOut, PasswordResetIn, RegisterIn, UserOut
from app.security import hash_password, hash_token, new_session_token, verify_password

router = APIRouter(prefix="/api/auth", tags=["auth"])


def user_out(user: User) -> UserOut:
    return UserOut(id=str(user.id), name=user.name, email=user.email)


def _create_session(session: Session, user: User, settings: Settings) -> AuthOut:
    token = new_session_token()
    session.add(
        SessionToken(
            token_hash=hash_token(token),
            user_id=user.id,
            expires_at=utcnow_naive() + timedelta(hours=settings.session_ttl_hours),
        )
    )
    session.commit()
    return AuthOut(token=token, user=user_out(user))


@router.post("/register", response_model=AuthOut, status_code=status.HTTP_201_CREATED)
def register(body: RegisterIn, session: SessionDep, settings: SettingsDep) -> AuthOut:
    email = body.email.strip().lower()
    if session.exec(select(User).where(User.email == email)).first():
        raise HTTPException(status.HTTP_409_CONFLICT, "Ya existe una cuenta con ese email")
    user = User(
        name=body.name.strip(),
        email=email,
        password_hash=hash_password(body.password, settings.password_hash_iterations),
    )
    session.add(user)
    session.commit()
    session.refresh(user)
    return _create_session(session, user, settings)


@router.post("/login", response_model=AuthOut)
def login(body: LoginIn, session: SessionDep, settings: SettingsDep) -> AuthOut:
    user = session.exec(select(User).where(User.email == body.email.strip().lower())).first()
    if user is None or not verify_password(body.password, user.password_hash):
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Email o contraseña incorrectos")
    return _create_session(session, user, settings)


@router.get("/me", response_model=UserOut)
def me(user: CurrentUser) -> UserOut:
    return user_out(user)


@router.post("/logout", status_code=status.HTTP_204_NO_CONTENT)
def logout(
    user: CurrentUser,
    session: SessionDep,
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer_scheme),
) -> Response:
    token = session.get(SessionToken, hash_token(credentials.credentials)) if credentials else None
    if token is not None:
        session.delete(token)
        session.commit()
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@router.post("/password-reset", response_model=MessageOut, status_code=status.HTTP_202_ACCEPTED)
def password_reset(body: PasswordResetIn) -> MessageOut:
    # Aún no se envían emails. La respuesta es siempre la misma para no revelar
    # qué emails están registrados.
    return MessageOut(message="Si el email está registrado, recibirás instrucciones.")
