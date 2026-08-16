package com.android.argosapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ArgosLightColorScheme = lightColorScheme(
    primary = ArgosGreen,
    onPrimary = ArgosWhite,
    background = ArgosBackground,
    onBackground = ArgosBlack,
    surface = ArgosFieldBackground,
    onSurface = ArgosGreen,
    onSurfaceVariant = ArgosPlaceholder,
    outline = ArgosFieldBorder,
    outlineVariant = ArgosDivider,
    error = ArgosFailure,
    onError = ArgosWhite,
)

@Composable
fun ArgosappTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ArgosLightColorScheme,
        typography = Typography,
        content = content
    )
}
