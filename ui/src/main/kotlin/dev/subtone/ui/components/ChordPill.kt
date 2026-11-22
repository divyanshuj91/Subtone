package dev.subtone.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.subtone.dsp.AudioAnalysisFrame
import dev.subtone.ui.glass.GlassSurface
import dev.subtone.ui.theme.SubtoneColors
import dev.subtone.ui.theme.SubtoneTypography
import java.util.Locale

/**
 * Centerpiece Liquid Glass floating orb/pill:
 * - 64sp Hero chord or single note readout
 * - Cent drift indicator needle
 * - Tabular Hz and timing delta metrics
 * - Missing chord tones indicator
 */
@Composable
fun ChordPill(
    analysisFrame: AudioAnalysisFrame,
    modifier: Modifier = Modifier
) {
    val chord = analysisFrame.detectedChord
    val heroText = when {
        analysisFrame.isSilence -> "—"
        chord != null -> chord.displayName
        analysisFrame.pitchClass != null -> analysisFrame.pitchClass!!.canonicalName
        else -> "…"
    }

    val centText = when {
        analysisFrame.isSilence || analysisFrame.centDrift == null -> ""
        analysisFrame.isInTune -> "IN TUNE"
        analysisFrame.centDrift!! > 0 -> String.format(Locale.US, "+%.1f¢", analysisFrame.centDrift)
        else -> String.format(Locale.US, "%.1f¢", analysisFrame.centDrift)
    }

    val centColor = when {
        analysisFrame.isInTune -> SubtoneColors.SignalLock
        analysisFrame.centDrift != null && kotlin.math.abs(analysisFrame.centDrift!!) <= 20f -> SubtoneColors.SignalDrift
        else -> SubtoneColors.SignalError
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        TimingPulse(
            isOnset = analysisFrame.isOnset,
            timingDeltaMs = analysisFrame.timingDeltaMs,
            modifier = Modifier.matchParentSize()
        )

        GlassSurface(
            shape = RoundedCornerShape(36.dp),
            modifier = Modifier.padding(16.dp),
            elevationLuminance = if (!analysisFrame.isSilence) 0.05f else 0.0f
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 48.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = heroText,
                    style = SubtoneTypography.HeroChord,
                    color = SubtoneColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                IntonationRibbon(
                    centDrift = analysisFrame.centDrift,
                    isInTune = analysisFrame.isInTune,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (centText.isNotEmpty()) {
                        Text(
                            text = centText,
                            style = SubtoneTypography.CaptionLabel,
                            color = centColor
                        )
                    }

                    if (analysisFrame.detectedPitchHz != null) {
                        Text(
                            text = String.format(Locale.US, "%.1f Hz", analysisFrame.detectedPitchHz),
                            style = SubtoneTypography.TelemetryMetric,
                            color = SubtoneColors.TextSecondary
                        )
                    }
                }

                // Missing tones warning (e.g. A7 missing C#)
                AnimatedVisibility(
                    visible = chord != null && chord.missingTones.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    if (chord != null && chord.missingTones.isNotEmpty()) {
                        val missingNames = chord.missingTones.joinToString { it.canonicalName }
                        Text(
                            text = "MISSING: $missingNames",
                            style = SubtoneTypography.CaptionLabel,
                            color = SubtoneColors.SignalError,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                }
            }
        }
    }
}
