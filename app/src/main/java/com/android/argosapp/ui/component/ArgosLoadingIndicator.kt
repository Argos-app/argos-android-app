package com.android.argosapp.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.argosapp.ui.theme.ArgosBackground
import com.android.argosapp.ui.theme.ArgosGreen
import com.android.argosapp.ui.theme.ArgosLoadingTrack

@Composable
fun ArgosLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 88.dp,
    color: Color = ArgosGreen,
    trackColor: Color = ArgosLoadingTrack,
    strokeWidth: Dp = 8.dp,
) {
    CircularProgressIndicator(
        modifier = modifier.size(size),
        color = color,
        strokeWidth = strokeWidth,
        trackColor = trackColor,
        strokeCap = StrokeCap.Round,
    )
}

@Composable
fun ArgosLoadingOverlay(
    visible: Boolean,
    text: String,
    modifier: Modifier = Modifier,
    overlayColor: Color = ArgosBackground.copy(alpha = 0.89f),
    indicatorColor: Color = ArgosGreen,
    indicatorTrackColor: Color = ArgosLoadingTrack,
    blurRadius: Dp = 4.dp,
    content: @Composable () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    LaunchedEffect(visible) {
        if (visible) focusManager.clearFocus(force = true)
    }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (visible) {
                        Modifier
                            .blur(blurRadius)
                            .clearAndSetSemantics {}
                    } else {
                        Modifier
                    },
                ),
        ) {
            content()
        }

        if (visible) {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .blockPointerInput(),
                containerColor = overlayColor,
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .semantics(mergeDescendants = true) {
                            liveRegion = LiveRegionMode.Polite
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    ArgosLoadingIndicator(
                        color = indicatorColor,
                        trackColor = indicatorTrackColor,
                    )
                    Text(
                        text = text,
                        modifier = Modifier.padding(top = 18.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = indicatorColor,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                        ),
                    )
                }
            }
        }
    }
}

private fun Modifier.blockPointerInput(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            event.changes.forEach { it.consume() }
        }
    }
}
