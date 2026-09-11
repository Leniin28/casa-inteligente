package com.ecore.demo2.core.model

import java.time.Instant

enum class HistoryPeriod { DAY, WEEK, MONTH }

/** Consumo agregado en un intervalo (una hora para DAY, un día para WEEK/MONTH). */
data class HistoryRecord(
    val timestamp: Instant,
    val electricityKwh: Double,
    val waterLiters: Double,
    val estimatedCost: Double? = null,
)
