package com.android.argosapp.ui.viewmodel

import android.content.Context
import com.android.argosapp.auth.error.AuthErrorCode
import com.android.argosapp.auth.error.AuthErrorMapper
import com.android.argosapp.auth.manager.ArgosApiAuthManager
import com.android.argosapp.auth.manager.FirebaseAuthManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginViewModel(
    private val context: Context,
    private val firebaseAuthManager: FirebaseAuthManager,
    private val argosApiAuthManager: ArgosApiAuthManager,
) {
    private val scope = CoroutineScope(Dispatchers.Main)

    fun restoreSession(onLoading: (Boolean) -> Unit, onAuthenticated: () -> Unit) = scope.launch {
        onLoading(true)
        val hasSession = !argosApiAuthManager.getBearerToken().isNullOrBlank() &&
            argosApiAuthManager.getUser() != null && firebaseAuthManager.hasValidSession()
        if (hasSession) onAuthenticated() else {
            argosApiAuthManager.clearSession()
            firebaseAuthManager.signOut()
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
                val token = firebaseAuthManager.loginWithCredentials(email.trim(), password)
                argosApiAuthManager.authenticate(token, rememberMe)
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
                val token = firebaseAuthManager.loginWithGoogle(idToken)
                argosApiAuthManager.authenticate(token, rememberMe)
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
        firebaseAuthManager.launchGoogleSignIn(
            context = context,
            webClientId = webClientId,
            scope = scope,
            onSuccess = { loginWithGoogle(it, rememberMe, onLoading, onError, onAuthenticated) },
            onError = { onError(AuthErrorMapper.from(it)) },
        )
    }
}
