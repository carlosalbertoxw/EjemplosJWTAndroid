package com.carlosalbertoxw.ejemplos_jwt_android

import android.app.Application
import android.content.Context
import com.carlosalbertoxw.ejemplos_jwt_android.auth.AuthApi
import com.carlosalbertoxw.ejemplos_jwt_android.auth.AuthInterceptor
import com.carlosalbertoxw.ejemplos_jwt_android.auth.AuthRepository
import com.carlosalbertoxw.ejemplos_jwt_android.auth.SessionApi
import com.carlosalbertoxw.ejemplos_jwt_android.auth.SessionStorage
import com.carlosalbertoxw.ejemplos_jwt_android.auth.TokenAuthenticator
import com.carlosalbertoxw.ejemplos_jwt_android.auth.TokenRefresher
import com.carlosalbertoxw.ejemplos_jwt_android.auth.TokenStore
import com.carlosalbertoxw.ejemplos_jwt_android.core.MoshiProvider
import com.carlosalbertoxw.ejemplos_jwt_android.products.ProductsApi
import com.carlosalbertoxw.ejemplos_jwt_android.products.ProductsRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class App : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/**
 * Contenedor manual de dependencias: aquí es el único lugar donde los
 * paquetes auth, products y ui se conocen entre sí. En una app grande este
 * cableado lo haría Hilt o Koin.
 *
 * La UI solo debe usar authRepository y productsRepository.
 */
class AppContainer(context: Context) {

    private val tokenStore: SessionStorage = TokenStore(context)

    // En debug, BODY imprime en Logcat las peticiones completas (tokens
    // incluidos): útil para estudiar el flujo JWT. En release no se
    // registra nada, por construcción.
    private val logging = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    // Cliente "plano" para login y refresh: sin token ni renovación
    // automática, de modo que renovar tokens nunca pueda entrar en bucle.
    private val plainClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    private val authApi: AuthApi = retrofit(plainClient).create(AuthApi::class.java)

    // Único punto de renovación (automática y manual), serializado con un lock.
    private val refresher = TokenRefresher(tokenStore, authApi)

    // Cliente autenticado: adjunta el access token a cada petición y, ante
    // un 401, renueva el par de tokens y reintenta automáticamente.
    private val apiClient = plainClient.newBuilder()
        .addInterceptor(AuthInterceptor(tokenStore))
        .authenticator(TokenAuthenticator(refresher))
        .build()

    private val sessionApi: SessionApi = retrofit(apiClient).create(SessionApi::class.java)
    private val productsApi: ProductsApi = retrofit(apiClient).create(ProductsApi::class.java)

    val authRepository = AuthRepository(authApi, sessionApi, tokenStore, refresher)
    val productsRepository = ProductsRepository(productsApi)

    // La URL base viene de BuildConfig: se configura en app/build.gradle.kts
    // (propiedad apiBaseUrl) sin tocar el código.
    private fun retrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(MoshiProvider.moshi))
        .build()
}
