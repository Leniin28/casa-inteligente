package com.ecore.demo2.feature.devices.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ecore.demo2.core.model.Device
import com.ecore.demo2.core.ui.components.SectionCard
import com.ecore.demo2.core.ui.components.ValueRow
import com.ecore.demo2.core.ui.format.Format
import com.ecore.demo2.core.ui.format.label

@Composable
fun DeviceItem(device: Device, modifier: Modifier = Modifier) {
    SectionCard(modifier = modifier, title = device.name) {
        Text(device.status.label)
        ValueRow("Potencia estimada", Format.watts(device.estimatedPowerWatts))
        device.confidence?.let { ValueRow("Confianza", Format.percent(it)) }
        ValueRow("Tipo", device.type.label)
    }
}
