package dev.subtone.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Spectral Flux Note Onset Detector with Beat Grid Timing Deviation.
 * Measures onset timestamp and rush/drag (ms) against a reference BPM.
 * Pure Kotlin — zero Android dependencies.
 */
class SpectralFluxOnset(
    val sampleRate: Int = 44100,
    val fftSize: Int = 1024,
    var thresholdMultiplier: Float = 1.6f
) {
    private val numBins = fftSize / 2
    private val prevMagnitude = FloatArray(numBins)
    private val currMagnitude = FloatArray(numBins)

    // Precomputed Hann window
    private val window = FloatArray(fftSize) { n ->
        (0.5 * (1.0 - cos(2.0 * PI * n / (fftSize - 1)))).toFloat()
    }

    // Adaptive moving average for onset thresholding
    private val fluxHistory = FloatArray(16)
    private var historyIndex = 0
    private var historyFilled = false

    /**
     * Evaluates a PCM buffer for note onset.
     * Returns true if a note onset was detected in this frame.
     */
    fun processFrame(buffer: FloatArray): Boolean {
        if (buffer.size < fftSize) return false

        // Compute magnitude spectrum using real-valued DFT approximation / Goertzel bins
        computeMagnitudeSpectrum(buffer)

        // Compute half-wave rectified spectral flux: sum(max(0, |X(t, k)| - |X(t-1, k)|))
        var spectralFlux = 0.0f
        for (k in 0 until numBins) {
            val diff = currMagnitude[k] - prevMagnitude[k]
            if (diff > 0f) {
                spectralFlux += diff
            }
            prevMagnitude[k] = currMagnitude[k]
        }

        // Update history
        fluxHistory[historyIndex] = spectralFlux
        historyIndex = (historyIndex + 1) % fluxHistory.size
        if (historyIndex == 0) historyFilled = true

        val count = if (historyFilled) fluxHistory.size else historyIndex
        var avgFlux = 0.0f
        for (i in 0 until count) {
            avgFlux += fluxHistory[i]
        }
        avgFlux = if (count > 0) avgFlux / count else 0.0f

        val dynamicThreshold = max(avgFlux * thresholdMultiplier, 0.05f)
        return spectralFlux > dynamicThreshold
    }

    /**
     * Calculates timing delta (in milliseconds) of an onset against an expected beat grid.
     * Negative value = rushing (ahead of beat).
     * Positive value = dragging (behind beat).
     */
    fun calculateTimingDelta(
        onsetTimestampMs: Long,
        gridStartTimeMs: Long,
        bpm: Int
    ): Float {
        val beatIntervalMs = (60_000.0 / bpm).toFloat()
        val elapsedMs = (onsetTimestampMs - gridStartTimeMs).toFloat()
        val beatNumber = (elapsedMs / beatIntervalMs)
        val nearestBeatMs = Math.round(beatNumber) * beatIntervalMs
        return elapsedMs - nearestBeatMs
    }

    private fun computeMagnitudeSpectrum(buffer: FloatArray) {
        // Approximate magnitude bins across 0..Nyquist
        for (k in 0 until numBins) {
            var real = 0.0f
            var imag = 0.0f
            val omega = 2.0 * PI * k / fftSize
            for (n in 0 until fftSize) {
                val s = buffer[n] * window[n]
                real += s * cos(omega * n).toFloat()
                imag -= s * kotlin.math.sin(omega * n).toFloat()
            }
            currMagnitude[k] = sqrt((real * real + imag * imag).toDouble()).toFloat()
        }
    }
}
