package com.ecore.demo2.core.model

/** Resumen para la pantalla de inicio (equivale a GET /api/dashboard). */
data class DashboardSummary(
    val electricity: ElectricalReading,
    val water: WaterReading,
    val activeDevices: Int,
    val unreadAlerts: Int,
    val budgets: List<Budget>,
    val systemStatus: SystemStatus,
)
