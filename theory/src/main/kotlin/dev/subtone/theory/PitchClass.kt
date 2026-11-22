package dev.subtone.theory

/**
 * 12-tone chromatic pitch classes, 0-indexed from C (0) to B (11).
 * Pure Kotlin — zero Android dependencies.
 */
enum class PitchClass(val semitone: Int, val canonicalName: String, val alternateName: String) {
    C(0, "C", "B#"),
    Db(1, "C#", "Db"),
    D(2, "D", "D"),
    Eb(3, "D#", "Eb"),
    E(4, "E", "Fb"),
    F(5, "F", "E#"),
    Gb(6, "F#", "Gb"),
    G(7, "G", "G"),
    Ab(8, "G#", "Ab"),
    A(9, "A", "A"),
    Bb(10, "A#", "Bb"),
    B(11, "B", "Cb");

    fun transpose(semitones: Int): PitchClass {
        val normalized = ((semitone + semitones) % 12 + 12) % 12
        return fromSemitone(normalized)
    }

    companion object {
        fun fromSemitone(semitone: Int): PitchClass {
            val normalized = (semitone % 12 + 12) % 12
            return entries[normalized]
        }

        fun fromMidiNote(midiNote: Int): PitchClass = fromSemitone(midiNote % 12)

        fun parse(name: String): PitchClass? {
            val trimmed = name.trim()
            return entries.firstOrNull {
                it.canonicalName.equals(trimmed, ignoreCase = true) ||
                it.alternateName.equals(trimmed, ignoreCase = true) ||
                it.name.equals(trimmed, ignoreCase = true)
            }
        }
    }
}
