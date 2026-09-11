package com.ecore.demo2.feature.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecore.demo2.core.repository.SmartHomeRepository
import com.ecore.demo2.core.ui.LoadState
import com.ecore.demo2.core.ui.loadCatching
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Dispositivos detectados/estimados a partir del consumo (no se controlan desde la app). */
@HiltViewModel
class DevicesViewModel @Inject constructor(
    private val repository: SmartHomeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DevicesUiState())
    val uiState: StateFlow<DevicesUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(devices = LoadState.Loading) }
            val result = loadCatching(isEmpty = { it.isEmpty() }) { repository.getDevices() }
            _uiState.update { it.copy(devices = result) }
        }
    }
}
