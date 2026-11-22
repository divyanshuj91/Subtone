package dev.subtone.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.subtone.ui.theme.SubtoneColors
import dev.subtone.ui.theme.SubtoneMotion
import kotlin.math.abs

/**
 * Cent drift ribbon needle [-50.0c, +50.0c].
 * Highlights ±5 cent in-tune lock zone.
 */
@Composable
fun IntonationRibbon(
    centDrift: Float?,
    isInTune: Boolean,
    modifier: Modifier = Modifier
) {
    val normalizedPosition = if (centDrift != null) {
        (centDrift.coerceIn(-50f, 50f) + 50f) / 100f
    } else 0.5f

    val needleXRatio by animateFloatAsState(
        targetValue = normalizedPosition,
        animationSpec = SubtoneMotion.SpringDefault,
        label = "intonationNeedle"
    )

    val signalColor by animateColorAsState(
        targetValue = when {
            centDrift == null -> SubtoneColors.TextTertiary
            isInTune -> SubtoneColors.SignalLock
            abs(centDrift) <= 20f -> SubtoneColors.SignalDrift
            else -> SubtoneColors.SignalError
        },
        animationSpec = SubtoneMotion.SpringDefault,
        label = "signalColor"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(18.dp)
            .padding(horizontal = 24.dp)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        // Track bar
        drawRoundRect(
            color = Color(0x22FFFFFF),
            topLeft = Offset(0f, centerY - 1.dp.toPx()),
            size = Size(width, 2.dp.toPx()),
            cornerRadius = CornerRadius(1.dp.toPx())
        )

        // Center in-tune lock zone tick (±5 cents = 10% width)
        val lockZoneWidth = width * 0.10f
        val lockZoneStart = (width - lockZoneWidth) / 2f
        drawRoundRect(
            color = if (isInTune) SubtoneColors.SignalLock.copy(alpha = 0.5f) else Color(0x33FFFFFF),
            topLeft = Offset(lockZoneStart, centerY - 3.dp.toPx()),
            size = Size(lockZoneWidth, 6.dp.toPx()),
            cornerRadius = CornerRadius(2.dp.toPx())
        )

        // Needle indicator
        if (centDrift != null) {
            val needleX = width * needleXRatio
            drawCircle(
                color = signalColor,
                radius = 5.dp.toPx(),
                center = Offset(needleX, centerY)
            )
        }
    }
}
