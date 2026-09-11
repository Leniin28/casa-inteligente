package com.ecore.demo2.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ecore.demo2.core.model.AuthSession
import com.ecore.demo2.core.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Guarda solo el token de sesión y los datos básicos del usuario. Nunca contraseñas. */
interface SessionStore {
    val session: Flow<AuthSession?>

    suspend fun save(session: AuthSession)
    suspend fun clear()
}

@Singleton
class DataStoreSessionStore @Inject constructor(
    @SessionPreferences private val dataStore: DataStore<Preferences>,
) : SessionStore {

    override val session: Flow<AuthSession?> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            val token = prefs[Keys.TOKEN] ?: return@map null
            AuthSession(
                token = token,
                user = User(
                    id = prefs[Keys.USER_ID].orEmpty(),
                    name = prefs[Keys.USER_NAME].orEmpty(),
                    email = prefs[Keys.USER_EMAIL].orEmpty(),
                ),
            )
        }
        .distinctUntilChanged()

    override suspend fun save(session: AuthSession) {
        dataStore.edit {
            it[Keys.TOKEN] = session.token
            it[Keys.USER_ID] = session.user.id
            it[Keys.USER_NAME] = session.user.name
            it[Keys.USER_EMAIL] = session.user.email
        }
    }

    override suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private object Keys {
        val TOKEN = stringPreferencesKey("token")
        val USER_ID = stringPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
    }
}
