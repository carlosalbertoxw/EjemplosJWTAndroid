package com.carlosalbertoxw.ejemplos_jwt_android.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.carlosalbertoxw.ejemplos_jwt_android.R
import com.carlosalbertoxw.ejemplos_jwt_android.products.Product
import java.util.Locale

/**
 * Lista de productos compartida por el catálogo público y el autenticado.
 * Cuando el servidor omite el precio (catálogo público) se muestra un candado.
 */
@Composable
fun ProductListCard(
    products: List<Product>,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(modifier.fillMaxWidth()) {
        Column {
            products.forEachIndexed { index, product ->
                ListItem(
                    headlineContent = {
                        Text(product.name ?: stringResource(R.string.product_no_name))
                    },
                    supportingContent = {
                        product.description?.let { Text(it) }
                    },
                    trailingContent = {
                        Text(
                            product.price?.let { formatPrice(it) }
                                ?: stringResource(R.string.product_price_hidden),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                )
                if (index < products.lastIndex) HorizontalDivider()
            }
            if (products.isEmpty() && !isLoading) {
                Text(stringResource(R.string.product_list_empty), Modifier.padding(16.dp))
            }
        }
    }
}

// Locale fijo: el separador decimal no debe depender del idioma del teléfono.
private fun formatPrice(price: Double): String =
    String.format(Locale.US, "$ %.2f", price)
