package com.carlosalbertoxw.ejemplos_jwt_android.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carlosalbertoxw.ejemplos_jwt_android.auth.AuthRepository
import com.carlosalbertoxw.ejemplos_jwt_android.auth.Me
import com.carlosalbertoxw.ejemplos_jwt_android.core.ApiFailure
import com.carlosalbertoxw.ejemplos_jwt_android.core.apiResult
import com.carlosalbertoxw.ejemplos_jwt_android.core.toApiFailure
import com.carlosalbertoxw.ejemplos_jwt_android.products.Product
import com.carlosalbertoxw.ejemplos_jwt_android.products.ProductsRepository
import kotlinx.coroutines.launch

/** Mensajes de éxito como datos: la UI los traduce con recursos de strings. */
sealed interface HomeMessage {
    data object TokensRefreshed : HomeMessage
    data class ProductCreated(val name: String?, val id: Int) : HomeMessage
}

data class HomeUiState(
    val me: Me? = null,
    val accessTokenPayload: String? = null,
    val products: List<Product> = emptyList(),
    val showPublicCatalog: Boolean = false,
    val isLoadingProducts: Boolean = false,
    val message: HomeMessage? = null,
    val error: ApiFailure? = null,
)

class HomeViewModel(
    private val auth: AuthRepository,
    private val products: ProductsRepository,
) : ViewModel() {

    var uiState by mutableStateOf(HomeUiState())
        private set

    init {
        loadMe()
        loadProducts(publicCatalog = false)
    }

    fun loadMe() {
        viewModelScope.launch {
            apiResult { auth.me() }
                .onSuccess { me ->
                    uiState = uiState.copy(
                        me = me,
                        accessTokenPayload = auth.accessTokenPayload(),
                    )
                }
                .onFailure {
                    uiState = uiState.copy(error = it.toApiFailure())
                }
        }
    }

    /** Alterna entre el catálogo público (sin precio) y el completo (con token). */
    fun loadProducts(publicCatalog: Boolean) {
        viewModelScope.launch {
            uiState = uiState.copy(
                isLoadingProducts = true,
                showPublicCatalog = publicCatalog,
                error = null,
            )
            apiResult { if (publicCatalog) products.publicCatalog() else products.fullCatalog() }
                .onSuccess {
                    uiState = uiState.copy(products = it, isLoadingProducts = false)
                }
                .onFailure {
                    uiState = uiState.copy(isLoadingProducts = false, error = it.toApiFailure())
                }
        }
    }

    /** Recarga la lista con el catálogo que esté seleccionado. */
    fun reloadProducts() = loadProducts(uiState.showPublicCatalog)

    /** Aviso desde la pantalla de creación: mostrar mensaje y refrescar. */
    fun onProductCreated(product: Product) {
        uiState = uiState.copy(
            message = HomeMessage.ProductCreated(product.name, product.id),
            error = null,
        )
        reloadProducts()
    }

    fun refreshTokens() {
        viewModelScope.launch {
            apiResult { auth.refreshTokens() }
                .onSuccess {
                    uiState = uiState.copy(message = HomeMessage.TokensRefreshed, error = null)
                    loadMe()
                }
                .onFailure {
                    uiState = uiState.copy(error = it.toApiFailure())
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            auth.logout()
        }
    }
}
