package com.carlosalbertoxw.ejemplos_jwt_android.auth

import java.io.IOException
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * OkHttp lo invoca automáticamente cuando el servidor responde 401 (access
 * token vencido o inválido). Canjea el refresh token por un par nuevo
 * (rotación) y reintenta la petición original con el token recién emitido.
 * Si el refresh también falla, borra la sesión y la UI vuelve al login.
 */
class TokenAuthenticator(
    private val storage: SessionStorage,
    private val authApi: AuthApi,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Ya se reintentó con un token renovado y sigue dando 401: rendirse.
        if (response.priorResponse != null) return null

        synchronized(this) {
            val session = storage.current() ?: return null
            val failedWith = response.request.header("Authorization")
                ?.removePrefix("Bearer ")

            // Otro hilo renovó los tokens mientras esperábamos el lock:
            // basta con reintentar usando el access token actual.
            if (failedWith != session.accessToken) {
                return response.request.withBearer(session.accessToken)
            }

            val result = try {
                authApi.refresh(RefreshRequest(session.refreshToken)).execute()
            } catch (e: IOException) {
                return null
            }

            val newTokens = result.body()
            if (!result.isSuccessful || newTokens == null) {
                // Refresh token inválido, expirado o revocado: la sesión murió.
                storage.clearBlocking()
                return null
            }

            storage.updateBlocking(newTokens)
            return response.request.withBearer(newTokens.accessToken)
        }
    }

    private fun Request.withBearer(token: String): Request =
        newBuilder().header("Authorization", "Bearer $token").build()
}
