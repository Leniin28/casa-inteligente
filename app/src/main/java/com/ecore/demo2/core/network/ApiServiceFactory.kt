package com.ecore.demo2.core.network

import com.ecore.demo2.core.datastore.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Crea el cliente Retrofit con la URL guardada en Ajustes y lo reutiliza
 * mientras la URL no cambie (así la URL se puede editar sin reiniciar la app).
 */
@Singleton
class ApiServiceFactory @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val settingsRepository: SettingsRepository,
) {
    private val mutex = Mutex()
    private var cachedUrl: String? = null
    private var cachedApi: SmartHomeApi? = null

    suspend fun api(): SmartHomeApi {
        val url = settingsRepository.settings.first().backendUrl
        return mutex.withLock {
            cachedApi?.takeIf { cachedUrl == url } ?: create(url).also {
                cachedApi = it
                cachedUrl = url
            }
        }
    }

    private fun create(baseUrl: String): SmartHomeApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create()
}
