import pytest


def test_dashboard_has_all_sections(client, auth_headers):
    body = client.get("/api/dashboard", headers=auth_headers).json()

    assert set(body) == {"electricity", "water", "active_devices", "unread_alerts", "budgets", "system_status"}
    assert len(body["budgets"]) == 2
    assert body["system_status"]["data_source"] == "demo"


def test_electricity_reading_is_coherent_and_reproducible(client, auth_headers):
    first = client.get("/api/electricity/current", headers=auth_headers).json()
    second = client.get("/api/electricity/current", headers=auth_headers).json()

    assert first == second
    assert 11.5 <= first["voltage"] <= 12.5
    assert first["power"] > 0
    assert first["current"] == pytest.approx(first["power"] / first["voltage"], abs=0.01)
    assert first["timestamp"].endswith("Z")


def test_water_reading(client, auth_headers):
    body = client.get("/api/water/current", headers=auth_headers).json()

    assert body["flow_liters_per_minute"] >= 0
    assert body["liters_today"] >= 0


@pytest.mark.parametrize(("period", "size"), [("day", 24), ("week", 7), ("month", 30)])
def test_history_sizes(client, auth_headers, period, size):
    response = client.get("/api/history", params={"period": period}, headers=auth_headers)

    assert response.status_code == 200
    assert len(response.json()) == size


def test_history_rejects_unknown_period(client, auth_headers):
    assert client.get("/api/history", params={"period": "year"}, headers=auth_headers).status_code == 422


def test_reading_histories(client, auth_headers):
    assert len(client.get("/api/electricity/history?period=day", headers=auth_headers).json()) == 24
    assert len(client.get("/api/water/history?period=week", headers=auth_headers).json()) == 7


def test_devices(client, auth_headers):
    devices = client.get("/api/devices", headers=auth_headers).json()

    assert len(devices) == 5
    assert all(0 <= d["confidence"] <= 1 for d in devices)
    assert all(d["controllable"] is False for d in devices)
    assert client.get("/api/devices/fan", headers=auth_headers).json()["name"] == "Ventilador"
    assert client.get("/api/devices/nope", headers=auth_headers).status_code == 404


def test_mark_alert_as_read_persists(client, auth_headers):
    alerts = client.get("/api/alerts", headers=auth_headers).json()
    unread = next(a for a in alerts if not a["read"])

    marked = client.patch(f"/api/alerts/{unread['id']}/read", headers=auth_headers)
    assert marked.status_code == 200
    assert marked.json()["read"] is True

    again = client.get("/api/alerts", headers=auth_headers).json()
    assert next(a for a in again if a["id"] == unread["id"])["read"] is True
    assert client.patch("/api/alerts/nope/read", headers=auth_headers).status_code == 404


def test_assistant_chat(client, auth_headers):
    response = client.post(
        "/api/assistant/chat",
        json={"message": "¿Cuánta electricidad consumo?", "history": [{"role": "user", "text": "hola"}]},
        headers=auth_headers,
    )

    assert response.status_code == 200
    reply = response.json()["reply"]
    assert reply["role"] == "assistant"
    assert " W" in reply["text"]


def test_system_status_is_public(client):
    body = client.get("/api/system/status").json()

    assert body["backend_connected"] is True
    assert body["data_source"] == "demo"
