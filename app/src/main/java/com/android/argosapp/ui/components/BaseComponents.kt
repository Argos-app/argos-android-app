package com.android.argosapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import com.android.argosapp.ui.theme.ArgosTransparent

@Composable
fun ArgosButton(
    text: String,
    onClick: () -> Unit,
    width: Dp = Dp.Unspecified,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val widthModifier = if (width != Dp.Unspecified) Modifier.width(width) else Modifier.fillMaxWidth()
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = widthModifier.then(modifier).height(54.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
        ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = colors.onPrimary,
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                ),
            )
        }
    }
}

@Composable
fun ArgosOutlinedButton(text: String, icon: Painter?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Button(onClick, modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(30.dp), border = BorderStroke(1.dp, colors.outline), colors = ButtonDefaults.buttonColors(containerColor = ArgosTransparent, contentColor = colors.primary)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) { Image(icon, null, Modifier.size(22.dp)); Spacer(Modifier.width(10.dp)) }
            Text(text, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp))
        }
    }
}

@Composable
fun ArgosTextInput(value: String, onValueChange: (String) -> Unit, label: String, placeholder: String, modifier: Modifier = Modifier, leadingContent: (@Composable () -> Unit)? = null, trailingContent: (@Composable () -> Unit)? = null, visualTransformation: VisualTransformation = VisualTransformation.None) {
    val colors = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodySmall.copy(color = colors.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp))
        BasicTextField(value, onValueChange, singleLine = true, visualTransformation = visualTransformation, cursorBrush = SolidColor(colors.primary), textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface, fontSize = 15.sp), modifier = Modifier.fillMaxWidth(), decorationBox = { inner ->
            Row(Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(30.dp)).background(colors.surface).border(1.dp, colors.outline, RoundedCornerShape(30.dp)).padding(horizontal = 19.dp), verticalAlignment = Alignment.CenterVertically) {
                leadingContent?.invoke(); if (leadingContent != null) Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f)) { if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurfaceVariant, fontSize = 15.sp)); inner() }
                if (trailingContent != null) { Spacer(Modifier.width(10.dp)); trailingContent() }
            }
        })
    }
}

@Composable
fun ArgosToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.size(18.dp).clip(CircleShape).background(if (checked) colors.primary else ArgosTransparent).border(1.dp, colors.primary, CircleShape).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onCheckedChange(!checked) }, contentAlignment = Alignment.Center) {
        if (checked) {
            Canvas(modifier = Modifier.size(11.dp)) {
                val strokeWidth = 1.8.dp.toPx()
                drawLine(
                    color = colors.onPrimary,
                    start = androidx.compose.ui.geometry.Offset(1.dp.toPx(), 5.5.dp.toPx()),
                    end = androidx.compose.ui.geometry.Offset(4.2.dp.toPx(), 8.5.dp.toPx()),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = colors.onPrimary,
                    start = androidx.compose.ui.geometry.Offset(4.2.dp.toPx(), 8.5.dp.toPx()),
                    end = androidx.compose.ui.geometry.Offset(10.dp.toPx(), 2.5.dp.toPx()),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}
