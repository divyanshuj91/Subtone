package dev.subtone.ui.glass

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.subtone.ui.theme.SubtoneColors

/**
 * Liquid Glass frosted surface container.
 * - API 31+: Gaussian backdrop blur via RenderEffect
 * - API 26-30: High-opacity frosted glass tint fallback
 * - 0.5dp specular hairline rim border
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    blurRadius: Dp = 32.dp,
    backgroundColor: Color = SubtoneColors.GlassPrimary,
    elevationLuminance: Float = 0.0f, // 0.0 to 0.1 for elevated glass
    content: @Composable () -> Unit
) {
    val specularRim = Brush.verticalGradient(
        0.0f to SubtoneColors.GlassSpecularRimTop,
        1.0f to SubtoneColors.GlassSpecularRimBottom
    )

    val surfaceTint = if (elevationLuminance > 0f) {
        SubtoneColors.GlassElevated
    } else {
        backgroundColor
    }

    val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.graphicsLayer {
            val radiusPx = blurRadius.toPx()
            if (radiusPx > 0) {
                renderEffect = RenderEffect.createBlurEffect(
                    radiusPx,
                    radiusPx,
                    Shader.TileMode.CLAMP
                ).asComposeRenderEffect()
            }
        }
    } else {
        // Fallback for API 26-30: Increase opacity to simulate frost
        Modifier.background(surfaceTint.copy(alpha = 0.45f), shape)
    }

    Box(
        modifier = modifier
            .clip(shape)
            .then(blurModifier)
            .background(surfaceTint, shape)
            .border(
                width = 0.5.dp,
                brush = specularRim,
                shape = shape
            )
    ) {
        content()
    }
}
