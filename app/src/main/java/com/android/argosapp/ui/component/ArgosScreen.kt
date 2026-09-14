package com.android.argosapp.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.argosapp.ui.theme.ArgosSpacing

@Composable
fun ArgosScreen(
    modifier: Modifier = Modifier,
    scrollable: Boolean = false,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalPadding: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val screenModifier = modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(
            horizontal = ArgosSpacing.screenHorizontal,
            vertical = verticalPadding,
        )

    val finalModifier = if (scrollable) {
        screenModifier.verticalScroll(rememberScrollState())
    } else {
        screenModifier
    }

    Column(
        modifier = finalModifier,
        horizontalAlignment = horizontalAlignment,
        content = content,
    )
}
