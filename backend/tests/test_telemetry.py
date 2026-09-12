from datetime import timedelta

import pytest
from fastapi.testclient import TestClient
from pydantic import SecretStr, ValidationError
from sqlmodel import Session, select

from app.config import Settings
from app.main import create_app
from app.models import TelemetryReading
from tests.conftest import DEMO_CREDENTIALS, FIXED_NOW

DEVICE_KEY = "test-device-key-0123456789"
KEY_HEADER = {"X-Device-Key": DEVICE_KEY}


class Clock:
    def __init__(self):
        self.now = FIXED_NOW

    def __call__(self):
        return self.now


def iso(at) -> str:
    return at.isoformat().replace("+00:00", "Z")


def packet(seconds_ago: float = 0, electricity: dict | None = None, water: dict | None = None, **extra) -> dict:
    body = {"device_id": "esp32-main", "timestamp": iso(FIXED_NOW - timedelta(seconds=seconds_ago)), **extra}
    if electricity is not None:
        body["electricity"] = electricity
    if water is not None:
        body["water"] = water
    return body


ELECTRICITY = {"voltage": 12.1, "current": 1.2, "power": 14.52, "energy_kwh": 0.42}
WATER = {"flow_liters_per_minute": 1.3, "total_liters": 18.7}


@pytest.fixture
def clock():
    return Clock()


def make_client(settings, clock):
    app = create_app(settings, clock=clock)
    client = TestClient(app)
    client.__enter__()
    token = client.post("/api/auth/login", json=DEMO_CREDENTIALS).json()["token"]
    client.headers["Authorization"] = f"Bearer {token}"
    return client


@pytest.fixture
def sensors(settings, clock):
    client = make_client(
        settings.model_copy(update={"data_mode": "sensors", "device_api_key": SecretStr(DEVICE_KEY)}), clock
    )
    yield client
    client.__exit__(None, None, None)


def post(client, body, headers=KEY_HEADER):
    return client.post("/api/telemetry", json=body, headers=headers)


# ---------------------------------------------------------------- Ingesta válida


def test_full_packet_is_accepted(sensors):
    response = post(sensors, packet(electricity=ELECTRICITY, water=WATER))

    assert response.status_code == 201
    body = response.json()
    assert body["device_id"] == "esp32-main"
    assert body["timestamp"] == "2026-09-11T18:30:00Z"
    assert body["duplicate"] is False


def test_camel_case_packet_from_the_spec_is_accepted(sensors):
    body = {
        "deviceId": "esp32-main",
        "timestamp": iso(FIXED_NOW),
        "electricity": {"voltage": 12.1, "current": 1.2, "power": 14.52, "energy": 0.42},
        "water": {"flowLitersPerMinute": 1.3, "totalLiters": 18.7},
    }
    assert post(sensors, body).status_code == 201


def test_electricity_only(sensors):
    assert post(sensors, packet(electricity=ELECTRICITY)).status_code == 201

    assert sensors.get("/api/electricity/current").json()["power"] == 14.52
    water = sensors.get("/api/water/current").json()
    assert water["flow_liters_per_minute"] == 0.0
    assert water["liters_today"] == 0.0


def test_water_only(sensors):
    assert post(sensors, packet(water={"flow_liters_per_minute": 2.5})).status_code == 201

    assert sensors.get("/api/water/current").json()["flow_liters_per_minute"] == 2.5
    assert sensors.get("/api/electricity/current").json()["power"] == 0.0


def test_timestamp_is_optional_and_defaults_to_reception(sensors):
    response = post(sensors, {"device_id": "esp32-main", "water": {"flow_liters_per_minute": 1.0}})

    assert response.status_code == 201
    assert response.json()["timestamp"] == "2026-09-11T18:30:00Z"


def test_timestamp_with_offset_is_normalized_to_utc(sensors):
    body = packet(electricity=ELECTRICITY)
    body["timestamp"] = "2026-09-11T13:30:00-05:00"

    assert post(sensors, body).json()["timestamp"] == "2026-09-11T18:30:00Z"


# ---------------------------------------------------------------- Payload inválido


