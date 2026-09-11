package com.ecore.demo2.core.network

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

/** Configuración JSON compartida: tolerante a campos nuevos y con snake_case automático. */
@OptIn(ExperimentalSerializationApi::class)
val NetworkJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
    namingStrategy = JsonNamingStrategy.SnakeCase
}
