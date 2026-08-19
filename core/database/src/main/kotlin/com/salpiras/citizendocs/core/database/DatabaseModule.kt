package com.salpiras.citizendocs.core.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt owns the database's lifetime. The previous version kept a hand-rolled, non-thread-safe
 * `lateinit` singleton inside the RoomDatabase companion *and* provided it as `@Singleton` —
 * two competing owners for one resource.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    @Provides
    @Singleton
    fun providesDatabase(@ApplicationContext context: Context): CitizenDocsDatabase = Room
        .databaseBuilder(
            context = context,
            klass = CitizenDocsDatabase::class.java,
            name = CitizenDocsDatabase.NAME,
        ).build()

    @Provides
    fun providesDocumentDao(database: CitizenDocsDatabase): DocumentDao = database.documentDao()
}
