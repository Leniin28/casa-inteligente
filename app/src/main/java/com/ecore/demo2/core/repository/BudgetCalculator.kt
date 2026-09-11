package com.ecore.demo2.core.repository

import com.ecore.demo2.core.model.Budget
import com.ecore.demo2.core.model.ResourceType
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

/**
 * Proyección lineal sencilla: consumo medio diario hasta hoy × días del periodo.
 * La usa el modo demo; el backend implementa la misma lógica en Python.
 */
object BudgetCalculator {

    fun project(
        id: String,
        resourceType: ResourceType,
        limit: Double,
        currentUsage: Double,
        periodStart: LocalDate,
        periodEnd: LocalDate,
        today: LocalDate,
    ): Budget {
        val totalDays = ChronoUnit.DAYS.between(periodStart, periodEnd) + 1
        val elapsedDays = (ChronoUnit.DAYS.between(periodStart, today) + 1).coerceIn(1, totalDays)
        val dailyAverage = currentUsage / elapsedDays
        val estimatedFinalUsage = dailyAverage * totalDays

        val estimatedLimitDate = when {
            currentUsage >= limit -> today
            dailyAverage <= 0.0 -> null
            else -> {
                val daysUntilLimit = ceil((limit - currentUsage) / dailyAverage).toLong()
                today.plusDays(daysUntilLimit).takeIf { !it.isAfter(periodEnd) }
            }
        }

        return Budget(
            id = id,
            resourceType = resourceType,
            limit = limit,
            currentUsage = currentUsage,
            estimatedFinalUsage = estimatedFinalUsage,
            estimatedLimitDate = estimatedLimitDate,
            periodStart = periodStart,
            periodEnd = periodEnd,
        )
    }
}
