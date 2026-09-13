package com.android.argosapp.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.argosapp.R
import com.android.argosapp.ui.component.ArgosButton
import com.android.argosapp.ui.component.ArgosEmailIcon
import com.android.argosapp.ui.component.ArgosLoadingOverlay
import com.android.argosapp.ui.component.ArgosPasswordInput
import com.android.argosapp.ui.component.ArgosTextInput
import com.android.argosapp.ui.theme.ArgosappTheme
import com.android.argosapp.util.PasswordPatternValidator

enum class RedefinePasswordFlowState {
    REQUEST_CODE,
    EMAIL_SENT,
    CHANGE_PASSWORD,
    PASSWORD_CHANGED
}

@Composable
fun RedefinePasswordFlowScreen(
    state: RedefinePasswordFlowState,
    email: String,
    newPassword: String,
    passwordConfirmation: String,
    newPasswordVisible: Boolean,
    passwordConfirmationVisible: Boolean,
    isLoading: Boolean,
    onEmailChange: (String) -> Unit,
    onSendEmailClick: () -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onPasswordConfirmationChange: (String) -> Unit,
    onNewPasswordVisibilityChange: () -> Unit,
    onPasswordConfirmationVisibilityChange: () -> Unit,
    onChangePasswordClick: () -> Unit,
    resendSecondsRemaining: Int,
    onResendEmailClick: () -> Unit,
    onOpenEmailAppClick: () -> Unit,
    onBackClick: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        RedefinePasswordFlowState.REQUEST_CODE -> RedefinePasswordScreen(
            email = email,
            isLoading = isLoading,
            onEmailChange = onEmailChange,
            onSendEmailClick = onSendEmailClick,
            onBackClick = onBackClick,
            modifier = modifier,
        )

        RedefinePasswordFlowState.EMAIL_SENT -> RedefinePasswordEmailSentScreen(
            onBackClick = onBackClick,
            resendText = if (resendSecondsRemaining == 0) {
                "Enviar novamente (00:00)"
            } else {
                "Enviar novamente (00:${resendSecondsRemaining.toString().padStart(2, '0')})"
            },
            isResendEnabled = resendSecondsRemaining == 0,
            onResendClick = onResendEmailClick,
            onOpenEmailAppClick = onOpenEmailAppClick,
            modifier = modifier,
        )

        RedefinePasswordFlowState.CHANGE_PASSWORD -> RedefinePasswordChangePasswordScreen(
            newPassword = newPassword,
            passwordConfirmation = passwordConfirmation,
            newPasswordVisible = newPasswordVisible,
            passwordConfirmationVisible = passwordConfirmationVisible,
            isLoading = isLoading,
            onNewPasswordChange = onNewPasswordChange,
            onPasswordConfirmationChange = onPasswordConfirmationChange,
            onNewPasswordVisibilityChange = onNewPasswordVisibilityChange,
            onPasswordConfirmationVisibilityChange = onPasswordConfirmationVisibilityChange,
            onChangePasswordClick = onChangePasswordClick,
            onBackClick = onBackClick,
            modifier = modifier,
        )

        RedefinePasswordFlowState.PASSWORD_CHANGED -> RedefinePasswordSuccessScreen(
            onNavigateToLogin = onNavigateToLogin,
            modifier = modifier,
        )
    }
}

@Composable
fun RedefinePasswordScreen(
    email: String,
    isLoading: Boolean,
    onEmailChange: (String) -> Unit,
    onSendEmailClick: () -> Unit,
    onBackClick: () -> Unit,
    emailError: String = "Caso o e-mail acima não seja o seu, entre em contato\ncom a sua organização para atualizar seus dados.",
    loadingText: String = "Enviando e-mail...",
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    ArgosLoadingOverlay(
        visible = isLoading,
        text = loadingText,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .verticalScroll(rememberScrollState()),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
            ) {
                BackButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 26.dp, top = 4.dp),
                )

                Text(
                    text = "Redefinir Senha",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = colors.primary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                    ),
                )
            }

            Spacer(modifier = Modifier.height(132.dp))

            Text(
                text = "Esqueceu a senha?",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = colors.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                ),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Um código de redefinição de senha será enviado\npara o e-mail cadastrado abaixo:",
                modifier = Modifier
                    .fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = colors.primary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                ),
            )

            ArgosTextInput(
                value = email,
                onValueChange = onEmailChange,
                label = "",
                placeholder = "seuemail@dominio.com",
                leadingContent = { ArgosEmailIcon() },
                modifier = Modifier.padding(horizontal = 29.dp),
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 0.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.warning_icon),
                    contentDescription = null,
                    modifier = Modifier.padding(top = 1.dp).size(13.dp),
                    contentScale = ContentScale.Fit,
                )
                Spacer(modifier = Modifier.width(15.dp))
                Text(
                    text = emailError,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        lineHeight = 12.sp,
                    )
                )
            }

            Spacer(modifier = Modifier.height(35.dp))

            ArgosButton(
                text = "Enviar Código",
                onClick = onSendEmailClick,
                modifier = Modifier.padding(horizontal = 36.dp),
            )

            Spacer(modifier = Modifier.weight(1f, fill = true))

        }
    }
}

