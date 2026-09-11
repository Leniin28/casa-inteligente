package com.ecore.demo2.core.ui.format

import com.ecore.demo2.core.model.AlertSeverity
import com.ecore.demo2.core.model.AlertType
import com.ecore.demo2.core.model.DataSourceType
import com.ecore.demo2.core.model.DeviceStatus
import com.ecore.demo2.core.model.DeviceType
import com.ecore.demo2.core.model.HistoryPeriod
import com.ecore.demo2.core.model.ResourceType
import com.ecore.demo2.core.model.ThemeMode

// Textos visibles de los enums del dominio.

val DeviceStatus.label: String
    get() = when (this) {
        DeviceStatus.ACTIVE -> "Activo"
        DeviceStatus.PROBABLY_ACTIVE -> "Probablemente activo"
        DeviceStatus.INACTIVE -> "Inactivo"
        DeviceStatus.UNKNOWN -> "Desconocido"
    }

val DeviceType.label: String
    get() = when (this) {
        DeviceType.LIGHTING -> "Iluminación"
        DeviceType.FAN -> "Ventilación"
        DeviceType.PUMP -> "Bomba"
        DeviceType.ELECTRONICS -> "Electrónica"
        DeviceType.APPLIANCE -> "Electrodoméstico"
        DeviceType.OTHER -> "Otro"
    }

val AlertSeverity.label: String
    get() = when (this) {
        AlertSeverity.INFO -> "Info"
        AlertSeverity.WARNING -> "Aviso"
        AlertSeverity.CRITICAL -> "Crítica"
    }

val AlertType.label: String
    get() = when (this) {
        AlertType.HIGH_CONSUMPTION -> "Consumo alto"
        AlertType.WATER_LEAK -> "Posible fuga"
        AlertType.BUDGET -> "Presupuesto"
        AlertType.DEVICE -> "Dispositivo"
        AlertType.SYSTEM -> "Sistema"
        AlertType.OTHER -> "Otra"
    }

val ResourceType.label: String
    get() = when (this) {
        ResourceType.ELECTRICITY -> "Electricidad"
        ResourceType.WATER -> "Agua"
    }

val HistoryPeriod.label: String
    get() = when (this) {
        HistoryPeriod.DAY -> "Día"
        HistoryPeriod.WEEK -> "Semana"
        HistoryPeriod.MONTH -> "Mes"
    }

val DataSourceType.label: String
    get() = when (this) {
        DataSourceType.DEMO -> "Demo"
        DataSourceType.API -> "Casa real (API)"
    }

val ThemeMode.label: String
    get() = when (this) {
        ThemeMode.SYSTEM -> "Sistema"
        ThemeMode.LIGHT -> "Claro"
        ThemeMode.DARK -> "Oscuro"
    }
