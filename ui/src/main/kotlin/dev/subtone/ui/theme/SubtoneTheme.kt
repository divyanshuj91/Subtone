package dev.subtone.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    background = SubtoneColors.SurfaceVoid,
    surface = SubtoneColors.GlassPrimary,
    onBackground = SubtoneColors.TextPrimary,
    onSurface = SubtoneColors.TextPrimary,
    primary = SubtoneColors.SignalLock,
    error = SubtoneColors.SignalError
)

object SubtoneMotion {
    val SpringDefault = spring<Float>(
        dampingRatio = 0.85f,
        stiffness = 380f
    )
}

@Composable
fun SubtoneTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = SubtoneTypography.MaterialTypography,
        content = content
    )
}
