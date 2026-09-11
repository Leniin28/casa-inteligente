package com.ecore.demo2.feature.electricity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecore.demo2.core.ui.LoadState
import com.ecore.demo2.core.ui.components.AutoRefreshEffect
import com.ecore.demo2.core.ui.components.LoadStateContent
import com.ecore.demo2.core.ui.components.MetricBlock
import com.ecore.demo2.core.ui.components.ScreenScaffold
import com.ecore.demo2.core.ui.format.Format
import com.ecore.demo2.core.ui.preview.PreviewData
import com.ecore.demo2.core.ui.theme.SmartHomeTheme

// ---- Conexión con el ViewModel ----

@Composable
fun ElectricityRoute(
    onOpenHistory: () -> Unit,
    onBack: () -> Unit,
    viewModel: ElectricityViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AutoRefreshEffect(onStart = viewModel::startAutoRefresh, onStop = viewModel::stopAutoRefresh)
    ElectricityScreen(state, onRetry = viewModel::refresh, onOpenHistory = onOpenHistory, onBack = onBack)
}

// ---- Diseño (Persona 2) ----

@Composable
fun ElectricityScreen(
    state: ElectricityUiState,
    onRetry: () -> Unit,
    onOpenHistory: () -> Unit,
    onBack: () -> Unit,
) {
    ScreenScaffold(title = "Electricidad", onBack = onBack) { padding ->
        LoadStateContent(state.reading, onRetry = onRetry, modifier = Modifier.padding(padding)) { reading ->
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MetricBlock("Potencia actual", Format.watts(reading.power))
                MetricBlock("Voltaje", Format.volts(reading.voltage))
                MetricBlock("Corriente", Format.amps(reading.current))
                MetricBlock("Consumo de hoy", Format.kwh(reading.energyTodayKwh))
                Text("Actualizado: ${Format.time(reading.timestamp)}", style = MaterialTheme.typography.bodySmall)
                Button(onClick = onOpenHistory) { Text("Historial") }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ElectricityScreenPreview() {
    SmartHomeTheme {
        ElectricityScreen(ElectricityUiState(LoadState.Success(PreviewData.electricity)), {}, {}, {})
    }
}
