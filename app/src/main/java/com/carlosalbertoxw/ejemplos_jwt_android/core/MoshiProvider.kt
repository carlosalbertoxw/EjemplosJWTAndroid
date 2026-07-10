package com.carlosalbertoxw.ejemplos_jwt_android.core

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * Instancia única de Moshi para toda la app. A diferencia de Gson, Moshi
 * respeta la null-safety de Kotlin al deserializar: si el servidor omite un
 * campo declarado no-nullable, la petición falla con un error claro en vez
 * de producir un NPE diferido lejos del origen.
 */
object MoshiProvider {

    val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val apiErrorAdapter: JsonAdapter<ApiError> by lazy {
        moshi.adapter(ApiError::class.java)
    }
}
