package com.android.argosapp.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.argosapp.R
import com.android.argosapp.ui.component.ArgosButton
import com.android.argosapp.ui.component.ArgosOutlinedButton
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
    val eyeInteraction = remember { MutableInteractionSource() }
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(28.dp))

        Image(
            painter = painterResource(R.drawable.argos_horizontal_logo),
            contentDescription = "Argos",
            modifier = Modifier
                .fillMaxWidth(0.74f)
                .height(62.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "O aplicativo que gere, cuida e organiza para você.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = colors.onBackground,
                fontSize = 14.sp,
            ),
        )

        Spacer(modifier = Modifier.weight(1f))

        ArgosTextInput(
            value = email,
            onValueChange = onEmailChange,
            label = "Email",
            placeholder = "seuemail@dominio.com",
            leadingContent = { EmailIcon() },
        )

        Spacer(modifier = Modifier.height(12.dp))

        ArgosTextInput(
            value = password,
            onValueChange = onPasswordChange,
            label = "Senha",
            placeholder = "senhadeacesso123",
            leadingContent = { LockIcon() },
            trailingContent = {
                EyeIcon(
                    passwordVisible = passwordVisible,
                    modifier = Modifier.clickable(
                        interactionSource = eyeInteraction,
                        indication = null,
                        onClick = onPasswordVisibilityChange,
                    ),
                )
            },
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
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

        val legalText = buildAnnotatedString {
            append("Ao entrar você concorda com os ")
            pushStringAnnotation(tag = "terms", annotation = "terms")
            withStyle(
                SpanStyle(
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                ),
            ) {
                append("Termos")
            }
            pop()
            append(" e a ")
            pushStringAnnotation(tag = "privacy", annotation = "privacy")
            withStyle(
                SpanStyle(
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                ),
            ) {
                append("Política de Privacidade")
            }
            pop()
            append(" do Argos.")
        }
        ClickableText(
            text = legalText,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            style = MaterialTheme.typography.bodySmall.copy(
                color = colors.onSurfaceVariant,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                lineHeight = 15.sp,
            ),
            onClick = { offset ->
                legalText
                    .getStringAnnotations("terms", offset, offset)
                    .firstOrNull()
                    ?.let { onTermsClick() }
                legalText
                    .getStringAnnotations("privacy", offset, offset)
                    .firstOrNull()
                    ?.let { onPrivacyClick() }
            },
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

@Composable
private fun EmailIcon(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.email_icon),
        contentDescription = "Email",
        modifier = modifier.size(20.dp),
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun LockIcon(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.lock_icon),
        contentDescription = "Senha",
        modifier = modifier.size(20.dp),
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun EyeIcon(
    passwordVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(
            if (passwordVisible) {
                R.drawable.open_eye_icon
            } else {
                R.drawable.closed_eye_icon
            },
        ),
        contentDescription = if (passwordVisible) "Ocultar senha" else "Mostrar senha",
        modifier = modifier.size(24.dp),
        contentScale = ContentScale.Fit,
    )
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
