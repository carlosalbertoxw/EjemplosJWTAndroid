package com.carlosalbertoxw.ejemplos_jwt_android.auth

import kotlinx.coroutines.flow.Flow

/**
 * Contrato de almacenamiento de la sesión. El interceptor, el authenticator
 * y el repositorio dependen de esta interfaz y no de una implementación
 * concreta, lo que permite probarlos con un almacén en memoria.
 */
interface SessionStorage {

    /** La UI observa este flujo: null = no hay sesión iniciada. */
    val session: Flow<Session?>

    /** Lectura síncrona para los hilos de red de OkHttp. */
    fun current(): Session?

    /** Login: guarda los tokens y estrena identificador de sesión. */
    suspend fun startSession(tokens: TokenPair)

    /** Logout: borra la sesión. */
    suspend fun clear()

    /**
     * Refresh: reemplaza los tokens conservando la misma sesión. Es bloqueante
     * porque lo llama el TokenRefresher desde los hilos de red de OkHttp.
     */
    fun updateBlocking(tokens: TokenPair)

    /** Borra la sesión desde un hilo de red (refresh rechazado). */
    fun clearBlocking()
}
