# Casa Inteligente

Aplicación Android + backend local para monitorizar el consumo de **electricidad** y **agua** de una casa
(proyecto universitario, equipo de 5 personas).

Esta primera fase deja la **base técnica** lista: arquitectura, navegación, lógica, datos demo, persistencia,
API y backend. Las pantallas son funcionales pero **deliberadamente sencillas**: el diseño visual es tarea
del resto del equipo.

---

## 1. Objetivo

- Mostrar en el móvil consumo eléctrico, consumo de agua, dispositivos detectados, alertas, presupuestos,
  historial y un asistente de IA.
- Funcionar **desde el primer día sin hardware** (modo Demo).
- Poder cambiar a datos reales (ESP32 → laptop → API) **sin tocar las pantallas**.

## 2. Arquitectura

```
ESP32 + sensores (INA226, YF-S401... aún por decidir)
        │
        ▼
Backend local en la laptop (FastAPI + SQLite)      ← adapta los sensores al contrato
        │   (en el futuro: análisis + IA local)
        ▼
REST API  (/api/...)
        │
        ▼
App Android (Kotlin + Jetpack Compose + Material 3)
```

Dentro de Android:

```
Screen (Compose, solo diseño)
   ▲ UiState            │ eventos (lambdas)
   │                    ▼
ViewModel (StateFlow) ──► Repository ──► DataSourceProvider ──┬─► DemoSmartHomeDataSource (simulado, sin red)
                              │                              └─► ApiSmartHomeDataSource  (Retrofit → backend)
                              └─► Room (caché offline) · DataStore (ajustes y sesión)
```

