package com.ecore.demo2.feature.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecore.demo2.core.ui.components.ScreenScaffold
import com.ecore.demo2.core.ui.theme.SmartHomeTheme

/** Pestaña "Datos": acceso a Electricidad, Agua, Dispositivos e Historial. */
@Composable
fun DataMenuScreen(
    onOpenElectricity: () -> Unit,
    onOpenWater: () -> Unit,
    onOpenDevices: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    ScreenScaffold(title = "Datos") { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MenuButton("Electricidad", onOpenElectricity)
            MenuButton("Agua", onOpenWater)
            MenuButton("Dispositivos", onOpenDevices)
            MenuButton("Historial", onOpenHistory)
        }
    }
}

@Composable
internal fun MenuButton(text: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(text) }
}

@Preview(showBackground = true)
@Composable
private fun DataMenuScreenPreview() {
    SmartHomeTheme { DataMenuScreen({}, {}, {}, {}) }
}
