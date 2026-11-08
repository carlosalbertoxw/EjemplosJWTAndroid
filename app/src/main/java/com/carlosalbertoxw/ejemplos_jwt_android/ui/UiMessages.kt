package com.carlosalbertoxw.ejemplos_jwt_android.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.carlosalbertoxw.ejemplos_jwt_android.R
import com.carlosalbertoxw.ejemplos_jwt_android.core.ApiFailure

/**
 * Traduce los errores de la API (datos) a texto localizable. Es el único
 * punto donde un ApiFailure se convierte en palabras.
 */
@Composable
fun ApiFailure.asText(): String = when (this) {
    ApiFailure.Network -> stringResource(R.string.error_network)

    is ApiFailure.Unknown -> detail ?: stringResource(R.string.error_unknown)

    is ApiFailure.Http -> {
        val base = apiMessage
            ?: if (code == 429) {
                stringResource(R.string.error_too_many_attempts)
            } else {
                stringResource(R.string.error_http, code)
            }
        val wait = if (code == 429 && retryAfterSeconds != null) {
            val seconds = retryAfterSeconds.toInt()
            " " + pluralStringResource(R.plurals.error_retry_after, seconds, seconds)
        } else {
            ""
        }
        base + wait
    }
}

/** Traduce los mensajes de éxito de la pantalla principal. */
@Composable
fun HomeMessage.asText(): String = when (this) {
    HomeMessage.TokensRefreshed -> stringResource(R.string.msg_tokens_refreshed)
    is HomeMessage.ProductCreated ->
        stringResource(R.string.msg_product_created, name ?: "", id)
}
