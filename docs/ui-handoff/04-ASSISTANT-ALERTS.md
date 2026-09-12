# Integrante 4: Asistente IA y Alertas

**Rama:** `feature/assistant-alerts-ui`

## Archivos

| Puede modificar | Puede crear | No debe modificar |
|---|---|---|
| `feature/assistant/AssistantScreen.kt` (diseño y previews) | `feature/assistant/components/*.kt` | `AssistantViewModel.kt`, `AssistantUiState.kt` |
| `feature/alerts/AlertsScreen.kt` (diseño y previews) | `feature/alerts/components/*.kt` (nuevos) | `AlertsViewModel.kt`, `AlertsUiState.kt` |
| `feature/alerts/components/AlertItem.kt` | Iconos `res/drawable/ic_assistant_*.xml`, `ic_alerts_*.xml` | `core/**`, `navigation/**`, `backend/**`, Gradle, `strings.xml` |

---

## Asistente IA

- **Archivo:** `feature/assistant/AssistantScreen.kt`
- **Firma:** `AssistantScreen(state, onInputChange, onSend, onClear)`
- Hoy cada mensaje se dibuja con la función `private` `MessageItem(message)` del mismo archivo
  (puedes rediseñarla o moverla a `components/`).

**UI STATE: `AssistantUiState`**

| Propiedad | Tipo | Uso |
|---|---|---|
| `messages` | `List<AssistantMessage>` | Conversación completa, en orden (el primero es el saludo) |
| `input` | `String` | Texto que el usuario está escribiendo |
| `isSending` | `Boolean` | `true` mientras el asistente responde ("escribiendo...") |
| `error` | `String?` | Error al enviar (listo para mostrar) |
| `canSend` | `Boolean` (calculado) | `input` no vacío y no se está enviando → habilita el botón |

**Cada `AssistantMessage`:** `id` (`String`, úsalo como `key`), `role` (`MessageRole.USER` / `MessageRole.ASSISTANT`), `text`, `timestamp` (`Instant`).

**ACCIONES:** `onInputChange(String)`, `onSend()` (envía `input`), `onClear()` (reinicia la conversación).

**ESTADOS A REPRESENTAR:** conversación normal, enviando (`isSending`), error (`error`), botón deshabilitado
(`!canSend`). No usa `LoadState`: nunca está "vacía" porque siempre hay un mensaje de bienvenida.
Mantén el campo de texto visible con el teclado abierto (`imePadding()`) y el scroll al último mensaje.

**PREVIEW:** `AssistantScreenPreview` → `AssistantUiState(PreviewData.messages, input = "¿Y el agua?")`.

### Contrato de diseño: Asistente IA

- **Entrada:** `AssistantUiState`
- **Puede mostrar:** burbujas de usuario/asistente, texto en edición, indicador "escribiendo", error.
- **Acciones:** escribir, enviar, limpiar conversación.
- **Debe manejar:** normal, enviando, error.
- **No conoce:** si responde el modo demo o la IA local de la laptop, Retrofit, `POST /api/assistant/chat`.

---

## Alertas

- **Archivos:** `feature/alerts/AlertsScreen.kt`, `feature/alerts/components/AlertItem.kt`
- **Firma:** `AlertsScreen(state, onRefresh, onMarkAsRead)` y `AlertItem(alert, onMarkAsRead, modifier)`

**UI STATE: `AlertsUiState`**

| Propiedad | Tipo | Uso |
|---|---|---|
| `alerts` | `LoadState<List<Alert>>` | Lista de alertas (más recientes primero) |
| `actionError` | `String?` | Error al marcar como leída (sin ocultar la lista) |
| `unreadCount` | `Int` (calculado) | Nº de alertas sin leer |

**Cada `Alert`:**

| Campo | Tipo | Nota |
|---|---|---|
| `id` | `String` | `key` de la lista y parámetro de `onMarkAsRead` |
| `type` | `AlertType` | `HIGH_CONSUMPTION, WATER_LEAK, BUDGET, DEVICE, SYSTEM, OTHER` → `alert.type.label` |
| `severity` | `AlertSeverity` | `INFO, WARNING, CRITICAL` → `alert.severity.label` (buen candidato para color/icono) |
| `title`, `message` | `String` | Textos listos |
| `timestamp` | `Instant` | `Format.dateTime(...)` |
| `read` | `Boolean` | Leída o no |

**ACCIONES:** `onRefresh()` (recarga), `onMarkAsRead(id: String)`.

**ESTADOS A REPRESENTAR:** `Loading`, `Success` (aviso si `fromCache`), `Empty` ("no hay alertas"),
`Error`, más `actionError`, y alerta leída vs. no leída.

**PREVIEW:** `AlertsScreenPreview` → `LoadState.Success(PreviewData.alerts)` (3 alertas: aviso, crítica, info leída).

### Contrato de diseño: Alertas

- **Entrada:** `AlertsUiState`
- **Puede mostrar:** nº sin leer, título, mensaje, tipo, severidad, fecha y estado leído de cada alerta.
- **Acciones:** actualizar, marcar como leída.
- **Debe manejar:** loading, success (con/sin caché), empty, error, error de acción.
- **No conoce:** cómo se generan las alertas, Retrofit, Room, notificaciones del sistema.

## Tests relevantes

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.ecore.demo2.architecture.*"
```

## Checklist de entrega

- [ ] Estoy en `feature/assistant-alerts-ui`
- [ ] Solo modifiqué `feature/assistant/**`, `feature/alerts/**` (y, si acaso, iconos nuevos con mi prefijo)
- [ ] `AssistantScreenPreview` y `AlertsScreenPreview` funcionan
- [ ] `.\gradlew.bat :app:assembleDebug` pasa
- [ ] Tests relevantes pasan (incluido `UiArchitectureTest`)
- [ ] Asistente: enviar funciona en Demo y en API; el teclado no tapa el campo; se ve "escribiendo"
- [ ] Alertas: marcar como leída actualiza el contador; vacío, error y "sin conexión" siguen visibles
- [ ] Modo oscuro y pantalla pequeña razonables
- [ ] No agregué dependencias, no hice network calls desde UI, no modifiqué backend
- [ ] Revisé `git diff`, hice commits claros y el working tree quedó limpio
