package com.ecore.demo2.core.repository.demo

import com.ecore.demo2.core.datastore.SessionStore
import com.ecore.demo2.core.model.Alert
import com.ecore.demo2.core.model.AssistantMessage
import com.ecore.demo2.core.model.AuthSession
import com.ecore.demo2.core.model.Budget
import com.ecore.demo2.core.model.DashboardSummary
import com.ecore.demo2.core.model.DataSourceType
import com.ecore.demo2.core.model.Device
import com.ecore.demo2.core.model.DeviceStatus
import com.ecore.demo2.core.model.ElectricalReading
import com.ecore.demo2.core.model.HistoryPeriod
import com.ecore.demo2.core.model.HistoryRecord
import com.ecore.demo2.core.model.MessageRole
import com.ecore.demo2.core.model.ResourceType
import com.ecore.demo2.core.model.SystemStatus
import com.ecore.demo2.core.model.User
import com.ecore.demo2.core.model.WaterReading
import com.ecore.demo2.core.repository.SmartHomeDataSource
import com.ecore.demo2.core.util.AppException
import com.ecore.demo2.core.util.Validators
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fuente de datos 100 % local para desarrollar sin ESP32, sin backend y sin IA.
 *
 * Login demo: cualquier email válido con contraseña de 6+ caracteres
 * (por ejemplo demo@casa.local / demo1234). No se guarda ninguna contraseña.
 */
@Singleton
class DemoSmartHomeDataSource @Inject constructor(
    private val clock: Clock,
    private val sessionStore: SessionStore,
) : SmartHomeDataSource {

    override val type = DataSourceType.DEMO

    private val simulator = DemoSimulator(clock.zone)

    // Estado en memoria (se reinicia al cerrar la app, suficiente para una demo).
    private val readAlertIds = MutableStateFlow(setOf("alert-device-new", "alert-system-reconnected"))
    private val budgetLimits = MutableStateFlow(
        mapOf(
            ResourceType.ELECTRICITY to DemoSimulator.DEFAULT_ELECTRICITY_LIMIT_KWH,
            ResourceType.WATER to DemoSimulator.DEFAULT_WATER_LIMIT_LITERS,
        ),
    )

    // ---------------------------------------------------------------- Auth

    override suspend fun login(email: String, password: String): AuthSession {
        simulateLatency()
        if (!Validators.isValidEmail(email) || !Validators.isValidPassword(password)) {
            throw AppException("Email o contraseña incorrectos.")
        }
        return demoSession(name = email.substringBefore('@').replaceFirstChar { it.uppercase() }, email = email)
    }

    override suspend fun register(name: String, email: String, password: String): AuthSession {
        simulateLatency()
        if (name.isBlank() || !Validators.isValidEmail(email) || !Validators.isValidPassword(password)) {
            throw AppException("Datos de registro no válidos.")
        }
        return demoSession(name = name, email = email)
    }

    override suspend fun logout() = Unit

    override suspend fun currentUser(): User =
        sessionStore.session.first()?.user ?: throw AppException("No hay sesión activa.")

    override suspend fun requestPasswordReset(email: String) = simulateLatency()

    private fun demoSession(name: String, email: String) = AuthSession(
        token = "demo-${UUID.randomUUID()}",
        user = User(id = "demo-user", name = name, email = email),
    )

    // ---------------------------------------------------------------- Datos

    override suspend fun getDashboard(): DashboardSummary {
        simulateLatency()
        val now = now()
        return DashboardSummary(
            electricity = simulator.electricalReading(now),
            water = simulator.waterReading(now),
            activeDevices = simulator.devices(now).count { it.status != DeviceStatus.INACTIVE },
            unreadAlerts = alerts(now).count { !it.read },
            budgets = budgets(now),
            systemStatus = status(now),
        )
    }

    override suspend fun getCurrentElectricity(): ElectricalReading {
        simulateLatency()
        return simulator.electricalReading(now())
    }

    override suspend fun getElectricityHistory(period: HistoryPeriod): List<ElectricalReading> {
        simulateLatency()
        return simulator.electricalHistory(period, now())
    }

    override suspend fun getCurrentWater(): WaterReading {
        simulateLatency()
        return simulator.waterReading(now())
    }

    override suspend fun getWaterHistory(period: HistoryPeriod): List<WaterReading> {
        simulateLatency()
        return simulator.waterHistory(period, now())
    }

    override suspend fun getDevices(): List<Device> {
        simulateLatency()
        return simulator.devices(now())
    }

    override suspend fun getDevice(id: String): Device =
        getDevices().firstOrNull { it.id == id } ?: throw AppException("Dispositivo no encontrado.")

    override suspend fun getAlerts(): List<Alert> {
        simulateLatency()
        return alerts(now())
    }

    override suspend fun markAlertRead(id: String) {
        readAlertIds.update { it + id }
    }

    override suspend fun getBudgets(): List<Budget> {
        simulateLatency()
        return budgets(now())
    }

    override suspend fun saveBudget(resourceType: ResourceType, limit: Double, id: String?): Budget {
        if (limit <= 0) throw AppException("El límite debe ser mayor que 0.")
        simulateLatency()
        budgetLimits.update { it + (resourceType to limit) }
        return simulator.budget(resourceType, limit, now())
    }

    override suspend fun getHistory(period: HistoryPeriod): List<HistoryRecord> {
        simulateLatency()
        return simulator.history(period, now())
    }

    override suspend fun sendAssistantMessage(
        text: String,
        previousMessages: List<AssistantMessage>,
    ): AssistantMessage {
        delay(ASSISTANT_DELAY_MS)
        val now = now()
        return AssistantMessage(
            id = UUID.randomUUID().toString(),
            role = MessageRole.ASSISTANT,
            text = DemoAssistant.reply(
                question = text,
                electricity = simulator.electricalReading(now),
                water = simulator.waterReading(now),
                budgets = budgets(now),
            ),
            timestamp = now,
        )
    }

    override suspend fun getSystemStatus(): SystemStatus {
        simulateLatency()
        return status(now())
    }

    // ---------------------------------------------------------------- Helpers

    private fun now(): Instant = clock.instant()

    private fun alerts(now: Instant) = simulator.alerts(now, readAlertIds.value)

    private fun budgets(now: Instant) = budgetLimits.value.map { (type, limit) -> simulator.budget(type, limit, now) }

    private fun status(now: Instant) = SystemStatus(
        backendConnected = true,
        esp32Connected = true,
        lastUpdate = now,
        dataSource = DataSourceType.DEMO,
        backendMode = "demo",
    )

    /** Pequeña espera para que los estados de carga sean visibles al diseñar. */
    private suspend fun simulateLatency() = delay(LATENCY_MS)

    private companion object {
        const val LATENCY_MS = 250L
        const val ASSISTANT_DELAY_MS = 700L
    }
}
