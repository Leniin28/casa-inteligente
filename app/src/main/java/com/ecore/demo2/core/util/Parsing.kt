package com.ecore.demo2.core.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

/** Acepta "2026-09-11T10:00:00Z", con offset, o sin zona (se asume UTC). */
fun parseInstant(value: String): Instant = try {
    OffsetDateTime.parse(value).toInstant()
} catch (_: DateTimeParseException) {
    LocalDateTime.parse(value).toInstant(ZoneOffset.UTC)
}

/** Convierte "probably_active" -> DeviceStatus.PROBABLY_ACTIVE, o null si no existe. */
inline fun <reified T : Enum<T>> enumValueOrNull(value: String?): T? =
    enumValues<T>().firstOrNull { it.name.equals(value, ignoreCase = true) }

/** Formato de los enums en la API: minúsculas con guion bajo. */
fun Enum<*>.toApiValue(): String = name.lowercase()
