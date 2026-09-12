# Integrante 2: Dashboard y Electricidad

**Rama:** `feature/dashboard-electricity-ui`

## Archivos

| Puede modificar | Puede crear | No debe modificar |
|---|---|---|
| `feature/dashboard/DashboardScreen.kt` (parte de diseño y previews) | `feature/dashboard/components/*.kt` | `DashboardViewModel.kt`, `DashboardUiState.kt` |
| `feature/electricity/ElectricityScreen.kt` (parte de diseño y previews) | `feature/electricity/components/*.kt` | `ElectricityViewModel.kt`, `ElectricityUiState.kt` |
| | Iconos `res/drawable/ic_dashboard_*.xml`, `ic_electricity_*.xml` | `core/**` (incluidos `core/ui/components` y `core/ui/format`), `navigation/**`, `backend/**`, Gradle, `strings.xml` |

En la parte `XxxRoute` **no quites** `AutoRefreshEffect(...)`: es lo que refresca los datos cada 5 s
mientras la pantalla está visible.

---

## Dashboard

- **Archivo:** `feature/dashboard/DashboardScreen.kt`
- **Firma:** `DashboardScreen(state, onRetry, onOpenElectricity, onOpenWater, onOpenDevices, onOpenAlerts, onOpenBudgets, onOpenSystemStatus)`
- Hoy el contenido está en la función `private` `DashboardContent(...)` del mismo archivo: puedes
  reescribirla o moverla a `components/`.

**UI STATE: `DashboardUiState`**

| Propiedad | Tipo | Uso |
|---|---|---|
| `houseName` | `String` | Nombre de la casa (Ajustes → Configuración de casa); hoy es el título |
| `summary` | `LoadState<DashboardSummary>` | Resumen de la casa |

**Datos dentro de `DashboardSummary`** (cuando `summary` es `Success`):

| Campo | Tipo | Ejemplo de uso |
|---|---|---|
| `electricity.power` | `Double` (W) | Potencia actual |
| `electricity.voltage`, `electricity.current` | `Double` (V, A) | Detalle eléctrico |
| `electricity.energyTodayKwh` | `Double` (kWh) | Consumo de hoy |
| `electricity.timestamp` | `Instant` | Hora de la lectura |
| `water.flowLitersPerMinute` | `Double` (L/min) | Caudal actual |
| `water.litersToday` | `Double` (L) | Agua de hoy |
| `activeDevices` | `Int` | Dispositivos activos estimados |
| `unreadAlerts` | `Int` | Alertas sin leer |
| `budgets` | `List<Budget>` | Presupuestos (`resourceType`, `limit`, `currentUsage`, `estimatedFinalUsage`, `estimatedLimitDate`, `usedFraction`, `isProjectedOverLimit`) |
| `systemStatus` | `SystemStatus` | `backendConnected`, `esp32Connected`, `lastUpdate`, `dataSource`, `backendMode` |

Formateo listo en `core/ui/format`: `Format.watts(...)`, `Format.kwh(...)`, `Format.flow(...)`,
`Format.liters(...)`, `Format.percent(budget.usedFraction)`, `Format.time(...)`, `budget.resourceType.label`.

**ACCIONES**

| Callback | Qué hace |
|---|---|
| `onRetry()` | Recarga (`DashboardViewModel.refresh()`) |
| `onOpenElectricity()`, `onOpenWater()`, `onOpenDevices()`, `onOpenAlerts()`, `onOpenBudgets()`, `onOpenSystemStatus()` | Navegan a esas pantallas |

**ESTADOS A REPRESENTAR:** `Loading`, `Success` (y `fromCache = true` → aviso sin conexión), `Error`.
(`Empty` no se produce en el Dashboard.)

**PREVIEW:** `DashboardScreenPreview` → `DashboardUiState("Mi casa", LoadState.Success(PreviewData.dashboard))`.

### Contrato de diseño: Dashboard

- **Entrada:** `DashboardUiState`
- **Puede mostrar:** nombre de la casa, potencia actual, consumo eléctrico de hoy, caudal y litros de
  hoy, dispositivos activos, alertas sin leer, % de uso de cada presupuesto, estado backend/ESP32 y
  hora de actualización.
- **Acciones:** reintentar, abrir Electricidad, Agua, Dispositivos, Alertas, Presupuestos, Estado del sistema.
- **Debe manejar:** loading, success (con/sin caché), error.
- **No conoce:** Retrofit, el sensor físico (INA226/YF-S401...), la base de datos, si es Demo o API.

---

## Electricidad

- **Archivo:** `feature/electricity/ElectricityScreen.kt`
- **Firma:** `ElectricityScreen(state, onRetry, onOpenHistory, onBack)`

**UI STATE: `ElectricityUiState`**

| Propiedad | Tipo |
|---|---|
| `reading` | `LoadState<ElectricalReading>` |

**Datos de `ElectricalReading`:** `power` (W), `voltage` (V), `current` (A), `energyTodayKwh` (kWh), `timestamp` (`Instant`).
Formato: `Format.watts`, `Format.volts`, `Format.amps`, `Format.kwh`, `Format.time`.

**ACCIONES:** `onRetry()` (recarga), `onOpenHistory()` (abre Historial), `onBack()`.

**ESTADOS A REPRESENTAR:** `Loading`, `Success` (con aviso si `fromCache`), `Error`. Se refresca cada 5 s
sin volver a mostrar "cargando".

**PREVIEW:** `ElectricityScreenPreview` → `LoadState.Success(PreviewData.electricity)`.

### Contrato de diseño: Electricidad

- **Entrada:** `ElectricityUiState`
- **Puede mostrar:** potencia actual, voltaje, corriente, consumo de hoy, hora de la lectura.
- **Acciones:** reintentar, ir al historial, volver.
- **Debe manejar:** loading, success (con/sin caché), error.
- **No conoce:** Retrofit, INA226/PZEM, Room, si es Demo o API.

## Tests relevantes

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.ecore.demo2.feature.electricity.*" --tests "com.ecore.demo2.architecture.*"
```

## Checklist de entrega

- [ ] Estoy en `feature/dashboard-electricity-ui`
- [ ] Solo modifiqué `feature/dashboard/**`, `feature/electricity/**` (y, si acaso, iconos nuevos con mi prefijo)
- [ ] `DashboardScreenPreview` y `ElectricityScreenPreview` funcionan
- [ ] `.\gradlew.bat :app:assembleDebug` pasa
- [ ] Tests relevantes pasan (incluido `UiArchitectureTest`)
- [ ] Funciona con Demo y con API; los valores cambian cada 5 s
- [ ] Error y aviso "sin conexión" siguen visibles (modo API con el backend apagado)
- [ ] Todos los accesos del Dashboard siguen navegando
- [ ] Modo oscuro y pantalla pequeña razonables
- [ ] No agregué dependencias, no hice network calls desde UI, no modifiqué backend
- [ ] Revisé `git diff`, hice commits claros y el working tree quedó limpio
