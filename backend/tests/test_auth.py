from sqlmodel import Session, select

from app.models import User
from tests.conftest import DEMO_CREDENTIALS


def test_demo_user_can_log_in(client):
    response = client.post("/api/auth/login", json=DEMO_CREDENTIALS)

    assert response.status_code == 200
    body = response.json()
    assert body["token"]
    assert body["user"]["email"] == "demo@casa.local"


def test_wrong_password_is_rejected(client):
    response = client.post("/api/auth/login", json={"email": "demo@casa.local", "password": "incorrecta"})

    assert response.status_code == 401


def test_register_me_and_duplicate(client):
    new_user = {"name": "Ana", "email": "Ana@Uni.edu", "password": "secreta1"}

    registered = client.post("/api/auth/register", json=new_user)
    assert registered.status_code == 201
    token = registered.json()["token"]

    me = client.get("/api/auth/me", headers={"Authorization": f"Bearer {token}"})
    assert me.json() == {"id": registered.json()["user"]["id"], "name": "Ana", "email": "ana@uni.edu"}

    assert client.post("/api/auth/register", json=new_user).status_code == 409


def test_register_validates_input(client):
    response = client.post("/api/auth/register", json={"name": "X", "email": "no-email", "password": "123"})

    assert response.status_code == 422


def test_passwords_are_stored_hashed(client):
    engine = client.app.state.engine
    with Session(engine) as session:
        user = session.exec(select(User).where(User.email == "demo@casa.local")).one()

    assert "demo1234" not in user.password_hash
    assert user.password_hash.startswith("pbkdf2_sha256$")


def test_logout_invalidates_the_token(client, auth_headers):
    assert client.post("/api/auth/logout", headers=auth_headers).status_code == 204

    assert client.get("/api/auth/me", headers=auth_headers).status_code == 401


def test_data_endpoints_require_a_session(client):
    assert client.get("/api/dashboard").status_code == 401
    assert client.get("/api/dashboard", headers={"Authorization": "Bearer inventado"}).status_code == 401


def test_password_reset_does_not_reveal_accounts(client):
    known = client.post("/api/auth/password-reset", json={"email": "demo@casa.local"})
    unknown = client.post("/api/auth/password-reset", json={"email": "nadie@casa.local"})

    assert known.status_code == unknown.status_code == 202
    assert known.json() == unknown.json()
