package com.ecore.demo2.core.model

enum class DeviceType { LIGHTING, FAN, PUMP, ELECTRONICS, APPLIANCE, OTHER }

enum class DeviceStatus { ACTIVE, PROBABLY_ACTIVE, INACTIVE, UNKNOWN }

/**
 * Dispositivo detectado/estimado a partir del consumo.
 *
 * Por ahora la app NO controla dispositivos: [controllable] queda reservado para el futuro.
 */
data class Device(
    val id: String,
    val name: String,
    val type: DeviceType,
    val estimatedPowerWatts: Double,
    val status: DeviceStatus,
    /** Confianza de la estimación entre 0.0 y 1.0, si el backend la proporciona. */
    val confidence: Double? = null,
    val controllable: Boolean = false,
)
