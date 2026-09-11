package com.ecore.demo2.feature.budget

import com.ecore.demo2.core.model.Budget
import com.ecore.demo2.core.ui.LoadState

data class BudgetUiState(
    val budgets: LoadState<List<Budget>> = LoadState.Loading,
    /** Id del presupuesto cuyo límite se está editando, o null. */
    val editingId: String? = null,
    val limitInput: String = "",
    val isSaving: Boolean = false,
    val editError: String? = null,
)
