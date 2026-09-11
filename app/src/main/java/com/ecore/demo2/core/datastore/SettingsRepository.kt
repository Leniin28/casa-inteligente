package com.ecore.demo2.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ecore.demo2.BuildConfig
import com.ecore.demo2.core.model.AppSettings
import com.ecore.demo2.core.model.DataSourceType
import com.ecore.demo2.core.model.ThemeMode
import com.ecore.demo2.core.util.enumValueOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Ajustes persistidos con DataStore. Valores por defecto seguros: modo Demo y URL del emulador. */
interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDataSource(type: DataSourceType)

    /** Debe recibir una URL ya validada/normalizada (ver [com.ecore.demo2.core.util.Validators]). */
    suspend fun setBackendUrl(url: String)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setHouseName(name: String)
}

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    @SettingsPreferences private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<AppSettings> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            AppSettings(
                themeMode = enumValueOrNull<ThemeMode>(prefs[Keys.THEME_MODE]) ?: ThemeMode.SYSTEM,
                dataSource = enumValueOrNull<DataSourceType>(prefs[Keys.DATA_SOURCE]) ?: DataSourceType.DEMO,
                backendUrl = prefs[Keys.BACKEND_URL] ?: BuildConfig.DEFAULT_BACKEND_URL,
                notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
                houseName = prefs[Keys.HOUSE_NAME] ?: AppSettings.DEFAULT_HOUSE_NAME,
            )
        }
        .distinctUntilChanged()

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    override suspend fun setDataSource(type: DataSourceType) {
        dataStore.edit { it[Keys.DATA_SOURCE] = type.name }
    }

    override suspend fun setBackendUrl(url: String) {
        dataStore.edit { it[Keys.BACKEND_URL] = url }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    override suspend fun setHouseName(name: String) {
        dataStore.edit { it[Keys.HOUSE_NAME] = name }
    }

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DATA_SOURCE = stringPreferencesKey("data_source")
        val BACKEND_URL = stringPreferencesKey("backend_url")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val HOUSE_NAME = stringPreferencesKey("house_name")
    }
}
