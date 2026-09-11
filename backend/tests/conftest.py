from datetime import UTC, datetime

import pytest
from fastapi.testclient import TestClient

from app.config import Settings
from app.main import create_app

FIXED_NOW = datetime(2026, 9, 11, 18, 30, tzinfo=UTC)
DEMO_CREDENTIALS = {"email": "demo@casa.local", "password": "demo1234"}


@pytest.fixture
def settings(tmp_path) -> Settings:
    return Settings(
        _env_file=None,
        database_url=f"sqlite:///{tmp_path / 'test.db'}",
        timezone="UTC",
        password_hash_iterations=1_000,  # rápido en tests; en producción 600k
    )


@pytest.fixture
def client(settings):
    app = create_app(settings, clock=lambda: FIXED_NOW)
    with TestClient(app) as test_client:
        yield test_client


@pytest.fixture
def auth_headers(client) -> dict[str, str]:
    response = client.post("/api/auth/login", json=DEMO_CREDENTIALS)
    assert response.status_code == 200
    return {"Authorization": f"Bearer {response.json()['token']}"}
