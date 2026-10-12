package com.salpiras.citizendocs.core.domain

import com.salpiras.citizendocs.core.data.DriveBackupRepository
import com.salpiras.citizendocs.core.data.DriveNotConnectedException
import javax.inject.Inject

sealed interface RestoreResult {
    data class Restored(val restored: Int, val alreadyHeld: Int) : RestoreResult

    /** Drive holds nothing this app put there. */
    data object NothingToRestore : RestoreResult

    data object NotConnected : RestoreResult

    data class Failed(val cause: Throwable) : RestoreResult
}

class RestoreFromDriveUseCase @Inject constructor(private val repository: DriveBackupRepository) {
    suspend operator fun invoke(): RestoreResult = runCatching { repository.restore() }.fold(
        onSuccess = { report ->
            when {
                report.restored == 0 && report.alreadyHeld == 0 -> RestoreResult.NothingToRestore

                else -> RestoreResult.Restored(
                    restored = report.restored,
                    alreadyHeld = report.alreadyHeld,
                )
            }
        },
        onFailure = { cause ->
            if (cause is DriveNotConnectedException) RestoreResult.NotConnected else RestoreResult.Failed(cause)
        },
    )
}
