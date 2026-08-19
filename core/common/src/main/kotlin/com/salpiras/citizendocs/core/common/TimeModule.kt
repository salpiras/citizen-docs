package com.salpiras.citizendocs.core.common

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

/**
 * "Now" is a dependency, not an ambient fact. Injecting it is what lets
 * `OfflineFirstDocumentsRepositoryTest` assert on an exact `createdAt`.
 */
@Module
@InstallIn(SingletonComponent::class)
object TimeModule {
    @Provides
    fun providesClock(): Clock = Clock.System

    @Provides
    fun providesTimeZone(): TimeZone = TimeZone.currentSystemDefault()
}
