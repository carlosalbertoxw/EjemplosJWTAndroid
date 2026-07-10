package com.carlosalbertoxw.ejemplos_jwt_android.auth

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adjunta el access token a cada petición del cliente autenticado:
 * Authorization: Bearer <access token>
 */
class AuthInterceptor(private val storage: SessionStorage) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val session = storage.current()
            ?: return chain.proceed(chain.request())

        val request = chain.request().newBuilder()
            .header("Authorization", "Bearer ${session.accessToken}")
            .build()
        return chain.proceed(request)
    }
}