**La app no conoce ningún sensor concreto.** Solo trabaja con modelos abstractos
(`ElectricalReading`, `WaterReading`, `Device`, `Alert`, `Budget`, `HistoryRecord`,
`AssistantMessage`, `SystemStatus`). Más detalle en [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## 3. Estructura Android

Módulo único `app`, paquete `com.ecore.demo2`:

```
app/src/main/java/com/ecore/demo2/
├── core/
│   ├── model/        Modelos de dominio (sin dependencias de Android ni de sensores)
│   ├── network/      Retrofit: SmartHomeApi, DTOs, mappers, ApiSmartHomeDataSource
│   ├── database/     Room: entidades, DAOs, caché
│   ├── datastore/    DataStore: ajustes y sesión
│   ├── repository/   Repositorios, DataSourceProvider, BudgetCalculator, demo/ (simulador)
│   ├── di/           Módulos Hilt
│   ├── ui/           Tema, LoadState, componentes comunes, formatos, datos de preview
│   └── util/         Validaciones, errores legibles, parsing
├── feature/
│   ├── auth/         splash, login, register, forgot, profile
│   ├── dashboard/  electricity/  water/  devices/
│   ├── assistant/  alerts/  budget/  history/
│   ├── settings/     ajustes, casa, notificaciones, status/, about/
│   └── menu/         menús de las pestañas "Datos" y "Más"
├── navigation/       Rutas, barra inferior, NavHost, AppViewModel
├── MainActivity.kt
└── SmartHomeApplication.kt
```

Cada feature tiene `XxxScreen.kt` (diseño + `@Preview`), `XxxViewModel.kt` y `XxxUiState.kt`.

Stack: Compose + Material 3, Navigation Compose (rutas tipadas), MVVM, Coroutines/StateFlow, Hilt,
Retrofit + OkHttp + kotlinx.serialization, Room, DataStore Preferences.

Versiones del proyecto (no se han subido): Gradle 9.5.0, AGP 9.3.2, Kotlin 2.2.10,
compileSdk/targetSdk 37, minSdk 29. KSP 2.3.12, Hilt 2.59.2, Room 2.8.4.

## 4. Estructura backend

```
backend/
├── app/
│   ├── main.py          create_app(), arranque
│   ├── config.py        configuración (.env, prefijo SMARTHOME_)
│   ├── database.py      motor SQLite
│   ├── models.py        tablas SQLModel (usuarios, sesiones, límites, alertas leídas)
│   ├── schemas.py       contrato JSON (Pydantic)
│   ├── security.py      hash PBKDF2 y tokens
│   ├── deps.py          dependencias FastAPI (sesión, reloj, usuario actual)
│   ├── routers/         auth, data, assistant, system
│   └── services/        simulator (demo), providers (adaptador de datos), budgets, assistant
├── tests/
├── requirements.txt / requirements-dev.txt
├── .env.example
└── README.md
```

## 5. Ejecutar Android

1. Abrir la carpeta raíz en **Android Studio** (usa su JDK integrado).
2. Esperar a que sincronice Gradle.
3. Ejecutar la configuración `app` en un emulador o móvil.

Por línea de comandos (Windows, con el JDK de Android Studio):

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:assembleDebug        # compilar
.\gradlew.bat :app:testDebugUnitTest    # tests unitarios
```

## 6. Ejecutar backend

```powershell
cd backend
python -m venv .venv
.\.venv\Scripts\activate           # Linux/macOS: source .venv/bin/activate
pip install -r requirements-dev.txt
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

- Salud: <http://localhost:8000/health>
- Documentación interactiva (Swagger): <http://localhost:8000/docs>
- Tests: `pytest`

Detalles en [backend/README.md](backend/README.md).

## 7. Modo Demo (por defecto)

- No necesita ESP32, sensores, backend ni IA.
- Datos **simulados de forma determinista** (misma hora → mismos valores): potencia, voltaje, corriente,
  consumo acumulado, caudal, dispositivos estimados, alertas, presupuestos e historial coherentes entre sí.
- Login demo: cualquier email válido con contraseña de 6+ caracteres, por ejemplo
  **`demo@casa.local` / `demo1234`**.
- El asistente responde por palabras clave (electricidad, agua, presupuesto, ahorro...).

## 8. Modo API (casa real)

1. Arranca el backend (sección 6).
2. En la app: pantalla de Login → **"Configurar servidor / modo demo"** (o *Más → Ajustes*).
3. Fuente de datos: **Casa real (API)**. Revisa la URL del backend.
4. Inicia sesión con `demo@casa.local` / `demo1234` (usuario creado por el backend) o regístrate.

Al cambiar de fuente se cierra la sesión y se borra la caché (el token de una fuente no vale en la otra).
Si el backend no responde, la app muestra los últimos datos guardados en Room con un aviso.

## 9. Configurar la URL del backend

La URL **no está fija en el código**: se guarda en DataStore y se edita en *Ajustes*.
Valor por defecto (en `app/build.gradle.kts`, `DEFAULT_BACKEND_URL`): `http://10.0.2.2:8000/`.

| Dónde corre la app | URL |
|---|---|
| Emulador de Android Studio | `http://10.0.2.2:8000/` (10.0.2.2 = el `localhost` del PC) |
| Móvil físico en la misma Wi-Fi | `http://<IP-LAN-de-la-laptop>:8000/` (ej. `http://192.168.1.50:8000/`) |

Para el **móvil físico**:

1. Arranca uvicorn con `--host 0.0.0.0` (no solo `127.0.0.1`).
2. Busca la IP de la laptop (`ipconfig` en Windows) y permite el puerto 8000 en el firewall.
3. Añade esa IP en `app/src/main/res/xml/network_security_config.xml`. El backend local usa HTTP sin
   TLS, y la app **solo** permite tráfico sin cifrar hacia los hosts listados ahí (no se desactiva la
   seguridad global).

## 10. Ramas y equipo

| Persona | Pantallas | Carpetas principales |
|---|---|---|
| 1 | Login, registro, recuperar, perfil, splash | `feature/auth/` |
| 2 | Dashboard, Electricidad | `feature/dashboard/`, `feature/electricity/` |
| 3 | Agua, Dispositivos | `feature/water/`, `feature/devices/` |
| 4 | Asistente IA, Alertas | `feature/assistant/`, `feature/alerts/` |
| 5 | Presupuestos, Historial | `feature/budget/`, `feature/history/` |

Una rama por persona (`feature/p2-dashboard-ui`...), Pull Requests pequeños y `core/` + `navigation/`
revisados por el responsable técnico. Guía completa: [docs/TEAM_WORKFLOW.md](docs/TEAM_WORKFLOW.md).

## 11. Añadir un sensor real sin tocar la UI

1. **ESP32**: envía las lecturas a la laptop (HTTP o MQTT, a decidir).
2. **Backend**: crea un proveedor que cumpla `DataProvider` (`backend/app/services/providers.py`),
   por ejemplo `SensorDataProvider`, que convierta los datos del INA226/PZEM/YF-S401 a los mismos
   diccionarios que devuelve el simulador (`voltage`, `current`, `power`, `flow_liters_per_minute`...).
   Selecciónalo con `SMARTHOME_DATA_MODE`.
3. **Android**: nada. La app ya consume el contrato REST en modo API.

Si cambia el sensor (p. ej. INA226 → PZEM-004T), solo cambia ese proveedor del backend.

## 12. Endpoints principales

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/health` | Estado del backend (público) |
| POST | `/api/auth/login` · `/api/auth/register` | Devuelven `token` + `user` |
| GET | `/api/auth/me` · POST `/api/auth/logout` | Usuario actual / cerrar sesión |
| GET | `/api/dashboard` | Resumen de la casa |
| GET | `/api/electricity/current` · `/api/electricity/history?period=` | Electricidad |
| GET | `/api/water/current` · `/api/water/history?period=` | Agua |
| GET | `/api/devices` · `/api/devices/{id}` | Dispositivos estimados |
| GET | `/api/alerts` · PATCH `/api/alerts/{id}/read` | Alertas |
| GET/POST | `/api/budgets` · PUT `/api/budgets/{id}` | Presupuestos |
| GET | `/api/history?period=day\|week\|month` | Historial agregado |
| POST | `/api/assistant/chat` | Asistente |
| GET | `/api/system/status` | Estado de conexiones (público) |

Contrato completo con ejemplos: [docs/API.md](docs/API.md).
