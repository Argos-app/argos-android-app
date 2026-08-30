package com.android.argosapp.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
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
import com.android.argosapp.ui.component.ArgosPasswordInput
import com.android.argosapp.ui.theme.ArgosIcon
import com.android.argosapp.ui.theme.ArgosappTheme

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
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val passwordCriteria = remember(newPassword) { passwordCriteriaFor(newPassword) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 23.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(56.dp))

        ArgosAuthHeader(
            subtitle = "Primeiro Acesso, defina uma nova senha.",
            subtitleSpacing = 10.dp,
        )

        Spacer(modifier = Modifier.height(54.dp))

        ReadOnlyInfoInput(
            value = fullName,
            label = "Nome Completo",
            leadingContent = { PersonIcon() },
        )

        Spacer(modifier = Modifier.height(18.dp))

        ReadOnlyInfoInput(
            value = email,
            label = "Email",
            leadingContent = { ArgosEmailIcon() },
        )

        Spacer(modifier = Modifier.height(18.dp))

        ArgosPasswordInput(
            value = newPassword,
            onValueChange = onNewPasswordChange,
            label = "Nova senha",
            placeholder = "senhadeacesso",
            passwordVisible = newPasswordVisible,
            onPasswordVisibilityChange = onNewPasswordVisibilityChange,
        )

        Spacer(modifier = Modifier.height(18.dp))

        ArgosPasswordInput(
            value = passwordConfirmation,
            onValueChange = onPasswordConfirmationChange,
            label = "Confirme a nova senha",
            placeholder = "senhadeacesso",
            passwordVisible = passwordConfirmationVisible,
            onPasswordVisibilityChange = onPasswordConfirmationVisibilityChange,
        )

        Spacer(modifier = Modifier.height(16.dp))

        PasswordCriteriaList(
            criteria = passwordCriteria,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 9.dp),
        )

        Spacer(modifier = Modifier.height(26.dp))

        ArgosButton(
            text = "Alterar Senha",
            onClick = onChangePasswordClick,
            isLoading = isLoading,
            modifier = Modifier.padding(horizontal = 5.dp),
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Voltar para tela de login",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = colors.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            ),
            modifier = Modifier.clickable(onClick = onBackToLoginClick),
        )

        Spacer(modifier = Modifier.height(28.dp))

        ArgosLegalText(
            onTermsClick = onTermsClick,
            onPrivacyClick = onPrivacyClick,
            modifier = Modifier.padding(horizontal = 30.dp),
            fontSize = 10.sp,
            lineHeight = 14.sp,
        )

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun ReadOnlyInfoInput(
    value: String,
    label: String,
    leadingContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Column(modifier = modifier) {
        Text(
            text = label,
            modifier = Modifier.padding(start = 9.dp),
            style = MaterialTheme.typography.bodySmall.copy(
                color = colors.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            ),
        )

        Spacer(modifier = Modifier.height(8.dp))

        BasicTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            cursorBrush = SolidColor(colors.primary),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = colors.onSurfaceVariant,
                fontSize = 12.sp,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .alpha(0.6f),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.outline, RoundedCornerShape(30.dp))
                        .padding(horizontal = 19.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    leadingContent()
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        innerTextField()
                    }
                }
            },
        )
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
            style = MaterialTheme.typography.bodySmall.copy(
                color = colors.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                lineHeight = 14.sp,
            ),
        )

        criteria.forEach { criterion ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                    ),
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = criterion.label,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                    ),
                )
                Spacer(modifier = Modifier.width(6.dp))
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
                    modifier = Modifier.size(10.dp),
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
}

@Composable
private fun PersonIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(22.dp)) {
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

private const val ALLOWED_SPECIAL_CHARACTERS = "@!#\$%&"

private fun passwordCriteriaFor(password: String): List<PasswordCriterion> {
    return listOf(
        PasswordCriterion(
            label = "Mínimo de 8 caracteres",
            isMet = password.length >= 8,
        ),
        PasswordCriterion(
            label = "Pelo menos uma letra maiúscula",
            isMet = password.any(Char::isUpperCase),
        ),
        PasswordCriterion(
            label = "Pelo menos uma letra minúscula",
            isMet = password.any(Char::isLowerCase),
        ),
        PasswordCriterion(
            label = "Pelo menos um número",
            isMet = password.any(Char::isDigit),
        ),
        PasswordCriterion(
            label = "Pelo menos um caractere especial (@ ! # \$ % &)",
            isMet = password.any { it in ALLOWED_SPECIAL_CHARACTERS },
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
    ArgosappTheme {
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
