package com.carlosalbertoxw.ejemplos_jwt_android

import com.carlosalbertoxw.ejemplos_jwt_android.auth.JwtDecoder
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JwtDecoderTest {

    /** Arma un JWT sintáctico (la firma es falsa: aquí solo se lee el payload). */
    private fun fakeJwt(payloadJson: String): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val header = encoder.encodeToString("""{"alg":"HS256","typ":"JWT"}""".toByteArray())
        val payload = encoder.encodeToString(payloadJson.toByteArray())
        return "$header.$payload.firma-falsa"
    }

    @Test
    fun `decodifica el payload de un jwt`() {
        val json = """{"sub":"1","role":"Admin"}"""
        assertEquals(json, JwtDecoder.payload(fakeJwt(json)))
    }

    @Test
    fun `devuelve null si el texto no es un jwt`() {
        assertNull(JwtDecoder.payload("no-es-un-jwt"))
        assertNull(JwtDecoder.payload("solo.dos"))
        assertNull(JwtDecoder.payload(""))
    }

    @Test
    fun `devuelve null si el payload no es base64url valido`() {
        assertNull(JwtDecoder.payload("cabecera.%%%no-base64%%%.firma"))
    }
}
