package com.android.argosapp.ui.navigation.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.argosapp.auth.manager.FirebaseAuthManager
import com.android.argosapp.auth.manager.MockedArgosApiAuthManager
import com.android.argosapp.auth.session.UserSessionStorage
import com.android.argosapp.ui.screen.FirstAccessChangePasswordScreen
import com.android.argosapp.ui.component.showErrorFeedback
import com.android.argosapp.ui.viewmodel.FirstAccessChangePasswordViewModel

@Composable
fun FirstAccessChangePasswordRoute(
    firebaseAuthManager: FirebaseAuthManager,
    argosApiAuthManager: MockedArgosApiAuthManager,
    tokenStorage: UserSessionStorage,
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val viewModel: FirstAccessChangePasswordViewModel = viewModel(
        factory = FirstAccessChangePasswordViewModel.factory(
            firebaseAuthManager = firebaseAuthManager,
            argosApiAuthManager = argosApiAuthManager,
            tokenStorage = tokenStorage,
        ),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.user) {
        if (state.user == null) onNavigateToLogin()
    }

    LaunchedEffect(state.isPasswordChanged) {
        if (state.isPasswordChanged) onNavigateToHome()
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

    val user = state.user ?: return
    FirstAccessChangePasswordScreen(
        fullName = user.userName,
        newPassword = state.newPassword,
        passwordConfirmation = state.passwordConfirmation,
        newPasswordVisible = state.newPasswordVisible,
        passwordConfirmationVisible = state.passwordConfirmationVisible,
        isLoading = state.isLoading,
        onNewPasswordChange = viewModel::onNewPasswordChanged,
        onPasswordConfirmationChange = viewModel::onPasswordConfirmationChanged,
        onNewPasswordVisibilityChange = viewModel::onNewPasswordVisibilityChanged,
        onPasswordConfirmationVisibilityChange = viewModel::onPasswordConfirmationVisibilityChanged,
        onChangePasswordClick = viewModel::changePassword,
        onBackToLoginClick = onNavigateToLogin,
    )
}
