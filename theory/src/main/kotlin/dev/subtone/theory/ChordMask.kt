package dev.subtone.theory

/**
 * 12-bit bitmask representing a set of pitch classes.
 * Bit i (0 <= i < 12) is 1 if PitchClass.fromSemitone(i) is active.
 *
 * Provides O(1) bitwise operations for chord matching, root transposition,
 * inversion detection, and missing/extra tone diffing.
 */
@JvmInline
value class ChordMask(val mask: Int) {

    init {
        require(mask and 0xFFF.inv() == 0) {
            "ChordMask must only use the lowest 12 bits (0x000 to 0xFFF), but got 0x${mask.toString(16)}"
        }
    }

    operator fun plus(pitchClass: PitchClass): ChordMask =
        ChordMask(mask or (1 shl pitchClass.semitone))

    operator fun minus(pitchClass: PitchClass): ChordMask =
        ChordMask(mask and (1 shl pitchClass.semitone).inv())

    operator fun contains(pitchClass: PitchClass): Boolean =
        (mask and (1 shl pitchClass.semitone)) != 0

    fun containsSemitone(semitone: Int): Boolean {
        val normalized = (semitone % 12 + 12) % 12
        return (mask and (1 shl normalized)) != 0
    }

    /**
     * Circularly bit-shifts the 12-bit mask by [semitones].
     * Transposing up by 2 semitones: bit i moves to (i + 2) % 12.
     */
    fun rotate(semitones: Int): ChordMask {
        val s = (semitones % 12 + 12) % 12
        if (s == 0) return this
        val low = (mask shl s) and 0xFFF
        val wrap = (mask shr (12 - s)) and 0xFFF
        return ChordMask(low or wrap)
    }

    /**
     * Number of active pitch classes in this mask.
     */
    val size: Int get() = Integer.bitCount(mask)

    /**
     * Pitch classes active in this mask in ascending chromatic order.
     */
    fun toPitchClasses(): List<PitchClass> {
        val result = mutableListOf<PitchClass>()
        for (i in 0 until 12) {
            if ((mask and (1 shl i)) != 0) {
                result.add(PitchClass.fromSemitone(i))
            }
        }
        return result
    }

    /**
     * Returns pitch classes present in [expected] but missing in this mask.
     */
    fun missingFrom(expected: ChordMask): List<PitchClass> {
        val missingBits = expected.mask and mask.inv()
        return ChordMask(missingBits).toPitchClasses()
    }

    /**
     * Returns pitch classes present in this mask that are extraneous to [expected].
     */
    fun extraComparedTo(expected: ChordMask): List<PitchClass> {
        val extraBits = mask and expected.mask.inv()
        return ChordMask(extraBits).toPitchClasses()
    }

    companion object {
        val EMPTY = ChordMask(0)
        val ALL = ChordMask(0xFFF)

        fun of(vararg pitchClasses: PitchClass): ChordMask {
            var m = 0
            for (pc in pitchClasses) {
                m = m or (1 shl pc.semitone)
            }
            return ChordMask(m)
        }

        fun ofSemitones(vararg semitones: Int): ChordMask {
            var m = 0
            for (s in semitones) {
                val norm = (s % 12 + 12) % 12
                m = m or (1 shl norm)
            }
            return ChordMask(m)
        }

        fun fromBinaryString(bin: String): ChordMask {
            val clean = bin.replace("_", "").trim()
            return ChordMask(clean.toInt(2) and 0xFFF)
        }
    }
}
