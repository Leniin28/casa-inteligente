"""Proyección de presupuestos (misma lógica que BudgetCalculator en Android)."""

import math
from datetime import date, timedelta

from app.schemas import BudgetOut, ResourceType


def project_budget(
    resource_type: ResourceType,
    limit: float,
    current_usage: float,
    period_start: date,
    period_end: date,
    today: date,
) -> BudgetOut:
    """Proyección lineal: consumo medio diario hasta hoy × días del periodo."""
    total_days = (period_end - period_start).days + 1
    elapsed_days = min(max((today - period_start).days + 1, 1), total_days)
    daily_average = current_usage / elapsed_days
    estimated_final = daily_average * total_days

    if current_usage >= limit:
        limit_date: date | None = today
    elif daily_average <= 0:
        limit_date = None
    else:
        days_until_limit = math.ceil((limit - current_usage) / daily_average)
        candidate = today + timedelta(days=days_until_limit)
        limit_date = candidate if candidate <= period_end else None

    return BudgetOut(
        id=resource_type,
        resource_type=resource_type,
        limit=limit,
        current_usage=current_usage,
        estimated_final_usage=round(estimated_final, 3),
        estimated_limit_date=limit_date,
        period_start=period_start,
        period_end=period_end,
    )
