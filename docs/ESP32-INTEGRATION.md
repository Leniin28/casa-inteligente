# Integración con el ESP32

Estado: backend y contrato listos; **sin firmware todavía**. Mientras tanto, el simulador de
`tools/esp32_simulator/` hace de ESP32 y envía telemetría real por HTTP.

## Flujo

```
 Sensores físicos (cualquiera)           Laptop (red local)                      Móvil
┌──────────────────────────┐  HTTP POST  ┌──────────────────────────────────┐  HTTP GET  ┌─────────┐
│ ESP32                    │ ──────────► │ FastAPI  POST /api/telemetry     │ ◄───────── │ Android │
│  lee INA226/PZEM/YF-S401 │  JSON +     │   valida + X-Device-Key          │  Bearer    │ (modo   │
│  y NORMALIZA a V,A,W,kWh │  X-Device-  │   ↓                              │  token     │  API)   │
│  L/min, L                │  Key        │ SQLite telemetry_readings        │            │         │
└──────────────────────────┘             │   ↓                              │            │         │
  (hoy: esp32_simulator.py)              │ TelemetryDataProvider (sensors)  │ ─────────► │ modelos │
                                         │   → /api/electricity, /water,    │  mismo     │ actuales│
                                         │     /dashboard, /system/status…  │  contrato  │         │
                                         └──────────────────────────────────┘            └─────────┘
```

1. El **ESP32** convierte las lecturas de sus sensores a magnitudes físicas y las envía cada pocos
   segundos a `POST /api/telemetry` ([contrato](TELEMETRY-CONTRACT.md)).
2. **FastAPI** autentica el dispositivo, valida el paquete y lo guarda en SQLite
   (`telemetry_readings`), calculando el consumo incremental desde el paquete anterior.
3. Con `SMARTHOME_DATA_MODE=sensors`, `TelemetryDataProvider` responde los endpoints que Android ya
   usa a partir de esa tabla.
4. **Android** no cambia: sigue recibiendo `ElectricalReading`, `WaterReading`, `Dashboard`,
   `SystemStatus`... La única diferencia visible es `data_source: "sensors"` (que la app ya contempla
   como `backendMode`).

## Fuentes de datos: tres interruptores distintos

| Dónde | Opción | Qué hace |
|---|---|---|
| App Android → Ajustes | **Demo** | Simulador local del móvil. No usa el backend. Sin cambios. |
| App Android → Ajustes | **API** | Consume el backend, sea cual sea su modo. |
| Backend `SMARTHOME_DATA_MODE` | `demo` (defecto) | El backend sirve su simulador determinista. |
| Backend `SMARTHOME_DATA_MODE` | `sensors` | El backend sirve la telemetría recibida. |

Demo y telemetría real no se mezclan nunca: en modo `demo` el backend acepta y guarda paquetes (si
hay clave), pero sigue sirviendo solo datos simulados; en modo `sensors` no hay ningún dato inventado
(dispositivos y alertas quedan vacíos hasta sus fases).

## Puesta en marcha con el simulador

```powershell
cd backend
.\.venv\Scripts\activate
copy .env.example .env      # si no existe
python -c "import secrets; print(secrets.token_urlsafe(32))"
```

En `backend/.env`:
```
SMARTHOME_DATA_MODE=sensors
SMARTHOME_DEVICE_API_KEY=<la clave generada>
```

```powershell
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

En otra terminal (desde la raíz del repositorio):
```powershell
$env:SMARTHOME_DEVICE_API_KEY = "<la misma clave>"
python tools/esp32_simulator/esp32_simulator.py --scenario normal --interval 5
```

Comprobación manual:
```powershell
curl http://localhost:8000/api/system/status      # esp32_connected: true, device_id: esp32-main
```
Y en la app, con la fuente **API**, el dashboard muestra los valores del simulador. Escenarios y
opciones en [tools/esp32_simulator/README.md](../tools/esp32_simulator/README.md).

## Decisión: autenticación del dispositivo

Clave compartida en la cabecera `X-Device-Key`, configurable con `SMARTHOME_DEVICE_API_KEY`.

- **Por qué**: es lo más simple que un ESP32 puede enviar (una cabecera HTTP) y evita que cualquier
  equipo de la LAN inyecte lecturas falsas. No requiere usuarios, tokens que caduquen ni reloj.
- **Cerrado por defecto**: sin clave configurada, el endpoint responde 503. Claves de menos de 16
  caracteres impiden arrancar el backend.
- **Sin secretos en git**: `.env` está ignorado; `.env.example` deja la clave vacía. En el firmware
  irá en un fichero de configuración no versionado (p. ej. `secrets.h` en `.gitignore`).
- **Separada de los usuarios**: la clave del ESP32 solo sirve para escribir telemetría; el token de
  usuario solo para leer. Comprometer uno no da el otro.
- **Límites conocidos**: HTTP sin TLS en la LAN (la clave se puede capturar en la red local) y una
  clave para todos los dispositivos. Suficiente para una maqueta en red doméstica; si el sistema sale
  de la LAN: clave por dispositivo (hash en BD) y TLS o MQTT con credenciales.

## Guía para el firmware (fase siguiente)

- Enviar un paquete cada 2–10 s. Un bloque por magnitud disponible: si solo hay sensor de agua, solo
  `water`.
- Normalizar en el ESP32: V, A, W (potencia activa), L/min. Nada de nombres de sensor en el JSON.
- Mantener contadores acumulados (`energy_kwh`, `total_liters`) si es posible: evitan perder consumo
  cuando se corta la WiFi. Si no se puede, omitirlos; el backend integra la potencia/caudal.
- Sincronizar la hora por NTP y enviar `timestamp` en UTC con `Z`. Sin NTP, omitir `timestamp`.
- Reintentar ante error de red o 5xx (el endpoint es idempotente por `device_id` + `timestamp`); no
  reintentar ante 401/422.
- `device_id` estable (p. ej. `esp32-main`), no la MAC si no se quiere exponer.

## Fuera de alcance de esta fase

Firmware, MQTT, WebSockets, sensores concretos, detección de dispositivos (NILM), IA, alertas
reales y control físico. El escenario `device_step` del simulador ya genera escalones de potencia
(+5, +12, +25 W) que servirán para desarrollar NILM más adelante.

## Pruebas

- `backend/tests/test_telemetry.py`: ingesta válida, bloques opcionales, validaciones, autenticación,
  persistencia, contadores, lecturas, historial, dashboard, estado y separación demo/sensors.
- `backend/tests/test_esp32_simulator.py`: los paquetes de todos los escenarios cumplen el contrato.
