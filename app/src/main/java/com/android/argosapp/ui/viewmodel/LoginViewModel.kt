package com.android.argosapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.argosapp.auth.manager.FirebaseAuthManager
import com.android.argosapp.auth.manager.MockedArgosApiAuthManager
import com.android.argosapp.auth.session.UserSessionStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val rememberMe: Boolean = true,
    val passwordVisible: Boolean = false,
    val error: String? = null,
    val errorEventId: Long = 0,
    val isAuthenticated: Boolean = false,
    val isFirstAccess: Boolean = false,
)
class LoginViewModel(
    private val firebaseAuthManager: FirebaseAuthManager,
    private val argosApiAuthManager: MockedArgosApiAuthManager,
    private val tokenStorage: UserSessionStorage,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginState(isLoading = true))
    val uiState: StateFlow<LoginState> = _uiState.asStateFlow()

    init {
        restoreSession()
    }

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(email = value, error = null) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value, error = null) }
    }

    fun onRememberMeChanged(value: Boolean) {
        _uiState.update { it.copy(rememberMe = value) }
    }

    fun onPasswordVisibilityChanged() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun login() {
        val state = uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            showError("Informe email e senha.")
            return
        }

        authenticate { firebaseAuthManager.loginWithCredentials(state.email.trim(), state.password) }
    }

    fun loginWithGoogle(idToken: String) {
        if (idToken.isBlank()) {
            showError("Não foi possível obter o token do Google.")
            return
        }

        authenticate { firebaseAuthManager.loginWithGoogle(idToken) }
    }

    fun onGoogleLoginError(throwable: Throwable) {
        showError(
            message = throwable.message ?: "Não foi possível entrar com o Google.",
            isLoading = false,
        )
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun authenticate(request: suspend () -> String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, isFirstAccess = false) }

            runCatching {
                val firebaseIdToken = request()
                val authResponse = argosApiAuthManager.authenticate(firebaseIdToken)

                tokenStorage.updateSession(
                    bearerToken = authResponse.bearerToken,
                    user = authResponse.user,
                    rememberMe = uiState.value.rememberMe,
                )

                authResponse
            }
                .onSuccess { authResponse ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = !authResponse.isFirstAccess,
                            isFirstAccess = authResponse.isFirstAccess,
                            error = null,
                            password = "",
                        )
                    }
                }
                .onFailure(::setError)
        }
    }

    private fun restoreSession() {
        viewModelScope.launch {
            val hasBearerToken = !tokenStorage.getBearerToken().isNullOrBlank()
            val hasUser = tokenStorage.getUser() != null
            val hasValidSession = hasBearerToken && hasUser && firebaseAuthManager.hasValidSession()

            if (!hasValidSession) {
                tokenStorage.clearSession()
                firebaseAuthManager.signOut()
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isAuthenticated = hasValidSession,
                    isFirstAccess = false,
                )
            }
        }
    }

    private fun setError(throwable: Throwable) {
        showError(
            message = throwable.message ?: "Não foi possível realizar o login.",
            isLoading = false,
        )
    }

    private fun showError(message: String, isLoading: Boolean = false) {
        _uiState.update {
            it.copy(
                isLoading = isLoading,
                error = message,
                errorEventId = it.errorEventId + 1,
            )
        }
    }

    companion object {
        fun factory(
            firebaseAuthManager: FirebaseAuthManager,
            argosApiAuthManager: MockedArgosApiAuthManager,
            tokenStorage: UserSessionStorage,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LoginViewModel(
                        firebaseAuthManager = firebaseAuthManager,
                        argosApiAuthManager = argosApiAuthManager,
                        tokenStorage = tokenStorage,
                    ) as T
                }
            }
    }
}
