from datetime import date

from app.services.budgets import project_budget

START = date(2026, 9, 1)
END = date(2026, 9, 30)


def test_linear_projection():
    # 2.5 kWh en 10 días -> 0.25 kWh/día -> 7.5 kWh en 30 días
    budget = project_budget("electricity", 5.0, 2.5, START, END, date(2026, 9, 10))

    assert budget.estimated_final_usage == 7.5
    assert budget.estimated_limit_date == date(2026, 9, 20)


def test_limit_not_reached_in_period():
    budget = project_budget("water", 10.0, 2.5, START, END, date(2026, 9, 10))

    assert budget.estimated_limit_date is None


def test_already_over_limit():
    today = date(2026, 9, 15)

    assert project_budget("water", 2.0, 3.0, START, END, today).estimated_limit_date == today


def test_budgets_endpoint_and_update(client, auth_headers):
    budgets = client.get("/api/budgets", headers=auth_headers).json()
    assert {b["id"] for b in budgets} == {"electricity", "water"}

    updated = client.put(
        "/api/budgets/water", json={"resource_type": "water", "limit": 1234}, headers=auth_headers
    )
    assert updated.status_code == 200
    assert updated.json()["limit"] == 1234

    water = next(b for b in client.get("/api/budgets", headers=auth_headers).json() if b["id"] == "water")
    assert water["limit"] == 1234
    assert water["period_start"] == "2026-09-01"
    assert water["period_end"] == "2026-09-30"


def test_budget_validation(client, auth_headers):
    assert client.put(
        "/api/budgets/water", json={"resource_type": "water", "limit": 0}, headers=auth_headers
    ).status_code == 422
    assert client.put(
        "/api/budgets/gas", json={"resource_type": "water", "limit": 5}, headers=auth_headers
    ).status_code == 404


def test_create_budget(client, auth_headers):
    response = client.post("/api/budgets", json={"resource_type": "electricity", "limit": 8}, headers=auth_headers)

    assert response.status_code == 201
    assert response.json()["limit"] == 8
