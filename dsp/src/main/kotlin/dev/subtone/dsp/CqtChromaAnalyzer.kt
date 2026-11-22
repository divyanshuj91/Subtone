package dev.subtone.dsp

import dev.subtone.theory.A4Calibration
import dev.subtone.theory.PitchClass
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Constant-Q-inspired log-frequency bank mapped to a 12-bin Chromagram [C..B].
 * Pure Kotlin — zero Android dependencies.
 *
 * Covers MIDI notes 40 (E2, ~82.4 Hz) to 84 (C6, ~1046.5 Hz).
 */
class CqtChromaAnalyzer(
    val sampleRate: Int = 44100,
    val windowSize: Int = 2048,
    val calibration: A4Calibration = A4Calibration()
) {
    private val minMidi = 40 // E2
    private val maxMidi = 84 // C6
    private val numNotes = maxMidi - minMidi + 1

    // Precomputed Hann window
    private val hannWindow = FloatArray(windowSize) { n ->
        (0.5 * (1.0 - cos(2.0 * PI * n / (windowSize - 1)))).toFloat()
    }

    // Precomputed kernel coefficients (cos and sin tables) for each musical note
    private val cosKernels: Array<FloatArray>
    private val sinKernels: Array<FloatArray>

    init {
        cosKernels = Array(numNotes) { FloatArray(windowSize) }
        sinKernels = Array(numNotes) { FloatArray(windowSize) }

        for (i in 0 until numNotes) {
            val midi = minMidi + i
            val freq = calibration.frequencyForMidiNote(midi)
            val omega = 2.0 * PI * freq / sampleRate
            for (n in 0 until windowSize) {
                val win = hannWindow[n]
                cosKernels[i][n] = (win * cos(omega * n)).toFloat()
                sinKernels[i][n] = (win * sin(omega * n)).toFloat()
            }
        }
    }

    /**
     * Extracts a normalized 12-bin chroma vector [C, C#, D, D#, E, F, F#, G, G#, A, A#, B]
     * from a mono PCM float buffer.
     */
    fun extractChroma(buffer: FloatArray): FloatArray {
        val chroma = FloatArray(12)
        if (buffer.size < windowSize) return chroma

        var maxEnergy = 0.0f

        // Compute energy per MIDI note and accumulate into chroma bins
        for (i in 0 until numNotes) {
            val midi = minMidi + i
            val pitchClass = midi % 12
            val cosK = cosKernels[i]
            val sinK = sinKernels[i]

            var real = 0.0f
            var imag = 0.0f
            for (n in 0 until windowSize) {
                val sample = buffer[n]
                real += sample * cosK[n]
                imag -= sample * sinK[n]
            }

            val energy = real * real + imag * imag
            chroma[pitchClass] += energy
            if (chroma[pitchClass] > maxEnergy) {
                maxEnergy = chroma[pitchClass]
            }
        }

        // Normalize chroma vector (L2 norm)
        var sumSquares = 0.0f
        for (p in 0 until 12) {
            sumSquares += chroma[p] * chroma[p]
        }
        val norm = sqrt(sumSquares.toDouble()).toFloat()
        if (norm > 1e-6f) {
            for (p in 0 until 12) {
                chroma[p] /= norm
            }
        }

        return chroma
    }
}
