package com.ecore.demo2.feature.settings.status

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
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
import com.ecore.demo2.core.ui.components.SectionCard
import com.ecore.demo2.core.ui.components.ValueRow
import com.ecore.demo2.core.ui.format.Format
import com.ecore.demo2.core.ui.format.label
import com.ecore.demo2.core.ui.preview.PreviewData
import com.ecore.demo2.core.ui.theme.SmartHomeTheme

@Composable
fun SystemStatusRoute(onBack: () -> Unit, viewModel: SystemStatusViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SystemStatusScreen(state, onRefresh = viewModel::refresh, onBack = onBack)
}

@Composable
fun SystemStatusScreen(state: SystemStatusUiState, onRefresh: () -> Unit, onBack: () -> Unit) {
    ScreenScaffold(title = "Estado del sistema", onBack = onBack) { padding ->
        LoadStateContent(state.status, onRetry = onRefresh, modifier = Modifier.padding(padding)) { status ->
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionCard(title = "Conexiones") {
                    ValueRow("Fuente de datos", status.dataSource.label)
                    status.backendMode?.let { ValueRow("Modo del backend", it) }
                    ValueRow("Backend", if (status.backendConnected) "Conectado" else "Sin conexión")
                    ValueRow("ESP32", if (status.esp32Connected) "Conectado" else "Sin conexión")
                    ValueRow("Última actualización", status.lastUpdate?.let(Format::dateTime) ?: "—")
                }
                SectionCard(title = "Backend configurado") { Text(state.backendUrl) }
                Button(onClick = onRefresh) { Text("Comprobar de nuevo") }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SystemStatusScreenPreview() {
    SmartHomeTheme {
        SystemStatusScreen(SystemStatusUiState(LoadState.Success(PreviewData.systemStatus), "http://10.0.2.2:8000/"), {}, {})
    }
}
