package com.ecore.demo2.core.model

import java.time.Instant

enum class MessageRole { USER, ASSISTANT }

data class AssistantMessage(
    val id: String,
    val role: MessageRole,
    val text: String,
    val timestamp: Instant,
)
