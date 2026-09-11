package com.ecore.demo2.core.repository

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

/**
 * Origen de datos intercambiable. Hay dos implementaciones:
 *  - [com.ecore.demo2.core.repository.demo.DemoSmartHomeDataSource]: datos simulados, sin red.
 *  - [com.ecore.demo2.core.network.ApiSmartHomeDataSource]: backend FastAPI local.
 *
 * Los repositorios eligen cuál usar según Ajustes; las pantallas nunca lo saben.
 * Todas las funciones lanzan excepción si fallan (los ViewModels la convierten en mensaje).
 */
interface SmartHomeDataSource {
    val type: DataSourceType

    suspend fun login(email: String, password: String): AuthSession
    suspend fun register(name: String, email: String, password: String): AuthSession
    suspend fun logout()
    suspend fun currentUser(): User
    suspend fun requestPasswordReset(email: String)

    suspend fun getDashboard(): DashboardSummary
    suspend fun getCurrentElectricity(): ElectricalReading
    suspend fun getElectricityHistory(period: HistoryPeriod): List<ElectricalReading>
    suspend fun getCurrentWater(): WaterReading
    suspend fun getWaterHistory(period: HistoryPeriod): List<WaterReading>
    suspend fun getDevices(): List<Device>
    suspend fun getDevice(id: String): Device
    suspend fun getAlerts(): List<Alert>
    suspend fun markAlertRead(id: String)
    suspend fun getBudgets(): List<Budget>

    /** Crea (id == null) o actualiza el presupuesto de un recurso. */
    suspend fun saveBudget(resourceType: ResourceType, limit: Double, id: String?): Budget
    suspend fun getHistory(period: HistoryPeriod): List<HistoryRecord>

    /** [previousMessages] es la conversación anterior, por si el backend/IA necesita contexto. */
    suspend fun sendAssistantMessage(text: String, previousMessages: List<AssistantMessage>): AssistantMessage
    suspend fun getSystemStatus(): SystemStatus
}
