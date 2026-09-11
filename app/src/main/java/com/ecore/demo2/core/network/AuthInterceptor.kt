package com.ecore.demo2.core.network

import com.ecore.demo2.core.datastore.SessionStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Añade "Authorization: Bearer <token>" si hay sesión.
 * OkHttp ejecuta los interceptores en sus propios hilos de fondo, por eso aquí
 * es aceptable leer DataStore de forma bloqueante.
 */
class AuthInterceptor @Inject constructor(
    private val sessionStore: SessionStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.header(HEADER) != null) return chain.proceed(request)

        val token = runBlocking { sessionStore.session.first()?.token } ?: return chain.proceed(request)
        return chain.proceed(request.newBuilder().header(HEADER, "Bearer $token").build())
    }

    private companion object {
        const val HEADER = "Authorization"
    }
}
