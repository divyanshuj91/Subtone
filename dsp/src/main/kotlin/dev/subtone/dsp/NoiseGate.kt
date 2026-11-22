package dev.subtone.dsp

import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

/**
 * RMS-based Noise Gate.
 * Drops or flags frames with energy below [thresholdDbFs].
 *
 * dBFS scale: 0 dBFS = full scale square/sine wave; -60 dBFS = quiet room noise.
 */
class NoiseGate(
    var thresholdDbFs: Float = DEFAULT_THRESHOLD_DBFS
) {
    /**
     * Computes the Root Mean Square (RMS) of float PCM buffer in range [-1.0, 1.0].
     */
    fun computeRms(buffer: FloatArray, offset: Int = 0, length: Int = buffer.size): Float {
        if (length <= 0) return 0f
        var sumSquares = 0.0
        val end = offset + length
        for (i in offset until end) {
            val sample = buffer[i]
            sumSquares += sample * sample
        }
        return sqrt(sumSquares / length).toFloat()
    }

    /**
     * Converts an RMS amplitude value to dBFS.
     */
    fun rmsToDbFs(rms: Float): Float {
        if (rms <= 1e-9f) return -120f
        return (20.0 * log10(max(rms.toDouble(), 1e-6))).toFloat()
    }

    /**
     * Evaluates whether the given buffer exceeds the noise gate threshold.
     * Returns a pair of (isAboveThreshold, rmsDbFs).
     */
    fun evaluate(buffer: FloatArray, offset: Int = 0, length: Int = buffer.size): GateResult {
        val rms = computeRms(buffer, offset, length)
        val dbFs = rmsToDbFs(rms)
        return GateResult(
            rms = rms,
            rmsDbFs = dbFs,
            isOpen = dbFs >= thresholdDbFs
        )
    }

    data class GateResult(
        val rms: Float,
        val rmsDbFs: Float,
        val isOpen: Boolean
    )

    companion object {
        const val DEFAULT_THRESHOLD_DBFS = -45.0f
    }
}
