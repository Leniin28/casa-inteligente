# Contrato de la API

Base: `http://<host>:8000`. Documentación interactiva generada por FastAPI en `/docs` (Swagger) y
`/openapi.json`.

## Convenciones

- JSON en **snake_case**.
- Fechas con hora en ISO-8601 UTC: `"2026-09-11T18:30:00Z"`. Fechas sin hora: `"2026-09-30"`.
- Enums en minúsculas: `"probably_active"`, `"critical"`, `"electricity"`.
- Periodos: `day` (24 registros horarios), `week` (7 diarios), `month` (30 diarios).
- Autenticación: `Authorization: Bearer <token>` en todo `/api/*` salvo login, registro,
  password-reset y `/api/system/status`.
- Errores: `{"detail": "mensaje"}` con 401 (sesión), 404 (no existe), 409 (email duplicado), 422 (validación).

Los campos nuevos que añada el backend no rompen la app (se ignoran los desconocidos).

## Sistema

### `GET /health` (público)
```json
{"status": "ok", "app": "Casa Inteligente API", "version": "0.1.0",
 "time": "2026-09-11T18:30:00Z", "data_mode": "demo", "database": "ok"}
```

### `GET /api/system/status` (público)
```json
{"backend_connected": true, "esp32_connected": true,
 "last_update": "2026-09-11T18:30:00Z", "data_source": "demo"}
```
`data_source` es el modo del backend: `demo` (simulado) o, en el futuro, `sensors`.

## Autenticación

| Método | Ruta | Cuerpo | Respuesta |
|---|---|---|---|
| POST | `/api/auth/register` | `{"name", "email", "password"}` (password ≥ 6) | 201 `AuthOut` · 409 si existe |
| POST | `/api/auth/login` | `{"email", "password"}` | 200 `AuthOut` · 401 |
| GET | `/api/auth/me` | — | `{"id", "name", "email"}` |
| POST | `/api/auth/logout` | — | 204 (invalida el token) |
| POST | `/api/auth/password-reset` | `{"email"}` | 202, respuesta idéntica exista o no el email |

```json
// AuthOut
{"token": "q2V...", "user": {"id": "1", "name": "Usuario demo", "email": "demo@casa.local"}}
```

## Datos

### `GET /api/dashboard`
```json
{
  "electricity": {...ElectricalReading},
  "water": {...WaterReading},
  "active_devices": 3,
  "unread_alerts": 2,
  "budgets": [...Budget],
  "system_status": {...SystemStatus}
}
```

### Electricidad — `GET /api/electricity/current`, `GET /api/electricity/history?period=week`
```json
{"timestamp": "2026-09-11T18:30:00Z", "voltage": 12.13, "current": 1.051,
 "power": 12.75, "energy_today_kwh": 0.182}
```

### Agua — `GET /api/water/current`, `GET /api/water/history?period=week`
```json
{"timestamp": "2026-09-11T18:30:00Z", "flow_liters_per_minute": 1.2, "liters_today": 184.0}
```

### Dispositivos — `GET /api/devices`, `GET /api/devices/{id}`
```json
{"id": "fan", "name": "Ventilador", "type": "fan", "estimated_power_watts": 12.0,
 "status": "probably_active", "confidence": 0.88, "controllable": false}
```
`type`: `lighting | fan | pump | electronics | appliance | other`.
`status`: `active | probably_active | inactive | unknown`.
`controllable` queda reservado; hoy siempre es `false` (no hay control físico).

### Alertas — `GET /api/alerts`, `PATCH /api/alerts/{id}/read`
```json
{"id": "alert-water-night", "type": "water_leak", "severity": "critical",
 "title": "Posible fuga de agua", "message": "...", "timestamp": "2026-09-11T13:00:00Z", "read": false}
```
`type`: `high_consumption | water_leak | budget | device | system | other`.
`severity`: `info | warning | critical`. El PATCH devuelve la alerta actualizada.

### Presupuestos — `GET /api/budgets`, `POST /api/budgets`, `PUT /api/budgets/{id}`
```json
{"id": "electricity", "resource_type": "electricity", "limit": 5.0,
 "current_usage": 2.1, "estimated_final_usage": 5.8, "estimated_limit_date": "2026-09-26",
 "period_start": "2026-09-01", "period_end": "2026-09-30"}
```
Cuerpo de POST/PUT: `{"resource_type": "electricity" | "water", "limit": 8.0}` (`limit > 0`).
Hay un presupuesto por recurso; su `id` es el propio recurso. Unidades: kWh y litros.
Proyección: consumo medio diario hasta hoy × días del mes. `estimated_limit_date` es `null` si no se
alcanzará el límite dentro del periodo.

### Historial — `GET /api/history?period=day|week|month`
```json
[{"timestamp": "2026-09-05T00:00:00Z", "electricity_kwh": 0.178, "water_liters": 212.4, "estimated_cost": 0.45}]
```

## Asistente — `POST /api/assistant/chat`
```json
// petición
{"message": "¿Cuánto consumo ahora?", "history": [{"role": "user", "text": "hola"}]}
// respuesta
{"reply": {"id": "uuid", "role": "assistant", "text": "Ahora mismo la casa consume 12.8 W...",
           "timestamp": "2026-09-11T18:30:00Z"}}
```
Hoy responde por reglas. Más adelante este endpoint llamará a la IA local de la laptop sin cambiar
el contrato.
