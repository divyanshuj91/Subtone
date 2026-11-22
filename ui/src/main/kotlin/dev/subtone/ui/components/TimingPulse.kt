package dev.subtone.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import dev.subtone.ui.theme.SubtoneColors

/**
 * Pulsing beat ring triggered on note onsets.
 * Green on-beat lock, Amber/Red on rush/drag.
 */
@Composable
fun TimingPulse(
    isOnset: Boolean,
    timingDeltaMs: Float?,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(1.0f) }
    val alpha = remember { Animatable(0.0f) }

    LaunchedEffect(isOnset) {
        if (isOnset) {
            scale.snapTo(1.0f)
            alpha.snapTo(0.8f)
            scale.animateTo(1.18f)
            alpha.animateTo(0.0f)
        }
    }

    val ringColor = when {
        timingDeltaMs == null -> SubtoneColors.SignalLock
        kotlin.math.abs(timingDeltaMs) <= 15f -> SubtoneColors.SignalLock
        timingDeltaMs < -15f -> SubtoneColors.SignalDrift // Rushing
        else -> SubtoneColors.SignalError // Dragging
    }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = (size.minDimension / 2f) * scale.value
            drawCircle(
                color = ringColor.copy(alpha = alpha.value),
                radius = radius,
                center = Offset(size.width / 2f, size.height / 2f),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}
