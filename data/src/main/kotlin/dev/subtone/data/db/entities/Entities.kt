package dev.subtone.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val sessionId: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val bpm: Int,
    val musicalKey: String,
    val instrument: String,
    val intonationAvgCents: Float,
    val timingOffsetAvgMs: Float,
    val timingTendency: String // "rushing", "dragging", "locked"
)

@Entity(tableName = "detected_events")
data class DetectedEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val timestampMs: Long,
    val noteOrChord: String,
    val centDrift: Float?,
    val timingDeltaMs: Float?
)

@Entity(tableName = "flagged_bars")
data class FlaggedBarEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val barNumber: Int,
    val expectedChord: String,
    val expectedTones: String, // Comma-separated or JSON
    val detectedTones: String,
    val missingTones: String,
    val timingDeltaMs: Float
)
