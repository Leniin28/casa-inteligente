package com.ecore.demo2.core.model

import java.time.Instant

enum class AlertType { HIGH_CONSUMPTION, WATER_LEAK, BUDGET, DEVICE, SYSTEM, OTHER }

enum class AlertSeverity { INFO, WARNING, CRITICAL }

data class Alert(
    val id: String,
    val type: AlertType,
    val severity: AlertSeverity,
    val title: String,
    val message: String,
    val timestamp: Instant,
    val read: Boolean,
)
