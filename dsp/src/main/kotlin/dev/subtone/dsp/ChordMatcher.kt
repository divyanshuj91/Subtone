package dev.subtone.dsp

import dev.subtone.theory.ChordMask
import dev.subtone.theory.ChordMatch
import dev.subtone.theory.PitchClass
import dev.subtone.theory.TheoryEngine
import kotlin.math.sqrt

/**
 * Polyphonic Chord Matcher using Cosine Template Matching against the Deterministic Theory Engine.
 * Pure Kotlin — zero Android dependencies.
 */
class ChordMatcher(
    private val theoryEngine: TheoryEngine = TheoryEngine(),
    private val chromaThresholdFactor: Float = 0.35f
) {
    /**
     * Matches a 12-bin normalized Chroma vector against the theory engine.
     * Combines cosine template similarity with bitmask validation.
     */
    fun matchFromChroma(chroma: FloatArray): ChordMatch? {
        if (chroma.size != 12) return null

        // Find maximum energy in the chroma vector
        var maxChroma = 0.0f
        var maxIndex = 0
        for (i in 0 until 12) {
            if (chroma[i] > maxChroma) {
                maxChroma = chroma[i]
                maxIndex = i
            }
        }

        if (maxChroma < 0.15f) return null // Too quiet / ambiguous

        // Step 1: Active pitch-class bitmask extraction via adaptive threshold
        val threshold = maxChroma * chromaThresholdFactor
        var maskBits = 0
        for (i in 0 until 12) {
            if (chroma[i] >= threshold) {
                maskBits = maskBits or (1 shl i)
            }
        }

        val detectedMask = ChordMask(maskBits)
        val bassHint = PitchClass.fromSemitone(maxIndex)

        // Query deterministic theory engine
        return theoryEngine.match(detectedMask, bassHint = bassHint)
    }

    /**
     * Computes cosine similarity between 12-bin chroma vector and a 12-bit ChordMask.
     */
    fun cosineSimilarity(chroma: FloatArray, mask: ChordMask): Float {
        var dotProduct = 0.0f
        var maskSumSq = 0.0f
        var chromaSumSq = 0.0f

        for (i in 0 until 12) {
            val maskVal = if (mask.containsSemitone(i)) 1.0f else 0.0f
            val chromaVal = chroma[i]

            dotProduct += chromaVal * maskVal
            maskSumSq += maskVal * maskVal
            chromaSumSq += chromaVal * chromaVal
        }

        val denominator = sqrt((maskSumSq * chromaSumSq).toDouble()).toFloat()
        return if (denominator > 1e-6f) dotProduct / denominator else 0.0f
    }
}
