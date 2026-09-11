package com.ecore.demo2.core.util

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object Validators {
    const val MIN_PASSWORD_LENGTH = 6

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email.trim())

    fun isValidPassword(password: String): Boolean = password.length >= MIN_PASSWORD_LENGTH

    /**
     * Normaliza la URL del backend ("http://10.0.2.2:8000" -> "http://10.0.2.2:8000/").
     * Devuelve null si no es una URL http/https válida.
     */
    fun normalizeBaseUrl(input: String): String? {
        val trimmed = input.trim()
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) return null
        val withSlash = if (trimmed.endsWith("/")) trimmed else "$trimmed/"
        return withSlash.toHttpUrlOrNull()?.toString()
    }
}
