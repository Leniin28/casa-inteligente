package com.ecore.demo2.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecore.demo2.core.datastore.SettingsRepository
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
class DashboardViewModel @Inject constructor(
    private val repository: SmartHomeRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val autoRefresher = AutoRefresher(viewModelScope) { load(silent = true) }

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update { it.copy(houseName = settings.houseName) }
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { load(silent = false) }
    }

    fun startAutoRefresh() = autoRefresher.start()

    fun stopAutoRefresh() = autoRefresher.stop()

    private suspend fun load(silent: Boolean) {
        if (!silent) _uiState.update { it.copy(summary = LoadState.Loading) }
        val result = loadCatching { repository.getDashboard() }
        _uiState.update { it.copy(summary = if (silent) result.keepDataOnError(it.summary) else result) }
    }
}
