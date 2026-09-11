package com.ecore.demo2.feature.assistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecore.demo2.core.model.AssistantMessage
import com.ecore.demo2.core.model.MessageRole
import com.ecore.demo2.core.ui.components.ScreenScaffold
import com.ecore.demo2.core.ui.components.SectionCard
import com.ecore.demo2.core.ui.preview.PreviewData
import com.ecore.demo2.core.ui.theme.SmartHomeTheme

@Composable
fun AssistantRoute(viewModel: AssistantViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AssistantScreen(
        state = state,
        onInputChange = viewModel::onInputChange,
        onSend = viewModel::send,
        onClear = viewModel::clearConversation,
    )
}

// ---- Diseño (Persona 4) ----

@Composable
fun AssistantScreen(
    state: AssistantUiState,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onClear: () -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }

    ScreenScaffold(
        title = "Asistente IA",
        actions = { TextButton(onClick = onClear) { Text("Limpiar") } },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.messages, key = { it.id }) { message -> MessageItem(message) }
            }

            if (state.isSending) Text("El asistente está escribiendo...", Modifier.padding(horizontal = 16.dp))
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = state.input,
                    onValueChange = onInputChange,
                    placeholder = { Text("Escribe una pregunta") },
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = onSend, enabled = state.canSend) { Text("Enviar") }
            }
        }
    }
}

@Composable
private fun MessageItem(message: AssistantMessage) {
    val author = if (message.role == MessageRole.USER) "Tú" else "Asistente"
    SectionCard(title = author) { Text(message.text) }
}

@Preview(showBackground = true)
@Composable
private fun AssistantScreenPreview() {
    SmartHomeTheme { AssistantScreen(AssistantUiState(PreviewData.messages, input = "¿Y el agua?"), {}, {}, {}) }
}
