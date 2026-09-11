package com.ecore.demo2.core.repository

import com.ecore.demo2.core.datastore.SessionStore
import com.ecore.demo2.core.model.AuthSession
import com.ecore.demo2.core.model.User
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

interface AuthRepository {
    /** Sesión actual (null = no hay usuario autenticado). */
    val session: Flow<AuthSession?>

    suspend fun login(email: String, password: String): User
    suspend fun register(name: String, email: String, password: String): User

    /** Cierra sesión en el backend (si es posible) y borra la sesión local. */
    suspend fun logout()

    /** Borra solo la sesión local, sin llamar al backend. */
    suspend fun clearLocalSession()

    suspend fun requestPasswordReset(email: String)

    /** Comprueba el token contra el backend y cierra sesión si ya no es válido (401). */
    suspend fun validateSession()
}

@Singleton
class DefaultAuthRepository @Inject constructor(
    private val dataSources: DataSourceProvider,
    private val sessionStore: SessionStore,
) : AuthRepository {

    override val session: Flow<AuthSession?> = sessionStore.session

    override suspend fun login(email: String, password: String): User {
        val session = dataSources.current().login(email.trim(), password)
        sessionStore.save(session)
        return session.user
    }

    override suspend fun register(name: String, email: String, password: String): User {
        val session = dataSources.current().register(name.trim(), email.trim(), password)
        sessionStore.save(session)
        return session.user
    }

    override suspend fun logout() {
        try {
            dataSources.current().logout()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Sin conexión: igualmente cerramos la sesión local.
        } finally {
            sessionStore.clear()
        }
    }

    override suspend fun clearLocalSession() = sessionStore.clear()

    override suspend fun requestPasswordReset(email: String) =
        dataSources.current().requestPasswordReset(email.trim())

    override suspend fun validateSession() {
        try {
            dataSources.current().currentUser()
        } catch (e: HttpException) {
            if (e.code() == 401) sessionStore.clear()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Sin conexión: mantenemos la sesión para poder usar la caché.
        }
    }
}
