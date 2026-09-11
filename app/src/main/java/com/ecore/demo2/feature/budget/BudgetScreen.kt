package com.ecore.demo2.feature.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecore.demo2.core.model.Budget
import com.ecore.demo2.core.ui.LoadState
import com.ecore.demo2.core.ui.components.LoadStateContent
import com.ecore.demo2.core.ui.components.ScreenScaffold
import com.ecore.demo2.core.ui.preview.PreviewData
import com.ecore.demo2.core.ui.theme.SmartHomeTheme
import com.ecore.demo2.feature.budget.components.BudgetItem

@Composable
fun BudgetRoute(onBack: () -> Unit, viewModel: BudgetViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BudgetScreen(
        state = state,
        onRefresh = viewModel::refresh,
        onEdit = viewModel::startEditing,
        onLimitInputChange = viewModel::onLimitInputChange,
        onSave = viewModel::saveLimit,
        onCancel = viewModel::cancelEditing,
        onBack = onBack,
    )
}

// ---- Diseño (Persona 5) ----

@Composable
fun BudgetScreen(
    state: BudgetUiState,
    onRefresh: () -> Unit,
    onEdit: (Budget) -> Unit,
    onLimitInputChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onBack: () -> Unit,
) {
    ScreenScaffold(
        title = "Presupuestos",
        onBack = onBack,
        actions = { TextButton(onClick = onRefresh) { Text("Actualizar") } },
    ) { padding ->
        LoadStateContent(
            state = state.budgets,
            onRetry = onRefresh,
            modifier = Modifier.padding(padding).imePadding(),
            emptyMessage = "No hay presupuestos configurados.",
        ) { budgets ->
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(budgets, key = { it.id }) { budget ->
                    val isEditing = state.editingId == budget.id
                    BudgetItem(
                        budget = budget,
                        isEditing = isEditing,
                        limitInput = state.limitInput,
                        isSaving = state.isSaving,
                        editError = if (isEditing) state.editError else null,
                        onEdit = { onEdit(budget) },
                        onLimitInputChange = onLimitInputChange,
                        onSave = onSave,
                        onCancel = onCancel,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BudgetScreenPreview() {
    SmartHomeTheme {
        BudgetScreen(BudgetUiState(LoadState.Success(PreviewData.budgets)), {}, {}, {}, {}, {}, {})
    }
}
