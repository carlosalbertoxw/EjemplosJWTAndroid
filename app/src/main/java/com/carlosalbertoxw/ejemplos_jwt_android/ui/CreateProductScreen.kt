package com.carlosalbertoxw.ejemplos_jwt_android.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carlosalbertoxw.ejemplos_jwt_android.R
import com.carlosalbertoxw.ejemplos_jwt_android.core.ApiFailure
import com.carlosalbertoxw.ejemplos_jwt_android.core.apiResult
import com.carlosalbertoxw.ejemplos_jwt_android.core.toApiFailure
import com.carlosalbertoxw.ejemplos_jwt_android.products.NewProduct
import com.carlosalbertoxw.ejemplos_jwt_android.products.Product
import com.carlosalbertoxw.ejemplos_jwt_android.products.ProductsRepository
import kotlinx.coroutines.launch

data class CreateProductUiState(
    val isCreating: Boolean = false,
    val error: ApiFailure? = null,
    /** Producto recién creado, pendiente de que la UI lo consuma. */
    val created: Product? = null,
)

class CreateProductViewModel(private val products: ProductsRepository) : ViewModel() {

    var uiState by mutableStateOf(CreateProductUiState())
        private set

    /** POST /api/products: solo Admin. Con el rol User el servidor responde 403. */
    fun create(name: String, description: String, price: Double) {
        if (uiState.isCreating) return
        viewModelScope.launch {
            uiState = uiState.copy(isCreating = true, error = null)
            apiResult {
                products.create(
                    NewProduct(
                        name = name.trim(),
                        description = description.trim().takeIf { it.isNotBlank() },
                        price = price,
                    )
                )
            }
                .onSuccess {
                    uiState = uiState.copy(isCreating = false, created = it)
                }
                .onFailure {
                    uiState = uiState.copy(isCreating = false, error = it.toApiFailure())
                }
        }
    }

    /** La UI ya reaccionó al producto creado; evita re-disparar el evento. */
    fun consumeCreated() {
        uiState = uiState.copy(created = null)
    }
}

/**
 * Pantalla dedicada al alta de productos (solo rol Admin).
 * Al crearse el producto se notifica con onCreated y se vuelve a la lista.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProductScreen(
    vm: CreateProductViewModel,
    onClose: () -> Unit,
    onCreated: (Product) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    val priceValue = price.replace(',', '.').toDoubleOrNull()
    val state = vm.uiState

    LaunchedEffect(state.created) {
        val product = state.created ?: return@LaunchedEffect
        vm.consumeCreated()
        onCreated(product)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.create_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.create_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(
                stringResource(R.string.create_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.create_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.create_description)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text(stringResource(R.string.create_price)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))

            Button(
                enabled = name.isNotBlank() && priceValue != null && priceValue >= 0 && !state.isCreating,
                onClick = { vm.create(name, description, priceValue ?: return@Button) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(
                        if (state.isCreating) R.string.create_submitting else R.string.create_submit
                    )
                )
            }

            state.error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it.asText(), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
