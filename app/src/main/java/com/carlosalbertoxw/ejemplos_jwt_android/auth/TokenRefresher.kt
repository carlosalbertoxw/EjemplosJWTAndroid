package com.carlosalbertoxw.ejemplos_jwt_android.auth

import java.io.IOException

/** Resultado de intentar renovar el par de tokens. */
sealed interface RefreshOutcome {
    /** Hay un access token vigente (recién emitido o renovado por otro hilo). */
    data class Refreshed(val accessToken: String) : RefreshOutcome

    /** El servidor rechazó el refresh token (inválido, expirado o revocado): la sesión murió. */
    data object SessionExpired : RefreshOutcome

    /** No hubo respuesta del servidor; la sesión se conserva para reintentar. */
    data class NetworkError(val cause: IOException) : RefreshOutcome
}

/**
 * Único punto donde se canjea el refresh token. Lo usan tanto la renovación
 * automática ante un 401 (TokenAuthenticator) como el botón "Renovar tokens".
 *
 * Todas las renovaciones pasan por el mismo lock: con rotación de refresh
 * tokens, dos canjes simultáneos del mismo token harían que el servidor
 * detectara un "reuso" y revocara todas las sesiones del usuario.
 */
class TokenRefresher(
    private val storage: SessionStorage,
    private val authApi: AuthApi,
) {

    /**
     * Renueva el par de tokens de forma síncrona (bloqueante: llamar fuera del
     * hilo principal).
     *
     * @param staleAccessToken el access token que falló. Si mientras se esperaba
     *   el lock otro hilo ya lo renovó, no se vuelve a canjear: basta con usar el
     *   token actual. Con null se fuerza la renovación (botón manual).
     */
    @Synchronized
    fun refresh(staleAccessToken: String?): RefreshOutcome {
        val session = storage.current() ?: return RefreshOutcome.SessionExpired

        if (staleAccessToken != null && staleAccessToken != session.accessToken) {
            return RefreshOutcome.Refreshed(session.accessToken)
        }

        val response = try {
            authApi.refresh(RefreshRequest(session.refreshToken)).execute()
        } catch (e: IOException) {
            return RefreshOutcome.NetworkError(e)
        }

        val newTokens = response.body()
        if (!response.isSuccessful || newTokens == null) {
            // Refresh token inválido, expirado o revocado: la sesión murió.
            storage.clearBlocking()
            return RefreshOutcome.SessionExpired
        }

        storage.updateBlocking(newTokens)
        return RefreshOutcome.Refreshed(newTokens.accessToken)
    }
}
