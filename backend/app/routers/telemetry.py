"""Ingesta de telemetría normalizada enviada por el ESP32 (o por tools/esp32_simulator).

Autenticación de dispositivo: cabecera `X-Device-Key` con el valor de SMARTHOME_DEVICE_API_KEY.
Sin clave configurada la ingesta está deshabilitada (503): el endpoint nunca queda abierto.
"""

import hmac
from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, Response, Security, status
from fastapi.security import APIKeyHeader

from app.deps import NowDep, SessionDep, SettingsDep
from app.schemas import TelemetryAckOut, TelemetryIn
from app.services.telemetry import TelemetryRejected, ingest, to_aware_utc

device_key_scheme = APIKeyHeader(
    name="X-Device-Key",
    scheme_name="DeviceKey",
    auto_error=False,
    description="Clave de dispositivo (SMARTHOME_DEVICE_API_KEY). Solo para POST /api/telemetry.",
)


def require_device_key(settings: SettingsDep, key: Annotated[str | None, Security(device_key_scheme)]) -> None:
    expected = settings.device_api_key
    if expected is None:
        raise HTTPException(
            status.HTTP_503_SERVICE_UNAVAILABLE,
            "Ingesta de telemetría deshabilitada: configura SMARTHOME_DEVICE_API_KEY",
        )
    if key is None or not hmac.compare_digest(key.encode(), expected.get_secret_value().encode()):
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Clave de dispositivo no válida")


router = APIRouter(prefix="/api", tags=["telemetry"])


@router.post(
    "/telemetry",
    response_model=TelemetryAckOut,
    status_code=status.HTTP_201_CREATED,
    dependencies=[Depends(require_device_key)],
    responses={200: {"description": "Paquete repetido (mismo device_id y timestamp): no se duplica"}},
)
def ingest_telemetry(packet: TelemetryIn, response: Response, session: SessionDep, now: NowDep) -> TelemetryAckOut:
    """Recibe un paquete de telemetría normalizada. Los bloques `electricity` y `water` son opcionales,
    pero al menos uno es obligatorio."""
    try:
        reading, duplicate = ingest(session, packet, now)
    except TelemetryRejected as exc:
        raise HTTPException(status.HTTP_422_UNPROCESSABLE_CONTENT, str(exc)) from exc
    if duplicate:
        response.status_code = status.HTTP_200_OK
    return TelemetryAckOut(
        id=reading.id,
        device_id=reading.device_id,
        timestamp=to_aware_utc(reading.timestamp),
        received_at=to_aware_utc(reading.received_at),
        duplicate=duplicate,
    )
