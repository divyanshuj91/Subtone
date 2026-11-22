package dev.subtone.ui.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import dev.subtone.dsp.AudioAnalysisFrame
import dev.subtone.ui.theme.SubtoneColors
import dev.subtone.ui.theme.SubtoneMotion

/**
 * Living ambient mesh canvas that dynamically shifts background mood
 * based on live DSP telemetry:
 * - Silent/rest: Deep Indigo/Violet
 * - Consonant lock (±5c & chord lock): Warm Amber / Emerald
 * - Intonation / timing drift: Amber Glow
 */
@Composable
fun AmbientMeshCanvas(
    analysisFrame: AudioAnalysisFrame,
    modifier: Modifier = Modifier
) {
    val (targetColor1, targetColor2) = when {
        analysisFrame.isSilence -> Pair(SubtoneColors.MeshSilenceSlate, SubtoneColors.SurfaceVoid)
        analysisFrame.isInTune || (analysisFrame.detectedChord != null && analysisFrame.detectedChord!!.matchScore > 0.9f) ->
            Pair(SubtoneColors.MeshConsonantGreen, SubtoneColors.MeshRestIndigo)
        analysisFrame.centDrift != null && kotlin.math.abs(analysisFrame.centDrift!!) > 15f ->
            Pair(SubtoneColors.MeshConsonantAmber, SubtoneColors.MeshRestViolet)
        else -> Pair(SubtoneColors.MeshRestIndigo, SubtoneColors.MeshRestViolet)
    }

    val color1 by animateColorAsState(
        targetValue = targetColor1,
        animationSpec = SubtoneMotion.SpringDefault,
        label = "ambientMesh1"
    )

    val color2 by animateColorAsState(
        targetValue = targetColor2,
        animationSpec = SubtoneMotion.SpringDefault,
        label = "ambientMesh2"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        // Base canvas fill
        drawRect(color = SubtoneColors.SurfaceVoid)

        // Radial ambient glow center
        val centerOffset = Offset(size.width * 0.5f, size.height * 0.42f)
        val maxRadius = size.maxDimension * 0.8f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color1, color2, Color.Transparent),
                center = centerOffset,
                radius = maxRadius
            ),
            center = centerOffset,
            radius = maxRadius
        )
    }
}
