package com.ecore.demo2.navigation

import kotlinx.serialization.Serializable

/**
 * Rutas tipadas de Navigation Compose. Los "Graph" agrupan las pantallas de cada pestaña
 * de la barra inferior.
 */
object Routes {
    // Autenticación (sin barra inferior)
    @Serializable data object Splash
    @Serializable data object Login
    @Serializable data object Register
    @Serializable data object ForgotPassword

    // Pestaña Inicio
    @Serializable data object HomeGraph
    @Serializable data object Dashboard

    // Pestaña Datos
    @Serializable data object DataGraph
    @Serializable data object DataMenu
    @Serializable data object Electricity
    @Serializable data object Water
    @Serializable data object Devices
    @Serializable data object History

    // Pestaña IA
    @Serializable data object AssistantGraph
    @Serializable data object Assistant

    // Pestaña Alertas
    @Serializable data object AlertsGraph
    @Serializable data object Alerts

    // Pestaña Más
    @Serializable data object MoreGraph
    @Serializable data object MoreMenu
    @Serializable data object Budgets
    @Serializable data object Profile
    @Serializable data object Settings
    @Serializable data object HouseSettings
    @Serializable data object NotificationSettings
    @Serializable data object SystemStatus
    @Serializable data object About
}
