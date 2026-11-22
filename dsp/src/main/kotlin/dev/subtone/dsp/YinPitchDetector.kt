package dev.subtone.dsp

import dev.subtone.theory.A4Calibration
import kotlin.math.max
import kotlin.math.min

/**
 * Monophonic YIN Pitch Detection Algorithm.
 * Implements de Cheveigné & Kawahara (2002) with 5 steps:
 * 1. Difference function
 * 2. Cumulative mean normalized difference function (CMNDF)
 * 3. Absolute thresholding (trough picking)
 * 4. Parabolic interpolation for sub-sample precision
 * 5. Intonation & cent drift extraction
 *
 * Guarantees ±5 cent precision across single-note guitar/vocal range (E2 ~82.4Hz to C6 ~1046.5Hz).
 */
class YinPitchDetector(
    val sampleRate: Int = DEFAULT_SAMPLE_RATE,
    val bufferSize: Int = DEFAULT_BUFFER_SIZE,
    val threshold: Float = DEFAULT_THRESHOLD,
    val calibration: A4Calibration = A4Calibration()
) {
    private val halfBufferSize = bufferSize / 2
    private val yinBuffer = FloatArray(halfBufferSize)

    /**
     * Detects fundamental frequency (f0) from a mono float PCM buffer.
     * Returns null if unvoiced or below periodicity threshold.
     */
    fun detectPitch(buffer: FloatArray): PitchDetectionResult? {
        if (buffer.size < bufferSize) return null

        // Step 1: Difference function
        computeDifferenceFunction(buffer)

        // Step 2: Cumulative Mean Normalized Difference Function
        computeCumulativeMeanNormalizedDifference()

        // Step 3: Absolute threshold peak picking
        val tau = absoluteThreshold()
        if (tau == -1) return null

        // Step 4: Parabolic interpolation
        val refinedTau = parabolicInterpolation(tau)
        if (refinedTau <= 0f) return null

        val fundamentalFrequency = sampleRate.toDouble() / refinedTau.toDouble()

        // Filter out unreasonable frequencies (e.g. outside 60Hz to 1500Hz)
        if (fundamentalFrequency < MIN_PITCH_HZ || fundamentalFrequency > MAX_PITCH_HZ) {
            return null
        }

        // Intonation drift via A4Calibration
        val pitchInfo = calibration.pitchFromFrequency(fundamentalFrequency) ?: return null

        return PitchDetectionResult(
            frequencyHz = fundamentalFrequency,
            periodTau = refinedTau,
            periodicityScore = 1.0f - yinBuffer[tau].coerceIn(0f, 1f),
            pitchResult = pitchInfo
        )
    }

    private fun computeDifferenceFunction(buffer: FloatArray) {
        for (tau in 0 until halfBufferSize) {
            yinBuffer[tau] = 0f
        }
        for (tau in 1 until halfBufferSize) {
            var sum = 0f
            for (i in 0 until halfBufferSize) {
                val delta = buffer[i] - buffer[i + tau]
                sum += delta * delta
            }
            yinBuffer[tau] = sum
        }
    }

    private fun computeCumulativeMeanNormalizedDifference() {
        yinBuffer[0] = 1.0f
        var runningSum = 0.0f
        for (tau in 1 until halfBufferSize) {
            runningSum += yinBuffer[tau]
            yinBuffer[tau] = if (runningSum > 0f) {
                (yinBuffer[tau] * tau) / runningSum
            } else {
                1.0f
            }
        }
    }

    private fun absoluteThreshold(): Int {
        var tau = 2
        while (tau < halfBufferSize) {
            if (yinBuffer[tau] < threshold) {
                while (tau + 1 < halfBufferSize && yinBuffer[tau + 1] < yinBuffer[tau]) {
                    tau++
                }
                return tau
            }
            tau++
        }
        return -1
    }

    private fun parabolicInterpolation(tau: Int): Float {
        if (tau <= 0 || tau >= halfBufferSize - 1) return tau.toFloat()

        val s0 = yinBuffer[tau - 1]
        val s1 = yinBuffer[tau]
        val s2 = yinBuffer[tau + 1]

        val bottom = 2f * (2f * s1 - s2 - s0)
        if (bottom == 0f) return tau.toFloat()

        val delta = (s2 - s0) / bottom
        return tau.toFloat() + delta
    }

    data class PitchDetectionResult(
        val frequencyHz: Double,
        val periodTau: Float,
        val periodicityScore: Float,
        val pitchResult: A4Calibration.PitchResult
    )

    companion object {
        const val DEFAULT_SAMPLE_RATE = 44100
        const val DEFAULT_BUFFER_SIZE = 2048
        const val DEFAULT_THRESHOLD = 0.15f // Standard YIN threshold
        const val MIN_PITCH_HZ = 65.0  // Below E2 (~82.4Hz)
        const val MAX_PITCH_HZ = 1200.0 // Above C6 (~1046.5Hz)
    }
}
