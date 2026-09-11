"""Endpoints de datos de la casa. Todos requieren sesión (Authorization: Bearer <token>)."""

import calendar
from datetime import date, datetime

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlmodel import Session, select

from app.config import Settings
from app.deps import NowDep, ProviderDep, SessionDep, SettingsDep, get_current_user
from app.models import AlertReadState, BudgetLimit
from app.routers.system import build_status
from app.schemas import (
    AlertOut,
    BudgetIn,
    BudgetOut,
    DashboardOut,
    DeviceOut,
    ElectricalReadingOut,
    HistoryRecordOut,
    Period,
    ResourceType,
    WaterReadingOut,
)
from app.services.budgets import project_budget
from app.services.providers import DataProvider
from app.services.simulator import DEFAULT_LIMITS

router = APIRouter(prefix="/api", dependencies=[Depends(get_current_user)])

PeriodQuery = Query(default="week", description="day = 24 h por horas, week = 7 días, month = 30 días")


# ---------------------------------------------------------------- Helpers compartidos


def build_alerts(provider: DataProvider, session: Session, now: datetime) -> list[AlertOut]:
    read_ids = set(session.exec(select(AlertReadState.alert_id)).all())
    return [AlertOut(**alert, read=alert["id"] in read_ids) for alert in provider.alerts(now)]


def build_budgets(provider: DataProvider, session: Session, now: datetime, today: date) -> list[BudgetOut]:
    limits = dict(DEFAULT_LIMITS)
    for row in session.exec(select(BudgetLimit)).all():
        limits[row.resource_type] = row.limit
    return [budget_for(provider, resource, limit, now, today) for resource, limit in limits.items()]


def budget_for(provider: DataProvider, resource: str, limit: float, now: datetime, today: date) -> BudgetOut:
    period_start = today.replace(day=1)
    period_end = today.replace(day=calendar.monthrange(today.year, today.month)[1])
    return project_budget(
        resource_type=resource,
        limit=limit,
        current_usage=provider.month_usage(resource, now),
        period_start=period_start,
        period_end=period_end,
        today=today,
    )


def local_today(settings: Settings, now: datetime) -> date:
    return now.astimezone(settings.tz()).date()


# ---------------------------------------------------------------- Dashboard


@router.get("/dashboard", response_model=DashboardOut, tags=["dashboard"])
def dashboard(provider: ProviderDep, session: SessionDep, settings: SettingsDep, now: NowDep) -> DashboardOut:
    alerts = build_alerts(provider, session, now)
    return DashboardOut(
        electricity=ElectricalReadingOut(**provider.electrical_reading(now)),
        water=WaterReadingOut(**provider.water_reading(now)),
        active_devices=sum(1 for d in provider.devices(now) if d["status"] != "inactive"),
        unread_alerts=sum(1 for a in alerts if not a.read),
        budgets=build_budgets(provider, session, now, local_today(settings, now)),
        system_status=build_status(provider, now),
    )


# ---------------------------------------------------------------- Electricidad y agua


@router.get("/electricity/current", response_model=ElectricalReadingOut, tags=["electricity"])
def electricity_current(provider: ProviderDep, now: NowDep) -> ElectricalReadingOut:
    return ElectricalReadingOut(**provider.electrical_reading(now))


@router.get("/electricity/history", response_model=list[ElectricalReadingOut], tags=["electricity"])
def electricity_history(provider: ProviderDep, now: NowDep, period: Period = PeriodQuery) -> list[ElectricalReadingOut]:
    return [ElectricalReadingOut(**r) for r in provider.electrical_history(period, now)]


@router.get("/water/current", response_model=WaterReadingOut, tags=["water"])
def water_current(provider: ProviderDep, now: NowDep) -> WaterReadingOut:
    return WaterReadingOut(**provider.water_reading(now))


@router.get("/water/history", response_model=list[WaterReadingOut], tags=["water"])
def water_history(provider: ProviderDep, now: NowDep, period: Period = PeriodQuery) -> list[WaterReadingOut]:
    return [WaterReadingOut(**r) for r in provider.water_history(period, now)]


# ---------------------------------------------------------------- Dispositivos


@router.get("/devices", response_model=list[DeviceOut], tags=["devices"])
def devices(provider: ProviderDep, now: NowDep) -> list[DeviceOut]:
    return [DeviceOut(**d) for d in provider.devices(now)]


@router.get("/devices/{device_id}", response_model=DeviceOut, tags=["devices"])
def device(device_id: str, provider: ProviderDep, now: NowDep) -> DeviceOut:
    for d in provider.devices(now):
        if d["id"] == device_id:
            return DeviceOut(**d)
    raise HTTPException(status.HTTP_404_NOT_FOUND, "Dispositivo no encontrado")


# ---------------------------------------------------------------- Alertas


@router.get("/alerts", response_model=list[AlertOut], tags=["alerts"])
def alerts(provider: ProviderDep, session: SessionDep, now: NowDep) -> list[AlertOut]:
    return build_alerts(provider, session, now)


@router.patch("/alerts/{alert_id}/read", response_model=AlertOut, tags=["alerts"])
def mark_alert_read(alert_id: str, provider: ProviderDep, session: SessionDep, now: NowDep) -> AlertOut:
    if alert_id not in {a["id"] for a in provider.alerts(now)}:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Alerta no encontrada")
    if session.get(AlertReadState, alert_id) is None:
        session.add(AlertReadState(alert_id=alert_id))
        session.commit()
    return next(a for a in build_alerts(provider, session, now) if a.id == alert_id)


# ---------------------------------------------------------------- Presupuestos


@router.get("/budgets", response_model=list[BudgetOut], tags=["budgets"])
def budgets(provider: ProviderDep, session: SessionDep, settings: SettingsDep, now: NowDep) -> list[BudgetOut]:
    return build_budgets(provider, session, now, local_today(settings, now))


def _save_limit(session: Session, resource: ResourceType, limit: float) -> None:
    row = session.get(BudgetLimit, resource)
    if row is None:
        session.add(BudgetLimit(resource_type=resource, limit=limit))
    else:
        row.limit = limit
        session.add(row)
    session.commit()


@router.post("/budgets", response_model=BudgetOut, status_code=status.HTTP_201_CREATED, tags=["budgets"])
def create_budget(body: BudgetIn, provider: ProviderDep, session: SessionDep, settings: SettingsDep, now: NowDep) -> BudgetOut:
    """Crea (o sustituye) el presupuesto de un recurso. Solo hay uno por recurso."""
    _save_limit(session, body.resource_type, body.limit)
    return budget_for(provider, body.resource_type, body.limit, now, local_today(settings, now))


@router.put("/budgets/{budget_id}", response_model=BudgetOut, tags=["budgets"])
def update_budget(
    budget_id: str, body: BudgetIn, provider: ProviderDep, session: SessionDep, settings: SettingsDep, now: NowDep
) -> BudgetOut:
    if budget_id not in DEFAULT_LIMITS:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Presupuesto no encontrado")
    if body.resource_type != budget_id:
        raise HTTPException(422, "resource_type no coincide con el id")
    _save_limit(session, budget_id, body.limit)
    return budget_for(provider, budget_id, body.limit, now, local_today(settings, now))


# ---------------------------------------------------------------- Historial


@router.get("/history", response_model=list[HistoryRecordOut], tags=["history"])
def history(provider: ProviderDep, now: NowDep, period: Period = PeriodQuery) -> list[HistoryRecordOut]:
    return [HistoryRecordOut(**r) for r in provider.history(period, now)]
