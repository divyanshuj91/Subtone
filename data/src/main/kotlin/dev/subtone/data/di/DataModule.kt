package dev.subtone.data.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.subtone.data.db.SessionDao
import dev.subtone.data.db.SubtoneDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideSubtoneDatabase(
        @ApplicationContext context: Context
    ): SubtoneDatabase {
        return Room.databaseBuilder(
            context,
            SubtoneDatabase::class.java,
            "subtone.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideSessionDao(database: SubtoneDatabase): SessionDao {
        return database.sessionDao()
    }
}
