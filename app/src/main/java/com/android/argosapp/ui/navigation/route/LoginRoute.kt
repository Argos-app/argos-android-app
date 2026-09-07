package com.android.argosapp.ui.navigation.route

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.android.argosapp.R
import com.android.argosapp.auth.manager.FirebaseAuthManager
import com.android.argosapp.auth.manager.MockedArgosApiAuthManager
import com.android.argosapp.auth.session.UserSessionStorage
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.argosapp.ui.viewmodel.LoginViewModel
import com.android.argosapp.ui.screen.LoginScreen
import com.android.argosapp.ui.component.showErrorFeedback

@Composable
fun LoginRoute(
    firebaseAuthManager: FirebaseAuthManager,
    argosApiAuthManager: MockedArgosApiAuthManager,
    tokenStorage: UserSessionStorage,
    onNavigateToHome: () -> Unit = {},
    onNavigateToFirstAccess: () -> Unit = {},
    onNavigateToRedefinePassword: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val loginViewModel: LoginViewModel = viewModel(
        factory = LoginViewModel.factory(
            firebaseAuthManager = firebaseAuthManager,
            argosApiAuthManager = argosApiAuthManager,
            tokenStorage = tokenStorage,
        ),
    )
    val state by loginViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isAuthenticated) {
        if (state.isAuthenticated) onNavigateToHome()
    }

    LaunchedEffect(state.isFirstAccess) {
        if (state.isFirstAccess) onNavigateToFirstAccess()
    }

    LaunchedEffect(state.errorEventId) {
        state.error?.let { error ->
            showErrorFeedback(
                context = context,
                message = error,
                backgroundColor = colors.error.toArgb(),
                textColor = colors.onError.toArgb(),
            )
        }
    }

    if (state.isAuthenticated) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background),
        )
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        LoginScreen(
            email = state.email,
            password = state.password,
            rememberMe = state.rememberMe,
            passwordVisible = state.passwordVisible,
            isLoading = state.isLoading,
            onEmailChange = loginViewModel::onEmailChanged,
            onPasswordChange = loginViewModel::onPasswordChanged,
            onRememberMeChange = loginViewModel::onRememberMeChanged,
            onPasswordVisibilityChange = loginViewModel::onPasswordVisibilityChanged,
            onForgotPasswordClick = onNavigateToRedefinePassword,
            onGoogleClick = {
                firebaseAuthManager.launchGoogleSignIn(
                    context = context,
                    webClientId = context.getString(R.string.default_web_client_id),
                    scope = scope,
                    onSuccess = loginViewModel::loginWithGoogle,
                    onError = loginViewModel::onGoogleLoginError,
                )
            },
            onLoginClick = loginViewModel::login,
            onTermsClick = {},
            onPrivacyClick = {},
        )

    }
}
