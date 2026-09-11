package com.ecore.demo2.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecore.demo2.core.ui.components.ScreenScaffold
import com.ecore.demo2.core.ui.theme.SmartHomeTheme

@Composable
fun HouseSettingsRoute(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HouseSettingsScreen(state, viewModel::onHouseNameChange, viewModel::saveHouseName, onBack)
}

@Composable
fun HouseSettingsScreen(
    state: SettingsUiState,
    onHouseNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    ScreenScaffold(title = "Configuración de casa", onBack = onBack) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.houseNameInput,
                onValueChange = onHouseNameChange,
                label = { Text("Nombre de la casa") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = onSave) { Text("Guardar") }
            state.message?.let { Text(it) }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HouseSettingsScreenPreview() {
    SmartHomeTheme { HouseSettingsScreen(SettingsUiState(houseNameInput = "Mi casa"), {}, {}, {}) }
}
