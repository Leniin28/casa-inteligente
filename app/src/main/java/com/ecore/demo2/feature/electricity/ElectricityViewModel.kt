package com.ecore.demo2.feature.electricity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecore.demo2.core.repository.SmartHomeRepository
import com.ecore.demo2.core.ui.AutoRefresher
import com.ecore.demo2.core.ui.LoadState
import com.ecore.demo2.core.ui.keepDataOnError
import com.ecore.demo2.core.ui.loadCatching
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ElectricityViewModel @Inject constructor(
    private val repository: SmartHomeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ElectricityUiState())
    val uiState: StateFlow<ElectricityUiState> = _uiState.asStateFlow()

    private val autoRefresher = AutoRefresher(viewModelScope) { load(silent = true) }

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { load(silent = false) }
    }

    fun startAutoRefresh() = autoRefresher.start()

    fun stopAutoRefresh() = autoRefresher.stop()

    private suspend fun load(silent: Boolean) {
        if (!silent) _uiState.update { it.copy(reading = LoadState.Loading) }
        val result = loadCatching { repository.getElectricity() }
        _uiState.update { it.copy(reading = if (silent) result.keepDataOnError(it.reading) else result) }
    }
}
