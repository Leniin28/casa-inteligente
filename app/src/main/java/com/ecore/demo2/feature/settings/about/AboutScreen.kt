package com.ecore.demo2.feature.settings.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecore.demo2.BuildConfig
import com.ecore.demo2.core.ui.components.ScreenScaffold
import com.ecore.demo2.core.ui.theme.SmartHomeTheme

@Composable
fun AboutRoute(onBack: () -> Unit) {
    AboutScreen(versionName = BuildConfig.VERSION_NAME, onBack = onBack)
}

@Composable
fun AboutScreen(versionName: String, onBack: () -> Unit) {
    ScreenScaffold(title = "Acerca de", onBack = onBack) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Casa Inteligente", style = MaterialTheme.typography.headlineSmall)
            Text("Versión $versionName")
            Text(
                "Proyecto universitario para monitorizar el consumo de electricidad y agua de una casa " +
                    "con ESP32, un backend local en una laptop e IA local.",
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AboutScreenPreview() {
    SmartHomeTheme { AboutScreen("1.0", {}) }
}
