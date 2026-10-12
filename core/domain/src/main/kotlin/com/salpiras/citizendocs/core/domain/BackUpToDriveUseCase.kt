package com.salpiras.citizendocs.core.domain

import com.salpiras.citizendocs.core.data.DriveBackupRepository
import com.salpiras.citizendocs.core.data.DriveNotConnectedException
import javax.inject.Inject

sealed interface BackupResult {
    data class Backed(val uploaded: Int, val upToDate: Int) : BackupResult

    data object NothingToBackUp : BackupResult

    /**
     * Drive access was never granted, or has been taken away.
     *
     * Its own case rather than a [Failed] because the answer is different: the user has to
     * be offered "connect", and offering them "try again" would be a loop.
     */
    data object NotConnected : BackupResult

    data class Failed(val cause: Throwable) : BackupResult
}

class BackUpToDriveUseCase @Inject constructor(private val repository: DriveBackupRepository) {
    suspend operator fun invoke(): BackupResult = runCatching { repository.backUp() }.fold(
        onSuccess = { report ->
            // Nothing uploaded and nothing already up there means an empty library, which is
            // worth saying plainly — "backed up 0 documents" reads like a failure.
            if (report.uploaded == 0 && report.upToDate == 0) {
                BackupResult.NothingToBackUp
            } else {
                BackupResult.Backed(uploaded = report.uploaded, upToDate = report.upToDate)
            }
        },
        onFailure = { cause ->
            if (cause is DriveNotConnectedException) BackupResult.NotConnected else BackupResult.Failed(cause)
        },
    )
}
