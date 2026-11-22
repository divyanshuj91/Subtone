package dev.subtone.dsp

import dev.subtone.theory.ChordMatch
import dev.subtone.theory.PitchClass

/**
 * Immutable analysis frame emitted by the DSP pipeline at 60Hz.
 * Pure Kotlin — zero Android dependencies.
 */
data class AudioAnalysisFrame(
    val timestampMs: Long,
    val rmsDbFs: Float,
    val isSilence: Boolean,
    val detectedPitchHz: Double?,
    val pitchClass: PitchClass?,
    val centDrift: Float?, // Cent deviation within [-50, +50]
    val isInTune: Boolean, // Within ±5 cents
    val detectedChord: ChordMatch?,
    val chroma12: FloatArray, // 12-bin normalized energy vector [C..B]
    val isOnset: Boolean,
    val timingDeltaMs: Float? = null // Milliseconds rush (<0) or drag (>0) against beat grid
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AudioAnalysisFrame

        if (timestampMs != other.timestampMs) return false
        if (rmsDbFs != other.rmsDbFs) return false
        if (isSilence != other.isSilence) return false
        if (detectedPitchHz != other.detectedPitchHz) return false
        if (pitchClass != other.pitchClass) return false
        if (centDrift != other.centDrift) return false
        if (isInTune != other.isInTune) return false
        if (detectedChord != other.detectedChord) return false
        if (!chroma12.contentEquals(other.chroma12)) return false
        if (isOnset != other.isOnset) return false
        if (timingDeltaMs != other.timingDeltaMs) return false

        return true
    }

    override fun hashCode(): Int {
        var result = timestampMs.hashCode()
        result = 31 * result + rmsDbFs.hashCode()
        result = 31 * result + isSilence.hashCode()
        result = 31 * result + (detectedPitchHz?.hashCode() ?: 0)
        result = 31 * result + (pitchClass?.hashCode() ?: 0)
        result = 31 * result + (centDrift?.hashCode() ?: 0)
        result = 31 * result + isInTune.hashCode()
        result = 31 * result + (detectedChord?.hashCode() ?: 0)
        result = 31 * result + chroma12.contentHashCode()
        result = 31 * result + isOnset.hashCode()
        result = 31 * result + (timingDeltaMs?.hashCode() ?: 0)
        return result
    }

    companion object {
        fun silence(timestampMs: Long = System.currentTimeMillis()): AudioAnalysisFrame {
            return AudioAnalysisFrame(
                timestampMs = timestampMs,
                rmsDbFs = -100.0f,
                isSilence = true,
                detectedPitchHz = null,
                pitchClass = null,
                centDrift = null,
                isInTune = false,
                detectedChord = null,
                chroma12 = FloatArray(12),
                isOnset = false,
                timingDeltaMs = null
            )
        }
    }
}