@Composable
fun RedefinePasswordChangePasswordScreen(
    newPassword: String,
    passwordConfirmation: String,
    newPasswordVisible: Boolean,
    passwordConfirmationVisible: Boolean,
    isLoading: Boolean,
    onNewPasswordChange: (String) -> Unit,
    onPasswordConfirmationChange: (String) -> Unit,
    onNewPasswordVisibilityChange: () -> Unit,
    onPasswordConfirmationVisibilityChange: () -> Unit,
    onChangePasswordClick: () -> Unit,
    onBackClick: () -> Unit,
    loadingText: String = "Alterando sua senha...",
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val criteria = remember(newPassword) { passwordResetCriteriaFor(newPassword) }

    ArgosLoadingOverlay(
        visible = isLoading,
        text = loadingText,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .verticalScroll(rememberScrollState()),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
            ) {
                BackButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 24.dp, top = 4.dp),
                )

                Text(
                    text = "Redefinir senha",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = colors.primary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                    ),
                )
            }

            Spacer(modifier = Modifier.height(57.dp))

            ArgosPasswordInput(
                value = newPassword,
                onValueChange = onNewPasswordChange,
                label = "Nova senha",
                placeholder = "senhadeacesso",
                passwordVisible = newPasswordVisible,
                onPasswordVisibilityChange = onNewPasswordVisibilityChange,
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            Spacer(modifier = Modifier.height(28.dp))

            ArgosPasswordInput(
                value = passwordConfirmation,
                onValueChange = onPasswordConfirmationChange,
                label = "Confirme sua nova senha",
                placeholder = "senhadeacesso",
                passwordVisible = passwordConfirmationVisible,
                onPasswordVisibilityChange = onPasswordConfirmationVisibilityChange,
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            Spacer(modifier = Modifier.height(20.dp))

            PasswordResetCriteriaList(
                criteria = criteria,
                modifier = Modifier.padding(horizontal = 33.dp),
            )

            Spacer(modifier = Modifier.height(28.dp))

            ArgosButton(
                text = "Alterar Senha",
                onClick = onChangePasswordClick,
                modifier = Modifier.padding(horizontal = 36.dp),
            )

            Spacer(modifier = Modifier.weight(1f, fill = true))

        }
    }
}

@Composable
private fun PasswordResetCriteriaList(
    criteria: List<PasswordResetCriterion>,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Column(modifier = modifier) {
        Text(
            text = "Para sua segurança, a senha deve conter:",
            style = MaterialTheme.typography.bodySmall.copy(
                color = colors.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                lineHeight = 14.sp,
            ),
        )

        criteria.forEach { criterion ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                    ),
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = criterion.label,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                    ),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Image(
                    painter = painterResource(
                        if (criterion.isMet) R.drawable.positive_icon else R.drawable.negative_icon,
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(10.dp),
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
}

private data class PasswordResetCriterion(
    val label: String,
    val isMet: Boolean,
)

private val passwordResetValidator = PasswordPatternValidator()

private fun passwordResetCriteriaFor(password: String): List<PasswordResetCriterion> {
    val validation = passwordResetValidator.validatePassword(password)

    return listOf(
        PasswordResetCriterion("Mínimo de 8 caracteres", validation.getValue("length")),
        PasswordResetCriterion("Pelo menos uma letra maiúscula", validation.getValue("minimalCapital")),
        PasswordResetCriterion("Pelo menos uma letra minúscula", validation.getValue("minimalNotCapital")),
        PasswordResetCriterion("Pelo menos um número", validation.getValue("minimalNumeric")),
        PasswordResetCriterion(
            "Pelo menos um caractere especial (@ ! # \$ % &)",
            validation.getValue("minimalSpecial"),
        ),
    )
}

@Composable
fun RedefinePasswordEmailSentScreen(
    onBackClick: () -> Unit,
    resendText: String = "Enviar novamente (00:59)",
    isResendEnabled: Boolean = false,
    onResendClick: () -> Unit = {},
    onOpenEmailAppClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
        ) {
            BackButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 26.dp, top = 4.dp),
            )

            Text(
                text = "Redefinir senha",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = colors.primary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                ),
            )
        }

        Spacer(modifier = Modifier.height(128.dp))

        Box(
            modifier = Modifier
                .width(140.dp)
                .height(133.dp)
                .background(
                    color = Color(0xFF0D6147).copy(alpha = 0.21f),
                    shape = RoundedCornerShape(67.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.email_sended),
                contentDescription = "E-mail enviado",
                modifier = Modifier.size(67.dp, 65.dp),
                contentScale = ContentScale.Fit,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Verifique seu e-mail",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = colors.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
            ),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Enviamos um link para redefinir sua senha para o seu e-mail.",
            modifier = Modifier.padding(horizontal = 50.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = colors.primary,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            ),
        )

        Spacer(modifier = Modifier.height(24.dp))

        ArgosButton(
            text = "Abrir App de Email",
            onClick = onOpenEmailAppClick,
            modifier = Modifier.padding(horizontal = 29.dp),
        )

        Spacer(modifier = Modifier.height(17.dp))

        Text(
            text = "Não recebeu o email?",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = colors.primary,
                fontSize = 16.sp,
            ),
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = resendText,
            modifier = if (isResendEnabled) Modifier.clickable(onClick = onResendClick) else Modifier,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = if (isResendEnabled) colors.primary else Color(0xFF8AAFA1),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textDecoration = TextDecoration.Underline,
            ),
        )

        Spacer(modifier = Modifier.weight(1f, fill = true))

    }
}

