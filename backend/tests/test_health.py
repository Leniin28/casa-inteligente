def test_health_reports_ok(client):
    response = client.get("/health")

    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "ok"
    assert body["database"] == "ok"
    assert body["data_mode"] == "demo"


def test_openapi_documents_the_contract(client):
    paths = client.get("/openapi.json").json()["paths"]

    for path in [
        "/api/dashboard",
        "/api/electricity/current",
        "/api/water/history",
        "/api/devices/{device_id}",
        "/api/alerts/{alert_id}/read",
        "/api/budgets/{budget_id}",
        "/api/history",
        "/api/assistant/chat",
        "/api/system/status",
        "/api/auth/login",
    ]:
        assert path in paths
