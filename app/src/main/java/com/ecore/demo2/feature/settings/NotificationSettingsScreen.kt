package com.ecore.demo2.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecore.demo2.core.model.AppSettings
import com.ecore.demo2.core.ui.components.ScreenScaffold
import com.ecore.demo2.core.ui.theme.SmartHomeTheme

@Composable
fun NotificationSettingsRoute(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    NotificationSettingsScreen(state, viewModel::setNotificationsEnabled, onBack)
}

/** Por ahora solo guarda la preferencia; el envío de notificaciones llegará en otra fase. */
@Composable
fun NotificationSettingsScreen(
    state: SettingsUiState,
    onNotificationsChange: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    ScreenScaffold(title = "Notificaciones", onBack = onBack) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Notificaciones de alertas")
                Switch(
                    checked = state.settings?.notificationsEnabled ?: true,
                    onCheckedChange = onNotificationsChange,
                    enabled = state.settings != null,
                )
            }
            Text(
                "Recibirás avisos de consumo alto, posibles fugas y presupuestos.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NotificationSettingsScreenPreview() {
    SmartHomeTheme {
        NotificationSettingsScreen(SettingsUiState(AppSettings(backendUrl = "http://10.0.2.2:8000/")), {}, {})
    }
}
