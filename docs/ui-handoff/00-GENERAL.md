# UI Team Handoff: guía general

Esta carpeta explica cómo rediseñar las pantallas de **Casa Inteligente** sin romper la lógica
que ya funciona (datos demo, backend FastAPI, caché offline, sesión).

| Documento | Para quién |
|---|---|
| [01-AUTH.md](01-AUTH.md) | Integrante 1: acceso (Splash, Login, Registro, Recuperar, Perfil) |
| [02-DASHBOARD-ELECTRICITY.md](02-DASHBOARD-ELECTRICITY.md) | Integrante 2: Dashboard y Electricidad |
| [03-WATER-DEVICES.md](03-WATER-DEVICES.md) | Integrante 3: Agua y Dispositivos |
| [04-ASSISTANT-ALERTS.md](04-ASSISTANT-ALERTS.md) | Integrante 4: Asistente IA y Alertas |
| [05-BUDGET-HISTORY.md](05-BUDGET-HISTORY.md) | Integrante 5: Presupuestos e Historial |
| [UI-RULES.md](UI-RULES.md) | Todos: qué se puede y qué no se puede tocar |
| [MERGE-WORKFLOW.md](MERGE-WORKFLOW.md) | Todos: ramas, commits e integración |

---

## 1. La arquitectura en 1 minuto

```
XxxScreen        ← TÚ trabajas aquí: solo dibuja lo que recibe
   ↑ UiState         (datos listos para mostrar)
   ↓ callbacks       (el usuario pulsó algo)
XxxViewModel     ← decide qué pasa (no lo tocas)
   ↓
Repository       ← consigue los datos (no lo tocas)
   ↓
Demo  o  API     ← datos simulados en el móvil, o el backend FastAPI de la laptop
```

- La pantalla **no sabe** si los datos vienen del modo Demo o del backend real.
- La pantalla **no llama** a internet, a la base de datos ni al backend.

**¿Por qué la pantalla no llama a la API directamente?** Porque así:

- tu diseño funciona igual con datos demo, con el backend y sin conexión (caché);
- si cambia el sensor, el backend o la base de datos, tu pantalla no se rompe;
- los errores, la carga y los reintentos ya están resueltos en el ViewModel.

## 2. Conceptos clave

### UiState

Es un `data class` con **todo** lo que la pantalla puede mostrar. Ejemplo real:

```kotlin
data class ElectricityUiState(
    val reading: LoadState<ElectricalReading> = LoadState.Loading,
)
```

Muchas pantallas usan `LoadState`, que tiene 4 casos:

| Caso | Significa | Qué debe mostrar tu diseño |
|---|---|---|
| `LoadState.Loading` | Cargando | Un indicador de carga |
| `LoadState.Success(data, fromCache)` | Hay datos | Los datos. Si `fromCache = true`, un aviso de "sin conexión" |
| `LoadState.Empty` | Cargó bien pero no hay nada | Un mensaje de lista vacía |
| `LoadState.Error(message)` | Falló | El mensaje y un botón para reintentar |

`LoadStateContent(state, onRetry) { data -> ... }` (en `core/ui/components/StateViews.kt`) ya pinta
carga, vacío y error por ti. Puedes seguir usándolo o sustituirlo en tu pantalla por un
`when (state) { ... }` propio si quieres un diseño distinto para cada caso.

### Callback

Es una función que la pantalla recibe como parámetro y llama cuando el usuario hace algo:

```kotlin
fun AlertsScreen(state: AlertsUiState, onRefresh: () -> Unit, onMarkAsRead: (String) -> Unit)
```

Tu botón solo hace `onClick = onRefresh`. Qué ocurre después (llamar al backend, guardar en caché...)
lo decide el ViewModel.

### Route y Screen

Cada `XxxScreen.kt` tiene dos partes:

```kotlin
// ---- Conexión con el ViewModel ----   ← NO la cambies (salvo que te lo indique tu documento)
@Composable fun XxxRoute(..., viewModel: XxxViewModel = hiltViewModel()) { ... }

// ---- Diseño (Persona N) ----          ← AQUÍ trabajas
@Composable fun XxxScreen(state: XxxUiState, onAlgo: () -> Unit, ...) { ... }

@Preview @Composable private fun XxxScreenPreview() { ... }
```

