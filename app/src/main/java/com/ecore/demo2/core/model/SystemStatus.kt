package com.ecore.demo2.core.model

import java.time.Instant

/** Origen de los datos de la app. Las pantallas no deben ramificar su lógica según este valor. */
enum class DataSourceType { DEMO, API }

data class SystemStatus(
    val backendConnected: Boolean,
    val esp32Connected: Boolean,
    val lastUpdate: Instant?,
    val dataSource: DataSourceType,
    /** Modo informado por el backend ("demo" = simulado, "sensors" = hardware real), si lo hay. */
    val backendMode: String? = null,
)