@Composable
fun RedefinePasswordSuccessScreen(
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(223.dp))

        Canvas(modifier = Modifier.size(141.dp)) {
            drawCircle(color = colors.primary)

            val strokeWidth = 12.dp.toPx()
            drawLine(
                color = colors.onPrimary,
                start = Offset(36.dp.toPx(), 72.dp.toPx()),
                end = Offset(61.dp.toPx(), 97.dp.toPx()),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = colors.onPrimary,
                start = Offset(61.dp.toPx(), 97.dp.toPx()),
                end = Offset(108.dp.toPx(), 46.dp.toPx()),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }

        Spacer(modifier = Modifier.height(43.dp))

        Text(
            text = "Senha alterada com sucesso!",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = colors.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            ),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Agora você pode entrar usando sua nova senha.",
            modifier = Modifier.padding(horizontal = 36.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = colors.primary,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            ),
        )

        Spacer(modifier = Modifier.weight(1f, fill = true))

        ArgosButton(
            text = "Ir para o login",
            onClick = onNavigateToLogin,
            modifier = Modifier.padding(horizontal = 29.dp),
        )

        Spacer(modifier = Modifier.height(57.dp))
    }
}

@Composable
private fun BackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(width = 42.dp, height = 42.dp)
            .background(Color(0xFFD0C6B2), RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.go_back),
            contentDescription = null,
            modifier = Modifier
                .padding(top = 1.dp, end = 2.dp)
                .size(20.dp),
            contentScale = ContentScale.Fit,
        )
    }
}

@Preview(
    name = "1. Solicitar código",
    showBackground = true,
    backgroundColor = 0xFFF4F1EB,
    widthDp = 402,
    heightDp = 874,
)
@Composable
private fun RedefinePasswordRequestCodePreview() {
    RedefinePasswordFlowPreview(RedefinePasswordFlowState.REQUEST_CODE)
}

@Preview(
    name = "2. E-mail enviado",
    showBackground = true,
    backgroundColor = 0xFFF4F1EB,
    widthDp = 402,
    heightDp = 874,
)
@Composable
private fun RedefinePasswordEmailSentPreview() {
    RedefinePasswordFlowPreview(RedefinePasswordFlowState.EMAIL_SENT)
}

@Preview(
    name = "3. Nova senha",
    showBackground = true,
    backgroundColor = 0xFFF4F1EB,
    widthDp = 402,
    heightDp = 874,
)
@Composable
private fun RedefinePasswordChangePasswordPreview() {
    RedefinePasswordFlowPreview(RedefinePasswordFlowState.CHANGE_PASSWORD)
}

@Preview(
    name = "4. Senha alterada",
    showBackground = true,
    backgroundColor = 0xFFF4F1EB,
    widthDp = 402,
    heightDp = 874,
)
@Composable
private fun RedefinePasswordSuccessPreview() {
    RedefinePasswordFlowPreview(RedefinePasswordFlowState.PASSWORD_CHANGED)
}

@Composable
private fun RedefinePasswordFlowPreview(state: RedefinePasswordFlowState) {
    ArgosappTheme {
        RedefinePasswordFlowScreen(
            state = state,
            email = "vinicius.argos@institutojef.org.br",
            newPassword = "Senha@123",
            passwordConfirmation = "Senha@123",
            newPasswordVisible = false,
            passwordConfirmationVisible = false,
            isLoading = false,
            onEmailChange = {},
            onSendEmailClick = {},
            onNewPasswordChange = {},
            onPasswordConfirmationChange = {},
            onNewPasswordVisibilityChange = {},
            onPasswordConfirmationVisibilityChange = {},
            onChangePasswordClick = {},
            resendSecondsRemaining = 59,
            onResendEmailClick = {},
            onOpenEmailAppClick = {},
            onBackClick = {},
            onNavigateToLogin = {},
        )
    }
}
