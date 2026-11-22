package dev.subtone.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.subtone.data.db.entities.DetectedEventEntity
import dev.subtone.data.db.entities.FlaggedBarEntity
import dev.subtone.data.db.entities.SessionEntity

@Database(
    entities = [
        SessionEntity::class,
        DetectedEventEntity::class,
        FlaggedBarEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SubtoneDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
}
