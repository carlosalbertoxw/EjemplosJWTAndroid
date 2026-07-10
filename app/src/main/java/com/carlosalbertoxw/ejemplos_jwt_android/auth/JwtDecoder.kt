package com.carlosalbertoxw.ejemplos_jwt_android.auth

import com.carlosalbertoxw.ejemplos_jwt_android.core.MoshiProvider
import java.util.Base64

/**
 * Utilidades para INSPECCIONAR un JWT en el cliente.
 *
 * Un JWT tiene tres partes separadas por puntos: header.payload.firma.
 * El header y el payload son solo Base64Url, así que cualquiera puede
 * leerlos; lo que garantiza su autenticidad es la firma, y esa solo puede
 * verificarla el servidor, que es quien conoce la clave secreta. Por eso
 * la app nunca firma ni verifica tokens: únicamente los transporta.
 */
object JwtDecoder {

    /** Devuelve el payload (claims) del JWT como texto JSON, o null si no parece un JWT. */
    fun payload(jwt: String): String? {
        val parts = jwt.split(".")
        if (parts.size != 3) return null
        return try {
            String(Base64.getUrlDecoder().decode(parts[1]), Charsets.UTF_8)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /** El payload formateado con sangría, para mostrarlo en pantalla. */
    fun prettyPayload(jwt: String): String? {
        val json = payload(jwt) ?: return null
        return try {
            val adapter = MoshiProvider.moshi.adapter(Any::class.java).indent("  ")
            adapter.toJson(adapter.fromJson(json))
        } catch (e: Exception) {
            json
        }
    }
}
