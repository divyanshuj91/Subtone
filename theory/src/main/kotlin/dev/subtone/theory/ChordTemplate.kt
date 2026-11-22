package dev.subtone.theory

/**
 * Standard chord qualities and their canonical pitch-class intervals relative to root (0).
 */
enum class ChordQuality(
    val symbol: String,
    val fullName: String,
    val intervalSemitones: IntArray,
    val rootMask: ChordMask
) {
    MAJOR("", "Major", intArrayOf(0, 4, 7), ChordMask.ofSemitones(0, 4, 7)),
    MINOR("m", "Minor", intArrayOf(0, 3, 7), ChordMask.ofSemitones(0, 3, 7)),
    DIMINISHED("dim", "Diminished", intArrayOf(0, 3, 6), ChordMask.ofSemitones(0, 3, 6)),
    AUGMENTED("aug", "Augmented", intArrayOf(0, 4, 8), ChordMask.ofSemitones(0, 4, 8)),
    SUS2("sus2", "Suspended 2nd", intArrayOf(0, 2, 7), ChordMask.ofSemitones(0, 2, 7)),
    SUS4("sus4", "Suspended 4th", intArrayOf(0, 5, 7), ChordMask.ofSemitones(0, 5, 7)),
    MAJOR_7TH("maj7", "Major 7th", intArrayOf(0, 4, 7, 11), ChordMask.ofSemitones(0, 4, 7, 11)),
    MINOR_7TH("m7", "Minor 7th", intArrayOf(0, 3, 7, 10), ChordMask.ofSemitones(0, 3, 7, 10)),
    DOMINANT_7TH("7", "Dominant 7th", intArrayOf(0, 4, 7, 10), ChordMask.ofSemitones(0, 4, 7, 10)),
    DIMINISHED_7TH("dim7", "Diminished 7th", intArrayOf(0, 3, 6, 9), ChordMask.ofSemitones(0, 3, 6, 9)),
    HALF_DIMINISHED_7TH("m7b5", "Half-Diminished 7th", intArrayOf(0, 3, 6, 10), ChordMask.ofSemitones(0, 3, 6, 10)),
    MAJOR_9TH("maj9", "Major 9th", intArrayOf(0, 2, 4, 7, 11), ChordMask.ofSemitones(0, 2, 4, 7, 11)),
    MINOR_9TH("m9", "Minor 9th", intArrayOf(0, 2, 3, 7, 10), ChordMask.ofSemitones(0, 2, 3, 7, 10)),
    DOMINANT_9TH("9", "Dominant 9th", intArrayOf(0, 2, 4, 7, 10), ChordMask.ofSemitones(0, 2, 4, 7, 10));

    /**
     * Compute the 12-bit mask for this chord quality with the specified root.
     */
    fun maskForRoot(root: PitchClass): ChordMask = rootMask.rotate(root.semitone)

    /**
     * Expected tone names for this chord rooted at [root].
     */
    fun expectedTones(root: PitchClass): List<PitchClass> {
        return intervalSemitones.map { root.transpose(it) }
    }
}
