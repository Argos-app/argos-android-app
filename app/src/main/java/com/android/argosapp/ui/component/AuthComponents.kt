package com.android.argosapp.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.argosapp.R

@Composable
fun ArgosAuthHeader(
    subtitle: String,
    modifier: Modifier = Modifier,
    subtitleSpacing: Dp = 16.dp,
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.argos_horizontal_logo),
            contentDescription = "Argos",
            modifier = Modifier
                .fillMaxWidth(0.74f)
                .height(62.dp),
            contentScale = ContentScale.Fit,
        )

        Spacer(modifier = Modifier.height(subtitleSpacing))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = colors.onBackground,
                fontSize = 14.sp,
            ),
        )
    }
}

@Composable
fun ArgosPasswordInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    passwordVisible: Boolean,
    onPasswordVisibilityChange: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val eyeInteraction = remember { MutableInteractionSource() }

    ArgosTextInput(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        modifier = modifier,
        leadingContent = { ArgosLockIcon() },
        trailingContent = {
            ArgosPasswordVisibilityIcon(
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
}

@Composable
fun ArgosLegalText(
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 11.sp,
    lineHeight: TextUnit = 15.sp,
) {
    val colors = MaterialTheme.colorScheme
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
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodySmall.copy(
            color = colors.onSurfaceVariant,
            fontSize = fontSize,
            textAlign = TextAlign.Center,
            lineHeight = lineHeight,
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

@Composable
fun ArgosEmailIcon(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.email_icon),
        contentDescription = null,
        modifier = modifier.size(20.dp),
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun ArgosLockIcon(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.lock_icon),
        contentDescription = null,
        modifier = modifier.size(20.dp),
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun ArgosPasswordVisibilityIcon(
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
