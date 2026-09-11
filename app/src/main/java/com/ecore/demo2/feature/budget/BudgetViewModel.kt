package com.ecore.demo2.feature.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecore.demo2.core.model.Budget
import com.ecore.demo2.core.repository.SmartHomeRepository
import com.ecore.demo2.core.ui.LoadState
import com.ecore.demo2.core.ui.dataOrNull
import com.ecore.demo2.core.ui.loadCatching
import com.ecore.demo2.core.util.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Las proyecciones (estimado final, fecha de límite) vienen calculadas del repositorio/backend. */
@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val repository: SmartHomeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(budgets = LoadState.Loading) }
            val result = loadCatching(isEmpty = { it.isEmpty() }) { repository.getBudgets() }
            _uiState.update { it.copy(budgets = result) }
        }
    }

    fun startEditing(budget: Budget) = _uiState.update {
        it.copy(editingId = budget.id, limitInput = budget.limit.toString(), editError = null)
    }

    fun onLimitInputChange(value: String) = _uiState.update { it.copy(limitInput = value, editError = null) }

    fun cancelEditing() = _uiState.update { it.copy(editingId = null, limitInput = "", editError = null) }

    fun saveLimit() {
        val state = _uiState.value
        val budget = state.budgets.dataOrNull?.firstOrNull { it.id == state.editingId } ?: return
        val newLimit = state.limitInput.replace(',', '.').toDoubleOrNull()
        if (newLimit == null || newLimit <= 0) {
            _uiState.update { it.copy(editError = "Introduce un límite mayor que 0.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, editError = null) }
            try {
                val updated = repository.updateBudgetLimit(budget, newLimit)
                _uiState.update { current ->
                    val list = current.budgets.dataOrNull.orEmpty().map { if (it.id == updated.id) updated else it }
                    current.copy(budgets = LoadState.Success(list), isSaving = false, editingId = null, limitInput = "")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, editError = e.toUserMessage()) }
            }
        }
    }
}
