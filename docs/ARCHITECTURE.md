# Arquitectura

## Principios

1. **La UI no contiene lógica de negocio.** Un `Screen` recibe un `UiState` y lambdas; no llama a
   repositorios, Retrofit, Room ni DataStore.
2. **La app no depende del hardware.** Solo modelos abstractos en `core/model`. El backend adapta
   los sensores reales.
3. **Demo y API son intercambiables.** Ambas implementan `SmartHomeDataSource`; las pantallas no
   saben cuál está activa.
4. **Sencillez.** Un único módulo, sin UseCases vacíos. Repositorio → fuente de datos es suficiente.

## Flujo de datos (Android)

```
XxxScreen ──evento──► XxxViewModel ──suspend──► SmartHomeRepository
    ▲                     │                          │
    └──── StateFlow<UiState>                         ├─► DataSourceProvider.current()
                                                     │        ├─ DemoSmartHomeDataSource
                                                     │        └─ ApiSmartHomeDataSource ─► ApiServiceFactory ─► Retrofit
                                                     └─► Room (guarda y lee caché)
```

| Pieza | Archivo | Responsabilidad |
|---|---|---|
| Modelos | `core/model/*` | Datos de dominio inmutables |
| `SmartHomeDataSource` | `core/repository/SmartHomeDataSource.kt` | Contrato común Demo/API |
| `DataSourceProvider` | `core/repository/DataSourceProvider.kt` | Elige la fuente según Ajustes |
| `DemoSimulator` | `core/repository/demo/DemoSimulator.kt` | Datos simulados deterministas |
| `ApiSmartHomeDataSource` | `core/network/` | Llama a la API y mapea DTO → dominio |
| `SmartHomeRepository` | `core/repository/` | Punto de entrada de los ViewModels + caché Room |
| `AuthRepository` | `core/repository/AuthRepository.kt` | Login, registro, logout, sesión |
| `AssistantRepository` | `core/repository/AssistantRepository.kt` | Conversación en memoria |
| `SettingsRepository` | `core/datastore/` | Tema, fuente, URL, notificaciones, nombre de la casa |
| `SessionStore` | `core/datastore/` | Token + datos básicos del usuario (nunca la contraseña) |

## Estados de UI

`core/ui/LoadState.kt` define el estado de cualquier carga:

```kotlin
Loading | Success(data, fromCache) | Empty | Error(message)
```

- `loadCatching { repository.getX() }` convierte el resultado (o la excepción) en `LoadState`.
- `LoadStateContent(state, onRetry) { data -> ... }` pinta carga/vacío/error automáticamente.
- `fromCache = true` → se muestra el aviso "Sin conexión: mostrando los últimos datos guardados".

Los repositorios **lanzan excepciones**; los ViewModels las convierten en texto con
`Throwable.toUserMessage()` (`core/util/AppException.kt`).

## Caché offline (Room)

Solo se guarda lo que aporta valor sin conexión:

- últimas lecturas de electricidad y agua (máx. 100 de cada),
- historial por periodo (día/semana/mes),
- alertas.

Si la fuente falla y hay caché → `DataResult(data, fromCache = true)`. Si no hay caché → error.
Al cambiar entre Demo y API se borra la caché. La base es solo caché, así que ante cambios de esquema
se recrea (`fallbackToDestructiveMigration`).

## Refresco en vivo

Dashboard, Electricidad y Agua se refrescan cada 5 s **solo mientras la pantalla está visible**
(`AutoRefreshEffect` en la pantalla + `AutoRefresher` en el ViewModel). Un fallo durante el refresco
automático no borra los datos que ya se veían.

## Navegación

- Rutas tipadas en `navigation/Routes.kt`.
- Barra inferior (`TopLevelDestination`): **Inicio · Datos · IA · Alertas · Más**.
  - Datos → Electricidad, Agua, Dispositivos, Historial.
  - Más → Presupuestos, Perfil, Ajustes (casa, notificaciones), Estado del sistema, Acerca de.
- `SmartHomeApp` observa la sesión: sin sesión → Login; con sesión → Inicio.
- Las pantallas reciben lambdas (`onBack`, `onOpenHistory`...), nunca el `NavController`.

## Inyección de dependencias (Hilt)

- `core/di/AppModule.kt`: `Clock`, DataStore, Room, OkHttp, JSON.
- `core/di/RepositoryModule.kt`: interfaces → implementaciones.
- ViewModels con `@HiltViewModel`, obtenidos en pantalla con `hiltViewModel()`.

El `Clock` inyectable permite tests reproducibles con una hora fija.

## Red

- Retrofit + kotlinx.serialization con `JsonNamingStrategy.SnakeCase` (JSON en snake_case, Kotlin en camelCase).
- `ApiServiceFactory` reconstruye Retrofit si cambia la URL en Ajustes.
- `AuthInterceptor` añade `Authorization: Bearer <token>`.
- El log HTTP (solo debug) es de nivel BASIC para no mostrar el token.
- `network_security_config.xml` permite HTTP solo hacia hosts locales concretos.

## Backend

```
routers (HTTP) ──► services/providers.DataProvider ──► DemoDataProvider (simulator.py)
      │                                               └─ (futuro) SensorDataProvider ← lecturas del ESP32
      └──► SQLite (usuarios, sesiones, límites de presupuesto, alertas leídas)
```

- `create_app(settings, clock)` permite tests aislados (BD temporal, hora fija).
- Contraseñas con PBKDF2-SHA256 (librería estándar, salt aleatorio); tokens opacos guardados como SHA-256.
- El simulador Python replica al de Android (mismo algoritmo y semilla).

## Dónde va cada cosa

| Quiero... | Archivo |
|---|---|
| Cambiar el aspecto de una pantalla | `feature/<x>/XxxScreen.kt` (y su `components/`) |
| Cambiar colores/tipografía global | `core/ui/theme/` |
| Cambiar cómo se ven carga/error/vacío | `core/ui/components/StateViews.kt` |
| Cambiar unidades o decimales | `core/ui/format/Formatters.kt` |
| Cambiar textos de enums (estados, severidades) | `core/ui/format/Labels.kt` |
| Añadir un campo que manda el backend | DTO + mapper + modelo (revisión del responsable técnico) |
| Añadir una pantalla | `feature/`, ruta en `Routes.kt`, entrada en `AppNavHost.kt` |
