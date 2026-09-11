package com.ecore.demo2.core.model

import java.time.Instant

/**
 * Lectura eléctrica abstracta.
 *
 * No depende de ningún sensor concreto (INA226, PZEM, ...): el backend (o el modo demo)
 * es quien adapta los datos del hardware a este modelo.
 */
data class ElectricalReading(
    val timestamp: Instant,
    /** Voltios. */
    val voltage: Double,
    /** Amperios. */
    val current: Double,
    /** Vatios. */
    val power: Double,
    /** Energía acumulada desde las 00:00 en kWh. */
    val energyTodayKwh: Double,
)

/** Lectura de agua abstracta (independiente del caudalímetro usado). */
data class WaterReading(
    val timestamp: Instant,
    val flowLitersPerMinute: Double,
    /** Litros acumulados desde las 00:00. */
    val litersToday: Double,
)
