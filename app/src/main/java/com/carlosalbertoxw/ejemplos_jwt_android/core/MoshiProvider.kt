package com.carlosalbertoxw.ejemplos_jwt_android.core

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi

/**
 * Instancia única de Moshi para toda la app. A diferencia de Gson, Moshi
 * respeta la null-safety de Kotlin al deserializar: si el servidor omite un
 * campo declarado no-nullable, la petición falla con un error claro en vez
 * de producir un NPE diferido lejos del origen.
 */
object MoshiProvider {

    // Los adaptadores de los modelos (@JsonClass(generateAdapter = true)) se
    // generan en compilación con KSP: sin reflexión ni kotlin-reflect, más
    // rápido y compatible con R8 sin reglas extra.
    val moshi: Moshi = Moshi.Builder().build()

    val apiErrorAdapter: JsonAdapter<ApiError> by lazy {
        moshi.adapter(ApiError::class.java)
    }
}
