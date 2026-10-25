package com.android.argosapp.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.android.argosapp.exception.auth.AuthErrorCode
import com.android.argosapp.exception.auth.AuthErrorMapper
import com.android.argosapp.util.auth.AuthManager
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authManager: AuthManager,
) : ViewModel() {

    fun restoreSession(onLoading: (Boolean) -> Unit, onAuthenticated: () -> Unit) = viewModelScope.launch {
        onLoading(true)
        val hasSession = authManager.isRememberMeEnabled() &&
            !authManager.getFirebaseIdToken().isNullOrBlank() &&
            !authManager.getBearerToken().isNullOrBlank() &&
            authManager.getUser() != null && authManager.hasValidSession()
        if (hasSession) onAuthenticated() else {
            authManager.clearSession()
            authManager.signOut()
        }
        onLoading(false)
    }

    fun login(
        email: String,
        password: String,
        rememberMe: Boolean,
        onLoading: (Boolean) -> Unit,
        onError: (AuthErrorCode) -> Unit,
        onAuthenticated: (Boolean) -> Unit,
    ) {
        if (email.isBlank() || password.isBlank()) return onError(AuthErrorCode.EMPTY_FIELDS)
        viewModelScope.launch {
            onLoading(true)
            runCatching {
                val token = authManager.loginWithCredentials(email.trim(), password)
                authManager.authenticate(token, rememberMe)
            }.onSuccess { onAuthenticated(it.isFirstAccess) }
                .onFailure { onError(AuthErrorMapper.from(it)) }
            onLoading(false)
        }
    }

    fun loginWithGoogle(
        idToken: String,
        rememberMe: Boolean,
        onLoading: (Boolean) -> Unit,
        onError: (AuthErrorCode) -> Unit,
        onAuthenticated: (Boolean) -> Unit,
    ) {
        if (idToken.isBlank()) {
            onLoading(false)
            return onError(AuthErrorCode.GOOGLE_CANCELLED)
        }
        viewModelScope.launch {
            onLoading(true)
            runCatching {
                val token = authManager.loginWithGoogle(idToken)
                authManager.authenticate(token, rememberMe)
            }.onSuccess { onAuthenticated(it.isFirstAccess) }
                .onFailure { onError(AuthErrorMapper.from(it)) }
            onLoading(false)
        }
    }

    fun startGoogleLogin(
        context: Context,
        webClientId: String,
        rememberMe: Boolean,
        onLoading: (Boolean) -> Unit,
        onError: (AuthErrorCode) -> Unit,
        onAuthenticated: (Boolean) -> Unit,
    ) {
        onLoading(true)
        authManager.launchGoogleSignIn(
            context = context,
            webClientId = webClientId,
            scope = viewModelScope,
            onSuccess = { loginWithGoogle(it, rememberMe, onLoading, onError, onAuthenticated) },
            onError = { error ->
                onLoading(false)
                if (error !is GetCredentialCancellationException) {
                    onError(AuthErrorMapper.from(error))
                }
            },
        )
    }
}
