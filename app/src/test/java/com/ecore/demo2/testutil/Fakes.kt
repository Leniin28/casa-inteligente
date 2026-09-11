package com.ecore.demo2.testutil

import com.ecore.demo2.core.datastore.SessionStore
import com.ecore.demo2.core.model.Alert
import com.ecore.demo2.core.model.AuthSession
import com.ecore.demo2.core.model.Budget
import com.ecore.demo2.core.model.DashboardSummary
import com.ecore.demo2.core.model.Device
import com.ecore.demo2.core.model.ElectricalReading
import com.ecore.demo2.core.model.HistoryPeriod
import com.ecore.demo2.core.model.HistoryRecord
import com.ecore.demo2.core.model.SystemStatus
import com.ecore.demo2.core.model.User
import com.ecore.demo2.core.model.WaterReading
import com.ecore.demo2.core.repository.AuthRepository
import com.ecore.demo2.core.repository.DataResult
import com.ecore.demo2.core.repository.SmartHomeRepository
import com.ecore.demo2.core.ui.preview.PreviewData
import kotlinx.coroutines.flow.MutableStateFlow

/** Repositorio falso configurable para testear ViewModels sin Room, red ni DataStore. */
class FakeSmartHomeRepository : SmartHomeRepository {
    var error: Exception? = null
    var electricity = DataResult(PreviewData.electricity)
    var history: List<HistoryRecord> = PreviewData.history
    var budgets: List<Budget> = PreviewData.budgets
    val requestedPeriods = mutableListOf<HistoryPeriod>()
    val savedLimits = mutableListOf<Pair<String, Double>>()

    private fun <T> answer(value: T): T {
        error?.let { throw it }
        return value
    }

    override suspend fun getDashboard(): DataResult<DashboardSummary> = answer(DataResult(PreviewData.dashboard))
    override suspend fun getElectricity(): DataResult<ElectricalReading> = answer(electricity)
    override suspend fun getElectricityHistory(period: HistoryPeriod): List<ElectricalReading> = answer(emptyList())
    override suspend fun getWater(): DataResult<WaterReading> = answer(DataResult(PreviewData.water))
    override suspend fun getWaterHistory(period: HistoryPeriod): List<WaterReading> = answer(emptyList())
    override suspend fun getDevices(): DataResult<List<Device>> = answer(DataResult(PreviewData.devices))
    override suspend fun getDevice(id: String): Device = answer(PreviewData.devices.first { it.id == id })
    override suspend fun getAlerts(): DataResult<List<Alert>> = answer(DataResult(PreviewData.alerts))
    override suspend fun markAlertRead(id: String) = answer(Unit)
    override suspend fun getBudgets(): DataResult<List<Budget>> = answer(DataResult(budgets))

    override suspend fun updateBudgetLimit(budget: Budget, newLimit: Double): Budget {
        error?.let { throw it }
        savedLimits += budget.id to newLimit
        return budget.copy(limit = newLimit)
    }

    override suspend fun getHistory(period: HistoryPeriod): DataResult<List<HistoryRecord>> {
        requestedPeriods += period
        return answer(DataResult(history))
    }

    override suspend fun getSystemStatus(): SystemStatus = PreviewData.systemStatus
    override suspend fun clearCache() = Unit
}

class FakeAuthRepository : AuthRepository {
    val sessionFlow = MutableStateFlow<AuthSession?>(null)
    override val session = sessionFlow
    var error: Exception? = null
    var loginCalls = 0

    override suspend fun login(email: String, password: String): User {
        loginCalls++
        error?.let { throw it }
        val user = User("1", "Demo", email)
        sessionFlow.value = AuthSession("token", user)
        return user
    }

    override suspend fun register(name: String, email: String, password: String): User {
        error?.let { throw it }
        val user = User("1", name, email)
        sessionFlow.value = AuthSession("token", user)
        return user
    }

    override suspend fun logout() {
        sessionFlow.value = null
    }

    override suspend fun clearLocalSession() {
        sessionFlow.value = null
    }

    override suspend fun requestPasswordReset(email: String) = Unit
    override suspend fun validateSession() = Unit
}

class FakeSessionStore(initial: AuthSession? = null) : SessionStore {
    override val session = MutableStateFlow(initial)
    override suspend fun save(session: AuthSession) {
        this.session.value = session
    }

    override suspend fun clear() {
        session.value = null
    }
}
