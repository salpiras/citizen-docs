package com.salpiras.citizendocs.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface DriveSyncDao {
    @Query("SELECT * FROM drive_sync WHERE document_uuid = :documentUuid")
    suspend fun find(documentUuid: String): DriveSyncEntity?

    @Query("SELECT * FROM drive_sync")
    suspend fun all(): List<DriveSyncEntity>

    /**
     * Upsert because a backup does not care whether this document has been sent before —
     * both cases end with the same row saying the same thing.
     */
    @Upsert
    suspend fun upsert(entity: DriveSyncEntity)

    @Query("DELETE FROM drive_sync WHERE document_uuid = :documentUuid")
    suspend fun deleteByUuid(documentUuid: String): Int
}
