package com.carlosalbertoxw.ejemplos_jwt_android.core

import com.squareup.moshi.JsonClass
import java.io.IOException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

/** Formato de error de la API: {"error": "..."} */
@JsonClass(generateAdapter = true)
data class ApiError(
    val error: String?,
)

/**
 * Errores de la API representados como datos, sin texto: la capa de UI los
 * traduce a mensajes con recursos de strings (localizables y testeables).
 */
sealed interface ApiFailure {

    /** El servidor respondió con un código de error. */
    data class Http(
        val code: Int,
        /** El mensaje del cuerpo {"error": "..."} si la API lo envió. */
        val apiMessage: String?,
        /** Segundos del encabezado Retry-After en un 429, si vino. */
        val retryAfterSeconds: Long?,
    ) : ApiFailure

    /** No hubo respuesta: servidor caído, sin red, URL base incorrecta… */
    data object Network : ApiFailure

    data class Unknown(val detail: String?) : ApiFailure
}

fun Throwable.toApiFailure(): ApiFailure = when (this) {
    is HttpException -> {
        val apiMessage = try {
            response()?.errorBody()?.string()
                ?.let { MoshiProvider.apiErrorAdapter.fromJson(it)?.error }
        } catch (e: Exception) {
            null
        }
        ApiFailure.Http(
            code = code(),
            apiMessage = apiMessage,
            retryAfterSeconds = response()?.headers()?.get("Retry-After")?.toLongOrNull(),
        )
    }
    is IOException -> ApiFailure.Network
    else -> ApiFailure.Unknown(message)
}

/**
 * Ejecuta una llamada a la API convirtiendo sus fallos en Result.failure,
 * pero dejando pasar la cancelación de la corrutina: cancelar no es un
 * error y tragársela rompería la cooperación entre corrutinas.
 */
suspend fun <T> apiResult(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}
