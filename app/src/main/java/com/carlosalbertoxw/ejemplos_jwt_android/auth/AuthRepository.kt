package com.carlosalbertoxw.ejemplos_jwt_android.auth

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.await

/**
 * Estado de la sesión para la UI. Loading solo ocurre al arrancar, mientras
 * se lee la sesión persistida: evita mostrar el catálogo un instante cuando
 * en realidad ya había una sesión guardada.
 */
sealed interface SessionState {
    data object Loading : SessionState
    data object LoggedOut : SessionState
    data class LoggedIn(val session: Session) : SessionState
}

/**
 * Fachada de autenticación: es lo único del paquete auth que las capas de
 * UI necesitan conocer. Orquesta la API de tokens, el almacenamiento local
 * y la decodificación del JWT.
 */
class AuthRepository(
    private val authApi: AuthApi,
    private val sessionApi: SessionApi,
    private val storage: SessionStorage,
) {

    /** La UI decide la pantalla observando este flujo (con initial = Loading). */
    val sessionState: Flow<SessionState> = storage.session.map { session ->
        if (session == null) SessionState.LoggedOut else SessionState.LoggedIn(session)
    }

    /** POST /api/auth/login: canjea credenciales por un par de tokens. */
    suspend fun login(username: String, password: String) {
        val tokens = authApi.login(LoginRequest(username.trim(), password))
        storage.startSession(tokens)
    }

    /**
     * POST /api/auth/refresh manual, para observar la rotación: el refresh
     * token usado queda revocado y llega un par completamente nuevo.
     * (Ante un 401 esta renovación además ocurre sola, en TokenAuthenticator.)
     */
    suspend fun refreshTokens() {
        val current = storage.session.first() ?: return
        val tokens = authApi.refresh(RefreshRequest(current.refreshToken)).await()
        storage.update(tokens)
    }

    /** GET /api/auth/me: pide al servidor los claims del access token actual. */
    suspend fun me(): Me = sessionApi.me()

    /** Claims del access token guardado, decodificados en el dispositivo. */
    suspend fun accessTokenPayload(): String? =
        storage.session.first()?.accessToken?.let { JwtDecoder.prettyPayload(it) }

    /** POST /api/auth/logout: revoca el refresh token y borra la sesión local. */
    suspend fun logout() {
        val current = storage.session.first()
        try {
            if (current != null) {
                sessionApi.logout(RefreshRequest(current.refreshToken))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Aunque el servidor no responda, la sesión local se borra igual.
        } finally {
            // NonCancellable: el borrado local debe completarse incluso si
            // la corrutina fue cancelada a mitad del logout.
            withContext(NonCancellable) {
                storage.clear()
            }
        }
    }
}
