package com.ecore.demo2.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecore.demo2.core.model.HistoryPeriod
import com.ecore.demo2.core.model.HistoryRecord
import com.ecore.demo2.core.repository.SmartHomeRepository
import com.ecore.demo2.core.ui.LoadState
import com.ecore.demo2.core.ui.dataOrNull
import com.ecore.demo2.core.ui.loadCatching
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: SmartHomeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        refresh()
    }

    fun selectPeriod(period: HistoryPeriod) {
        if (period == _uiState.value.period) return
        _uiState.update { it.copy(period = period) }
        refresh()
    }

    fun refresh() {
        val period = _uiState.value.period
        // Si el usuario cambia rápido de filtro, se cancela la carga anterior.
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(records = LoadState.Loading, totals = null) }
            val result = loadCatching(isEmpty = { it.isEmpty() }) { repository.getHistory(period) }
            _uiState.update { it.copy(records = result, totals = result.dataOrNull?.let(::totalsOf)) }
        }
    }

    private fun totalsOf(records: List<HistoryRecord>) = HistoryTotals(
        electricityKwh = records.sumOf { it.electricityKwh },
        waterLiters = records.sumOf { it.waterLiters },
        estimatedCost = if (records.all { it.estimatedCost != null }) records.sumOf { it.estimatedCost ?: 0.0 } else null,
    )
}
