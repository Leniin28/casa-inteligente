# Backend local — Casa Inteligente

API REST en **FastAPI** que se ejecuta en la laptop. Hoy sirve datos **demo** simulados; más adelante
recibirá los datos del ESP32 y conectará la IA local, sin cambiar el contrato con Android.

Stack: Python 3.12+ (probado con 3.14), FastAPI, SQLModel (SQLAlchemy + Pydantic), SQLite, Uvicorn.

## Puesta en marcha

```powershell
cd backend
python -m venv .venv
.\.venv\Scripts\activate            # Linux/macOS: source .venv/bin/activate
pip install -r requirements-dev.txt
copy .env.example .env              # opcional
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

- <http://localhost:8000/health> → `{"status": "ok", ...}`
- <http://localhost:8000/docs> → Swagger. Pulsa **Authorize** y pega el token de `/api/auth/login`.

Usuario demo creado al arrancar: `demo@casa.local` / `demo1234` (configurable en `.env`).

`--host 0.0.0.0` hace que el backend acepte conexiones desde el móvil en la misma red. Si solo usas
el emulador, basta con `127.0.0.1` (el emulador llega al PC a través de `10.0.2.2`). No lo expongas a
Internet.

Si la app muestra "No se pudo conectar con el backend" en Android 17+, comprueba que se concedió el
permiso de red local ("Dispositivos cercanos"): sin él el sistema bloquea la conexión.

## Tests

```powershell
pytest
```

Los tests usan una base de datos temporal y una hora fija (`tests/conftest.py`), así que son reproducibles.

## Configuración

Variables con prefijo `SMARTHOME_` (ver `.env.example`):

| Variable | Por defecto | Descripción |
|---|---|---|
| `SMARTHOME_DATABASE_URL` | `sqlite:///./smarthome.db` | Base de datos |
| `SMARTHOME_DATA_MODE` | `demo` | Origen de los datos |
| `SMARTHOME_TIMEZONE` | zona del sistema | Para calcular "hoy" |
| `SMARTHOME_SESSION_TTL_HOURS` | `72` | Duración de la sesión |
| `SMARTHOME_SEED_DEMO_USER` | `true` | Crear usuario demo |

No hay secretos en el repositorio. `.env` y `*.db` están en `.gitignore`.

## Estructura

| Archivo | Contenido |
|---|---|
| `app/main.py` | `create_app()`: routers, BD, usuario demo |
| `app/routers/auth.py` | registro, login, me, logout, password-reset |
| `app/routers/data.py` | dashboard, electricidad, agua, dispositivos, alertas, presupuestos, historial |
| `app/routers/assistant.py` | chat del asistente |
| `app/routers/system.py` | `/health` y estado del sistema |
| `app/services/simulator.py` | simulador determinista (igual que el de Android) |
| `app/services/providers.py` | `DataProvider`: capa que adapta el hardware |
| `app/services/budgets.py` | proyección de presupuestos |
| `app/services/assistant.py` | respuestas del asistente (reglas; futuro: IA local) |
| `app/security.py` | PBKDF2-SHA256 y tokens de sesión |

## Seguridad

- Contraseñas con PBKDF2-SHA256 (600 000 iteraciones, salt aleatorio). Nunca en claro.
- Tokens de sesión aleatorios; en la BD solo se guarda su SHA-256. Logout los invalida.
- `password-reset` responde igual exista o no el email.
- Es un servidor de red local sin TLS: no exponerlo a Internet.

## Próximos pasos (fuera de esta fase)

- **ESP32 real**: endpoint de ingesta (o MQTT) que guarde lecturas en SQLite + `SensorDataProvider`
  que las lea. Los routers y la app no cambian.
- **IA local**: sustituir `generate_reply` en `app/services/assistant.py` por la llamada al modelo local.
