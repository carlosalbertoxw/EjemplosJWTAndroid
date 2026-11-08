package com.carlosalbertoxw.ejemplos_jwt_android.auth

import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * OkHttp lo invoca automáticamente cuando el servidor responde 401 (access
 * token vencido o inválido). Canjea el refresh token por un par nuevo
 * (rotación) mediante el TokenRefresher compartido y reintenta la petición
 * original con el token recién emitido. Si el refresh también falla, el
 * refresher borra la sesión y la UI vuelve al login.
 */
class TokenAuthenticator(
    private val refresher: TokenRefresher,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Ya se reintentó con un token renovado y sigue dando 401: rendirse.
        if (response.priorResponse != null) return null

        val failedWith = response.request.header("Authorization")
            ?.removePrefix("Bearer ")
            ?: return null

        return when (val outcome = refresher.refresh(failedWith)) {
            is RefreshOutcome.Refreshed -> response.request.newBuilder()
                .header("Authorization", "Bearer ${outcome.accessToken}")
                .build()
            RefreshOutcome.SessionExpired, is RefreshOutcome.NetworkError -> null
        }
    }
}
