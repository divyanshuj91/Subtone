package dev.subtone.theory

import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * A4 reference frequency calibration (415 Hz to 466 Hz, default 440 Hz)
 * and cent drift calculation.
 */
class A4Calibration(
    val a4FrequencyHz: Double = DEFAULT_A4_HZ
) {
    init {
        require(a4FrequencyHz in MIN_A4_HZ..MAX_A4_HZ) {
            "A4 calibration must be between $MIN_A4_HZ Hz and $MAX_A4_HZ Hz, got $a4FrequencyHz"
        }
    }

    /**
     * Frequency of a given MIDI note number under current A4 calibration.
     * MIDI note 69 is A4.
     */
    fun frequencyForMidiNote(midiNote: Int): Double {
        return a4FrequencyHz * 2.0.pow((midiNote - 69) / 12.0)
    }

    /**
     * Frequency of a given pitch class and octave (e.g. A4 -> pc=A, octave=4).
     * C4 is MIDI 60, A4 is MIDI 69.
     */
    fun frequencyForNote(pitchClass: PitchClass, octave: Int): Double {
        val midiNote = (octave + 1) * 12 + pitchClass.semitone
        return frequencyForMidiNote(midiNote)
    }

    /**
     * Converts a raw frequency (in Hz) to:
     * - Nearest MIDI note number
     * - Nearest PitchClass
     * - Cent drift in [-50.0, +50.0] cents relative to nearest semitone
     */
    fun pitchFromFrequency(frequencyHz: Double): PitchResult? {
        if (frequencyHz <= 0.0 || frequencyHz.isNaN() || frequencyHz.isInfinite()) return null

        // midi = 69 + 12 * log2(f / a4)
        val fractionalMidi = 69.0 + 12.0 * log2(frequencyHz / a4FrequencyHz)
        val nearestMidi = fractionalMidi.roundToInt()
        val centDrift = (fractionalMidi - nearestMidi) * 100.0

        val octave = (nearestMidi / 12) - 1
        val pitchClass = PitchClass.fromMidiNote(nearestMidi)
        val targetFreq = frequencyForMidiNote(nearestMidi)

        return PitchResult(
            frequencyHz = frequencyHz,
            targetFrequencyHz = targetFreq,
            midiNote = nearestMidi,
            pitchClass = pitchClass,
            octave = octave,
            centDrift = centDrift.toFloat(),
            isInTune = kotlin.math.abs(centDrift) <= IN_TUNE_THRESHOLD_CENTS
        )
    }

    data class PitchResult(
        val frequencyHz: Double,
        val targetFrequencyHz: Double,
        val midiNote: Int,
        val pitchClass: PitchClass,
        val octave: Int,
        val centDrift: Float, // in cents, -50 to +50
        val isInTune: Boolean // true if within ±5 cents
    ) {
        val noteName: String get() = "${pitchClass.canonicalName}$octave"
    }

    companion object {
        const val DEFAULT_A4_HZ = 440.0
        const val MIN_A4_HZ = 415.0
        const val MAX_A4_HZ = 466.0
        const val IN_TUNE_THRESHOLD_CENTS = 5.0f
    }
}
