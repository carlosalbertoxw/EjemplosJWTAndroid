package com.carlosalbertoxw.ejemplos_jwt_android.ui

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.carlosalbertoxw.ejemplos_jwt_android.auth.AuthApi
import com.carlosalbertoxw.ejemplos_jwt_android.auth.AuthRepository
import com.carlosalbertoxw.ejemplos_jwt_android.auth.FakeSessionStorage
import com.carlosalbertoxw.ejemplos_jwt_android.auth.LoginRequest
import com.carlosalbertoxw.ejemplos_jwt_android.auth.Me
import com.carlosalbertoxw.ejemplos_jwt_android.auth.MessageResponse
import com.carlosalbertoxw.ejemplos_jwt_android.auth.RefreshRequest
import com.carlosalbertoxw.ejemplos_jwt_android.auth.SessionApi
import com.carlosalbertoxw.ejemplos_jwt_android.auth.TokenPair
import com.carlosalbertoxw.ejemplos_jwt_android.auth.TokenRefresher
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Call

/**
 * Prueba la pantalla de login como la usa una persona: escribir, pulsar
 * "Entrar" y ver la sesión iniciada o el error. Corre en la JVM con
 * Robolectric, sin emulador, contra una API y un almacén de sesión falsos.
 */
@RunWith(RobolectricTestRunner::class)
class LoginScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val storage = FakeSessionStorage()

    @Test
    fun `el boton se habilita solo con usuario y contrasena`() {
        showLogin(FakeAuthApi())

        compose.onNodeWithText("Entrar").assertIsNotEnabled()
        compose.onNodeWithText("Usuario").performTextInput("admin")
        compose.onNodeWithText("Entrar").assertIsNotEnabled()
        compose.onNodeWithText("Contraseña").performTextInput("Admin123!")
        compose.onNodeWithText("Entrar").assertIsEnabled()
    }

    @Test
    fun `un login correcto inicia la sesion con los tokens recibidos`() {
        val api = FakeAuthApi()
        showLogin(api)

        compose.onNodeWithText("Usuario").performTextInput("  admin ")
        compose.onNodeWithText("Contraseña").performTextInput("Admin123!")
        compose.onNodeWithText("Entrar").performClick()
        compose.waitUntil { storage.current() != null }

        assertEquals(LoginRequest("admin", "Admin123!"), api.lastLogin)
        assertEquals("access", storage.current()?.accessToken)
    }

    @Test
    fun `sin conexion muestra el error y no inicia sesion`() {
        showLogin(FakeAuthApi(failWith = IOException("sin red")))

        compose.onNodeWithText("Usuario").performTextInput("admin")
        compose.onNodeWithText("Contraseña").performTextInput("Admin123!")
        compose.onNodeWithText("Entrar").performClick()

        compose.onNodeWithText("No se pudo conectar", substring = true).assertExists()
        assertNull(storage.current())
    }

    private fun showLogin(api: FakeAuthApi) {
        val repository = AuthRepository(api, FakeSessionApi, storage, TokenRefresher(storage, api))
        val vm = LoginViewModel(repository)
        compose.setContent { LoginScreen(vm, onBrowseCatalog = {}) }
    }

    private class FakeAuthApi(private val failWith: Exception? = null) : AuthApi {
        var lastLogin: LoginRequest? = null

        override suspend fun login(body: LoginRequest): TokenPair {
            lastLogin = body
            failWith?.let { throw it }
            return TokenPair(accessToken = "access", refreshToken = "refresh")
        }

        override fun refresh(body: RefreshRequest): Call<TokenPair> =
            throw UnsupportedOperationException("no se usa en estas pruebas")

        override suspend fun logout(body: RefreshRequest): MessageResponse =
            throw UnsupportedOperationException("no se usa en estas pruebas")
    }

    private object FakeSessionApi : SessionApi {
        override suspend fun me(): Me = throw UnsupportedOperationException("no se usa en estas pruebas")
    }
}
