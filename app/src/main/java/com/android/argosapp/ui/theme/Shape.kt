package com.android.argosapp.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object ArgosShapes {
    val field = RoundedCornerShape(30.dp)
    val button = RoundedCornerShape(28.dp)
    val outlinedButton = RoundedCornerShape(30.dp)
    val circle = CircleShape
}

val ArgosMaterialShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(30.dp),
)