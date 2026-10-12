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
    entities = [DocumentEntity::class, DriveSyncEntity::class],
    version = CitizenDocsDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(DocumentConverters::class)
abstract class CitizenDocsDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao

    abstract fun driveSyncDao(): DriveSyncDao

    companion object {
        /**
         * 2 added `uuid` and `updated_at` ([MIGRATION_1_2]).
         * 3 added `drive_sync` ([MIGRATION_2_3]).
         */
        const val VERSION = 3
        const val NAME = "citizen-docs.db"
    }
}
