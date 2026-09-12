# Reglas de UI

## ✅ Se puede (en tus archivos)

- Cambiar `Column`, `Row`, `Box`, `LazyColumn`, `LazyVerticalGrid`...
- Tamaños, `padding`, espaciados, alineaciones, formas.
- Colores **usando `MaterialTheme.colorScheme`** (o colores propios de tu feature si hace falta).
- Tipografía usando `MaterialTheme.typography`.
- `Card`, `ElevatedCard`, `Surface`, `Divider`, `LinearProgressIndicator`, chips, etc. de Material 3.
- Iconos: vectores nuevos en `res/drawable/` con prefijo de tu feature (p. ej. `ic_water_drop.xml`).
- Reorganizar la información de la pantalla.
- Crear componentes Compose reutilizables **dentro de tu feature**: `feature/<tu-feature>/components/*.kt`.
- Mejorar accesibilidad: `contentDescription`, tamaños táctiles de 48 dp, contraste.
- Añadir más `@Preview` (estados de carga/error/vacío, modo oscuro, pantalla pequeña).
- Reemplazar `LoadStateContent` por un `when (state)` propio en tu pantalla.
- Usar solo lo que ya está en Material 3 y Compose (sin nuevas librerías).

## ⛔ No se puede sin consultar al responsable técnico

- Modificar modelos de dominio (`core/model/`).
- Modificar DTOs, Retrofit o endpoints (`core/network/`).
- Modificar Room (`core/database/`) o DataStore (`core/datastore/`).
- Modificar DI (`core/di/`) o repositorios (`core/repository/`).
- Modificar ViewModels o UiStates (si necesitas un dato: *Solicitud de dato*, abajo).
- Cambiar la navegación central (`navigation/`, `MainActivity.kt`).
- Añadir o actualizar librerías en Gradle (`build.gradle.kts`, `libs.versions.toml`).
- Modificar el backend (`backend/`).
- Cambiar autenticación o el cambio Demo/API.
- Cambiar componentes compartidos (`core/ui/`) o `strings.xml` (ver "Archivos compartidos").

## 🚫 Nunca en una pantalla

- Llamadas de red, a la base de datos o al backend desde un `@Composable`.
- `hiltViewModel()` dentro de `XxxScreen` o de un componente (solo en `XxxRoute`, que ya existe).
- ViewModels nuevos "para una sola cosa visual".
- Copias de modelos (`data class MiDevice(...)`) en lugar de usar los de `core/model`.
- Datos inventados fijos en la pantalla real (los datos de ejemplo solo en `@Preview`).
- Cálculos de negocio (proyecciones, totales, conversiones de consumo). Formatear para mostrar sí.
- Arreglar un problema visual cambiando un Repository.

`UiArchitectureTest` (en `app/src/test/.../architecture/`) falla si una pantalla o componente importa
repositorios, red, base de datos, DataStore, DI o navegación. Si falla, revisa tus imports.

## Archivos compartidos (alto riesgo de conflicto)

| Archivo / carpeta | Por qué es delicado | Qué hacer |
|---|---|---|
| `core/ui/theme/` (`Color.kt`, `Theme.kt`, `Type.kt`) | Afecta a toda la app | Cambios globales solo coordinados con el responsable técnico. Nada de "5 temas" |
| `core/ui/components/` (`ScreenScaffold`, `SectionCard`, `ValueRow`, `MetricBlock`, `OptionSelector`, `StateViews`, `AutoRefreshEffect`) | Los usan todas las pantallas | No los edites. Si quieres otra variante, crea la tuya en `feature/<tuya>/components/` |
| `core/ui/format/` (`Format`, `Labels.kt`) | Unidades y textos de enums de toda la app | No los edites. Si necesitas otro formato solo visual, una función `private` en tu feature |
| `core/ui/preview/PreviewData.kt` | Todos lo leen | No lo edites. Crea datos de ejemplo `private` en tu propio archivo |
| `res/values/strings.xml`, `themes.xml` | Un solo archivo para todos | No añadas entradas en esta fase |
| `res/drawable/` | Nombres pueden chocar | Solo **añadir** archivos nuevos con prefijo de tu feature. No modificar `ic_nav_*` ni `ic_arrow_back` |
| `navigation/`, `MainActivity.kt` | Navegación y permisos globales | Solo el responsable técnico |
| `build.gradle.kts`, `gradle/libs.versions.toml` | Dependencias de todos | Solo el responsable técnico |

**Colores propios de una feature:** si tu diseño necesita un color concreto (p. ej. azul agua),
defínelo en `feature/<tuya>/components/<Feature>Colors.kt` como `val WaterBlue = Color(...)` y úsalo
solo en tu feature. Si el color debería ser global, pídelo al responsable técnico para añadirlo al tema.

## Solicitud de dato para backend/ViewModel

Si tu `UiState` no trae algo que el diseño necesita, envía esto al responsable técnico
(issue, mensaje o `docs/ui-handoff/requests/<fecha>-<feature>.md`):

```
SOLICITUD DE DATO PARA BACKEND/VIEWMODEL
Pantalla: Electricidad
Dato que necesito: consumo de ayer (kWh) para compararlo con hoy
Dónde lo mostraría: debajo de "Consumo de hoy"
Formato esperado: número con 3 decimales, en kWh
¿Obligatorio o opcional?: opcional
Solicitado por: Integrante 2 (rama feature/dashboard-electricity-ui)
```

Mientras tanto, diseña el hueco usando una `@Preview` con datos inventados **solo en la preview**.
