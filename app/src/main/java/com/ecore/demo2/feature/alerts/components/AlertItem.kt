package com.ecore.demo2.feature.alerts.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ecore.demo2.core.model.Alert
import com.ecore.demo2.core.ui.components.SectionCard
import com.ecore.demo2.core.ui.format.Format
import com.ecore.demo2.core.ui.format.label

@Composable
fun AlertItem(alert: Alert, onMarkAsRead: () -> Unit, modifier: Modifier = Modifier) {
    SectionCard(modifier = modifier, title = alert.title) {
        Text("${alert.severity.label} · ${alert.type.label}", style = MaterialTheme.typography.labelMedium)
        Text(alert.message)
        Text(Format.dateTime(alert.timestamp), style = MaterialTheme.typography.bodySmall)
        if (alert.read) {
            Text("Leída", style = MaterialTheme.typography.bodySmall)
        } else {
            TextButton(onClick = onMarkAsRead) { Text("Marcar como leída") }
        }
    }
}
