package dev.subtone.theory

import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * Result of deterministic chord matching.
 */
data class ChordMatch(
    val root: PitchClass,
    val quality: ChordQuality,
    val bass: PitchClass = root,
    val inversion: Int = 0, // 0 = root position, 1 = 1st inversion, 2 = 2nd inversion, etc.
    val matchScore: Float = 1.0f, // 1.0 = exact bitmask match
    val expectedTones: List<PitchClass>,
    val detectedTones: List<PitchClass>,
    val missingTones: List<PitchClass>,
    val extraTones: List<PitchClass>
) {
    val displayName: String
        get() {
            val base = "${root.canonicalName}${quality.symbol}"
            return if (bass != root) "$base/${bass.canonicalName}" else base
        }
}

/**
 * Core Deterministic Music Theory Engine.
 * 0ms latency, zero hallucinations, pure Kotlin bitwise operations.
 */
class TheoryEngine {

    // Precomputed cache of all (root, quality) -> ChordMask
    private val templateMasks: Map<Pair<PitchClass, ChordQuality>, ChordMask> = buildMap {
        for (root in PitchClass.entries) {
            for (quality in ChordQuality.entries) {
                put(root to quality, quality.maskForRoot(root))
            }
        }
    }

    /**
     * Matches a 12-bit chord mask against all known chord qualities.
     * Returns the closest matching chord(s) sorted by match score.
     *
     * Exact match: detectedMask == templateMask (score = 1.0)
     * Subset match (e.g. rootless or omitted 5th): score = overlap / expectedSize
     */
    fun match(detectedMask: ChordMask, bassHint: PitchClass? = null): ChordMatch? {
        if (detectedMask.size < 2) return null // Need at least 2 distinct pitch classes

        var bestMatch: ChordMatch? = null
        var bestScore = 0.0f

        for (root in PitchClass.entries) {
            for (quality in ChordQuality.entries) {
                val expectedMask = templateMasks.getValue(root to quality)

                val intersection = detectedMask.mask and expectedMask.mask
                val intersectionCount = Integer.bitCount(intersection)
                val expectedCount = expectedMask.size
                val detectedCount = detectedMask.size

                // Must have the root and at least one characteristic tone (3rd, 5th, or 7th)
                val hasRoot = (intersection and (1 shl root.semitone)) != 0
                if (!hasRoot && detectedCount >= 3) {
                    // Could be rootless voicing, but prioritize rooted matches
                    continue
                }

                // Jaccard-like similarity with penalty for missing essential tones
                val unionCount = Integer.bitCount(detectedMask.mask or expectedMask.mask)
                var score = intersectionCount.toFloat() / unionCount.toFloat()

                // Bonus for exact match
                if (detectedMask.mask == expectedMask.mask) {
                    score = 1.0f
                } else if (intersectionCount == expectedCount && detectedCount == expectedCount + 1) {
                    // Small penalty for one passing tone
                    score = 0.85f
                } else if (intersectionCount == expectedCount - 1 && detectedCount == expectedCount - 1) {
                    // Omitted tone (e.g. omitted 5th in 7th/9th chords is standard in jazz/guitar)
                    val missingTone = expectedMask.mask and detectedMask.mask.inv()
                    val fifthSemitone = (root.semitone + 7) % 12
                    if (missingTone == (1 shl fifthSemitone) && (quality == ChordQuality.DOMINANT_7TH || quality == ChordQuality.MAJOR_7TH || quality == ChordQuality.MINOR_7TH || quality == ChordQuality.DOMINANT_9TH)) {
                        score = 0.92f // High score for omitted 5th
                    }
                }

                if (score > bestScore) {
                    val expectedTones = quality.expectedTones(root)
                    val detectedTones = detectedMask.toPitchClasses()
                    val missing = detectedMask.missingFrom(expectedMask)
                    val extra = detectedMask.extraComparedTo(expectedMask)

                    val bass = bassHint ?: root
                    val inversion = determineInversion(root, quality, bass)

                    bestScore = score
                    bestMatch = ChordMatch(
                        root = root,
                        quality = quality,
                        bass = bass,
                        inversion = inversion,
                        matchScore = score,
                        expectedTones = expectedTones,
                        detectedTones = detectedTones,
                        missingTones = missing,
                        extraTones = extra
                    )
                }
            }
        }

        return if (bestScore >= 0.5f) bestMatch else null
    }

    private fun determineInversion(root: PitchClass, quality: ChordQuality, bass: PitchClass): Int {
        if (bass == root) return 0
        val intervals = quality.intervalSemitones
        val bassInterval = (bass.semitone - root.semitone + 12) % 12
        val index = intervals.indexOf(bassInterval)
        return if (index >= 0) index else 0
    }

    /**
     * Compute musical intervals between two pitch classes in semitones [0..11].
     */
    fun interval(from: PitchClass, to: PitchClass): Int {
        return (to.semitone - from.semitone + 12) % 12
    }

    /**
     * Common diatonic scale templates.
     */
    enum class ScaleType(val semitones: IntArray) {
        MAJOR(intArrayOf(0, 2, 4, 5, 7, 9, 11)),
        NATURAL_MINOR(intArrayOf(0, 2, 3, 5, 7, 8, 10)),
        HARMONIC_MINOR(intArrayOf(0, 2, 3, 5, 7, 8, 11)),
        MELODIC_MINOR(intArrayOf(0, 2, 3, 5, 7, 9, 11)),
        DORIAN(intArrayOf(0, 2, 3, 5, 7, 9, 10)),
        MIXOLYDIAN(intArrayOf(0, 2, 4, 5, 7, 9, 10)),
        PENTATONIC_MAJOR(intArrayOf(0, 2, 4, 7, 9)),
        PENTATONIC_MINOR(intArrayOf(0, 3, 5, 7, 10)),
        BLUES(intArrayOf(0, 3, 5, 6, 7, 10))
    }

    fun scaleMask(root: PitchClass, scaleType: ScaleType): ChordMask {
        return ChordMask.ofSemitones(*scaleType.semitones).rotate(root.semitone)
    }

    fun isToneInScale(tone: PitchClass, root: PitchClass, scaleType: ScaleType): Boolean {
        return tone in scaleMask(root, scaleType)
    }
}
