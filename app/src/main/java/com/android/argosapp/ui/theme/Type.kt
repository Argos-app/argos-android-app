package com.android.argosapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.android.argosapp.R

val ArgosFontFamily = FontFamily(
    Font(R.font.dm_sans),
)

val ArgosTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = ArgosFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = ArgosFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = ArgosFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = ArgosFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
)

val Typography.argosScreenTitle: TextStyle
    get() = titleLarge.copy(
        fontFamily = ArgosFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
    )

val Typography.argosFieldLabel: TextStyle
    get() = bodySmall.copy(
        fontFamily = ArgosFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
    )

val Typography.argosButtonLabel: TextStyle
    get() = labelLarge.copy(
        fontFamily = ArgosFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
    )

val Typography.argosBody: TextStyle
    get() = bodyLarge.copy(
        fontFamily = ArgosFontFamily,
        fontSize = 15.sp,
    )

val Typography.argosSupportingText: TextStyle
    get() = bodySmall.copy(
        fontFamily = ArgosFontFamily,
        fontSize = 12.sp,
    )

val Typography.argosButtonLabelSmall: TextStyle
    get() = labelLarge.copy(
        fontFamily = ArgosFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
    )

val Typography.argosCaption: TextStyle
    get() = bodySmall.copy(
        fontFamily = ArgosFontFamily,
        fontSize = 11.sp,
        lineHeight = 14.sp,
    )

val Typography.argosLegalText: TextStyle
    get() = bodySmall.copy(
        fontFamily = ArgosFontFamily,
        fontSize = 11.sp,
        lineHeight = 15.sp,
    )
