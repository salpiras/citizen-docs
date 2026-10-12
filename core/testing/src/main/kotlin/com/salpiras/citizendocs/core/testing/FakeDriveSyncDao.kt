package com.salpiras.citizendocs.core.testing

import com.salpiras.citizendocs.core.database.DriveSyncDao
import com.salpiras.citizendocs.core.database.DriveSyncEntity
import com.salpiras.citizendocs.core.drive.DriveAuth
import com.salpiras.citizendocs.core.drive.DriveAuthorization

class FakeDriveSyncDao : DriveSyncDao {
    private val rows = linkedMapOf<String, DriveSyncEntity>()

    override suspend fun find(documentUuid: String): DriveSyncEntity? = rows[documentUuid]

    override suspend fun all(): List<DriveSyncEntity> = rows.values.toList()

    override suspend fun upsert(entity: DriveSyncEntity) {
        rows[entity.documentUuid] = entity
    }

    override suspend fun deleteByUuid(documentUuid: String): Int = if (rows.remove(documentUuid) != null) 1 else 0
}

/** Grants access, unless [authorization] is set to something else. */
class FakeDriveAuth(
    var token: String? = "test-token",
    var authorization: DriveAuthorization = DriveAuthorization.Granted,
) : DriveAuth {
    override suspend fun accessToken(forceRefresh: Boolean): String? = token

    override suspend fun authorize(): DriveAuthorization = authorization
}
