package com.carlosalbertoxw.ejemplos_jwt_android.auth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking

/** Almacén de sesión en memoria para las pruebas: sin Android ni disco. */
class FakeSessionStorage(initial: Session? = null) : SessionStorage {

    private val state = MutableStateFlow(initial)

    override val session: Flow<Session?> = state

    override fun current(): Session? = state.value

    override suspend fun startSession(tokens: TokenPair) {
        state.value = Session(tokens.accessToken, tokens.refreshToken, "sesion-de-prueba")
    }

    override suspend fun clear() {
        state.value = null
    }

    override fun updateBlocking(tokens: TokenPair) {
        state.value = Session(
            tokens.accessToken,
            tokens.refreshToken,
            state.value?.sessionId ?: "sesion-de-prueba",
        )
    }

    override fun clearBlocking() = runBlocking { clear() }
}
