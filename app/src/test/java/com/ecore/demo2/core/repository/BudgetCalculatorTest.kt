package com.ecore.demo2.core.repository

import com.ecore.demo2.core.model.ResourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BudgetCalculatorTest {

    private val start = LocalDate.of(2026, 9, 1)
    private val end = LocalDate.of(2026, 9, 30)

    private fun project(limit: Double, usage: Double, today: LocalDate) =
        BudgetCalculator.project("electricity", ResourceType.ELECTRICITY, limit, usage, start, end, today)

    @Test
    fun `projects linearly from the daily average`() {
        // 2.5 kWh en 10 días -> 0.25 kWh/día -> 7.5 kWh en 30 días
        val budget = project(limit = 5.0, usage = 2.5, today = LocalDate.of(2026, 9, 10))

        assertEquals(7.5, budget.estimatedFinalUsage, 1e-9)
        assertEquals(LocalDate.of(2026, 9, 20), budget.estimatedLimitDate)
        assertTrue(budget.isProjectedOverLimit)
        assertEquals(0.5, budget.usedFraction, 1e-9)
    }

    @Test
    fun `no limit date when the limit is not reached within the period`() {
        val budget = project(limit = 10.0, usage = 2.5, today = LocalDate.of(2026, 9, 10))

        assertNull(budget.estimatedLimitDate)
        assertFalse(budget.isProjectedOverLimit)
    }

    @Test
    fun `limit date is today when usage already exceeds the limit`() {
        val today = LocalDate.of(2026, 9, 15)
        val budget = project(limit = 2.0, usage = 3.0, today = today)

        assertEquals(today, budget.estimatedLimitDate)
    }

    @Test
    fun `zero usage gives no projection`() {
        val budget = project(limit = 5.0, usage = 0.0, today = LocalDate.of(2026, 9, 10))

        assertEquals(0.0, budget.estimatedFinalUsage, 1e-9)
        assertNull(budget.estimatedLimitDate)
    }
}
