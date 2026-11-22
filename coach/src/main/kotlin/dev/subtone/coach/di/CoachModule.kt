package dev.subtone.coach.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object CoachModule {
    // LiteRT-LM inference providers wired when Phases 1-5 pass
}
