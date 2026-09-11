package com.ecore.demo2.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecore.demo2.core.model.DashboardSummary
import com.ecore.demo2.core.ui.LoadState
import com.ecore.demo2.core.ui.components.AutoRefreshEffect
import com.ecore.demo2.core.ui.components.LoadStateContent
import com.ecore.demo2.core.ui.components.MetricBlock
import com.ecore.demo2.core.ui.components.ScreenScaffold
import com.ecore.demo2.core.ui.components.SectionCard
import com.ecore.demo2.core.ui.components.ValueRow
import com.ecore.demo2.core.ui.format.Format
import com.ecore.demo2.core.ui.format.label
import com.ecore.demo2.core.ui.preview.PreviewData
import com.ecore.demo2.core.ui.theme.SmartHomeTheme

// ---- Conexión con el ViewModel ----

@Composable
fun DashboardRoute(
    onOpenElectricity: () -> Unit,
    onOpenWater: () -> Unit,
    onOpenDevices: () -> Unit,
    onOpenAlerts: () -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenSystemStatus: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AutoRefreshEffect(onStart = viewModel::startAutoRefresh, onStop = viewModel::stopAutoRefresh)
    DashboardScreen(
        state = state,
        onRetry = viewModel::refresh,
        onOpenElectricity = onOpenElectricity,
        onOpenWater = onOpenWater,
        onOpenDevices = onOpenDevices,
        onOpenAlerts = onOpenAlerts,
        onOpenBudgets = onOpenBudgets,
        onOpenSystemStatus = onOpenSystemStatus,
    )
}

// ---- Diseño (Persona 2) ----

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onRetry: () -> Unit,
    onOpenElectricity: () -> Unit,
    onOpenWater: () -> Unit,
    onOpenDevices: () -> Unit,
    onOpenAlerts: () -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenSystemStatus: () -> Unit,
) {
    ScreenScaffold(title = state.houseName.ifBlank { "Inicio" }) { padding ->
        LoadStateContent(state.summary, onRetry = onRetry, modifier = Modifier.padding(padding)) { summary ->
            DashboardContent(
                summary, onOpenElectricity, onOpenWater, onOpenDevices, onOpenAlerts, onOpenBudgets, onOpenSystemStatus,
            )
        }
    }
}

@Composable
private fun DashboardContent(
    summary: DashboardSummary,
    onOpenElectricity: () -> Unit,
    onOpenWater: () -> Unit,
    onOpenDevices: () -> Unit,
    onOpenAlerts: () -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenSystemStatus: () -> Unit,
) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionCard(title = "Electricidad") {
            MetricBlock("Potencia actual", Format.watts(summary.electricity.power))
            ValueRow("Consumo de hoy", Format.kwh(summary.electricity.energyTodayKwh))
            TextButton(onClick = onOpenElectricity) { Text("Ver electricidad") }
        }
        SectionCard(title = "Agua") {
            MetricBlock("Caudal actual", Format.flow(summary.water.flowLitersPerMinute))
            ValueRow("Litros hoy", Format.liters(summary.water.litersToday))
            TextButton(onClick = onOpenWater) { Text("Ver agua") }
        }
        SectionCard(title = "Dispositivos") {
            ValueRow("Activos (estimados)", summary.activeDevices.toString())
            TextButton(onClick = onOpenDevices) { Text("Ver dispositivos") }
        }
        SectionCard(title = "Alertas") {
            ValueRow("Sin leer", summary.unreadAlerts.toString())
            TextButton(onClick = onOpenAlerts) { Text("Ver alertas") }
        }
        SectionCard(title = "Presupuestos") {
            summary.budgets.forEach { budget ->
                ValueRow(budget.resourceType.label, "${Format.percent(budget.usedFraction)} usado")
            }
            TextButton(onClick = onOpenBudgets) { Text("Ver presupuestos") }
        }
        SectionCard(title = "Sistema") {
            ValueRow("Backend", if (summary.systemStatus.backendConnected) "Conectado" else "Sin conexión")
            ValueRow("ESP32", if (summary.systemStatus.esp32Connected) "Conectado" else "Sin conexión")
            summary.systemStatus.lastUpdate?.let { ValueRow("Actualizado", Format.time(it)) }
            TextButton(onClick = onOpenSystemStatus) { Text("Estado del sistema") }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    SmartHomeTheme {
        DashboardScreen(
            state = DashboardUiState("Mi casa", LoadState.Success(PreviewData.dashboard)),
            onRetry = {}, onOpenElectricity = {}, onOpenWater = {}, onOpenDevices = {},
            onOpenAlerts = {}, onOpenBudgets = {}, onOpenSystemStatus = {},
        )
    }
}
