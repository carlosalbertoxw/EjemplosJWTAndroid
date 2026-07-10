package com.carlosalbertoxw.ejemplos_jwt_android.auth

import com.squareup.moshi.Json

/** Cuerpo de POST /api/auth/login */
data class LoginRequest(
    val username: String,
    val password: String,
)

/** Cuerpo de POST /api/auth/refresh y POST /api/auth/logout */
data class RefreshRequest(
    @Json(name = "refresh_token") val refreshToken: String,
)

/**
 * Respuesta de login y refresh: un access token (JWT de corta vida que se
 * envía en el encabezado Authorization) y un refresh token (opaco, de larga
 * vida, que solo sirve para pedir un par nuevo).
 */
data class TokenPair(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "token_type") val tokenType: String?,
    @Json(name = "expires_in") val expiresIn: Long?,
)

/** Respuesta de GET /api/auth/me: los claims que el servidor extrae del access token */
data class Me(
    val id: String?,
    val username: String?,
    val role: String?,
    @Json(name = "token_expires_at") val tokenExpiresAt: Long?,
)

/** Respuestas informativas, p. ej. la de logout: {"message": "..."} */
data class MessageResponse(
    val message: String?,
)

/**
 * Sesión guardada en el dispositivo. sessionId identifica un login concreto:
 * cambia al iniciar sesión pero se conserva cuando solo se renuevan tokens.
 */
data class Session(
    val accessToken: String,
    val refreshToken: String,
    val sessionId: String,
)
