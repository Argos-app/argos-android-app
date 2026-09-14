package com.android.argosapp.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.semantics.Role
import com.android.argosapp.ui.theme.ArgosTransparent
import com.android.argosapp.ui.theme.ArgosDimensions
import com.android.argosapp.ui.theme.ArgosShapes
import com.android.argosapp.ui.theme.ArgosSpacing
import com.android.argosapp.ui.theme.argosBody
import com.android.argosapp.ui.theme.argosButtonLabel
import com.android.argosapp.ui.theme.argosFieldLabel
import com.android.argosapp.ui.theme.argosButtonLabelSmall

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
        modifier = widthModifier.then(modifier).height(ArgosDimensions.buttonHeight),
        shape = ArgosShapes.button,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
        ),
    ) {
        if (isLoading) {
            ArgosLoadingIndicator(
                size = ArgosDimensions.mediumIcon,
                color = colors.onPrimary,
                trackColor = colors.onPrimary.copy(alpha = 0.22f),
                strokeWidth = ArgosDimensions.progressStrokeWidth,
            )
        } else {
            Text(
                text,
                style = MaterialTheme.typography.argosButtonLabel,
            )
        }
    }
}

@Composable
fun ArgosOutlinedButton(text: String, icon: Painter?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(ArgosDimensions.outlinedButtonHeight),
        shape = ArgosShapes.outlinedButton,
        border = BorderStroke(1.dp, colors.outline),
        colors = ButtonDefaults.buttonColors(
            containerColor = ArgosTransparent,
            contentColor = colors.primary,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Image(icon, null, Modifier.size(ArgosDimensions.largeIcon))
                Spacer(Modifier.width(ArgosSpacing.sm))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.argosButtonLabelSmall,
            )
        }
    }
}

@Composable
fun ArgosTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val borderColor = if (isError) {
        colors.error
    } else {
        colors.outline
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(ArgosSpacing.sm),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(start = ArgosSpacing.sm),
            style = MaterialTheme.typography.argosFieldLabel.copy(
                color = if (isError) colors.error else colors.primary,
            ),
        )

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            readOnly = readOnly,
            singleLine = true,
            visualTransformation = visualTransformation,
            cursorBrush = SolidColor(colors.primary),
            textStyle = if (readOnly || !enabled) {
                MaterialTheme.typography.argosSupportingText.copy(
                    color = colors.onSurfaceVariant,
                )
            } else {
                MaterialTheme.typography.argosBody.copy(
                    color = colors.onSurface,
                )
            },
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ArgosDimensions.textFieldHeight)
                        .clip(ArgosShapes.field)
                        .background(colors.surface)
                        .alpha(if (readOnly || !enabled) 0.6f else 1f)
                        .border(
                            width = 1.dp,
                            color = borderColor,
                            shape = ArgosShapes.field,
                        )
                        .padding(horizontal = ArgosDimensions.textFieldHorizontalPadding),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    leadingContent?.invoke()

                    if (leadingContent != null) {
                        Spacer(modifier = Modifier.width(ArgosSpacing.md))
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.argosBody.copy(
                                    color = colors.onSurfaceVariant,
                                ),
                            )
                        }

                        innerTextField()
                    }

                    if (trailingContent != null) {
                        Spacer(modifier = Modifier.width(ArgosSpacing.sm))
                        trailingContent()
                    }
                }
            },
        )
    }
}

@Composable
fun ArgosToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .size(ArgosDimensions.touchTarget)
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(ArgosDimensions.smallIcon)
                .clip(ArgosShapes.circle)
                .background(if (checked) colors.primary else ArgosTransparent)
                .border(1.dp, colors.primary, ArgosShapes.circle),
            contentAlignment = Alignment.Center,
        ) {
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
}
