package com.android.argosapp.ui.screen.login

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.android.argosapp.R
import com.android.argosapp.auth.GoogleSignInHelper
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LoginRoute(
    onNavigateToHome: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val googleSignInHelper = remember(context, scope) {
        GoogleSignInHelper(
            context = context,
            webClientId = context.getString(R.string.default_web_client_id),
            scope = scope,
        )
    }
    val loginViewModel: LoginViewModel = viewModel(
        factory = LoginViewModel.factory(context.applicationContext),
    )
    val state by loginViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isAuthenticated) {
        if (state.isAuthenticated) onNavigateToHome()
    }

    LaunchedEffect(state.error) {
        state.error?.let { error ->
            showErrorToast(
                context = context,
                message = error,
                backgroundColor = colors.error.toArgb(),
                textColor = colors.onError.toArgb(),
            )
            vibrateOnError(context)
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
            onForgotPasswordClick = {},
            onGoogleClick = {
                googleSignInHelper.launch(
                    loginViewModel::loginWithGoogle,
                    loginViewModel::onGoogleLoginError,
                )
            },
            onLoginClick = loginViewModel::login,
            onTermsClick = {},
            onPrivacyClick = {},
        )

    }
}

private fun showErrorToast(
    context: Context,
    message: String,
    backgroundColor: Int,
    textColor: Int,
) {
    val density = context.resources.displayMetrics.density
    val toastView = TextView(context).apply {
        text = message
        setTextColor(textColor)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        maxLines = 2
        setPadding(
            (16 * density).toInt(),
            (10 * density).toInt(),
            (16 * density).toInt(),
            (10 * density).toInt(),
        )
        background = GradientDrawable().apply {
            setColor(backgroundColor)
            cornerRadius = 24 * density
        }
    }

    Toast(context).apply {
        duration = Toast.LENGTH_LONG
        view = toastView
        setGravity(
            Gravity.TOP or Gravity.CENTER_HORIZONTAL,
            0,
            (64 * density).toInt(),
        )
    }.show()
}

private fun vibrateOnError(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    if (!vibrator.hasVibrator()) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(
            VibrationEffect.createOneShot(
                180L,
                VibrationEffect.DEFAULT_AMPLITUDE,
            ),
        )
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(180L)
    }
}
