package dev.subtone.ui.livestage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.subtone.ui.components.ChordPill
import dev.subtone.ui.glass.AmbientMeshCanvas
import dev.subtone.ui.glass.GlassSurface
import dev.subtone.ui.theme.SubtoneColors
import dev.subtone.ui.theme.SubtoneTypography
import java.util.Locale

/**
 * Live Stage Screen (Home).
 * - Full-bleed dynamic ambient canvas
 * - Centerpiece floating Liquid Glass chord/pitch orb
 * - Floating translucent transport bar (Record, Metronome, A4 Reference)
 */
@Composable
fun LiveStageScreen(
    viewModel: LiveStageViewModel,
    modifier: Modifier = Modifier
) {
    val analysisFrame by viewModel.analysisFrame.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val isMetronomeOn by viewModel.isMetronomeOn.collectAsStateWithLifecycle()
    val bpm by viewModel.metronomeBpm.collectAsStateWithLifecycle()
    val a4Hz by viewModel.a4ReferenceHz.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Dynamic Ambient Mesh
        AmbientMeshCanvas(analysisFrame = analysisFrame)

        // 2. Center Stage (Floating Chord Pill)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ChordPill(
                analysisFrame = analysisFrame,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        // 3. Floating Bottom Transport Bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            GlassSurface(
                shape = RoundedCornerShape(32.dp),
                elevationLuminance = 0.08f,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Metronome Control
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { viewModel.toggleMetronome() }
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isMetronomeOn) SubtoneColors.SignalLock else Color(0x33FFFFFF))
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "$bpm BPM",
                            style = SubtoneTypography.CaptionLabel,
                            color = SubtoneColors.TextPrimary
                        )
                    }

                    // Record Button (Primary Action)
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(
                                if (isRecording) SubtoneColors.SignalError else SubtoneColors.TextPrimary
                            )
                            .clickable { viewModel.toggleRecording() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isRecording) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(SubtoneColors.SurfaceVoid)
                            )
                        }
                    }

                    // A4 Calibration Indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = String.format(Locale.US, "A4=%.0fHz", a4Hz),
                            style = SubtoneTypography.CaptionLabel,
                            color = SubtoneColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}
