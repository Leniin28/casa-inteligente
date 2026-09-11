package com.ecore.demo2.core.model

import java.time.LocalDate

enum class ResourceType(val unit: String) {
    ELECTRICITY("kWh"),
    WATER("L"),
}

/**
 * Presupuesto de un recurso para un periodo (normalmente el mes actual).
 * Las proyecciones las calcula el backend o el repositorio, nunca la UI.
 */
data class Budget(
    val id: String,
    val resourceType: ResourceType,
    val limit: Double,
    val currentUsage: Double,
    val estimatedFinalUsage: Double,
    /** Fecha estimada en la que se alcanzará el límite, o null si no se alcanzará en el periodo. */
    val estimatedLimitDate: LocalDate? = null,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
) {
    val usedFraction: Double get() = if (limit > 0) currentUsage / limit else 0.0
    val isProjectedOverLimit: Boolean get() = estimatedFinalUsage > limit
}
