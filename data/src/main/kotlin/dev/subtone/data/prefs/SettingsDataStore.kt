package dev.subtone.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "subtone_settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val keyA4Hz = doublePreferencesKey("a4_reference_hz")
    private val keyNoiseGate = floatPreferencesKey("noise_gate_dbfs")
    private val keyTransposition = intPreferencesKey("semitone_transposition")
    private val keyBpm = intPreferencesKey("metronome_bpm")

    val a4ReferenceHz: Flow<Double> = context.dataStore.data.map { prefs ->
        prefs[keyA4Hz] ?: 440.0
    }

    val noiseGateDbFs: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[keyNoiseGate] ?: -45.0f
    }

    val semitoneTransposition: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[keyTransposition] ?: 0
    }

    val metronomeBpm: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[keyBpm] ?: 120
    }

    suspend fun setA4ReferenceHz(hz: Double) {
        context.dataStore.edit { prefs -> prefs[keyA4Hz] = hz }
    }

    suspend fun setNoiseGateDbFs(dbFs: Float) {
        context.dataStore.edit { prefs -> prefs[keyNoiseGate] = dbFs }
    }

    suspend fun setSemitoneTransposition(semitones: Int) {
        context.dataStore.edit { prefs -> prefs[keyTransposition] = semitones }
    }

    suspend fun setMetronomeBpm(bpm: Int) {
        context.dataStore.edit { prefs -> prefs[keyBpm] = bpm }
    }
}
