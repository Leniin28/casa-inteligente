package com.ecore.demo2.feature.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecore.demo2.core.repository.SmartHomeRepository
import com.ecore.demo2.core.ui.LoadState
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

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val repository: SmartHomeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(alerts = LoadState.Loading, actionError = null) }
            val result = loadCatching(isEmpty = { it.isEmpty() }) { repository.getAlerts() }
            _uiState.update { it.copy(alerts = result) }
        }
    }

    fun markAsRead(id: String) {
        viewModelScope.launch {
            try {
                repository.markAlertRead(id)
                _uiState.update { state ->
                    val current = state.alerts
                    if (current !is LoadState.Success) return@update state
                    val updated = current.data.map { if (it.id == id) it.copy(read = true) else it }
                    state.copy(alerts = current.copy(data = updated), actionError = null)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(actionError = e.toUserMessage()) }
            }
        }
    }
}
