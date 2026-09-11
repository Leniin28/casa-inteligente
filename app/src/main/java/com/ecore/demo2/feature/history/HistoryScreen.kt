package com.ecore.demo2.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecore.demo2.core.model.HistoryPeriod
import com.ecore.demo2.core.model.HistoryRecord
import com.ecore.demo2.core.ui.LoadState
import com.ecore.demo2.core.ui.components.LoadStateContent
import com.ecore.demo2.core.ui.components.OptionSelector
import com.ecore.demo2.core.ui.components.ScreenScaffold
import com.ecore.demo2.core.ui.components.SectionCard
import com.ecore.demo2.core.ui.components.ValueRow
import com.ecore.demo2.core.ui.format.Format
import com.ecore.demo2.core.ui.format.label
import com.ecore.demo2.core.ui.preview.PreviewData
import com.ecore.demo2.core.ui.theme.SmartHomeTheme

@Composable
fun HistoryRoute(onBack: () -> Unit, viewModel: HistoryViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(state, onSelectPeriod = viewModel::selectPeriod, onRetry = viewModel::refresh, onBack = onBack)
}

// ---- Diseño (Persona 5) ----

@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onSelectPeriod: (HistoryPeriod) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    ScreenScaffold(title = "Historial", onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OptionSelector(
                options = HistoryPeriod.entries,
                selected = state.period,
                label = { it.label },
                onSelect = onSelectPeriod,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LoadStateContent(
                state = state.records,
                onRetry = onRetry,
                emptyMessage = "No hay registros para este periodo.",
            ) { records ->
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.totals?.let { totals ->
                        item {
                            SectionCard(title = "Total del periodo") {
                                ValueRow("Electricidad", Format.kwh(totals.electricityKwh))
                                ValueRow("Agua", Format.liters(totals.waterLiters))
                                totals.estimatedCost?.let { ValueRow("Coste estimado", Format.cost(it)) }
                            }
                        }
                    }
                    items(records, key = { it.timestamp.toEpochMilli() }) { record ->
                        HistoryRow(record, state.period)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(record: HistoryRecord, period: HistoryPeriod) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(Format.historyLabel(record.timestamp, period))
        ValueRow("Electricidad", Format.kwh(record.electricityKwh))
        ValueRow("Agua", Format.liters(record.waterLiters))
        record.estimatedCost?.let { ValueRow("Coste", Format.cost(it)) }
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoryScreenPreview() {
    SmartHomeTheme {
        HistoryScreen(
            HistoryUiState(HistoryPeriod.WEEK, LoadState.Success(PreviewData.history), HistoryTotals(1.4, 1500.0, 3.1)),
            {}, {}, {},
        )
    }
}
