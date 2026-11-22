package dev.subtone.dsp

import dev.subtone.theory.A4Calibration
import dev.subtone.theory.ChordQuality
import dev.subtone.theory.PitchClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class DspCoreTest {

    private val sampleRate = 44100
    private lateinit var calibration: A4Calibration
    private lateinit var yinDetector: YinPitchDetector
    private lateinit var chromaAnalyzer: CqtChromaAnalyzer
    private lateinit var chordMatcher: ChordMatcher
    private lateinit var noiseGate: NoiseGate

    @Before
    fun setUp() {
        calibration = A4Calibration(440.0)
        yinDetector = YinPitchDetector(sampleRate = sampleRate, bufferSize = 2048, calibration = calibration)
        chromaAnalyzer = CqtChromaAnalyzer(sampleRate = sampleRate, windowSize = 2048, calibration = calibration)
        chordMatcher = ChordMatcher()
        noiseGate = NoiseGate(thresholdDbFs = -45.0f)
    }

    // --- Synthetic Audio Helpers ---

    private fun generateSine(freqHz: Double, numSamples: Int, amplitude: Float = 0.8f): FloatArray {
        return FloatArray(numSamples) { n ->
            (amplitude * sin(2.0 * PI * freqHz * n / sampleRate)).toFloat()
        }
    }

    private fun generateSawtooth(freqHz: Double, numSamples: Int, amplitude: Float = 0.8f): FloatArray {
        val period = sampleRate / freqHz
        return FloatArray(numSamples) { n ->
            val phase = (n % period) / period
            (amplitude * (2.0 * phase - 1.0)).toFloat()
        }
    }

    private fun generateTriad(freq1: Double, freq2: Double, freq3: Double, numSamples: Int): FloatArray {
        return FloatArray(numSamples) { n ->
            val s1 = sin(2.0 * PI * freq1 * n / sampleRate)
            val s2 = sin(2.0 * PI * freq2 * n / sampleRate)
            val s3 = sin(2.0 * PI * freq3 * n / sampleRate)
            ((s1 + s2 + s3) / 3.0 * 0.85).toFloat()
        }
    }

    // --- Tests ---

    @Test
    fun `test Noise Gate on silence vs signal`() {
        val silence = FloatArray(1024) { 0f }
        val gateQuiet = noiseGate.evaluate(silence)
        assertTrue("Silence should not open noise gate", !gateQuiet.isOpen)
        assertTrue("Silence dBFS should be <= -100", gateQuiet.rmsDbFs <= -100f)

        val signal = generateSine(440.0, 1024, amplitude = 0.5f)
        val gateActive = noiseGate.evaluate(signal)
        assertTrue("Active 440Hz tone should open noise gate", gateActive.isOpen)
        assertTrue("Active tone should be > -45 dBFS", gateActive.rmsDbFs > -45f)
    }

    @Test
    fun `test YIN Pitch Accuracy within 5 cents on A4 (440Hz)`() {
        val pcm = generateSine(440.0, 2048)
        val result = yinDetector.detectPitch(pcm)

        assertNotNull("YIN should detect pitch on 440Hz sine", result)
        assertEquals(440.0, result!!.frequencyHz, 1.2) // Within 1.2 Hz (approx 4.7 cents at 440)
        assertEquals(PitchClass.A, result.pitchResult.pitchClass)
        assertEquals(4, result.pitchResult.octave)
        assertTrue("Cent drift must be within ±5 cents (got ${result.pitchResult.centDrift})",
            abs(result.pitchResult.centDrift) <= 5.0f)
        assertTrue(result.pitchResult.isInTune)
    }

    @Test
    fun `test YIN Pitch Accuracy within 5 cents on C4 (261_63Hz)`() {
        val c4Freq = calibration.frequencyForMidiNote(60) // 261.6255 Hz
        val pcm = generateSine(c4Freq, 2048)
        val result = yinDetector.detectPitch(pcm)

        assertNotNull(result)
        assertEquals(c4Freq, result!!.frequencyHz, 0.8)
        assertEquals(PitchClass.C, result.pitchResult.pitchClass)
        assertEquals(4, result.pitchResult.octave)
        assertTrue("Cent drift must be within ±5 cents (got ${result.pitchResult.centDrift})",
            abs(result.pitchResult.centDrift) <= 5.0f)
    }

    @Test
    fun `test YIN Pitch Accuracy on Sawtooth harmonic wave (Guitar simulation)`() {
        val e2Freq = calibration.frequencyForMidiNote(40) // 82.4 Hz (Low E on guitar)
        val pcm = generateSawtooth(e2Freq, 2048)
        val result = yinDetector.detectPitch(pcm)

        assertNotNull("YIN must track harmonic rich sawtooth", result)
        assertEquals(e2Freq, result!!.frequencyHz, 0.8)
        assertEquals(PitchClass.E, result.pitchResult.pitchClass)
        assertEquals(2, result.pitchResult.octave)
        assertTrue("Cent drift on E2 must be within ±5 cents",
            abs(result.pitchResult.centDrift) <= 5.0f)
    }

    @Test
    fun `test Polyphonic C Major Triad detection (C4 + E4 + G4)`() {
        val c4 = calibration.frequencyForMidiNote(60) // 261.63 Hz
        val e4 = calibration.frequencyForMidiNote(64) // 329.63 Hz
        val g4 = calibration.frequencyForMidiNote(67) // 392.00 Hz

        val triadPcm = generateTriad(c4, e4, g4, 2048)
        val chroma = chromaAnalyzer.extractChroma(triadPcm)

        // Verify C, E, G bins have the highest energy
        assertTrue("C should have high chroma energy", chroma[PitchClass.C.semitone] > 0.3f)
        assertTrue("E should have high chroma energy", chroma[PitchClass.E.semitone] > 0.3f)
        assertTrue("G should have high chroma energy", chroma[PitchClass.G.semitone] > 0.3f)

        val chordMatch = chordMatcher.matchFromChroma(chroma)
        assertNotNull("C Major triad should be detected", chordMatch)
        assertEquals(PitchClass.C, chordMatch!!.root)
        assertEquals(ChordQuality.MAJOR, chordMatch.quality)
        assertTrue("Match score should be > 0.85", chordMatch.matchScore > 0.85f)
    }

    @Test
    fun `test Polyphonic A Minor Triad detection (A3 + C4 + E4)`() {
        val a3 = calibration.frequencyForMidiNote(57) // 220.0 Hz
        val c4 = calibration.frequencyForMidiNote(60) // 261.63 Hz
        val e4 = calibration.frequencyForMidiNote(64) // 329.63 Hz

        val triadPcm = generateTriad(a3, c4, e4, 2048)
        val chroma = chromaAnalyzer.extractChroma(triadPcm)

        val chordMatch = chordMatcher.matchFromChroma(chroma)
        assertNotNull("A Minor triad should be detected", chordMatch)
        assertEquals(PitchClass.A, chordMatch!!.root)
        assertEquals(ChordQuality.MINOR, chordMatch.quality)
    }

    @Test
    fun `test Spectral Flux Onset Detection and Timing Calculation`() {
        val onsetDetector = SpectralFluxOnset(sampleRate = sampleRate, fftSize = 1024)

        // Rush calculation: played at 480ms when beat 1 is at 500ms (120 bpm = 500ms intervals)
        val deltaRush = onsetDetector.calculateTimingDelta(
            onsetTimestampMs = 480L,
            gridStartTimeMs = 0L,
            bpm = 120
        )
        assertEquals(-20.0f, deltaRush, 0.1f) // 20ms rush

        // Drag calculation: played at 518ms when beat 1 is at 500ms
        val deltaDrag = onsetDetector.calculateTimingDelta(
            onsetTimestampMs = 518L,
            gridStartTimeMs = 0L,
            bpm = 120
        )
        assertEquals(18.0f, deltaDrag, 0.1f) // 18ms drag
    }
}
