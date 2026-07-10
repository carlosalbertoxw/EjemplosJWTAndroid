package com.carlosalbertoxw.ejemplos_jwt_android.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carlosalbertoxw.ejemplos_jwt_android.R
import com.carlosalbertoxw.ejemplos_jwt_android.core.ApiFailure
import com.carlosalbertoxw.ejemplos_jwt_android.core.apiResult
import com.carlosalbertoxw.ejemplos_jwt_android.core.toApiFailure
import com.carlosalbertoxw.ejemplos_jwt_android.products.Product
import com.carlosalbertoxw.ejemplos_jwt_android.products.ProductsRepository
import kotlinx.coroutines.launch

data class PublicCatalogUiState(
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val error: ApiFailure? = null,
)

class PublicCatalogViewModel(private val products: ProductsRepository) : ViewModel() {

    var uiState by mutableStateOf(PublicCatalogUiState())
        private set

    init {
        load()
    }

    /** GET /api/products/public: el único recurso de la API que no pide token. */
    fun load() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            apiResult { products.publicCatalog() }
                .onSuccess {
                    uiState = uiState.copy(products = it, isLoading = false)
                }
                .onFailure {
                    uiState = uiState.copy(isLoading = false, error = it.toApiFailure())
                }
        }
    }
}

/**
 * Primera pantalla de la app: el catálogo público, visible sin sesión.
 * Desde aquí se accede al login con el botón de la barra superior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicCatalogScreen(vm: PublicCatalogViewModel, onLoginClick: () -> Unit) {
    val state = vm.uiState

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.catalog_title)) },
                actions = {
                    Button(
                        onClick = onLoginClick,
                        modifier = Modifier.padding(end = 12.dp),
                    ) {
                        Text(stringResource(R.string.catalog_login_button))
                    }
                },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = vm::load,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                Text(
                    stringResource(R.string.catalog_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))

                state.error?.let {
                    Text(it.asText(), color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = vm::load) {
                        Text(stringResource(R.string.retry))
                    }
                    Spacer(Modifier.height(8.dp))
                }

                ProductListCard(products = state.products, isLoading = state.isLoading)
            }
        }
    }
}
