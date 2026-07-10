package com.carlosalbertoxw.ejemplos_jwt_android.auth

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore(name = "session")

/**
 * Persiste el par de tokens en DataStore, cifrados con una clave del
 * Android Keystore (ver TokenCipher). La app nunca conoce la clave con la
 * que el servidor firma los JWT: solo guarda y reenvía los tokens emitidos.
 *
 * Mantiene además una caché en memoria para que el interceptor no tenga que
 * releer (y descifrar) el disco en cada petición HTTP.
 */
class TokenStore(private val context: Context) : SessionStorage {

    @Volatile
    private var cached: Session? = null

    @Volatile
    private var cacheLoaded = false

    override val session: Flow<Session?> = context.dataStore.data.map { prefs ->
        readSession(prefs).also {
            cached = it
            cacheLoaded = true
        }
    }

    override fun current(): Session? {
        if (!cacheLoaded) {
            synchronized(this) {
                if (!cacheLoaded) {
                    cached = runBlocking { session.first() }
                    cacheLoaded = true
                }
            }
        }
        return cached
    }

    override suspend fun startSession(tokens: TokenPair) {
        val sessionId = UUID.randomUUID().toString()
        context.dataStore.edit { prefs ->
            prefs[ACCESS] = TokenCipher.encrypt(tokens.accessToken)
            prefs[REFRESH] = TokenCipher.encrypt(tokens.refreshToken)
            prefs[SESSION_ID] = sessionId
        }
        cached = Session(tokens.accessToken, tokens.refreshToken, sessionId)
        cacheLoaded = true
    }

    override suspend fun update(tokens: TokenPair) {
        var sessionId = cached?.sessionId
        context.dataStore.edit { prefs ->
            sessionId = prefs[SESSION_ID] ?: sessionId
            prefs[ACCESS] = TokenCipher.encrypt(tokens.accessToken)
            prefs[REFRESH] = TokenCipher.encrypt(tokens.refreshToken)
        }
        cached = sessionId?.let { Session(tokens.accessToken, tokens.refreshToken, it) }
        cacheLoaded = true
    }

    override suspend fun clear() {
        context.dataStore.edit { it.clear() }
        cached = null
        cacheLoaded = true
    }

    override fun updateBlocking(tokens: TokenPair) = runBlocking { update(tokens) }

    override fun clearBlocking() = runBlocking { clear() }

    private fun readSession(prefs: Preferences): Session? {
        val access = prefs[ACCESS]?.let(TokenCipher::decrypt)
        val refresh = prefs[REFRESH]?.let(TokenCipher::decrypt)
        val id = prefs[SESSION_ID]
        return if (access != null && refresh != null && id != null) {
            Session(access, refresh, id)
        } else {
            null
        }
    }

    private companion object {
        val ACCESS = stringPreferencesKey("access_token")
        val REFRESH = stringPreferencesKey("refresh_token")
        val SESSION_ID = stringPreferencesKey("session_id")
    }
}
