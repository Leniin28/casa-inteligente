package com.ecore.demo2.feature.assistant

import com.ecore.demo2.core.model.AssistantMessage

data class AssistantUiState(
    val messages: List<AssistantMessage> = emptyList(),
    val input: String = "",
    val isSending: Boolean = false,
    val error: String? = null,
) {
    val canSend: Boolean get() = input.isNotBlank() && !isSending
}
