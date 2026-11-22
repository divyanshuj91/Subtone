package dev.subtone.audio

import dev.subtone.dsp.AudioAnalysisFrame
import dev.subtone.dsp.ChordMatcher
import dev.subtone.dsp.CqtChromaAnalyzer
import dev.subtone.dsp.NoiseGate
import dev.subtone.dsp.SpectralFluxOnset
import dev.subtone.dsp.YinPitchDetector
import dev.subtone.theory.A4Calibration
import dev.subtone.theory.Transposer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioEngine @Inject constructor() {

    init {
        try {
            System.loadLibrary("subtone_audio")
            nativeInit()
        } catch (e: UnsatisfiedLinkError) {
            // Logged or handled gracefully during unit testing / preview
        }
    }

    private val _analysisFrame = MutableStateFlow(AudioAnalysisFrame.silence())
    val analysisFrame: StateFlow<AudioAnalysisFrame> = _analysisFrame.asStateFlow()

    private var dspJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    // Pipeline processors (Worker Domain 2)
    private var calibration = A4Calibration()
    private var transposer = Transposer.CONCERT_PITCH
    private var noiseGate = NoiseGate()
    private var yinDetector = YinPitchDetector(calibration = calibration)
    private var chromaAnalyzer = CqtChromaAnalyzer(calibration = calibration)
    private var chordMatcher = ChordMatcher()
    private var onsetDetector = SpectralFluxOnset()

    private var currentBpm = 120
    private var isRecording = false

    fun start(sampleRate: Int = 44100): Boolean {
        if (isRecording) return true
        val started = nativeStart()
        if (!started) return false

        isRecording = true
        startDspWorker()
        return true
    }

    fun stop() {
        if (!isRecording) return
        isRecording = false
        dspJob?.cancel()
        nativeStop()
        _analysisFrame.value = AudioAnalysisFrame.silence()
    }

    fun setMetronome(enabled: Boolean, bpm: Int) {
        currentBpm = bpm
        nativeSetMetronome(enabled, bpm)
    }

    fun playReferenceTone(freqHz: Double, amplitude: Float = 0.5f) {
        nativeSetReferenceTone(freqHz, amplitude)
    }

    fun stopReferenceTone() {
        nativeSetReferenceTone(0.0, 0.0f)
    }

    fun updateCalibration(a4Hz: Double) {
        calibration = A4Calibration(a4Hz)
        yinDetector = YinPitchDetector(calibration = calibration)
        chromaAnalyzer = CqtChromaAnalyzer(calibration = calibration)
    }

    fun updateTransposition(semitones: Int) {
        transposer = Transposer(semitones)
    }

    fun updateNoiseGateThreshold(thresholdDbFs: Float) {
        noiseGate.thresholdDbFs = thresholdDbFs
    }

    private fun startDspWorker() {
        dspJob?.cancel()
        dspJob = scope.launch {
            val buffer = FloatArray(2048)
            val gridStartTime = System.currentTimeMillis()

            // 60fps loop ~ 16.6ms
            while (isActive && isRecording) {
                val readCount = nativeReadInputSamples(buffer, 0, buffer.size)
                val now = System.currentTimeMillis()

                if (readCount >= 1024) {
                    val gateResult = noiseGate.evaluate(buffer, 0, readCount)

                    if (!gateResult.isOpen) {
                        _analysisFrame.value = AudioAnalysisFrame.silence(now)
                    } else {
                        // Monophonic pitch tracking
                        val pitchResult = yinDetector.detectPitch(buffer)

                        // Polyphonic chromagram & chord matching
                        val chroma = chromaAnalyzer.extractChroma(buffer)
                        val chord = chordMatcher.matchFromChroma(chroma)

                        // Onset detection
                        val isOnset = onsetDetector.processFrame(buffer)
                        val timingDelta = if (isOnset) {
                            onsetDetector.calculateTimingDelta(now, gridStartTime, currentBpm)
                        } else null

                        // Apply transposition if set
                        val finalPitchClass = pitchResult?.pitchResult?.pitchClass?.let {
                            transposer.transposePitchClass(it)
                        }

                        _analysisFrame.value = AudioAnalysisFrame(
                            timestampMs = now,
                            rmsDbFs = gateResult.rmsDbFs,
                            isSilence = false,
                            detectedPitchHz = pitchResult?.frequencyHz,
                            pitchClass = finalPitchClass,
                            centDrift = pitchResult?.pitchResult?.centDrift,
                            isInTune = pitchResult?.pitchResult?.isInTune ?: false,
                            detectedChord = chord,
                            chroma12 = chroma,
                            isOnset = isOnset,
                            timingDeltaMs = timingDelta
                        )
                    }
                }

                // Throttle DSP emission to ~60fps
                delay(16)
            }
        }
    }

    // --- Native JNI declarations ---
    private external fun nativeInit(): Boolean
    private external fun nativeStart(): Boolean
    private external fun nativeStop()
    private external fun nativeReadInputSamples(buffer: FloatArray, offset: Int, length: Int): Int
    private external fun nativeSetMetronome(enabled: Boolean, bpm: Int)
    private external fun nativeSetReferenceTone(freqHz: Double, amplitude: Float)
    private external fun nativeDestroy()
}
