package com.salpiras.citizendocs.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Gives every existing document a stable cross-device identity and a last-modified time.
 *
 * The backfill is pure SQL on purpose. `randomblob(16)` through `hex` produces exactly the
 * 32 lowercase hex characters that
 * [com.salpiras.citizendocs.core.model.newDocumentUuid] generates for new rows, so there is
 * no post-migration Kotlin pass to remember, no second code path producing a second id
 * format, and no window in which a row exists without an identity. SQLite evaluates
 * `randomblob` per row, so the UPDATE gives each document its own value rather than
 * stamping all of them with one.
 *
 * Order matters: the unique index is created *after* the backfill. Creating it first would
 * fail the moment a second row took the `''` default.
 *
 * `updated_at` seeds from `created_at` rather than from "now". A document imported from a
 * previous version has not been edited; claiming it changed at migration time would tell a
 * future backup that the entire library needs re-uploading.
 */
internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE documents ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
        db.execSQL("UPDATE documents SET uuid = lower(hex(randomblob(16)))")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_documents_uuid ON documents(uuid)")

        db.execSQL("ALTER TABLE documents ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE documents SET updated_at = created_at")
    }
}

/** Every migration, in order. The database builder takes them as a group. */
internal val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2)
