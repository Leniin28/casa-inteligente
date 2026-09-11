# Flujo de trabajo del equipo

## Reparto

| Persona | Pantallas | Archivos que puede modificar libremente |
|---|---|---|
| 1 | Splash, Login, Registro, Recuperar contraseña, Perfil | `feature/auth/**` |
| 2 | Dashboard, Electricidad | `feature/dashboard/**`, `feature/electricity/**` |
| 3 | Agua, Dispositivos | `feature/water/**`, `feature/devices/**` |
| 4 | Asistente IA, Alertas | `feature/assistant/**`, `feature/alerts/**` |
| 5 | Presupuestos, Historial | `feature/budget/**`, `feature/history/**` |
| Responsable técnico | Base técnica, Ajustes, menús, backend | `core/**`, `navigation/**`, `feature/settings/**`, `feature/menu/**`, `backend/**`, Gradle |

Recursos compartidos (`core/ui/theme`, `core/ui/components`, `res/drawable`): se pueden cambiar,
pero avisando al equipo, porque afectan a todas las pantallas.

## Cómo diseñar una pantalla

Cada `XxxScreen.kt` tiene dos partes:

```kotlin
// ---- Conexión con el ViewModel ----   ← normalmente no hace falta tocarla
@Composable fun XxxRoute(..., viewModel: XxxViewModel = hiltViewModel()) { ... }

// ---- Diseño (Persona N) ----          ← aquí se trabaja
@Composable fun XxxScreen(state: XxxUiState, onAlgo: () -> Unit, ...) { ... }

@Preview @Composable private fun XxxScreenPreview() { ... PreviewData ... }
```

- Trabaja en `XxxScreen` y en la carpeta `components/` de tu feature.
- Usa el **@Preview** con `PreviewData` para diseñar sin arrancar la app.
- Todos los datos llegan en `state`. Si necesitas un dato que no está, **pídelo** al responsable técnico
  (se añade al `UiState`/ViewModel), no lo calcules en la pantalla.
- No llames a repositorios, Retrofit, Room ni DataStore desde un Composable.
- Los formatos (`12.5 W`, `0.182 kWh`) salen de `Format`; los textos de estados, de `Labels.kt`.
- Puedes cambiar la distribución de `LoadStateContent`, o sustituirlo por un `when (state)` propio si
  quieres vistas de carga/error específicas.

## Ramas

- `master`: siempre compila y pasa los tests.
- Una rama por persona y tarea: `feature/p1-login-ui`, `feature/p2-dashboard-ui`, `feature/p4-alerts-cards`...
- Cambios de base técnica: `feature/core-...` (responsable técnico).

```bash
git switch master && git pull
git switch -c feature/p3-water-ui
# ... trabajo ...
git add app/src/main/java/com/ecore/demo2/feature/water
git commit -m "feat(water): diseño de tarjetas de caudal"
git push -u origin feature/p3-water-ui   # y abrir Pull Request
```

## Pull Requests

Checklist antes de pedir revisión:

- [ ] `.\gradlew.bat :app:assembleDebug` compila.
- [ ] `.\gradlew.bat :app:testDebugUnitTest` pasa.
- [ ] Solo toco archivos de mi feature (o he avisado si toco algo compartido).
- [ ] La pantalla sigue mostrando carga, error y vacío.
- [ ] El `@Preview` funciona.
- [ ] No he añadido archivos de `build/`, `.idea/workspace.xml`, `local.properties` ni secretos.

PR pequeños (una pantalla o componente). Los cambios en `core/` o `navigation/` los revisa el
responsable técnico.

## Mensajes de commit

Formato corto tipo *Conventional Commits*:

- `feat(dashboard): tarjeta de potencia actual`
- `fix(login): error al girar la pantalla`
- `style(alerts): colores por severidad`
- `docs: explicar configuración de IP`
- `test(history): filtro por mes`

## Probar con backend real

1. Arranca el backend (`backend/README.md`).
2. En la app: Ajustes → Fuente de datos → **Casa real (API)**.
3. Emulador: `http://10.0.2.2:8000/`. Móvil: IP de la laptop (ver README, sección 9).
