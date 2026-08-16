package com.android.argosapp.ui.screen.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.argosapp.data.repository.AuthenticatedSession
import com.android.argosapp.data.repository.FirebaseAuthRepository
import com.android.argosapp.data.repository.IAuthRepository
import com.android.argosapp.data.session.AuthTokenStorage
import com.android.argosapp.data.session.AuthSessionManager
import com.android.argosapp.data.session.FirebaseAuthSessionManager
import com.android.argosapp.data.session.SharedPreferencesAuthTokenStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val repository: IAuthRepository,
    private val tokenStorage: AuthTokenStorage,
    private val sessionManager: AuthSessionManager,
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
            _uiState.update { it.copy(error = "Informe email e senha.") }
            return
        }

        authenticate { repository.login(state.email.trim(), state.password) }
    }

    fun loginWithGoogle(idToken: String) {
        if (idToken.isBlank()) {
            _uiState.update { it.copy(error = "Não foi possível obter o token do Google.") }
            return
        }

        authenticate { repository.googleLogin(idToken) }
    }

    fun onGoogleLoginError(throwable: Throwable) {
        _uiState.update {
            it.copy(
                isLoading = false,
                error = throwable.message ?: "Não foi possível entrar com o Google.",
            )
        }
    }

    private fun authenticate(request: suspend () -> AuthenticatedSession) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            runCatching { request() }
                .onSuccess { result ->
                    runCatching { persistSessionIfNeeded(result) }
                        .onSuccess {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isAuthenticated = true,
                                    error = null,
                                    password = "",
                                )
                            }
                        }
                        .onFailure(::setError)
                }
                .onFailure(::setError)
        }
    }

    private suspend fun persistSessionIfNeeded(session: AuthenticatedSession) {
        if (!uiState.value.rememberMe) {
            tokenStorage.clear()
            return
        }

        tokenStorage.save(session.idToken)
    }

    private fun restoreSession() {
        viewModelScope.launch {
            val hasPersistedToken = !tokenStorage.read().isNullOrBlank()
            val hasValidSession = hasPersistedToken && sessionManager.hasValidSession()

            if (!hasValidSession) {
                tokenStorage.clear()
                sessionManager.clear()
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isAuthenticated = hasValidSession,
                )
            }
        }
    }

    private fun setError(throwable: Throwable) {
        _uiState.update {
            it.copy(
                isLoading = false,
                error = throwable.message ?: "Não foi possível realizar o login.",
            )
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LoginViewModel(
                        repository = FirebaseAuthRepository(),
                        tokenStorage = SharedPreferencesAuthTokenStorage(context.applicationContext),
                        sessionManager = FirebaseAuthSessionManager(),
                    ) as T
                }
            }
    }
}
