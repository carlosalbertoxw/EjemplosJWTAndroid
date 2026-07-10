package com.carlosalbertoxw.ejemplos_jwt_android.auth

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Emisión de tokens. Va por un cliente OkHttp SIN interceptor ni
 * authenticator: no necesita access token y así el proceso de renovación
 * nunca puede entrar en bucle.
 */
interface AuthApi {

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): TokenPair

    /**
     * Devuelve Call en lugar de ser suspend para poder ejecutarlo de forma
     * síncrona dentro del Authenticator de OkHttp (.execute()) y de forma
     * asíncrona desde una corrutina (.await()).
     */
    @POST("api/auth/refresh")
    fun refresh(@Body body: RefreshRequest): Call<TokenPair>
}

/**
 * Endpoints de sesión que sí requieren access token: van por el cliente
 * autenticado (interceptor + renovación automática ante un 401).
 */
interface SessionApi {

    /** Claims del usuario autenticado. */
    @GET("api/auth/me")
    suspend fun me(): Me

    /** Revoca el refresh token en el servidor: la sesión ya no podrá renovarse. */
    @POST("api/auth/logout")
    suspend fun logout(@Body body: RefreshRequest): MessageResponse
}
