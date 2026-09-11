import uuid
from datetime import UTC

from fastapi import APIRouter, Depends

from app.deps import NowDep, ProviderDep, SessionDep, SettingsDep, get_current_user
from app.routers.data import build_budgets, local_today
from app.schemas import AssistantMessageOut, ChatIn, ChatOut
from app.services.assistant import generate_reply

router = APIRouter(prefix="/api/assistant", tags=["assistant"], dependencies=[Depends(get_current_user)])


@router.post("/chat", response_model=ChatOut)
def chat(body: ChatIn, provider: ProviderDep, session: SessionDep, settings: SettingsDep, now: NowDep) -> ChatOut:
    reply = generate_reply(
        message=body.message,
        history=body.history,
        electricity=provider.electrical_reading(now),
        water=provider.water_reading(now),
        budgets=build_budgets(provider, session, now, local_today(settings, now)),
    )
    return ChatOut(
        reply=AssistantMessageOut(id=str(uuid.uuid4()), role="assistant", text=reply, timestamp=now.astimezone(UTC))
    )
