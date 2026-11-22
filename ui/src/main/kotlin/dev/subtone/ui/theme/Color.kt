package dev.subtone.ui.theme

import androidx.compose.ui.graphics.Color

object SubtoneColors {
    // Surface & Canvas
    val SurfaceVoid = Color(0xFF090A0F)

    // Liquid Glass Surfaces
    val GlassPrimary = Color(0x17FFFFFF)  // rgba(255, 255, 255, 0.09)
    val GlassElevated = Color(0x24FFFFFF) // rgba(255, 255, 255, 0.14)
    val GlassSpecularRimTop = Color(0x47FFFFFF) // rgba(255, 255, 255, 0.28)
    val GlassSpecularRimBottom = Color(0x0AFFFFFF) // rgba(255, 255, 255, 0.04)

    // Typography
    val TextPrimary = Color(0xFFF5F5F7)
    val TextSecondary = Color(0x9EF5F5F7) // rgba(245, 245, 247, 0.62)
    val TextTertiary = Color(0x5CF5F5F7)

    // Signals
    val SignalLock = Color(0xFF30D158)  // In-tune (±5 cents) & on-beat
    val SignalDrift = Color(0xFFFFD60A) // Slight sharp/flat or minor timing drift
    val SignalError = Color(0xFFFF453A) // Missed chord tone or >20ms rush/drag

    // Ambient Mesh Colors
    val MeshRestIndigo = Color(0xFF16152B)
    val MeshRestViolet = Color(0xFF1A1230)
    val MeshConsonantAmber = Color(0xFF2E2211)
    val MeshConsonantGreen = Color(0xFF112618)
    val MeshSilenceSlate = Color(0xFF0E1116)
}
