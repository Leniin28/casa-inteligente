package com.ecore.demo2.feature.alerts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecore.demo2.core.ui.LoadState
import com.ecore.demo2.core.ui.components.LoadStateContent
import com.ecore.demo2.core.ui.components.ScreenScaffold
import com.ecore.demo2.core.ui.preview.PreviewData
import com.ecore.demo2.core.ui.theme.SmartHomeTheme
import com.ecore.demo2.feature.alerts.components.AlertItem

@Composable
fun AlertsRoute(viewModel: AlertsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AlertsScreen(state, onRefresh = viewModel::refresh, onMarkAsRead = viewModel::markAsRead)
}

// ---- Diseño (Persona 4) ----

@Composable
fun AlertsScreen(state: AlertsUiState, onRefresh: () -> Unit, onMarkAsRead: (String) -> Unit) {
    ScreenScaffold(
        title = "Alertas",
        actions = { TextButton(onClick = onRefresh) { Text("Actualizar") } },
    ) { padding ->
        LoadStateContent(
            state = state.alerts,
            onRetry = onRefresh,
            modifier = Modifier.padding(padding),
            emptyMessage = "No hay alertas. Todo en orden.",
        ) { alerts ->
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Text("Sin leer: ${state.unreadCount}", style = MaterialTheme.typography.titleMedium) }
                state.actionError?.let { error ->
                    item { Text(error, color = MaterialTheme.colorScheme.error) }
                }
                items(alerts, key = { it.id }) { alert ->
                    AlertItem(alert, onMarkAsRead = { onMarkAsRead(alert.id) })
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AlertsScreenPreview() {
    SmartHomeTheme { AlertsScreen(AlertsUiState(LoadState.Success(PreviewData.alerts)), {}, {}) }
}
