package com.ecore.demo2.navigation

import androidx.annotation.DrawableRes
import com.ecore.demo2.R
import kotlin.reflect.KClass

/** Pestañas de la barra inferior. Para cambiar textos/iconos basta con tocar este archivo. */
enum class TopLevelDestination(
    val label: String,
    @param:DrawableRes val iconRes: Int,
    val graph: Any,
    val graphClass: KClass<*>,
) {
    HOME("Inicio", R.drawable.ic_nav_home, Routes.HomeGraph, Routes.HomeGraph::class),
    DATA("Datos", R.drawable.ic_nav_data, Routes.DataGraph, Routes.DataGraph::class),
    ASSISTANT("IA", R.drawable.ic_nav_assistant, Routes.AssistantGraph, Routes.AssistantGraph::class),
    ALERTS("Alertas", R.drawable.ic_nav_alerts, Routes.AlertsGraph, Routes.AlertsGraph::class),
    MORE("Más", R.drawable.ic_nav_more, Routes.MoreGraph, Routes.MoreGraph::class),
}
