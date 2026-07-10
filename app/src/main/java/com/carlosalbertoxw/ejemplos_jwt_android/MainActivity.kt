package com.carlosalbertoxw.ejemplos_jwt_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.carlosalbertoxw.ejemplos_jwt_android.auth.Session
import com.carlosalbertoxw.ejemplos_jwt_android.auth.SessionState
import com.carlosalbertoxw.ejemplos_jwt_android.ui.CreateProductScreen
import com.carlosalbertoxw.ejemplos_jwt_android.ui.CreateProductViewModel
import com.carlosalbertoxw.ejemplos_jwt_android.ui.HomeScreen
import com.carlosalbertoxw.ejemplos_jwt_android.ui.HomeViewModel
import com.carlosalbertoxw.ejemplos_jwt_android.ui.LoginScreen
import com.carlosalbertoxw.ejemplos_jwt_android.ui.LoginViewModel
import com.carlosalbertoxw.ejemplos_jwt_android.ui.PublicCatalogScreen
import com.carlosalbertoxw.ejemplos_jwt_android.ui.PublicCatalogViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as App).container
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    AppRoot(container)
                }
            }
        }
    }
}

/**
 * El estado de sesión decide qué grafo de navegación se muestra: sin tokens,
 * el catálogo público con acceso al login; con tokens, la pantalla principal.
 * Login, logout y expiración solo modifican la sesión del AuthRepository y
 * el cambio de grafo ocurre solo.
 */
@Composable
private fun AppRoot(container: AppContainer) {
    val sessionState by container.authRepository.sessionState
        .collectAsState(initial = SessionState.Loading)

    when (val state = sessionState) {
        SessionState.Loading -> LoadingIndicator()
        SessionState.LoggedOut -> LoggedOutNav(container)
        is SessionState.LoggedIn -> LoggedInNav(container, state.session)
    }
}

/** Se muestra solo un instante al arrancar, mientras se lee la sesión guardada. */
@Composable
private fun LoadingIndicator() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun LoggedOutNav(container: AppContainer) {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "catalogo") {
        composable("catalogo") {
            val vm: PublicCatalogViewModel =
                viewModel { PublicCatalogViewModel(container.productsRepository) }
            PublicCatalogScreen(vm, onLoginClick = { nav.navigate("login") })
        }
        composable("login") {
            val vm: LoginViewModel = viewModel { LoginViewModel(container.authRepository) }
            // El botón "atrás" del sistema también vuelve al catálogo (NavHost).
            LoginScreen(vm, onBrowseCatalog = { nav.popBackStack() })
        }
    }
}

@Composable
private fun LoggedInNav(container: AppContainer, session: Session) {
    // Elevado fuera del NavHost (ámbito de la Activity) con clave por
    // sessionId: se crea uno nuevo en cada login, se conserva cuando solo se
    // renuevan tokens y sobrevive a la pantalla de creación de productos.
    val homeVm: HomeViewModel = viewModel(key = session.sessionId) {
        HomeViewModel(container.authRepository, container.productsRepository)
    }

    val nav = rememberNavController()
    NavHost(nav, startDestination = "inicio") {
        composable("inicio") {
            HomeScreen(homeVm, onCreateProduct = { nav.navigate("crear-producto") })
        }
        composable("crear-producto") {
            // Con ámbito de esta entrada del back stack: al volver se destruye
            // y la pantalla siempre se abre limpia.
            val vm: CreateProductViewModel =
                viewModel { CreateProductViewModel(container.productsRepository) }
            CreateProductScreen(
                vm = vm,
                onClose = { nav.popBackStack() },
                onCreated = { product ->
                    homeVm.onProductCreated(product)
                    nav.popBackStack()
                },
            )
        }
    }
}
