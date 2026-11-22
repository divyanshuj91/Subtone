package dev.subtone.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Tabular numerals ("tnum") strictly enforced on all metric readouts
 * to prevent horizontal jitter during 60fps updates.
 */
object SubtoneTypography {
    val HeroChord = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 64.sp,
        letterSpacing = (-1.5).sp,
        fontFeatureSettings = "tnum"
    )

    val SectionTitle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        letterSpacing = (-0.5).sp
    )

    val TelemetryMetric = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        fontFeatureSettings = "tnum"
    )

    val CaptionLabel = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.8.sp,
        fontFeatureSettings = "tnum"
    )

    val MaterialTypography = Typography(
        headlineLarge = HeroChord,
        titleLarge = SectionTitle,
        bodyMedium = TelemetryMetric,
        labelSmall = CaptionLabel
    )
}
