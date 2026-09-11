package com.ecore.demo2.core.repository

import com.ecore.demo2.core.model.AssistantMessage
import com.ecore.demo2.core.model.MessageRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Conversación con el asistente. En modo API llama a POST /api/assistant/chat,
 * que en el futuro conectará con la IA local de la laptop.
 */
interface AssistantRepository {
    val messages: StateFlow<List<AssistantMessage>>

    /** Añade el mensaje del usuario y devuelve la respuesta del asistente. */
    suspend fun send(text: String): AssistantMessage

    fun clear()
}

@Singleton
class DefaultAssistantRepository @Inject constructor(
    private val dataSources: DataSourceProvider,
    private val clock: Clock,
) : AssistantRepository {

    private val _messages = MutableStateFlow(listOf(welcomeMessage()))
    override val messages: StateFlow<List<AssistantMessage>> = _messages.asStateFlow()

    override suspend fun send(text: String): AssistantMessage {
        val previous = _messages.value
        val userMessage = AssistantMessage(
            id = UUID.randomUUID().toString(),
            role = MessageRole.USER,
            text = text.trim(),
            timestamp = clock.instant(),
        )
        _messages.update { it + userMessage }
        val reply = dataSources.current().sendAssistantMessage(userMessage.text, previous)
        _messages.update { it + reply }
        return reply
    }

    override fun clear() {
        _messages.value = listOf(welcomeMessage())
    }

    private fun welcomeMessage() = AssistantMessage(
        id = "welcome",
        role = MessageRole.ASSISTANT,
        text = "Hola, soy el asistente de tu casa. Pregúntame por tu consumo de electricidad, " +
            "de agua, tus presupuestos o consejos de ahorro.",
        timestamp = clock.instant(),
    )
}