## 3. Previews: diseñar sin ejecutar la app

1. Abre tu `XxxScreen.kt` en Android Studio.
2. Arriba a la derecha, elige la vista **Split** o **Design**.
3. Verás el `@Preview` renderizado con datos de ejemplo de `core/ui/preview/PreviewData.kt`.
4. Tras cambiar código, la preview se actualiza (o pulsa *Build & Refresh*).
5. Con el icono ▶ junto al `@Preview` puedes lanzar **solo esa preview** en el emulador.

Las previews **no necesitan backend ni login**. Puedes añadir más previews en tu archivo para ver
otros estados, por ejemplo:

```kotlin
@Preview(showBackground = true)
@Composable
private fun ElectricityScreenErrorPreview() {
    SmartHomeTheme {
        ElectricityScreen(ElectricityUiState(LoadState.Error("No se pudo conectar")), {}, {}, {})
    }
}
```

Si necesitas datos de ejemplo distintos, créalos **dentro de tu archivo** (`private val ...`) en vez
de editar `PreviewData.kt`, que es compartido.

## 4. Ejecutar la app

- Android Studio → configuración `app` → ▶ en un emulador.
- Por consola (Windows):

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:assembleDebug        # compilar
.\gradlew.bat :app:testDebugUnitTest    # todos los tests unitarios
```

Solo los tests de tu feature (ejemplo):

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.ecore.demo2.feature.history.*"
.\gradlew.bat :app:testDebugUnitTest --tests "com.ecore.demo2.architecture.*"   # fronteras UI/lógica
```

## 5. Modo Demo y modo API

- **Demo** (por defecto): datos simulados dentro del móvil. Sirve para el 95 % del trabajo de diseño.
  Login: `demo@casa.local` / `demo1234` (en demo vale cualquier email válido con 6+ caracteres).
- **API**: datos del backend FastAPI en la laptop. Login → *Configurar servidor / modo demo* →
  *Casa real (API)*. URL del emulador: `http://10.0.2.2:8000/`. En Android 17+ acepta el permiso
  de "Dispositivos cercanos". Cómo arrancar el backend: [README principal](../../README.md).

Los datos demo son **reproducibles**: la misma hora da los mismos valores y cambian con el tiempo
(Dashboard, Electricidad y Agua se refrescan cada 5 s). Son útiles para ver textos largos,
listas y estados reales.

## 6. Comprobar tus cambios visuales

- [ ] La preview se ve bien.
- [ ] En la app (modo Demo) la pantalla muestra datos reales, y navegar hacia/desde ella funciona.
- [ ] Probaste los estados: carga, error (p. ej. modo API con el backend apagado) y vacío si aplica.
- [ ] Modo oscuro (Más → Ajustes → Tema → Oscuro) y una pantalla pequeña (emulador *Small Phone*).
- [ ] `.\gradlew.bat :app:assembleDebug` y `.\gradlew.bat :app:testDebugUnitTest` pasan.

## 7. Commits pequeños y claros

Haz un commit por cambio con sentido propio:

```
style: redesign dashboard layout
style: improve electricity cards
style: redesign water screen
style: improve assistant chat layout
style(alerts): color by severity
docs(budget): add screenshot to handoff
```

**No** uses mensajes como `cambios`, `avance`, `final` o `cosas`: dentro de un mes nadie sabrá
qué contienen.

## 8. Textos (strings)

Hoy los textos de las pantallas están escritos directamente en Compose (`Text("Electricidad")`) y
`res/values/strings.xml` solo contiene `app_name`. En esta fase **mantén esa convención**: escribe
tus textos en tu propia pantalla y **no** añadas entradas a `strings.xml` (es un archivo compartido
y provocaría conflictos entre ramas). La migración a string resources, si se hace, la coordinará el
responsable técnico más adelante.

## 9. ¿Te falta un dato?

Si tu diseño necesita algo que tu `UiState` no tiene (p. ej. "consumo de ayer"), **no lo calcules en
la pantalla**. Escribe una *Solicitud de dato para backend/ViewModel* (plantilla en
[UI-RULES.md](UI-RULES.md)) y pásasela al responsable técnico.
