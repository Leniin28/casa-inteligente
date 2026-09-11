package com.ecore.demo2.feature.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecore.demo2.core.ui.components.ScreenScaffold
import com.ecore.demo2.core.ui.theme.SmartHomeTheme

/** Pestaña "Más": presupuestos, perfil, ajustes, estado del sistema y acerca de. */
@Composable
fun MoreMenuScreen(
    onOpenBudgets: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSystemStatus: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    ScreenScaffold(title = "Más") { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MenuButton("Presupuestos", onOpenBudgets)
            MenuButton("Perfil", onOpenProfile)
            MenuButton("Ajustes", onOpenSettings)
            MenuButton("Estado del sistema", onOpenSystemStatus)
            MenuButton("Acerca de", onOpenAbout)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MoreMenuScreenPreview() {
    SmartHomeTheme { MoreMenuScreen({}, {}, {}, {}, {}) }
}
