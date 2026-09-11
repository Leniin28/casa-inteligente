package com.ecore.demo2.core.repository

import com.ecore.demo2.core.model.Alert
import com.ecore.demo2.core.model.Budget
import com.ecore.demo2.core.model.DashboardSummary
import com.ecore.demo2.core.model.Device
import com.ecore.demo2.core.model.ElectricalReading
import com.ecore.demo2.core.model.HistoryPeriod
import com.ecore.demo2.core.model.HistoryRecord
import com.ecore.demo2.core.model.SystemStatus
import com.ecore.demo2.core.model.WaterReading

/**
 * Resultado de una lectura. [fromCache] = true cuando no hubo conexión y se devuelven
 * los últimos datos guardados en Room.
 */
data class DataResult<out T>(
    val data: T,
    val fromCache: Boolean = false,
)

/**
 * Punto de entrada de los ViewModels para los datos de la casa.
 * Si algo falla y no hay caché, la función lanza la excepción original.
 */
interface SmartHomeRepository {
    suspend fun getDashboard(): DataResult<DashboardSummary>
    suspend fun getElectricity(): DataResult<ElectricalReading>
    suspend fun getElectricityHistory(period: HistoryPeriod): List<ElectricalReading>
    suspend fun getWater(): DataResult<WaterReading>
    suspend fun getWaterHistory(period: HistoryPeriod): List<WaterReading>
    suspend fun getDevices(): DataResult<List<Device>>
    suspend fun getDevice(id: String): Device
    suspend fun getAlerts(): DataResult<List<Alert>>
    suspend fun markAlertRead(id: String)
    suspend fun getBudgets(): DataResult<List<Budget>>
    suspend fun updateBudgetLimit(budget: Budget, newLimit: Double): Budget
    suspend fun getHistory(period: HistoryPeriod): DataResult<List<HistoryRecord>>

    /** Nunca lanza: si el backend no responde devuelve un estado "desconectado". */
    suspend fun getSystemStatus(): SystemStatus

    /** Borra la caché local (p. ej. al cambiar entre Demo y API). */
    suspend fun clearCache()
}
