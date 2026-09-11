package com.ecore.demo2.core.network

import com.ecore.demo2.core.model.Alert
import com.ecore.demo2.core.model.AssistantMessage
import com.ecore.demo2.core.model.AuthSession
import com.ecore.demo2.core.model.Budget
import com.ecore.demo2.core.model.DashboardSummary
import com.ecore.demo2.core.model.DataSourceType
import com.ecore.demo2.core.model.Device
import com.ecore.demo2.core.model.ElectricalReading
import com.ecore.demo2.core.model.HistoryPeriod
import com.ecore.demo2.core.model.HistoryRecord
import com.ecore.demo2.core.model.ResourceType
import com.ecore.demo2.core.model.SystemStatus
import com.ecore.demo2.core.model.User
import com.ecore.demo2.core.model.WaterReading
import com.ecore.demo2.core.network.dto.BudgetRequestDto
import com.ecore.demo2.core.network.dto.ChatRequestDto
import com.ecore.demo2.core.network.dto.LoginRequestDto
import com.ecore.demo2.core.network.dto.PasswordResetRequestDto
import com.ecore.demo2.core.network.dto.RegisterRequestDto
import com.ecore.demo2.core.network.mapper.toChatDto
import com.ecore.demo2.core.network.mapper.toDomain
import com.ecore.demo2.core.repository.SmartHomeDataSource
import com.ecore.demo2.core.util.toApiValue
import javax.inject.Inject
import javax.inject.Singleton

/** Fuente de datos real: backend FastAPI de la laptop (que a su vez recibe los datos del ESP32). */
@Singleton
class ApiSmartHomeDataSource @Inject constructor(
    private val apiFactory: ApiServiceFactory,
) : SmartHomeDataSource {

    override val type = DataSourceType.API

    private suspend fun api() = apiFactory.api()

    override suspend fun login(email: String, password: String): AuthSession =
        api().login(LoginRequestDto(email, password)).toDomain()

    override suspend fun register(name: String, email: String, password: String): AuthSession =
        api().register(RegisterRequestDto(name, email, password)).toDomain()

    override suspend fun logout() = api().logout()

    override suspend fun currentUser(): User = api().me().toDomain()

    override suspend fun requestPasswordReset(email: String) =
        api().requestPasswordReset(PasswordResetRequestDto(email))

    override suspend fun getDashboard(): DashboardSummary = api().dashboard().toDomain()

    override suspend fun getCurrentElectricity(): ElectricalReading = api().currentElectricity().toDomain()

    override suspend fun getElectricityHistory(period: HistoryPeriod): List<ElectricalReading> =
        api().electricityHistory(period.toApiValue()).map { it.toDomain() }

    override suspend fun getCurrentWater(): WaterReading = api().currentWater().toDomain()

    override suspend fun getWaterHistory(period: HistoryPeriod): List<WaterReading> =
        api().waterHistory(period.toApiValue()).map { it.toDomain() }

    override suspend fun getDevices(): List<Device> = api().devices().map { it.toDomain() }

    override suspend fun getDevice(id: String): Device = api().device(id).toDomain()

    override suspend fun getAlerts(): List<Alert> = api().alerts().map { it.toDomain() }

    override suspend fun markAlertRead(id: String) {
        api().markAlertRead(id)
    }

    override suspend fun getBudgets(): List<Budget> = api().budgets().map { it.toDomain() }

    override suspend fun saveBudget(resourceType: ResourceType, limit: Double, id: String?): Budget {
        val body = BudgetRequestDto(resourceType = resourceType.toApiValue(), limit = limit)
        val dto = if (id == null) api().createBudget(body) else api().updateBudget(id, body)
        return dto.toDomain()
    }

    override suspend fun getHistory(period: HistoryPeriod): List<HistoryRecord> =
        api().history(period.toApiValue()).map { it.toDomain() }

    override suspend fun sendAssistantMessage(
        text: String,
        previousMessages: List<AssistantMessage>,
    ): AssistantMessage {
        val request = ChatRequestDto(
            message = text,
            history = previousMessages.takeLast(MAX_HISTORY).map { it.toChatDto() },
        )
        return api().chat(request).reply.toDomain()
    }

    override suspend fun getSystemStatus(): SystemStatus = api().systemStatus().toDomain()

    private companion object {
        const val MAX_HISTORY = 10
    }
}
