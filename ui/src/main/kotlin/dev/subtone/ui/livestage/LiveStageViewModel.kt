package dev.subtone.ui.livestage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.subtone.audio.AudioEngine
import dev.subtone.data.db.SessionDao
import dev.subtone.data.prefs.SettingsDataStore
import dev.subtone.dsp.AudioAnalysisFrame
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Live Stage ViewModel wired via Hilt.
 * (Hard Rule: Never use manual *ViewModelFactory classes).
 */
@HiltViewModel
class LiveStageViewModel @Inject constructor(
    private val audioEngine: AudioEngine,
    private val settingsDataStore: SettingsDataStore,
    private val sessionDao: SessionDao
) : ViewModel() {

    // Throttled 60fps real-time analysis frame
    val analysisFrame: StateFlow<AudioAnalysisFrame> = audioEngine.analysisFrame
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AudioAnalysisFrame.silence()
        )

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isMetronomeOn = MutableStateFlow(false)
    val isMetronomeOn: StateFlow<Boolean> = _isMetronomeOn.asStateFlow()

    val metronomeBpm: StateFlow<Int> = settingsDataStore.metronomeBpm
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 120)

    val a4ReferenceHz: StateFlow<Double> = settingsDataStore.a4ReferenceHz
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 440.0)

    init {
        viewModelScope.launch {
            settingsDataStore.a4ReferenceHz.collect { hz ->
                audioEngine.updateCalibration(hz)
            }
        }
        viewModelScope.launch {
            settingsDataStore.noiseGateDbFs.collect { db ->
                audioEngine.updateNoiseGateThreshold(db)
            }
        }
        viewModelScope.launch {
            settingsDataStore.semitoneTransposition.collect { st ->
                audioEngine.updateTransposition(st)
            }
        }
    }

    fun toggleRecording() {
        if (_isRecording.value) {
            audioEngine.stop()
            _isRecording.value = false
        } else {
            val started = audioEngine.start()
            if (started) {
                _isRecording.value = true
            }
        }
    }

    fun toggleMetronome() {
        val next = !_isMetronomeOn.value
        _isMetronomeOn.value = next
        audioEngine.setMetronome(next, metronomeBpm.value)
    }

    fun setBpm(bpm: Int) {
        viewModelScope.launch {
            settingsDataStore.setMetronomeBpm(bpm)
            if (_isMetronomeOn.value) {
                audioEngine.setMetronome(true, bpm)
            }
        }
    }

    fun setA4Reference(hz: Double) {
        viewModelScope.launch {
            settingsDataStore.setA4ReferenceHz(hz)
        }
    }

    fun playReferenceA4() {
        audioEngine.playReferenceTone(a4ReferenceHz.value, 0.4f)
    }

    fun stopReferenceA4() {
        audioEngine.stopReferenceTone()
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.stop()
    }
}
