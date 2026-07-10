package com.carlosalbertoxw.ejemplos_jwt_android.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carlosalbertoxw.ejemplos_jwt_android.auth.AuthRepository
import com.carlosalbertoxw.ejemplos_jwt_android.core.ApiFailure
import com.carlosalbertoxw.ejemplos_jwt_android.core.apiResult
import com.carlosalbertoxw.ejemplos_jwt_android.core.toApiFailure
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: ApiFailure? = null,
)

class LoginViewModel(private val auth: AuthRepository) : ViewModel() {

    var uiState by mutableStateOf(LoginUiState())
        private set

    fun login(username: String, password: String) {
        if (uiState.isLoading) return
        viewModelScope.launch {
            uiState = LoginUiState(isLoading = true)
            apiResult { auth.login(username, password) }
                .onSuccess {
                    // No hay navegación manual: la UI observa la sesión del
                    // AuthRepository y cambia de pantalla al aparecer.
                    uiState = LoginUiState()
                }
                .onFailure {
                    uiState = LoginUiState(error = it.toApiFailure())
                }
        }
    }
}
