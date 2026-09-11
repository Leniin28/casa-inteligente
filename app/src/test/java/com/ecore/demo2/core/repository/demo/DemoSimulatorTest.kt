package com.ecore.demo2.core.repository.demo

import com.ecore.demo2.core.model.HistoryPeriod
import com.ecore.demo2.core.model.ResourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class DemoSimulatorTest {

    private val simulator = DemoSimulator(ZoneOffset.UTC)
    private val now = Instant.parse("2026-09-11T18:30:00Z")

    @Test
    fun `same instant always produces the same values`() {
        assertEquals(simulator.electricalReading(now), DemoSimulator(ZoneOffset.UTC).electricalReading(now))
        assertEquals(simulator.waterReading(now), simulator.waterReading(now))
        assertEquals(simulator.history(HistoryPeriod.WEEK, now), simulator.history(HistoryPeriod.WEEK, now))
    }

    @Test
    fun `electrical reading is physically coherent for a 12 V model`() {
        val reading = simulator.electricalReading(now)

        assertTrue(reading.voltage in 11.5..12.5)
        assertTrue(reading.power > 0.0 && reading.power < 40.0)
        assertEquals(reading.power / reading.voltage, reading.current, 0.01)
    }

    @Test
    fun `energy and liters of today only grow during the day`() {
        val earlier = now.minus(3, ChronoUnit.HOURS)

        assertTrue(simulator.electricalReading(now).energyTodayKwh >= simulator.electricalReading(earlier).energyTodayKwh)
        assertTrue(simulator.waterReading(now).litersToday >= simulator.waterReading(earlier).litersToday)
    }

    @Test
    fun `history has one record per hour or per day`() {
        assertEquals(24, simulator.history(HistoryPeriod.DAY, now).size)
        assertEquals(7, simulator.history(HistoryPeriod.WEEK, now).size)
        assertEquals(30, simulator.history(HistoryPeriod.MONTH, now).size)
    }

    @Test
    fun `budget usage matches the month history`() {
        val budget = simulator.budget(ResourceType.ELECTRICITY, 5.0, now)
        // Del 1 al 11 de septiembre = 11 registros diarios del historial mensual.
        val monthSum = simulator.history(HistoryPeriod.MONTH, now).takeLast(11).sumOf { it.electricityKwh }

        assertEquals(monthSum, budget.currentUsage, 0.01)
        assertTrue(budget.estimatedFinalUsage >= budget.currentUsage)
    }

    @Test
    fun `devices and alerts come from a fixed catalog`() {
        assertEquals(5, simulator.devices(now).size)
        val alerts = simulator.alerts(now, readIds = setOf("alert-system-reconnected"))
        assertEquals(5, alerts.size)
        assertEquals(1, alerts.count { it.read })
    }
}
