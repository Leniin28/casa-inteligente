package com.ecore.demo2.core.repository

import com.ecore.demo2.core.database.dao.AlertDao
import com.ecore.demo2.core.database.dao.HistoryDao
import com.ecore.demo2.core.database.dao.ReadingDao
import com.ecore.demo2.core.database.toDomain
import com.ecore.demo2.core.database.toEntity
import com.ecore.demo2.core.model.Alert
import com.ecore.demo2.core.model.Budget
import com.ecore.demo2.core.model.DashboardSummary
import com.ecore.demo2.core.model.Device
import com.ecore.demo2.core.model.ElectricalReading
import com.ecore.demo2.core.model.HistoryPeriod
import com.ecore.demo2.core.model.HistoryRecord
import com.ecore.demo2.core.model.SystemStatus
import com.ecore.demo2.core.model.WaterReading
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pide los datos a la fuente activa y guarda en Room lo más útil para funcionar sin conexión:
 * últimas lecturas, historial reciente y alertas.
 */
@Singleton
class DefaultSmartHomeRepository @Inject constructor(
    private val dataSources: DataSourceProvider,
    private val readingDao: ReadingDao,
    private val historyDao: HistoryDao,
    private val alertDao: AlertDao,
) : SmartHomeRepository {

    override suspend fun getDashboard(): DataResult<DashboardSummary> = fetchOrCache(
        fetch = {
            dataSources.current().getDashboard().also {
                cacheElectrical(it.electricity)
                cacheWater(it.water)
            }
        },
        cached = ::cachedDashboard,
    )

    override suspend fun getElectricity(): DataResult<ElectricalReading> = fetchOrCache(
        fetch = { dataSources.current().getCurrentElectricity().also { cacheElectrical(it) } },
        cached = { readingDao.latestElectrical()?.toDomain() },
    )

    override suspend fun getElectricityHistory(period: HistoryPeriod): List<ElectricalReading> =
        dataSources.current().getElectricityHistory(period)

    override suspend fun getWater(): DataResult<WaterReading> = fetchOrCache(
        fetch = { dataSources.current().getCurrentWater().also { cacheWater(it) } },
        cached = { readingDao.latestWater()?.toDomain() },
    )

    override suspend fun getWaterHistory(period: HistoryPeriod): List<WaterReading> =
        dataSources.current().getWaterHistory(period)

    override suspend fun getDevices(): DataResult<List<Device>> =
        DataResult(dataSources.current().getDevices())

    override suspend fun getDevice(id: String): Device = dataSources.current().getDevice(id)

    override suspend fun getAlerts(): DataResult<List<Alert>> = fetchOrCache(
        fetch = {
            dataSources.current().getAlerts().also { alerts ->
                alertDao.replaceAll(alerts.map { it.toEntity() })
            }
        },
        cached = { alertDao.getAll().takeIf { it.isNotEmpty() }?.map { it.toDomain() } },
    )

    override suspend fun markAlertRead(id: String) {
        dataSources.current().markAlertRead(id)
        alertDao.markRead(id)
    }

    override suspend fun getBudgets(): DataResult<List<Budget>> =
        DataResult(dataSources.current().getBudgets())

    override suspend fun updateBudgetLimit(budget: Budget, newLimit: Double): Budget {
        require(newLimit > 0) { "El límite debe ser mayor que 0" }
        return dataSources.current().saveBudget(budget.resourceType, newLimit, budget.id)
    }

    override suspend fun getHistory(period: HistoryPeriod): DataResult<List<HistoryRecord>> =
        fetchOrCache(
            fetch = {
                dataSources.current().getHistory(period).also { records ->
                    historyDao.replacePeriod(period.name, records.map { it.toEntity(period) })
                }
            },
            cached = { historyDao.getByPeriod(period.name).takeIf { it.isNotEmpty() }?.map { it.toDomain() } },
        )

    override suspend fun getSystemStatus(): SystemStatus = try {
        dataSources.current().getSystemStatus()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        SystemStatus(
            backendConnected = false,
            esp32Connected = false,
            lastUpdate = readingDao.latestElectrical()?.toDomain()?.timestamp,
            dataSource = dataSources.currentType(),
        )
    }

    override suspend fun clearCache() {
        readingDao.clearElectrical()
        readingDao.clearWater()
        historyDao.clear()
        alertDao.clear()
    }

    private suspend fun cachedDashboard(): DashboardSummary? {
        val electricity = readingDao.latestElectrical()?.toDomain() ?: return null
        val water = readingDao.latestWater()?.toDomain() ?: return null
        return DashboardSummary(
            electricity = electricity,
            water = water,
            activeDevices = 0,
            unreadAlerts = alertDao.countUnread(),
            budgets = emptyList(),
            systemStatus = SystemStatus(
                backendConnected = false,
                esp32Connected = false,
                lastUpdate = electricity.timestamp,
                dataSource = dataSources.currentType(),
            ),
        )
    }

    private suspend fun cacheElectrical(reading: ElectricalReading) {
        readingDao.insertElectrical(reading.toEntity())
        readingDao.trimElectrical(READINGS_TO_KEEP)
    }

    private suspend fun cacheWater(reading: WaterReading) {
        readingDao.insertWater(reading.toEntity())
        readingDao.trimWater(READINGS_TO_KEEP)
    }

    private suspend fun <T> fetchOrCache(
        fetch: suspend () -> T,
        cached: suspend () -> T?,
    ): DataResult<T> = try {
        DataResult(fetch())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        cached()?.let { DataResult(it, fromCache = true) } ?: throw e
    }

    private companion object {
        const val READINGS_TO_KEEP = 100
    }
}
