package com.android.argosapp.ui.navigation.route

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.argosapp.auth.manager.FirebaseAuthManager
import com.android.argosapp.ui.component.showErrorFeedback
import com.android.argosapp.ui.screen.RedefinePasswordFlowScreen
import com.android.argosapp.ui.screen.RedefinePasswordFlowState
import com.android.argosapp.ui.viewmodel.RedefinePasswordViewModel

@Composable
fun RedefinePasswordRoute(
    firebaseAuthManager: FirebaseAuthManager,
    onNavigateToLogin: () -> Unit,
    passwordResetCode: String? = null,
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val viewModel: RedefinePasswordViewModel = viewModel(
        factory = RedefinePasswordViewModel.factory(firebaseAuthManager),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(passwordResetCode) {
        passwordResetCode?.let(viewModel::onPasswordResetCodeReceived)
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

    RedefinePasswordFlowScreen(
        state = state.flowState,
        email = state.email,
        newPassword = state.newPassword,
        passwordConfirmation = state.passwordConfirmation,
        newPasswordVisible = state.newPasswordVisible,
        passwordConfirmationVisible = state.passwordConfirmationVisible,
        isLoading = state.isLoading,
        onEmailChange = viewModel::onEmailChanged,
        onSendEmailClick = viewModel::sendResetEmail,
        onNewPasswordChange = viewModel::onNewPasswordChanged,
        onPasswordConfirmationChange = viewModel::onPasswordConfirmationChanged,
        onNewPasswordVisibilityChange = viewModel::onNewPasswordVisibilityChanged,
        onPasswordConfirmationVisibilityChange = viewModel::onPasswordConfirmationVisibilityChanged,
        onChangePasswordClick = viewModel::changePassword,
        resendSecondsRemaining = state.resendSecondsRemaining,
        onResendEmailClick = viewModel::resendResetEmail,
        onBackClick = {
            if (state.flowState == RedefinePasswordFlowState.REQUEST_CODE) {
                onNavigateToLogin()
            } else {
                viewModel.goBack()
            }
        },
        onNavigateToLogin = onNavigateToLogin,
    )
}
