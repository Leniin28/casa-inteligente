package com.ecore.demo2.feature.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecore.demo2.core.repository.AssistantRepository
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
class AssistantViewModel @Inject constructor(
    private val assistantRepository: AssistantRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            assistantRepository.messages.collect { messages -> _uiState.update { it.copy(messages = messages) } }
        }
    }

    fun onInputChange(value: String) = _uiState.update { it.copy(input = value, error = null) }

    fun send() {
        val state = _uiState.value
        if (!state.canSend) return
        val text = state.input
        _uiState.update { it.copy(input = "", isSending = true, error = null) }
        viewModelScope.launch {
            try {
                assistantRepository.send(text)
                _uiState.update { it.copy(isSending = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isSending = false, error = e.toUserMessage()) }
            }
        }
    }

    fun clearConversation() = assistantRepository.clear()
}
