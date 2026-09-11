package com.ecore.demo2.core.ui.preview

import com.ecore.demo2.core.model.Alert
import com.ecore.demo2.core.model.AlertSeverity
import com.ecore.demo2.core.model.AlertType
import com.ecore.demo2.core.model.AssistantMessage
import com.ecore.demo2.core.model.Budget
import com.ecore.demo2.core.model.DashboardSummary
import com.ecore.demo2.core.model.DataSourceType
import com.ecore.demo2.core.model.Device
import com.ecore.demo2.core.model.DeviceStatus
import com.ecore.demo2.core.model.DeviceType
import com.ecore.demo2.core.model.ElectricalReading
import com.ecore.demo2.core.model.HistoryRecord
import com.ecore.demo2.core.model.MessageRole
import com.ecore.demo2.core.model.ResourceType
import com.ecore.demo2.core.model.SystemStatus
import com.ecore.demo2.core.model.User
import com.ecore.demo2.core.model.WaterReading
import java.time.Instant
import java.time.LocalDate

/**
 * Datos fijos para los @Preview de Android Studio.
 * Permiten diseñar cada pantalla sin ejecutar la app.
 */
object PreviewData {
    private val now: Instant = Instant.parse("2026-09-11T18:30:00Z")

    val electricity = ElectricalReading(now, voltage = 12.1, current = 1.03, power = 12.5, energyTodayKwh = 0.182)
    val water = WaterReading(now, flowLitersPerMinute = 1.2, litersToday = 184.0)

    val devices = listOf(
        Device("fan", "Ventilador", DeviceType.FAN, 12.0, DeviceStatus.PROBABLY_ACTIVE, 0.88),
        Device("lights", "Luces LED sala", DeviceType.LIGHTING, 4.8, DeviceStatus.PROBABLY_ACTIVE, 0.81),
        Device("router", "Router + ESP32", DeviceType.ELECTRONICS, 2.5, DeviceStatus.ACTIVE, 0.95),
        Device("pump", "Bomba de agua", DeviceType.PUMP, 6.0, DeviceStatus.INACTIVE, 0.74),
    )

    val alerts = listOf(
        Alert("a1", AlertType.HIGH_CONSUMPTION, AlertSeverity.WARNING, "Consumo elevado", "La potencia superó 25 W.", now, read = false),
        Alert("a2", AlertType.WATER_LEAK, AlertSeverity.CRITICAL, "Posible fuga de agua", "Flujo continuo de madrugada.", now.minusSeconds(18_000), read = false),
        Alert("a3", AlertType.SYSTEM, AlertSeverity.INFO, "ESP32 reconectado", "El nodo volvió a enviar datos.", now.minusSeconds(90_000), read = true),
    )

    val budgets = listOf(
        Budget("electricity", ResourceType.ELECTRICITY, 5.0, 2.1, 5.8, LocalDate.of(2026, 9, 26), LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)),
        Budget("water", ResourceType.WATER, 7000.0, 2300.0, 6270.0, null, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)),
    )

    val history = (0 until 7).map { i ->
        HistoryRecord(now.minusSeconds(86_400L * (6 - i)), electricityKwh = 0.17 + i * 0.01, waterLiters = 210.0 + i * 5, estimatedCost = 0.45)
    }

    val systemStatus = SystemStatus(backendConnected = true, esp32Connected = true, lastUpdate = now, dataSource = DataSourceType.DEMO, backendMode = "demo")

    val dashboard = DashboardSummary(electricity, water, activeDevices = 3, unreadAlerts = 2, budgets = budgets, systemStatus = systemStatus)

    val user = User("demo-user", "Demo", "demo@casa.local")

    val messages = listOf(
        AssistantMessage("1", MessageRole.ASSISTANT, "Hola, ¿en qué te ayudo?", now),
        AssistantMessage("2", MessageRole.USER, "¿Cuánto consumo ahora?", now),
        AssistantMessage("3", MessageRole.ASSISTANT, "Ahora mismo la casa consume 12.5 W.", now),
    )
}