@pytest.mark.parametrize(
    "body",
    [
        pytest.param({}, id="empty"),
        pytest.param({"device_id": "esp32-main"}, id="no-blocks"),
        pytest.param({"device_id": "esp32-main", "electricity": None, "water": None}, id="null-blocks"),
        pytest.param(packet(electricity=ELECTRICITY) | {"device_id": ""}, id="empty-device-id"),
        pytest.param(packet(electricity=ELECTRICITY) | {"device_id": "esp 32/../x"}, id="bad-device-id"),
        pytest.param(packet(electricity=ELECTRICITY) | {"device_id": "x" * 65}, id="long-device-id"),
        pytest.param(packet(electricity=ELECTRICITY) | {"timestamp": "ayer"}, id="bad-timestamp"),
        pytest.param(packet(electricity=ELECTRICITY) | {"timestamp": "2026-09-11T18:30:00"}, id="naive-timestamp"),
        pytest.param(packet(-600, electricity=ELECTRICITY), id="future-timestamp"),
        pytest.param(packet(31 * 86400, electricity=ELECTRICITY), id="too-old-timestamp"),
        pytest.param(packet(electricity=ELECTRICITY | {"voltage": -1}), id="negative-voltage"),
        pytest.param(packet(electricity=ELECTRICITY | {"voltage": 5000}), id="absurd-voltage"),
        pytest.param(packet(electricity=ELECTRICITY | {"power": 1e9}), id="absurd-power"),
        pytest.param(packet(electricity=ELECTRICITY | {"current": "1.2"}), id="string-number"),
        pytest.param(packet(electricity={"voltage": 12.1, "power": 5}), id="missing-current"),
        pytest.param(packet(water={"flow_liters_per_minute": 5000}), id="absurd-flow"),
        pytest.param(packet(water={"flow_liters_per_minute": 1, "total_liters": -3}), id="negative-counter"),
        pytest.param(packet(electricity=ELECTRICITY | {"ina226_shunt": 0.1}), id="unknown-field"),
        pytest.param(packet(electricity=ELECTRICITY, temperature=21), id="unknown-block"),
    ],
)
def test_invalid_payloads_are_rejected(sensors, body):
    response = post(sensors, body)

    assert response.status_code == 422
    assert "detail" in response.json()


@pytest.mark.parametrize("value", ["NaN", "Infinity", "-Infinity"])
def test_non_finite_values_are_rejected(sensors, value):
    raw = f'{{"device_id": "esp32-main", "electricity": {{"voltage": 12, "current": 1, "power": {value}}}}}'
    response = sensors.post("/api/telemetry", content=raw, headers=KEY_HEADER | {"Content-Type": "application/json"})

    assert response.status_code == 422


def test_invalid_packet_is_not_stored(sensors):
    post(sensors, packet(electricity=ELECTRICITY | {"voltage": 5000}))

    assert stored(sensors) == []


# ---------------------------------------------------------------- Autenticación del dispositivo


@pytest.mark.parametrize("headers", [{}, {"X-Device-Key": "wrong-key-wrong-key"}, {"X-Device-Key": ""}])
def test_wrong_device_key_is_rejected(sensors, headers):
    response = post(sensors, packet(electricity=ELECTRICITY), headers=headers)

    assert response.status_code == 401
    assert stored(sensors) == []


def test_auth_is_checked_before_payload_validation(sensors):
    assert post(sensors, {}, headers={}).status_code == 401


def test_user_session_token_is_not_a_device_key(sensors):
    token = sensors.headers["Authorization"].removeprefix("Bearer ")
    assert post(sensors, packet(electricity=ELECTRICITY), headers={"X-Device-Key": token}).status_code == 401


def test_ingestion_is_disabled_without_configured_key(settings, clock):
    client = make_client(settings.model_copy(update={"data_mode": "sensors"}), clock)
    try:
        assert post(client, packet(electricity=ELECTRICITY)).status_code == 503
    finally:
        client.__exit__(None, None, None)


@pytest.mark.parametrize(("raw", "expected"), [("", None), ("   ", None), (None, None)])
def test_empty_device_key_disables_ingestion(raw, expected):
    assert Settings(_env_file=None, device_api_key=raw).device_api_key is expected


def test_short_device_key_is_refused_at_startup():
    with pytest.raises(ValidationError):
        Settings(_env_file=None, device_api_key="short")


# ---------------------------------------------------------------- Persistencia


