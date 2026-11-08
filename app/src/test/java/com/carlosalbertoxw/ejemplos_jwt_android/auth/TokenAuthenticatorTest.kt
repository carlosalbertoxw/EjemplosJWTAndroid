package com.carlosalbertoxw.ejemplos_jwt_android.auth

import com.carlosalbertoxw.ejemplos_jwt_android.core.MoshiProvider
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Prueba el flujo completo 401 -> refresh -> reintento contra un servidor
 * HTTP falso (MockWebServer), con el interceptor y el authenticator reales.
 */
class TokenAuthenticatorTest {

    private lateinit var server: MockWebServer
    private lateinit var storage: FakeSessionStorage
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        storage = FakeSessionStorage(Session("access-viejo", "refresh-viejo", "s1"))

        val authApi = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(MoshiConverterFactory.create(MoshiProvider.moshi))
            .build()
            .create(AuthApi::class.java)

        client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(storage))
            .authenticator(TokenAuthenticator(TokenRefresher(storage, authApi)))
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun callProtectedEndpoint() = client
        .newCall(Request.Builder().url(server.url("/api/protegido")).build())
        .execute()

    private fun tokenPairJson(access: String, refresh: String) =
        """{"access_token":"$access","refresh_token":"$refresh","token_type":"Bearer","expires_in":900}"""

    @Test
    fun `ante un 401 renueva el par y reintenta con el token nuevo`() {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(200).setBody(tokenPairJson("access-nuevo", "refresh-nuevo")))
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

        val response = callProtectedEndpoint()

        assertEquals(200, response.code)

        // 1a petición: con el access token viejo
        assertEquals("Bearer access-viejo", server.takeRequest().getHeader("Authorization"))
        // 2a petición: el refresh, canjeando el refresh token viejo
        val refresh = server.takeRequest()
        assertEquals("/api/auth/refresh", refresh.path)
        assertTrue(refresh.body.readUtf8().contains("refresh-viejo"))
        // 3a petición: el reintento, ya con el access token nuevo
        assertEquals("Bearer access-nuevo", server.takeRequest().getHeader("Authorization"))

        // El par rotado quedó guardado
        assertEquals("access-nuevo", storage.current()?.accessToken)
        assertEquals("refresh-nuevo", storage.current()?.refreshToken)
    }

    @Test
    fun `si el refresh tambien falla borra la sesion y devuelve el 401`() {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":"Refresh token revocado"}"""))

        val response = callProtectedEndpoint()

        assertEquals(401, response.code)
        assertNull(storage.current())
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `se rinde tras un unico reintento aunque siga el 401`() {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(200).setBody(tokenPairJson("access-nuevo", "refresh-nuevo")))
        server.enqueue(MockResponse().setResponseCode(401))

        val response = callProtectedEndpoint()

        // Sin este límite el authenticator entraría en bucle infinito.
        assertEquals(401, response.code)
        assertEquals(3, server.requestCount)
    }

    @Test
    fun `sin sesion no intenta renovar nada`() {
        storage.clearBlocking()
        server.enqueue(MockResponse().setResponseCode(401))

        val response = callProtectedEndpoint()

        assertEquals(401, response.code)
        assertEquals(1, server.requestCount)
    }
}
