package com.android.argosapp.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.argosapp.R
import com.android.argosapp.ui.component.ArgosAuthHeader
import com.android.argosapp.ui.component.ArgosButton
import com.android.argosapp.ui.component.ArgosEmailIcon
import com.android.argosapp.ui.component.ArgosLegalText
import com.android.argosapp.ui.component.ArgosLoadingOverlay
import com.android.argosapp.ui.component.ArgosPasswordInput
import com.android.argosapp.ui.component.ArgosScreen
import com.android.argosapp.ui.component.ArgosTextInput
import com.android.argosapp.ui.theme.ArgosDimensions
import com.android.argosapp.ui.theme.ArgosIcon
import com.android.argosapp.ui.theme.ArgosSpacing
import com.android.argosapp.ui.theme.ArgosTheme
import com.android.argosapp.ui.theme.argosCaption
import com.android.argosapp.ui.theme.argosButtonLabelSmall
import com.android.argosapp.util.PasswordPatternValidator

@Composable
fun FirstAccessChangePasswordScreen(
    fullName: String,
    email: String,
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
    onBackToLoginClick: () -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    loadingText: String = "Alterando sua senha...",
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val passwordCriteria = remember(newPassword) { passwordCriteriaFor(newPassword) }

    ArgosLoadingOverlay(
        visible = isLoading,
        text = loadingText,
        modifier = modifier.fillMaxSize(),
    ) {
        ArgosScreen(
            modifier = Modifier,
            scrollable = true,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(ArgosSpacing.section))

            ArgosAuthHeader(
                subtitle = "Primeiro Acesso, defina uma nova senha.",
                subtitleSpacing = ArgosSpacing.md,
            )

            Spacer(modifier = Modifier.height(ArgosSpacing.section))

            ArgosTextInput(
                value = fullName,
                onValueChange = {},
                label = "Nome Completo",
                placeholder = "",
                readOnly = true,
                leadingContent = { PersonIcon() },
            )

            Spacer(modifier = Modifier.height(ArgosSpacing.lg))

            ArgosTextInput(
                value = email,
                onValueChange = {},
                label = "Email",
                placeholder = "",
                readOnly = true,
                leadingContent = { ArgosEmailIcon() },
            )

            Spacer(modifier = Modifier.height(ArgosSpacing.lg))

            ArgosPasswordInput(
                value = newPassword,
                onValueChange = onNewPasswordChange,
                label = "Nova senha",
                placeholder = "senhadeacesso",
                passwordVisible = newPasswordVisible,
                onPasswordVisibilityChange = onNewPasswordVisibilityChange,
            )

            Spacer(modifier = Modifier.height(ArgosSpacing.lg))

            ArgosPasswordInput(
                value = passwordConfirmation,
                onValueChange = onPasswordConfirmationChange,
                label = "Confirme a nova senha",
                placeholder = "senhadeacesso",
                passwordVisible = passwordConfirmationVisible,
                onPasswordVisibilityChange = onPasswordConfirmationVisibilityChange,
            )

            Spacer(modifier = Modifier.height(ArgosSpacing.lg))

            PasswordCriteriaList(
                criteria = passwordCriteria,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ArgosSpacing.sm),
            )

            Spacer(modifier = Modifier.height(ArgosSpacing.xl))

            ArgosButton(
                text = "Alterar Senha",
                onClick = onChangePasswordClick,
                modifier = Modifier.padding(horizontal = ArgosSpacing.xs),
            )

            Spacer(modifier = Modifier.height(ArgosSpacing.md))

            Text(
                text = "Voltar para tela de login",
                style = MaterialTheme.typography.argosButtonLabelSmall.copy(
                    color = colors.primary,
                ),
                modifier = Modifier.clickable(onClick = onBackToLoginClick),
            )

            Spacer(modifier = Modifier.height(ArgosSpacing.xxl))

            ArgosLegalText(
                onTermsClick = onTermsClick,
                onPrivacyClick = onPrivacyClick,
                modifier = Modifier.padding(horizontal = ArgosSpacing.legalTextHorizontal),
            )

            Spacer(modifier = Modifier.height(ArgosSpacing.md))
        }
    }
}

@Composable
private fun PasswordCriteriaList(
    criteria: List<PasswordCriterion>,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Column(modifier = modifier) {
        Text(
            text = "Para sua segurança, a senha deve conter:",
            style = MaterialTheme.typography.argosCaption.copy(
                color = colors.primary,
                fontWeight = FontWeight.Bold,
            ),
        )

        criteria.forEach { criterion ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.argosCaption.copy(
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(modifier = Modifier.width(ArgosSpacing.xs))
                Text(
                    text = criterion.label,
                    style = MaterialTheme.typography.argosCaption.copy(
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(modifier = Modifier.width(ArgosSpacing.tight))
                Image(
                    painter = painterResource(
                        if (criterion.isMet) {
                            R.drawable.positive_icon
                        } else {
                            R.drawable.negative_icon
                        },
                    ),
                    contentDescription = if (criterion.isMet) {
                        "Critério atendido"
                    } else {
                        "Critério pendente"
                    },
                    modifier = Modifier.size(ArgosDimensions.statusIcon),
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
}

@Composable
private fun PersonIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(ArgosDimensions.mediumIcon)) {
        val iconColor = ArgosIcon
        drawCircle(
            color = iconColor,
            radius = 5.dp.toPx(),
            center = Offset(size.width / 2f, 5.dp.toPx()),
        )
        drawArc(
            color = iconColor,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(1.dp.toPx(), 11.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(20.dp.toPx(), 11.dp.toPx()),
        )
    }
}

private data class PasswordCriterion(
    val label: String,
    val isMet: Boolean,
)

private val passwordValidator = PasswordPatternValidator()

private fun passwordCriteriaFor(password: String): List<PasswordCriterion> {
    val validation = passwordValidator.validatePassword(password)

    return listOf(
        PasswordCriterion(
            label = "Mínimo de 8 caracteres",
            isMet = validation.getValue("length"),
        ),
        PasswordCriterion(
            label = "Pelo menos uma letra maiúscula",
            isMet = validation.getValue("minimalCapital"),
        ),
        PasswordCriterion(
            label = "Pelo menos uma letra minúscula",
            isMet = validation.getValue("minimalNotCapital"),
        ),
        PasswordCriterion(
            label = "Pelo menos um número",
            isMet = validation.getValue("minimalNumeric"),
        ),
        PasswordCriterion(
            label = "Pelo menos um caractere especial (@ ! # \$ % &)",
            isMet = validation.getValue("minimalSpecial"),
        ),
    )
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFFF4F1EB,
    widthDp = 402,
    heightDp = 874,
)
@Composable
private fun FirstAccessChangePasswordScreenPreview() {
    ArgosTheme {
        FirstAccessChangePasswordScreen(
            fullName = "Vinícius de Oliveira",
            email = "vinicius.argos@institutojef.org.br",
            newPassword = "senhadeacesso",
            passwordConfirmation = "senhadeacesso",
            newPasswordVisible = false,
            passwordConfirmationVisible = true,
            isLoading = false,
            onNewPasswordChange = {},
            onPasswordConfirmationChange = {},
            onNewPasswordVisibilityChange = {},
            onPasswordConfirmationVisibilityChange = {},
            onChangePasswordClick = {},
            onBackToLoginClick = {},
            onTermsClick = {},
            onPrivacyClick = {},
        )
    }
}
