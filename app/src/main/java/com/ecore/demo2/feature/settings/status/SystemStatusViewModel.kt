package com.ecore.demo2.feature.settings.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecore.demo2.core.datastore.SettingsRepository
import com.ecore.demo2.core.repository.SmartHomeRepository
import com.ecore.demo2.core.ui.LoadState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SystemStatusViewModel @Inject constructor(
    private val repository: SmartHomeRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SystemStatusUiState())
    val uiState: StateFlow<SystemStatusUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings -> _uiState.update { it.copy(backendUrl = settings.backendUrl) } }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(status = LoadState.Loading) }
            // getSystemStatus nunca lanza: si no hay backend devuelve "desconectado".
            _uiState.update { it.copy(status = LoadState.Success(repository.getSystemStatus())) }
        }
    }
}
