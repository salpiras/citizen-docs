package com.salpiras.citizendocs.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Instant

/**
 * What this device has already put in Drive, and when.
 *
 * A table of its own rather than columns on `documents`, for two reasons. Sync is optional —
 * someone who never connects Drive carries an empty table instead of three dead columns on
 * every row — and the document entity stays about documents.
 *
 * Keyed on the document's uuid, not its row id, because the whole point of the uuid is that
 * it survives leaving this device. A row here says "the document with this identity is in
 * Drive as this file, as it looked at this moment".
 */
@Entity(tableName = "drive_sync")
data class DriveSyncEntity(
    @PrimaryKey @ColumnInfo(name = "document_uuid") val documentUuid: String,
    @ColumnInfo(name = "drive_file_id") val driveFileId: String,
    /**
     * Compared against `documents.updated_at` to decide whether to send the file again.
     * Recorded only after Drive has confirmed the write, so a backup interrupted half way
     * resumes rather than assuming the files it never sent are safe.
     */
    @ColumnInfo(name = "uploaded_at") val uploadedAt: Instant,
)
