package com.carlosalbertoxw.ejemplos_jwt_android.auth

import com.carlosalbertoxw.ejemplos_jwt_android.core.MoshiProvider
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
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
 * Prueba el punto único de renovación: varias renovaciones simultáneas del
 * mismo token deben producir UN solo canje en el servidor (con rotación, un
 * segundo canje se detectaría como robo y revocaría todas las sesiones).
 */
class TokenRefresherTest {

    private lateinit var server: MockWebServer
    private lateinit var storage: FakeSessionStorage
    private lateinit var refresher: TokenRefresher

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
        refresher = TokenRefresher(storage, authApi)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `renovaciones simultaneas del mismo token hacen un solo canje`() {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"access_token":"access-nuevo","refresh_token":"refresh-nuevo","token_type":"Bearer","expires_in":900}"""
            )
        )
        val threads = 6
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(threads)
        val results = (1..threads).map {
            pool.submit<RefreshOutcome> {
                start.await()
                refresher.refresh(staleAccessToken = "access-viejo")
            }
        }
        start.countDown()
        val outcomes = results.map { it.get(5, TimeUnit.SECONDS) }
        pool.shutdown()

        assertEquals(1, server.requestCount)
        assertTrue(outcomes.all { it == RefreshOutcome.Refreshed("access-nuevo") })
        assertEquals("refresh-nuevo", storage.current()?.refreshToken)
    }

    @Test
    fun `si el servidor rechaza el refresh borra la sesion`() {
        server.enqueue(MockResponse().setResponseCode(401))

        val outcome = refresher.refresh(staleAccessToken = null)

        assertEquals(RefreshOutcome.SessionExpired, outcome)
        assertNull(storage.current())
    }

    @Test
    fun `sin red conserva la sesion para reintentar`() {
        server.shutdown()

        val outcome = refresher.refresh(staleAccessToken = null)

        assertTrue(outcome is RefreshOutcome.NetworkError)
        assertEquals("refresh-viejo", storage.current()?.refreshToken)
    }
}
