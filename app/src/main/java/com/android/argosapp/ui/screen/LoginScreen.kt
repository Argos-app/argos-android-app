package com.android.argosapp.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.argosapp.R
import com.android.argosapp.ui.component.ArgosAuthHeader
import com.android.argosapp.ui.component.ArgosButton
import com.android.argosapp.ui.component.ArgosEmailIcon
import com.android.argosapp.ui.component.ArgosLegalText
import com.android.argosapp.ui.component.ArgosOutlinedButton
import com.android.argosapp.ui.component.ArgosPasswordInput
import com.android.argosapp.ui.component.ArgosTextInput
import com.android.argosapp.ui.component.ArgosToggle

@Composable
fun LoginScreen(
    email: String,
    password: String,
    rememberMe: Boolean,
    passwordVisible: Boolean,
    isLoading: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRememberMeChange: (Boolean) -> Unit,
    onPasswordVisibilityChange: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onGoogleClick: () -> Unit,
    onLoginClick: () -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(28.dp))

        ArgosAuthHeader(
            subtitle = "O aplicativo que gere, cuida e organiza para você.",
        )

        Spacer(modifier = Modifier.weight(1f))

        ArgosTextInput(
            value = email,
            onValueChange = onEmailChange,
            label = "Email",
            placeholder = "seuemail@dominio.com",
            leadingContent = { ArgosEmailIcon() },
        )

        Spacer(modifier = Modifier.height(12.dp))

        ArgosPasswordInput(
            value = password,
            onValueChange = onPasswordChange,
            label = "Senha",
            placeholder = "senhadeacesso123",
            passwordVisible = passwordVisible,
            onPasswordVisibilityChange = onPasswordVisibilityChange,
        )
        Spacer(modifier = Modifier.height(17.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArgosToggle(
                checked = rememberMe,
                onCheckedChange = onRememberMeChange,
            )
            Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Lembrar de mim",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    ),
                    modifier = Modifier.clickable {
                        onRememberMeChange(!rememberMe)
                    },
                )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Esqueci a senha",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = colors.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                ),
                modifier = Modifier.clickable(onClick = onForgotPasswordClick),
            )
        }

        Spacer(modifier = Modifier.height(17.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DividerLine(modifier = Modifier.weight(1f))
            Text(
                text = "ou",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = colors.onSurfaceVariant,
                    fontSize = 12.sp,
                ),
                modifier = Modifier.padding(horizontal = 6.dp),
            )
            DividerLine(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(17.dp))

        ArgosOutlinedButton(
            text = "Entrar com Google",
            icon = painterResource(R.drawable.google_icon),
            onClick = onGoogleClick,
        )

        Spacer(modifier = Modifier.height(42.dp))

        ArgosButton(
            text = "Entrar",
            onClick = onLoginClick,
            isLoading = isLoading,
        )

        Spacer(modifier = Modifier.weight(1f))

        ArgosLegalText(
            onTermsClick = onTermsClick,
            onPrivacyClick = onPrivacyClick,
            modifier = Modifier
                .padding(horizontal = 4.dp),
        )
    }
}

@Composable
private fun DividerLine(modifier: Modifier) {
    val colors = MaterialTheme.colorScheme

    Canvas(modifier.height(1.dp)) {
        drawLine(
            color = colors.outlineVariant,
            start = Offset.Zero,
            end = Offset(size.width, 0f),
            strokeWidth = 1.dp.toPx(),
        )
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFFF4F1EB,
    widthDp = 402,
    heightDp = 874,
)
@Composable
private fun LoginScreenPreview() {
    LoginScreen(
        email = "",
        password = "",
        rememberMe = true,
        passwordVisible = false,
        isLoading = false,
        onEmailChange = {},
        onPasswordChange = {},
        onRememberMeChange = {},
        onPasswordVisibilityChange = {},
        onForgotPasswordClick = {},
        onGoogleClick = {},
        onLoginClick = {},
        onTermsClick = {},
        onPrivacyClick = {},
    )
}
