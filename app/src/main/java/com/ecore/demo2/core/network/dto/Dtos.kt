package com.ecore.demo2.core.network.dto

import kotlinx.serialization.Serializable

// DTOs del contrato REST (ver docs/API.md). El JSON usa snake_case; la conversión la hace
// la estrategia de nombres configurada en NetworkJson, por eso aquí no hay @SerialName.
// Fechas: ISO-8601 (String). Enums: minúsculas ("probably_active").

@Serializable
data class ElectricalReadingDto(
    val timestamp: String,
    val voltage: Double,
    val current: Double,
    val power: Double,
    val energyTodayKwh: Double,
)

@Serializable
data class WaterReadingDto(
    val timestamp: String,
    val flowLitersPerMinute: Double,
    val litersToday: Double,
)

@Serializable
data class DeviceDto(
    val id: String,
    val name: String,
    val type: String,
    val estimatedPowerWatts: Double,
    val status: String,
    val confidence: Double? = null,
    val controllable: Boolean = false,
)

@Serializable
data class AlertDto(
    val id: String,
    val type: String,
    val severity: String,
    val title: String,
    val message: String,
    val timestamp: String,
    val read: Boolean,
)

@Serializable
data class BudgetDto(
    val id: String,
    val resourceType: String,
    val limit: Double,
    val currentUsage: Double,
    val estimatedFinalUsage: Double,
    val estimatedLimitDate: String? = null,
    val periodStart: String,
    val periodEnd: String,
)

@Serializable
data class BudgetRequestDto(
    val resourceType: String,
    val limit: Double,
)

@Serializable
data class HistoryRecordDto(
    val timestamp: String,
    val electricityKwh: Double,
    val waterLiters: Double,
    val estimatedCost: Double? = null,
)

@Serializable
data class SystemStatusDto(
    val backendConnected: Boolean,
    val esp32Connected: Boolean,
    val lastUpdate: String? = null,
    /** Modo del backend: "demo" (simulado) o "sensors" (datos reales). */
    val dataSource: String,
)

@Serializable
data class DashboardDto(
    val electricity: ElectricalReadingDto,
    val water: WaterReadingDto,
    val activeDevices: Int,
    val unreadAlerts: Int,
    val budgets: List<BudgetDto>,
    val systemStatus: SystemStatusDto,
)

@Serializable
data class UserDto(
    val id: String,
    val name: String,
    val email: String,
)

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
)

@Serializable
data class RegisterRequestDto(
    val name: String,
    val email: String,
    val password: String,
)

@Serializable
data class PasswordResetRequestDto(
    val email: String,
)

@Serializable
data class AuthResponseDto(
    val token: String,
    val user: UserDto,
)

@Serializable
data class ChatMessageDto(
    val role: String,
    val text: String,
)

@Serializable
data class ChatRequestDto(
    val message: String,
    val history: List<ChatMessageDto> = emptyList(),
)

@Serializable
data class AssistantMessageDto(
    val id: String,
    val role: String,
    val text: String,
    val timestamp: String,
)

@Serializable
data class ChatResponseDto(
    val reply: AssistantMessageDto,
)