def stored(client) -> list[TelemetryReading]:
    with Session(client.app.state.engine) as session:
        return session.exec(select(TelemetryReading).order_by(TelemetryReading.timestamp)).all()


def test_packet_is_persisted(sensors):
    post(sensors, packet(electricity=ELECTRICITY, water=WATER))

    [row] = stored(sensors)
    assert row.device_id == "esp32-main"
    assert (row.voltage, row.current, row.power, row.energy_kwh) == (12.1, 1.2, 14.52, 0.42)
    assert (row.flow_liters_per_minute, row.total_liters) == (1.3, 18.7)


def test_repeated_packet_is_idempotent(sensors):
    first = post(sensors, packet(electricity=ELECTRICITY))
    again = post(sensors, packet(electricity=ELECTRICITY))

    assert again.status_code == 200
    assert again.json()["duplicate"] is True
    assert again.json()["id"] == first.json()["id"]
    assert len(stored(sensors)) == 1


def test_energy_uses_counter_differences_and_tolerates_resets(sensors):
    for seconds_ago, counter in [(180, 1.00), (120, 1.05), (60, 0.02), (0, 0.03)]:  # reinicio a 0.02
        post(sensors, packet(seconds_ago, electricity=ELECTRICITY | {"energy_kwh": counter}))

    assert sensors.get("/api/electricity/current").json()["energy_today_kwh"] == pytest.approx(0.05 + 0.02 + 0.01)


def test_energy_is_integrated_when_no_counter_is_sent(sensors):
    for seconds_ago in (120, 60, 0):
        post(sensors, packet(seconds_ago, electricity={"voltage": 12.0, "current": 5.0, "power": 60.0}))

    # 2 intervalos de 60 s a 60 W = 7200 J = 0.002 kWh
    assert sensors.get("/api/electricity/current").json()["energy_today_kwh"] == 0.002


def test_long_gaps_are_not_integrated(sensors):
    post(sensors, packet(3600, electricity={"voltage": 12.0, "current": 5.0, "power": 60.0}))
    post(sensors, packet(0, electricity={"voltage": 12.0, "current": 5.0, "power": 60.0}))

    assert sensors.get("/api/electricity/current").json()["energy_today_kwh"] == 0.0


def test_out_of_order_packet_does_not_double_count(sensors):
    post(sensors, packet(120, electricity=ELECTRICITY | {"energy_kwh": 1.0}))
    post(sensors, packet(0, electricity=ELECTRICITY | {"energy_kwh": 1.1}))
    post(sensors, packet(60, electricity=ELECTRICITY | {"energy_kwh": 1.05}))  # llega tarde

    assert sensors.get("/api/electricity/current").json()["energy_today_kwh"] == pytest.approx(0.1)


def test_water_liters_from_flow_and_counter(sensors):
    for seconds_ago in (120, 60, 0):  # sin contador: 2 L/min durante 2 min = 4 L
        post(sensors, packet(seconds_ago, water={"flow_liters_per_minute": 2.0}))
    assert sensors.get("/api/water/current").json()["liters_today"] == 4.0


def test_water_counter_differences(sensors):
    for seconds_ago, total in [(120, 10.0), (60, 12.5), (0, 13.0)]:
        post(sensors, packet(seconds_ago, water={"flow_liters_per_minute": 0.5, "total_liters": total}))

    assert sensors.get("/api/water/current").json()["liters_today"] == 3.0


# ---------------------------------------------------------------- Lecturas tras la ingesta


def test_current_readings_after_ingestion(sensors):
    post(sensors, packet(60, electricity=ELECTRICITY, water=WATER))
    post(sensors, packet(0, electricity={"voltage": 12.0, "current": 2.0, "power": 24.0, "energy_kwh": 0.5},
                         water={"flow_liters_per_minute": 0.8, "total_liters": 19.5}))

    electricity = sensors.get("/api/electricity/current").json()
    assert electricity == {"timestamp": "2026-09-11T18:30:00Z", "voltage": 12.0, "current": 2.0, "power": 24.0,
                           "energy_today_kwh": 0.08}
    water = sensors.get("/api/water/current").json()
    assert water == {"timestamp": "2026-09-11T18:30:00Z", "flow_liters_per_minute": 0.8, "liters_today": 0.8}


