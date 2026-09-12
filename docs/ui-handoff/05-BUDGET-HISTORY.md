# Integrante 5: Presupuestos e Historial

**Rama:** `feature/budget-history-ui`

## Archivos

| Puede modificar | Puede crear | No debe modificar |
|---|---|---|
| `feature/budget/BudgetScreen.kt` (diseño y previews) | `feature/budget/components/*.kt` (nuevos) | `BudgetViewModel.kt`, `BudgetUiState.kt` |
| `feature/budget/components/BudgetItem.kt` | `feature/history/components/*.kt` | `HistoryViewModel.kt`, `HistoryUiState.kt` |
| `feature/history/HistoryScreen.kt` (diseño y previews) | Iconos `res/drawable/ic_budget_*.xml`, `ic_history_*.xml` | `core/**` (incl. `BudgetCalculator`), `navigation/**`, `backend/**`, Gradle, `strings.xml` |

**Nada de librerías de gráficas en esta fase.** Si quieres una barra o gráfico simple, dibújalo con
Compose (`LinearProgressIndicator`, `Box` con ancho proporcional, `Canvas` básico) **usando solo
los datos que ya recibes**.

---

## Presupuestos

- **Archivos:** `feature/budget/BudgetScreen.kt`, `feature/budget/components/BudgetItem.kt`
- **Firma:** `BudgetScreen(state, onRefresh, onEdit, onLimitInputChange, onSave, onCancel, onBack)`
- `BudgetItem(budget, isEditing, limitInput, isSaving, editError, onEdit, onLimitInputChange, onSave, onCancel, modifier)`

**UI STATE: `BudgetUiState`**

| Propiedad | Tipo | Uso |
|---|---|---|
| `budgets` | `LoadState<List<Budget>>` | Presupuestos (electricidad y agua) |
| `editingId` | `String?` | Id del presupuesto que se está editando (o `null`) |
| `limitInput` | `String` | Texto del campo "nuevo límite" |
| `isSaving` | `Boolean` | Guardando el nuevo límite |
| `editError` | `String?` | Error de validación/guardado del límite |

**Cada `Budget`:**

| Campo | Tipo | Nota |
|---|---|---|
| `id` | `String` | `"electricity"` / `"water"` |
| `resourceType` | `ResourceType` | `ELECTRICITY` / `WATER`; `resourceType.unit` = `"kWh"` / `"L"`; `resourceType.label` |
| `limit`, `currentUsage`, `estimatedFinalUsage` | `Double` | `Format.usage(valor, budget.resourceType)` |
| `estimatedLimitDate` | `LocalDate?` | `null` = no se alcanzará en el periodo → `Format.date(...)` |
| `periodStart`, `periodEnd` | `LocalDate` | Mes actual |
| `usedFraction` | `Double` (calculado) | 0.0–1.0+ para barras de progreso |
| `isProjectedOverLimit` | `Boolean` (calculado) | Aviso "superarás el límite" |

Las proyecciones vienen calculadas del backend/repositorio: **no las recalcules en la pantalla**.

**ACCIONES**

| Callback | Qué hace |
|---|---|
| `onRefresh()` | Recarga |
| `onEdit(budget)` | Abre la edición de ese presupuesto |
| `onLimitInputChange(String)` | Actualiza el campo |
| `onSave()` | Valida y guarda el límite (acepta `7,5` o `7.5`) |
| `onCancel()` | Cancela la edición |
| `onBack()` | Volver |

**ESTADOS A REPRESENTAR:** `Loading`, `Success`, `Empty`, `Error` (sin caché offline), y dentro de
cada presupuesto: normal, editando, guardando, error de edición, proyección por encima del límite.

**PREVIEW:** `BudgetScreenPreview` → `LoadState.Success(PreviewData.budgets)` (electricidad por encima del límite, agua por debajo).

### Contrato de diseño: Presupuestos

- **Entrada:** `BudgetUiState`
- **Puede mostrar:** límite, consumo actual, estimado final, fecha estimada del límite, % usado,
  aviso de exceso, formulario de edición.
- **Acciones:** actualizar, editar, escribir límite, guardar, cancelar, volver.
- **Debe manejar:** loading, success, empty, error, editando/guardando/error de edición.
- **No conoce:** cómo se proyecta el consumo, Retrofit, `PUT /api/budgets/{id}`, base de datos.

---

## Historial

- **Archivo:** `feature/history/HistoryScreen.kt`
- **Firma:** `HistoryScreen(state, onSelectPeriod, onRetry, onBack)`
- Cada fila se dibuja hoy con la función `private` `HistoryRow(record, period)` del mismo archivo.

**UI STATE: `HistoryUiState`**

| Propiedad | Tipo | Uso |
|---|---|---|
| `period` | `HistoryPeriod` | Filtro activo: `DAY`, `WEEK` (por defecto), `MONTH` → `period.label` |
| `records` | `LoadState<List<HistoryRecord>>` | Registros del periodo |
| `totals` | `HistoryTotals?` | Totales del periodo (`null` si no hay datos) |

**Cada `HistoryRecord`:** `timestamp` (`Instant`: inicio de la hora en DAY, del día en WEEK/MONTH),
`electricityKwh` (`Double`), `waterLiters` (`Double`), `estimatedCost` (`Double?`).
Etiqueta lista: `Format.historyLabel(record.timestamp, period)`.

**`HistoryTotals`:** `electricityKwh`, `waterLiters` (`Double`), `estimatedCost` (`Double?`).

Tamaños reales: DAY = 24 registros, WEEK = 7, MONTH = 30.

**ACCIONES:** `onSelectPeriod(HistoryPeriod)` (cambia el filtro y recarga), `onRetry()`, `onBack()`.

**ESTADOS A REPRESENTAR:** `Loading`, `Success` (aviso si `fromCache`), `Empty`, `Error`, más el filtro seleccionado.

**PREVIEW:** `HistoryScreenPreview` → semana con `PreviewData.history` y totales de ejemplo.

### Contrato de diseño: Historial

- **Entrada:** `HistoryUiState`
- **Puede mostrar:** filtro Día/Semana/Mes, totales del periodo, lista de registros (electricidad,
  agua, coste estimado), gráficos simples hechos con Compose a partir de esos mismos datos.
- **Acciones:** cambiar periodo, reintentar, volver.
- **Debe manejar:** loading, success (con/sin caché), empty, error.
- **No conoce:** cómo se agregan los datos, Retrofit, Room, sensores.

## Tests relevantes

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.ecore.demo2.feature.budget.*" --tests "com.ecore.demo2.feature.history.*" --tests "com.ecore.demo2.architecture.*"
```

## Checklist de entrega

- [ ] Estoy en `feature/budget-history-ui`
- [ ] Solo modifiqué `feature/budget/**`, `feature/history/**` (y, si acaso, iconos nuevos con mi prefijo)
- [ ] `BudgetScreenPreview` y `HistoryScreenPreview` funcionan
- [ ] `.\gradlew.bat :app:assembleDebug` pasa
- [ ] Tests relevantes pasan (incluido `UiArchitectureTest`)
- [ ] Editar y guardar un límite funciona en Demo y en API
- [ ] Día/Semana/Mes cambian los datos; vacío, error y "sin conexión" siguen visibles
- [ ] Modo oscuro y pantalla pequeña razonables; el teclado no tapa el campo de límite
- [ ] No agregué dependencias (ni de gráficas), no hice network calls desde UI, no modifiqué backend
- [ ] Revisé `git diff`, hice commits claros y el working tree quedó limpio
