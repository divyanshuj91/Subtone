package dev.subtone.theory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TheoryEngineTest {

    private lateinit var engine: TheoryEngine
    private lateinit var calibration: A4Calibration

    @Before
    fun setUp() {
        engine = TheoryEngine()
        calibration = A4Calibration(440.0)
    }

    @Test
    fun `test Major Triad detection across roots`() {
        for (root in PitchClass.entries) {
            val mask = ChordQuality.MAJOR.maskForRoot(root)
            val match = engine.match(mask)
            assertNotNull("Major triad for $root should match", match)
            assertEquals(root, match!!.root)
            assertEquals(ChordQuality.MAJOR, match.quality)
            assertEquals(1.0f, match.matchScore, 0.001f)
            assertTrue(match.missingTones.isEmpty())
            assertTrue(match.extraTones.isEmpty())
        }
    }

    @Test
    fun `test Minor Triad detection across roots`() {
        for (root in PitchClass.entries) {
            val mask = ChordQuality.MINOR.maskForRoot(root)
            val match = engine.match(mask)
            assertNotNull("Minor triad for $root should match", match)
            assertEquals(root, match!!.root)
            assertEquals(ChordQuality.MINOR, match.quality)
            assertEquals(1.0f, match.matchScore, 0.001f)
        }
    }

    @Test
    fun `test Diminished and Augmented triads`() {
        // C Diminished: C, Eb, Gb
        val cDim = ChordQuality.DIMINISHED.maskForRoot(PitchClass.C)
        val dimMatch = engine.match(cDim)
        assertNotNull(dimMatch)
        assertEquals(PitchClass.C, dimMatch!!.root)
        assertEquals(ChordQuality.DIMINISHED, dimMatch.quality)

        // G Augmented: G, B, D#
        val gAug = ChordQuality.AUGMENTED.maskForRoot(PitchClass.G)
        val augMatch = engine.match(gAug)
        assertNotNull(augMatch)
        assertEquals(PitchClass.G, augMatch!!.root)
        assertEquals(ChordQuality.AUGMENTED, augMatch.quality)
    }

    @Test
    fun `test Sus2 and Sus4 chords`() {
        // D Sus2: D, E, A
        val dSus2 = ChordQuality.SUS2.maskForRoot(PitchClass.D)
        val sus2Match = engine.match(dSus2)
        assertNotNull(sus2Match)
        assertEquals(PitchClass.D, sus2Match!!.root)
        assertEquals(ChordQuality.SUS2, sus2Match.quality)

        // A Sus4: A, D, E
        val aSus4 = ChordQuality.SUS4.maskForRoot(PitchClass.A)
        val sus4Match = engine.match(aSus4)
        assertNotNull(sus4Match)
        assertEquals(PitchClass.A, sus4Match!!.root)
        assertEquals(ChordQuality.SUS4, sus4Match.quality)
    }

    @Test
    fun `test 7th Chords - Maj7, Min7, Dom7`() {
        // F Maj7: F, A, C, E
        val fMaj7 = ChordQuality.MAJOR_7TH.maskForRoot(PitchClass.F)
        val maj7Match = engine.match(fMaj7)
        assertNotNull(maj7Match)
        assertEquals(PitchClass.F, maj7Match!!.root)
        assertEquals(ChordQuality.MAJOR_7TH, maj7Match.quality)

        // E Min7: E, G, B, D
        val eMin7 = ChordQuality.MINOR_7TH.maskForRoot(PitchClass.E)
        val min7Match = engine.match(eMin7)
        assertNotNull(min7Match)
        assertEquals(PitchClass.E, min7Match!!.root)
        assertEquals(ChordQuality.MINOR_7TH, min7Match.quality)

        // G Dom7: G, B, D, F
        val gDom7 = ChordQuality.DOMINANT_7TH.maskForRoot(PitchClass.G)
        val dom7Match = engine.match(gDom7)
        assertNotNull(dom7Match)
        assertEquals(PitchClass.G, dom7Match!!.root)
        assertEquals(ChordQuality.DOMINANT_7TH, dom7Match.quality)
    }

    @Test
    fun `test 9th Chords - Maj9, Min9, Dom9`() {
        // C Maj9: C, D, E, G, B
        val cMaj9 = ChordQuality.MAJOR_9TH.maskForRoot(PitchClass.C)
        val maj9Match = engine.match(cMaj9)
        assertNotNull(maj9Match)
        assertEquals(PitchClass.C, maj9Match!!.root)
        assertEquals(ChordQuality.MAJOR_9TH, maj9Match.quality)

        // A Min9: A, B, C, E, G
        val aMin9 = ChordQuality.MINOR_9TH.maskForRoot(PitchClass.A)
        val min9Match = engine.match(aMin9)
        assertNotNull(min9Match)
        assertEquals(PitchClass.A, min9Match!!.root)
        assertEquals(ChordQuality.MINOR_9TH, min9Match.quality)

        // D Dom9: D, E, F#, A, C
        val dDom9 = ChordQuality.DOMINANT_9TH.maskForRoot(PitchClass.D)
        val dom9Match = engine.match(dDom9)
        assertNotNull(dom9Match)
        assertEquals(PitchClass.D, dom9Match!!.root)
        assertEquals(ChordQuality.DOMINANT_9TH, dom9Match.quality)
    }

    @Test
    fun `test Inversion detection`() {
        // C Major in first inversion (E in bass): C, E, G with bass = E
        val cMajor = ChordQuality.MAJOR.maskForRoot(PitchClass.C)
        val firstInversionMatch = engine.match(cMajor, bassHint = PitchClass.E)
        assertNotNull(firstInversionMatch)
        assertEquals(PitchClass.C, firstInversionMatch!!.root)
        assertEquals(PitchClass.E, firstInversionMatch.bass)
        assertEquals(1, firstInversionMatch.inversion)
        assertEquals("C/E", firstInversionMatch.displayName)
    }

    @Test
    fun `test Missing tone calculation for telemetry`() {
        // User plays A7 without C# (expected: A, C#, E, G; detected: A, E, G)
        val detected = ChordMask.of(PitchClass.A, PitchClass.E, PitchClass.G)
        val expected = ChordQuality.DOMINANT_7TH.maskForRoot(PitchClass.A)

        val missing = detected.missingFrom(expected)
        assertEquals(1, missing.size)
        assertEquals(PitchClass.Db, missing[0]) // C#
    }

    @Test
    fun `test A4 Calibration calculations`() {
        val cal440 = A4Calibration(440.0)

        // A4 = 440 Hz -> MIDI 69, 0 cents drift
        val resA4 = cal440.pitchFromFrequency(440.0)
        assertNotNull(resA4)
        assertEquals(PitchClass.A, resA4!!.pitchClass)
        assertEquals(4, resA4.octave)
        assertEquals(0.0f, resA4.centDrift, 0.05f)
        assertTrue(resA4.isInTune)

        // 441.27 Hz is approximately +5 cents sharp
        val resSharp = cal440.pitchFromFrequency(441.27)
        assertNotNull(resSharp)
        assertEquals(PitchClass.A, resSharp!!.pitchClass)
        assertEquals(5.0f, resSharp.centDrift, 0.5f)

        // Historic A4 = 432 Hz calibration
        val cal432 = A4Calibration(432.0)
        val res432 = cal432.pitchFromFrequency(432.0)
        assertNotNull(res432)
        assertEquals(PitchClass.A, res432!!.pitchClass)
        assertEquals(0.0f, res432.centDrift, 0.05f)
    }

    @Test
    fun `test Semitone Transposition`() {
        val capo2 = Transposer.capo(2)
        // D transposed up 2 semitones is E
        assertEquals(PitchClass.E, capo2.transposePitchClass(PitchClass.D))

        // C Major chord transposed up 2 semitones is D Major
        val cMajor = ChordQuality.MAJOR.maskForRoot(PitchClass.C)
        val dMajorExpected = ChordQuality.MAJOR.maskForRoot(PitchClass.D)
        assertEquals(dMajorExpected, capo2.transposeChordMask(cMajor))
    }
}
