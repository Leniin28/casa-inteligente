package com.ecore.demo2.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecore.demo2.core.model.AppSettings
import com.ecore.demo2.core.model.DataSourceType
import com.ecore.demo2.core.model.ThemeMode
import com.ecore.demo2.core.ui.components.LoadingView
import com.ecore.demo2.core.ui.components.OptionSelector
import com.ecore.demo2.core.ui.components.ScreenScaffold
import com.ecore.demo2.core.ui.components.SectionCard
import com.ecore.demo2.core.ui.format.label
import com.ecore.demo2.core.ui.theme.SmartHomeTheme

@Composable
fun SettingsRoute(
    onOpenHouseSettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onThemeChange = viewModel::setThemeMode,
        onDataSourceChange = viewModel::setDataSource,
        onBackendUrlChange = viewModel::onBackendUrlChange,
        onSaveBackendUrl = viewModel::saveBackendUrl,
        onResetBackendUrl = viewModel::resetBackendUrl,
        onOpenHouseSettings = onOpenHouseSettings,
        onOpenNotificationSettings = onOpenNotificationSettings,
        onBack = onBack,
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onThemeChange: (ThemeMode) -> Unit,
    onDataSourceChange: (DataSourceType) -> Unit,
    onBackendUrlChange: (String) -> Unit,
    onSaveBackendUrl: () -> Unit,
    onResetBackendUrl: () -> Unit,
    onOpenHouseSettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onBack: () -> Unit,
) {
    ScreenScaffold(title = "Ajustes", onBack = onBack) { padding ->
        val settings = state.settings
        if (settings == null) {
            LoadingView(Modifier.padding(padding))
            return@ScreenScaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionCard(title = "Tema") {
                OptionSelector(ThemeMode.entries, settings.themeMode, { it.label }, onThemeChange)
            }

            SectionCard(title = "Fuente de datos") {
                OptionSelector(DataSourceType.entries, settings.dataSource, { it.label }, onDataSourceChange)
                Text(
                    "Al cambiar la fuente se cierra la sesión y se borran los datos guardados.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            SectionCard(title = "Servidor local (backend)") {
                OutlinedTextField(
                    value = state.backendUrlInput,
                    onValueChange = onBackendUrlChange,
                    label = { Text("URL del backend") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Emulador: http://10.0.2.2:8000/\nMóvil físico: http://IP-DE-LA-LAPTOP:8000/",
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onSaveBackendUrl) { Text("Guardar URL") }
                    TextButton(onClick = onResetBackendUrl) { Text("Restablecer") }
                }
            }

            state.message?.let { Text(it) }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            TextButton(onClick = onOpenHouseSettings) { Text("Configuración de casa") }
            TextButton(onClick = onOpenNotificationSettings) { Text("Notificaciones") }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    SmartHomeTheme {
        SettingsScreen(
            SettingsUiState(AppSettings(backendUrl = "http://10.0.2.2:8000/"), backendUrlInput = "http://10.0.2.2:8000/"),
            {}, {}, {}, {}, {}, {}, {}, {},
        )
    }
}
