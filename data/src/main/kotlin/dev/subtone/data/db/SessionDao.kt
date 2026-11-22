package dev.subtone.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import dev.subtone.data.db.entities.DetectedEventEntity
import dev.subtone.data.db.entities.FlaggedBarEntity
import dev.subtone.data.db.entities.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<DetectedEventEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlaggedBars(bars: List<FlaggedBarEntity>)

    @Transaction
    suspend fun commitSessionWithEvents(
        session: SessionEntity,
        events: List<DetectedEventEntity>,
        flaggedBars: List<FlaggedBarEntity>
    ) {
        insertSession(session)
        insertEvents(events)
        insertFlaggedBars(flaggedBars)
    }

    @Query("SELECT * FROM sessions ORDER BY startTimeMs DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): SessionEntity?

    @Query("SELECT * FROM flagged_bars WHERE sessionId = :sessionId ORDER BY barNumber ASC")
    suspend fun getFlaggedBarsForSession(sessionId: String): List<FlaggedBarEntity>
}
