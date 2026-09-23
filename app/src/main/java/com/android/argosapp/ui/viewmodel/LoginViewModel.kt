package com.android.argosapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.argosapp.auth.error.AuthErrorCode
import com.android.argosapp.auth.error.AuthErrorMapper
import com.android.argosapp.auth.manager.ArgosApiAuthManager
import com.android.argosapp.auth.manager.FirebaseAuthManager
import com.android.argosapp.ui.event.AuthUiEvent
import com.android.argosapp.util.AppLogger
import java.util.concurrent.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val rememberMe: Boolean = true,
    val passwordVisible: Boolean = false,
    val isAuthenticated: Boolean = false,
    val isFirstAccess: Boolean = false,
)
class LoginViewModel(
    private val firebaseAuthManager: FirebaseAuthManager,
    private val argosApiAuthManager: ArgosApiAuthManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginState(isLoading = true))
    val uiState: StateFlow<LoginState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<AuthUiEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    init {
        restoreSession()
    }

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(email = value) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value) }
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
            AppLogger.debug(AppLogger.LOGIN_TAG, "login_validation_failed", "code" to AuthErrorCode.EMPTY_FIELDS.name)
            showError(AuthErrorCode.EMPTY_FIELDS)
            return
        }

        authenticate(method = "password") {
            firebaseAuthManager.loginWithCredentials(state.email.trim(), state.password)
        }
    }

    fun loginWithGoogle(idToken: String) {
        if (idToken.isBlank()) {
            AppLogger.debug(AppLogger.LOGIN_TAG, "login_validation_failed", "code" to AuthErrorCode.GOOGLE_CANCELLED.name)
            showError(AuthErrorCode.GOOGLE_CANCELLED)
            return
        }

        authenticate(method = "google") { firebaseAuthManager.loginWithGoogle(idToken) }
    }

    fun onGoogleLoginError(throwable: Throwable) {
        if (throwable is CancellationException) throw throwable

        val code = AuthErrorMapper.from(throwable)
        AppLogger.error(AppLogger.LOGIN_TAG, "login_failure", throwable, "code" to code.name)
        showError(code)
    }

    private fun authenticate(method: String, request: suspend () -> String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isFirstAccess = false) }

            try {
                val firebaseIdToken = request()
                val authResponse = argosApiAuthManager.authenticate(
                    firebaseIdToken = firebaseIdToken,
                    rememberMe = uiState.value.rememberMe,
                )

                AppLogger.info(
                    AppLogger.LOGIN_TAG,
                    "login_success",
                    "method" to method,
                    "first_access" to authResponse.isFirstAccess,
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isAuthenticated = !authResponse.isFirstAccess,
                        isFirstAccess = authResponse.isFirstAccess,
                        password = "",
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                val code = AuthErrorMapper.from(error)
                AppLogger.error(
                    AppLogger.LOGIN_TAG,
                    "login_failure",
                    error,
                    "method" to method,
                    "code" to code.name,
                )
                showError(code)
            }
        }
    }

    private fun restoreSession() {
        viewModelScope.launch {
            AppLogger.debug(AppLogger.SESSION_TAG, "session_restore_started")
            val hasBearerToken = !argosApiAuthManager.getBearerToken().isNullOrBlank()
            val hasUser = argosApiAuthManager.getUser() != null
            val hasValidSession = hasBearerToken && hasUser && firebaseAuthManager.hasValidSession()

            if (!hasValidSession) {
                argosApiAuthManager.clearSession()
                firebaseAuthManager.signOut()
            }

            AppLogger.info(
                AppLogger.SESSION_TAG,
                "session_restore_finished",
                "valid" to hasValidSession,
            )

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isAuthenticated = hasValidSession,
                    isFirstAccess = false,
                )
            }
        }
    }

    private fun showError(code: AuthErrorCode) {
        _uiState.update { it.copy(isLoading = false) }
        _events.tryEmit(AuthUiEvent.ShowError(code))
    }

    companion object {
        fun factory(
            firebaseAuthManager: FirebaseAuthManager,
            argosApiAuthManager: ArgosApiAuthManager,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LoginViewModel(
                        firebaseAuthManager = firebaseAuthManager,
                        argosApiAuthManager = argosApiAuthManager,
                    ) as T
                }
            }
    }
}
