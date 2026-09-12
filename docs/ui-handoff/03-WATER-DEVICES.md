# Integrante 3: Agua y Dispositivos conectados

**Rama:** `feature/water-devices-ui`

## Archivos

| Puede modificar | Puede crear | No debe modificar |
|---|---|---|
| `feature/water/WaterScreen.kt` (diseño y previews) | `feature/water/components/*.kt` | `WaterViewModel.kt`, `WaterUiState.kt` |
| `feature/devices/DevicesScreen.kt` (diseño y previews) | `feature/devices/components/*.kt` (nuevos) | `DevicesViewModel.kt`, `DevicesUiState.kt` |
| `feature/devices/components/DeviceItem.kt` | Iconos `res/drawable/ic_water_*.xml`, `ic_devices_*.xml` | `core/**`, `navigation/**`, `backend/**`, Gradle, `strings.xml` |

En `WaterRoute` **no quites** `AutoRefreshEffect(...)` (refresco cada 5 s).

---

## Agua

- **Archivo:** `feature/water/WaterScreen.kt`
- **Firma:** `WaterScreen(state, onRetry, onOpenHistory, onBack)`

**UI STATE: `WaterUiState`**

| Propiedad | Tipo |
|---|---|
| `reading` | `LoadState<WaterReading>` |

**Datos de `WaterReading`:** `flowLitersPerMinute` (L/min, `0.0` = sin consumo ahora), `litersToday` (L), `timestamp` (`Instant`).
Formato: `Format.flow`, `Format.liters`, `Format.time`.

**ACCIONES:** `onRetry()`, `onOpenHistory()`, `onBack()`.

**ESTADOS A REPRESENTAR:** `Loading`, `Success` (aviso si `fromCache`), `Error`. Además, visualmente:
"hay consumo ahora" (`flowLitersPerMinute > 0`) vs. "sin consumo". Se refresca cada 5 s.

**PREVIEW:** `WaterScreenPreview` → `LoadState.Success(PreviewData.water)` (1.2 L/min, 184 L).

### Contrato de diseño: Agua

- **Entrada:** `WaterUiState`
- **Puede mostrar:** caudal actual, litros de hoy, si hay consumo en este momento, hora de la lectura.
- **Acciones:** reintentar, ir al historial, volver.
- **Debe manejar:** loading, success (con/sin caché), error.
- **No conoce:** YF-S401 ni ningún caudalímetro concreto, Retrofit, Room, si es Demo o API.

---

## Dispositivos conectados

- **Archivos:** `feature/devices/DevicesScreen.kt`, `feature/devices/components/DeviceItem.kt`
- **Firma:** `DevicesScreen(state, onRefresh, onBack)` y `DeviceItem(device, modifier)`

**UI STATE: `DevicesUiState`**

| Propiedad | Tipo |
|---|---|
| `devices` | `LoadState<List<Device>>` |

**Datos de cada `Device`:**

| Campo | Tipo | Nota |
|---|---|---|
| `id` | `String` | Úsalo como `key` en `LazyColumn` |
| `name` | `String` | "Ventilador" |
| `type` | `DeviceType` | `LIGHTING, FAN, PUMP, ELECTRONICS, APPLIANCE, OTHER` → texto con `device.type.label` |
| `estimatedPowerWatts` | `Double` | `Format.watts(...)` |
| `status` | `DeviceStatus` | `ACTIVE, PROBABLY_ACTIVE, INACTIVE, UNKNOWN` → `device.status.label` |
| `confidence` | `Double?` | 0.0–1.0, puede ser `null` → `Format.percent(...)` |
| `controllable` | `Boolean` | Hoy siempre `false`: **no** dibujes interruptores de encendido/apagado |

Los dispositivos son **estimados** a partir del consumo, no controlados.

**ACCIONES:** `onRefresh()` (recarga), `onBack()`.

**ESTADOS A REPRESENTAR:** `Loading`, `Success`, `Empty` ("todavía no se ha detectado ningún
dispositivo"), `Error` (esta pantalla **no** tiene caché offline: sin conexión muestra error).

**PREVIEW:** `DevicesScreenPreview` → `LoadState.Success(PreviewData.devices)` (4 dispositivos).

### Contrato de diseño: Dispositivos

- **Entrada:** `DevicesUiState`
- **Puede mostrar:** nombre, tipo, estado estimado, potencia estimada y confianza de cada dispositivo.
- **Acciones:** actualizar, volver.
- **Debe manejar:** loading, success, empty, error.
- **No conoce:** cómo se estiman (NILM futuro), Retrofit, sensores, base de datos.

## Tests relevantes

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.ecore.demo2.architecture.*"
```

(Agua y Dispositivos no tienen tests de ViewModel propios; el test de arquitectura cubre tus archivos.)

## Checklist de entrega

- [ ] Estoy en `feature/water-devices-ui`
- [ ] Solo modifiqué `feature/water/**`, `feature/devices/**` (y, si acaso, iconos nuevos con mi prefijo)
- [ ] `WaterScreenPreview` y `DevicesScreenPreview` funcionan (y previews extra de vacío/error si las añadí)
- [ ] `.\gradlew.bat :app:assembleDebug` pasa
- [ ] Tests relevantes pasan (incluido `UiArchitectureTest`)
- [ ] Funciona con Demo y con API
- [ ] Error, vacío y aviso "sin conexión" siguen visibles
- [ ] Volver e Historial siguen navegando
- [ ] Modo oscuro y pantalla pequeña razonables; listas largas hacen scroll
- [ ] No agregué dependencias, no hice network calls desde UI, no modifiqué backend
- [ ] Revisé `git diff`, hice commits claros y el working tree quedó limpio
