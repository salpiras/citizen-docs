package com.salpiras.citizendocs.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * `exportSchema = true` writes a schema JSON per version into `core/database/schemas`, which
 * is committed. That is the prerequisite for real migrations — the old database used
 * `fallbackToDestructiveMigration()`, which silently wiped user documents on any schema change.
 */
@Database(
    entities = [DocumentEntity::class],
    version = CitizenDocsDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(DocumentConverters::class)
abstract class CitizenDocsDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao

    companion object {
        const val VERSION = 1
        const val NAME = "citizen-docs.db"
    }
}
