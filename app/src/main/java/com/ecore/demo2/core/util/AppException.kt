package com.ecore.demo2.core.util

import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

/** Error de negocio con un mensaje ya pensado para mostrarse al usuario. */
class AppException(message: String) : Exception(message)

/** Traduce cualquier error a un texto legible. Los ViewModels usan esto; las pantallas solo muestran el texto. */
fun Throwable.toUserMessage(): String = when (this) {
    is AppException -> message ?: GENERIC_ERROR
    is HttpException -> when (code()) {
        400, 422 -> "Datos no válidos."
        401 -> "Sesión no válida o credenciales incorrectas."
        404 -> "El recurso no existe en el backend."
        409 -> "Ya existe una cuenta con ese email."
        in 500..599 -> "Error en el servidor (${code()})."
        else -> "Error del servidor (${code()})."
    }
    is IOException -> "No se pudo conectar con el backend. Revisa la URL en Ajustes y que la app " +
        "tenga el permiso de red local (dispositivos cercanos)."
    is SerializationException -> "El backend respondió con un formato inesperado."
    is IllegalArgumentException -> "Configuración no válida: ${message ?: "revisa la URL del backend"}"
    else -> message ?: GENERIC_ERROR
}

private const val GENERIC_ERROR = "Ha ocurrido un error inesperado."
