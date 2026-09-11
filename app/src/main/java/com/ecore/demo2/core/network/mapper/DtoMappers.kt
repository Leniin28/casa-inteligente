package com.ecore.demo2.core.network.mapper

import com.ecore.demo2.core.model.Alert
import com.ecore.demo2.core.model.AlertSeverity
import com.ecore.demo2.core.model.AlertType
import com.ecore.demo2.core.model.AssistantMessage
import com.ecore.demo2.core.model.AuthSession
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
import com.ecore.demo2.core.network.dto.AlertDto
import com.ecore.demo2.core.network.dto.AssistantMessageDto
import com.ecore.demo2.core.network.dto.AuthResponseDto
import com.ecore.demo2.core.network.dto.BudgetDto
import com.ecore.demo2.core.network.dto.ChatMessageDto
import com.ecore.demo2.core.network.dto.DashboardDto
import com.ecore.demo2.core.network.dto.DeviceDto
import com.ecore.demo2.core.network.dto.ElectricalReadingDto
import com.ecore.demo2.core.network.dto.HistoryRecordDto
import com.ecore.demo2.core.network.dto.SystemStatusDto
import com.ecore.demo2.core.network.dto.UserDto
import com.ecore.demo2.core.network.dto.WaterReadingDto
import com.ecore.demo2.core.util.enumValueOrNull
import com.ecore.demo2.core.util.parseInstant
import com.ecore.demo2.core.util.toApiValue
import java.time.LocalDate

// Conversión DTO (red) -> modelo de dominio. Si el backend cambia de formato, solo se toca aquí.

fun ElectricalReadingDto.toDomain() = ElectricalReading(
    timestamp = parseInstant(timestamp),
    voltage = voltage,
    current = current,
    power = power,
    energyTodayKwh = energyTodayKwh,
)

fun WaterReadingDto.toDomain() = WaterReading(
    timestamp = parseInstant(timestamp),
    flowLitersPerMinute = flowLitersPerMinute,
    litersToday = litersToday,
)

fun DeviceDto.toDomain() = Device(
    id = id,
    name = name,
    type = enumValueOrNull<DeviceType>(type) ?: DeviceType.OTHER,
    estimatedPowerWatts = estimatedPowerWatts,
    status = enumValueOrNull<DeviceStatus>(status) ?: DeviceStatus.UNKNOWN,
    confidence = confidence,
    controllable = controllable,
)

fun AlertDto.toDomain() = Alert(
    id = id,
    type = enumValueOrNull<AlertType>(type) ?: AlertType.OTHER,
    severity = enumValueOrNull<AlertSeverity>(severity) ?: AlertSeverity.INFO,
    title = title,
    message = message,
    timestamp = parseInstant(timestamp),
    read = read,
)

fun BudgetDto.toDomain() = Budget(
    id = id,
    resourceType = enumValueOrNull<ResourceType>(resourceType) ?: ResourceType.ELECTRICITY,
    limit = limit,
    currentUsage = currentUsage,
    estimatedFinalUsage = estimatedFinalUsage,
    estimatedLimitDate = estimatedLimitDate?.let(LocalDate::parse),
    periodStart = LocalDate.parse(periodStart),
    periodEnd = LocalDate.parse(periodEnd),
)

fun HistoryRecordDto.toDomain() = HistoryRecord(
    timestamp = parseInstant(timestamp),
    electricityKwh = electricityKwh,
    waterLiters = waterLiters,
    estimatedCost = estimatedCost,
)

/** El origen para la app es siempre API; el modo del backend ("demo"/"sensors") se conserva aparte. */
fun SystemStatusDto.toDomain() = SystemStatus(
    backendConnected = backendConnected,
    esp32Connected = esp32Connected,
    lastUpdate = lastUpdate?.let(::parseInstant),
    dataSource = DataSourceType.API,
    backendMode = dataSource,
)

fun DashboardDto.toDomain() = DashboardSummary(
    electricity = electricity.toDomain(),
    water = water.toDomain(),
    activeDevices = activeDevices,
    unreadAlerts = unreadAlerts,
    budgets = budgets.map { it.toDomain() },
    systemStatus = systemStatus.toDomain(),
)

fun UserDto.toDomain() = User(id = id, name = name, email = email)

fun AuthResponseDto.toDomain() = AuthSession(token = token, user = user.toDomain())

fun AssistantMessageDto.toDomain() = AssistantMessage(
    id = id,
    role = enumValueOrNull<MessageRole>(role) ?: MessageRole.ASSISTANT,
    text = text,
    timestamp = parseInstant(timestamp),
)

fun AssistantMessage.toChatDto() = ChatMessageDto(role = role.toApiValue(), text = text)
