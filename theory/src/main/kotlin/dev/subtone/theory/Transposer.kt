package dev.subtone.theory

/**
 * Semitone transposer supporting:
 * - Capo positions (0 to +11 semitones)
 * - Transposing instruments (e.g. Bb trumpet/tenor sax = -2, Eb alto sax = -9, F horn = -7)
 * - Pitch shift in range [-12, +12] semitones
 */
class Transposer(
    val semitoneOffset: Int = 0
) {
    init {
        require(semitoneOffset in -12..12) {
            "Semitone offset must be between -12 and +12, got $semitoneOffset"
        }
    }

    /**
     * Transposes an input pitch class to sounding (concert) pitch or vice-versa.
     */
    fun transposePitchClass(pc: PitchClass): PitchClass {
        return pc.transpose(semitoneOffset)
    }

    /**
     * Transposes a 12-bit chord mask by this offset.
     */
    fun transposeChordMask(mask: ChordMask): ChordMask {
        return mask.rotate(semitoneOffset)
    }

    /**
     * Transposes a MIDI note number by this offset.
     */
    fun transposeMidiNote(midiNote: Int): Int {
        return (midiNote + semitoneOffset).coerceIn(0, 127)
    }

    companion object {
        val CONCERT_PITCH = Transposer(0)
        val BB_INSTRUMENT = Transposer(-2) // Bb Trumpet, Clarinet, Tenor Sax
        val EB_INSTRUMENT = Transposer(-9) // Eb Alto Sax, Baritone Sax
        val F_INSTRUMENT = Transposer(-7)  // French Horn

        fun capo(fret: Int): Transposer {
            require(fret in 0..11) { "Capo fret must be between 0 and 11, got $fret" }
            return Transposer(fret)
        }
    }
}
