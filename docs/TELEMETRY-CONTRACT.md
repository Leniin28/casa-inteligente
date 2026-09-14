# Contrato de telemetría (ESP32 → backend)

Contrato **normalizado**: el ESP32 envía magnitudes físicas (V, A, W, kWh, L/min, L), nunca datos
de un sensor concreto. Cambiar INA226 por PZEM, o YF-S401 por otro caudalímetro, solo cambia el
firmware; el backend y Android no cambian.

Implementación: `backend/app/schemas.py` (`TelemetryIn`), `backend/app/routers/telemetry.py`,
`backend/app/services/telemetry.py`. Flujo completo en [ESP32-INTEGRATION.md](ESP32-INTEGRATION.md).

## Endpoint

```
POST /api/telemetry
Content-Type: application/json
X-Device-Key: <SMARTHOME_DEVICE_API_KEY>
```

Mismo prefijo `/api` que el resto de la API. No usa el token de sesión de usuario: usa la clave de
dispositivo (ver [Autenticación](#autenticación)).

## Paquete

```json
{
  "device_id": "esp32-main",
  "timestamp": "2026-09-11T18:30:00.000Z",
  "electricity": {
    "voltage": 12.1,
    "current": 1.2,
    "power": 14.52,
    "energy_kwh": 0.42
  },
  "water": {
    "flow_liters_per_minute": 1.3,
    "total_liters": 18.7
  }
}
```

La API usa snake_case, pero también se aceptan las claves camelCase (`deviceId`,
`flowLitersPerMinute`, `totalLiters`, `energyKwh`) y `energy` como sinónimo de `energy_kwh`. Así el
paquete de ejemplo del diseño original es válido tal cual.

| Campo | Tipo | Obligatorio | Unidad | Rango aceptado |
|---|---|---|---|---|
| `device_id` | string | sí | — | `^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$` |
| `timestamp` | ISO-8601 **con zona** | no | — | ≤ 5 min en el futuro, ≤ 30 días en el pasado |
| `electricity` | objeto | no\* | — | — |
| `electricity.voltage` | número | sí (en el bloque) | V | 0 – 1 000 |
| `electricity.current` | número | sí (en el bloque) | A | 0 – 500 |
| `electricity.power` | número | sí (en el bloque) | W (potencia activa) | 0 – 100 000 |
| `electricity.energy_kwh` | número | no | kWh, contador acumulado | 0 – 10 000 000 |
| `water` | objeto | no\* | — | — |
| `water.flow_liters_per_minute` | número | sí (en el bloque) | L/min | 0 – 1 000 |
| `water.total_liters` | número | no | L, contador acumulado | 0 – 1 000 000 000 |

\* Al menos uno de `electricity` o `water`.

### Reglas de validación

- Números JSON finitos: `NaN`, `Infinity` y números entre comillas (`"12.1"`) se rechazan.
- Los rangos solo filtran valores **físicamente absurdos** (un sensor desconectado, un overflow);
  no son reglas de la maqueta de 12 V. Una instalación de 230 V AC cabe en el mismo contrato.
- No se exige `voltage × current = power`: en AC depende del factor de potencia.
- `timestamp` sin zona (`2026-09-11T18:30:00`) se rechaza: no se puede adivinar la hora del ESP32.
  Si el ESP32 no tiene hora (sin NTP), se omite `timestamp` y el backend usa la hora de recepción.
- Campos o bloques desconocidos se rechazan (422) para detectar erratas en el firmware. Añadir un
  campo al contrato requiere actualizar antes el backend.
- Negativos no se admiten: el firmware normaliza el sentido de la corriente.

### Semántica de los contadores

`energy_kwh` y `total_liters` son **contadores acumulados** del dispositivo (como un medidor). Son
opcionales:

- **Con contador**: el consumo de cada paquete es la diferencia con el paquete anterior del mismo
  `device_id`. Si el contador baja, el ESP32 se reinició: se toma el valor nuevo como consumo desde 0.
  Los huecos de conexión no pierden consumo.
- **Sin contador**: el backend integra la potencia/caudal del paquete anterior durante el tiempo
  transcurrido. Si el hueco supera 5 minutos no se integra (no se inventa consumo).
- Un paquete que llega fuera de orden se guarda, pero no suma consumo (ya lo cubre el siguiente).

## Respuestas

### 201 Created — paquete guardado
```json
{"id": 42, "device_id": "esp32-main", "timestamp": "2026-09-11T18:30:00Z",
 "received_at": "2026-09-11T18:30:00.412Z", "duplicate": false}
```

### 200 OK — paquete repetido
Mismo `device_id` y `timestamp` que uno ya guardado (p. ej. un reintento tras un timeout). No se
duplica: devuelve el `id` existente con `"duplicate": true`. Reenviar es seguro.

### Errores

| Código | Cuándo | Cuerpo |
|---|---|---|
| 401 | Falta `X-Device-Key` o no coincide | `{"detail": "Clave de dispositivo no válida"}` |
| 422 | JSON inválido, campo fuera de rango, bloque vacío, `timestamp` sin zona... | `{"detail": [{"loc": [...], "msg": "...", ...}]}` |
| 422 | `timestamp` en el futuro o demasiado antiguo | `{"detail": "timestamp está en el futuro (revisa el reloj/NTP del dispositivo)"}` |
| 503 | El backend no tiene `SMARTHOME_DEVICE_API_KEY` configurada | `{"detail": "Ingesta de telemetría deshabilitada: configura SMARTHOME_DEVICE_API_KEY"}` |

La autenticación se comprueba antes que el cuerpo: sin clave válida siempre es 401.

Ejemplo de 422:
```json
{"detail": [{"type": "less_than_equal", "loc": ["body", "electricity", "voltage"],
             "msg": "Input should be less than or equal to 1000", "input": 5000, "ctx": {"le": 1000.0}}]}
```

El firmware debe: reintentar en 5xx o error de red (es idempotente), **no** reintentar en 401/422
(el paquete o la clave están mal; reintentar no lo arregla).

## Autenticación

Clave compartida de dispositivo en la cabecera `X-Device-Key`, configurada en el backend con
`SMARTHOME_DEVICE_API_KEY` (mínimo 16 caracteres; se compara en tiempo constante).

- Sin clave configurada, la ingesta responde 503: el endpoint **nunca** queda abierto por olvido.
- La clave no está en el repositorio. `.env.example` la deja vacía y explica cómo generarla.
- Es distinta del token de usuario: un token de sesión no sirve para enviar telemetría y la clave del
  ESP32 no da acceso a los datos de la casa.

Limitaciones asumidas en esta fase (red local, sin TLS): la clave viaja en claro por la LAN y es una
sola para todos los dispositivos. Futuro: una clave por `device_id` guardada como hash en la BD, y TLS
o MQTT con credenciales si el sistema sale de la red local.

## Qué alimenta cada paquete

Con `SMARTHOME_DATA_MODE=sensors`, la tabla `telemetry_readings` es la fuente de los endpoints que ya
usa Android (el contrato de salida no cambia; ver [API.md](API.md)):

| Endpoint | Se calcula como |
|---|---|
| `GET /api/electricity/current` | último paquete con `electricity`; `energy_today_kwh` = suma de consumos del día local |
| `GET /api/water/current` | último paquete con `water`; `liters_today` = suma de litros del día local |
| `GET /api/electricity/history`, `/api/water/history` | media de V/A/W o caudal por hora (`day`) o día (`week`, `month`); acumulado del día al final de cada tramo |
| `GET /api/history` | kWh y litros por tramo + coste estimado |
| `GET /api/budgets` | consumo del mes local |
| `GET /api/dashboard` | todo lo anterior + estado |
| `GET /api/system/status` | `esp32_connected`, `last_update`, `device_id` (ver abajo) |
| `GET /api/devices`, `/api/alerts` | `[]` hasta la fase de detección de dispositivos (NILM) y alertas reales |

Sin ningún paquete todavía, las lecturas actuales devuelven ceros con la hora actual y el estado
indica `esp32_connected: false`.

### Estado del sistema

```json
{"backend_connected": true, "esp32_connected": true, "last_update": "2026-09-11T18:30:00.412Z",
 "data_source": "sensors", "device_id": "esp32-main"}
```

- `last_update`: hora de **recepción** del último paquete (no la de medida).
- `esp32_connected`: `true` si el último paquete llegó hace ≤ `SMARTHOME_ESP32_TIMEOUT_SECONDS`
  (60 s por defecto). Es aproximado: el backend solo sabe cuándo recibió el último paquete.
- `device_id`: el dispositivo que envió ese paquete. Campo nuevo y opcional (`null` en demo); Android
  ignora los campos desconocidos, así que no rompe la app.