def test_histories_after_ingestion(sensors):
    post(sensors, packet(60, electricity=ELECTRICITY | {"energy_kwh": 0.40}, water=WATER | {"total_liters": 10.0}))
    post(sensors, packet(0, electricity=ELECTRICITY | {"energy_kwh": 0.46}, water=WATER | {"total_liters": 13.0}))

    day = sensors.get("/api/electricity/history?period=day").json()
    assert len(day) == 24
    assert day[-1]["timestamp"] == "2026-09-11T18:00:00Z"
    assert day[-1]["power"] == 14.52
    assert day[-1]["energy_today_kwh"] == 0.06
    assert day[0]["power"] == 0.0

    week = sensors.get("/api/water/history?period=week").json()
    assert len(week) == 7
    assert week[-1]["liters_today"] == 3.0

    history = sensors.get("/api/history?period=week").json()
    assert history[-1]["electricity_kwh"] == 0.06
    assert history[-1]["water_liters"] == 3.0
    assert sum(r["electricity_kwh"] for r in history[:-1]) == 0


def test_dashboard_and_budgets_reflect_telemetry(sensors):
    post(sensors, packet(60, electricity=ELECTRICITY | {"energy_kwh": 1.0}))
    post(sensors, packet(0, electricity=ELECTRICITY | {"energy_kwh": 1.5}, water=WATER))

    body = sensors.get("/api/dashboard").json()
    assert body["electricity"]["power"] == 14.52
    assert body["electricity"]["energy_today_kwh"] == 0.5
    assert body["water"]["flow_liters_per_minute"] == 1.3
    assert body["active_devices"] == 0
    assert body["unread_alerts"] == 0
    assert next(b for b in body["budgets"] if b["id"] == "electricity")["current_usage"] == 0.5
    assert body["system_status"]["data_source"] == "sensors"
    assert body["system_status"]["esp32_connected"] is True


def test_assistant_works_with_telemetry(sensors):
    post(sensors, packet(electricity=ELECTRICITY))

    reply = sensors.post("/api/assistant/chat", json={"message": "¿cuánta potencia?"}).json()["reply"]["text"]
    assert "14.5 W" in reply


def test_sensors_mode_without_data_returns_empty_readings(sensors):
    assert sensors.get("/api/electricity/current").json()["power"] == 0.0
    assert sensors.get("/api/devices").json() == []
    assert len(sensors.get("/api/history?period=month").json()) == 30


# ---------------------------------------------------------------- Estado del sistema


def test_system_status_before_any_packet(sensors):
    body = sensors.get("/api/system/status").json()

    assert body == {"backend_connected": True, "esp32_connected": False, "last_update": None,
                    "data_source": "sensors", "device_id": None}


def test_system_status_tracks_last_packet_and_timeout(sensors, clock):
    post(sensors, packet(30, electricity=ELECTRICITY) | {"device_id": "esp32-lab"})

    body = sensors.get("/api/system/status").json()
    assert body["esp32_connected"] is True
    assert body["device_id"] == "esp32-lab"
    assert body["last_update"] == "2026-09-11T18:30:00Z"  # recepción, no el timestamp de la medida

    clock.now = FIXED_NOW + timedelta(seconds=60)
    assert sensors.get("/api/system/status").json()["esp32_connected"] is True
    clock.now = FIXED_NOW + timedelta(seconds=61)
    assert sensors.get("/api/system/status").json()["esp32_connected"] is False


def test_health_reports_sensors_mode(sensors):
    assert sensors.get("/health").json()["data_mode"] == "sensors"


# ---------------------------------------------------------------- Separación demo / sensors


def test_demo_mode_ignores_ingested_telemetry(settings, clock):
    client = make_client(settings.model_copy(update={"device_api_key": SecretStr(DEVICE_KEY)}), clock)
    try:
        demo_before = client.get("/api/electricity/current").json()
        assert post(client, packet(electricity=ELECTRICITY | {"power": 99.0})).status_code == 201

        assert client.get("/api/electricity/current").json() == demo_before
        status = client.get("/api/system/status").json()
        assert status["data_source"] == "demo"
        assert status["device_id"] is None
        assert len(client.get("/api/devices").json()) == 5
    finally:
        client.__exit__(None, None, None)
