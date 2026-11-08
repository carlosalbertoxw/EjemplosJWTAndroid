package com.carlosalbertoxw.ejemplos_jwt_android.auth

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Emisión y revocación de tokens. Va por un cliente OkHttp SIN interceptor ni
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

    /**
     * Revoca el refresh token en el servidor (cierra solo esta sesión). Va por
     * el cliente plano: el refresh token es la credencial y no hace falta un
     * access token vigente. Por el cliente autenticado, un 401 dispararía una
     * renovación que rotaría el refresh token y dejaría vivo el nuevo.
     */
    @POST("api/auth/logout")
    suspend fun logout(@Body body: RefreshRequest): MessageResponse
}

/**
 * Endpoints de sesión que sí requieren access token: va por el cliente
 * autenticado (interceptor + renovación automática ante un 401).
 */
interface SessionApi {

    /** Claims del usuario autenticado. */
    @GET("api/auth/me")
    suspend fun me(): Me
}
