package com.ecore.demo2.feature.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.ecore.demo2.feature.devices.components.DeviceItem

@Composable
fun DevicesRoute(onBack: () -> Unit, viewModel: DevicesViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DevicesScreen(state, onRefresh = viewModel::refresh, onBack = onBack)
}

// ---- Diseño (Persona 3) ----

@Composable
fun DevicesScreen(state: DevicesUiState, onRefresh: () -> Unit, onBack: () -> Unit) {
    ScreenScaffold(
        title = "Dispositivos",
        onBack = onBack,
        actions = { TextButton(onClick = onRefresh) { Text("Actualizar") } },
    ) { padding ->
        LoadStateContent(
            state = state.devices,
            onRetry = onRefresh,
            modifier = Modifier.padding(padding),
            emptyMessage = "Todavía no se ha detectado ningún dispositivo.",
        ) { devices ->
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(devices, key = { it.id }) { device -> DeviceItem(device) }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DevicesScreenPreview() {
    SmartHomeTheme { DevicesScreen(DevicesUiState(LoadState.Success(PreviewData.devices)), {}, {}) }
}
