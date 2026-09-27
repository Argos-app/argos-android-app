package com.android.argosapp.viewmodel

import android.content.Context
import com.android.argosapp.exception.auth.AuthErrorCode
import com.android.argosapp.exception.auth.AuthErrorMapper
import com.android.argosapp.util.auth.AuthManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginViewModel(
    private val context: Context,
    private val authManager: AuthManager,
) {
    private val scope = CoroutineScope(Dispatchers.Main)

    fun restoreSession(onLoading: (Boolean) -> Unit, onAuthenticated: () -> Unit) = scope.launch {
        onLoading(true)
        val hasSession = !authManager.getBearerToken().isNullOrBlank() &&
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
        scope.launch {
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
        if (idToken.isBlank()) return onError(AuthErrorCode.GOOGLE_CANCELLED)
        scope.launch {
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
        webClientId: String,
        rememberMe: Boolean,
        onLoading: (Boolean) -> Unit,
        onError: (AuthErrorCode) -> Unit,
        onAuthenticated: (Boolean) -> Unit,
    ) {
        authManager.launchGoogleSignIn(
            context = context,
            webClientId = webClientId,
            scope = scope,
            onSuccess = { loginWithGoogle(it, rememberMe, onLoading, onError, onAuthenticated) },
            onError = { onError(AuthErrorMapper.from(it)) },
        )
    }
}
