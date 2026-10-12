package com.salpiras.citizendocs.core.data

import android.app.PendingIntent

/** How many documents moved, and how many did not need to. */
data class BackupReport(val uploaded: Int, val upToDate: Int)

/** [alreadyHeld] is the count skipped because this device already has that document. */
data class RestoreReport(val restored: Int, val alreadyHeld: Int)

/**
 * Where the app stands with Drive.
 *
 * [NeedsConsent] carries a [PendingIntent] because only an Activity can show Google's
 * consent screen. The same shape as `ScanOutcome` handing back an `IntentSender`: the
 * decision is the UI's, the rest of the flow is not.
 */
sealed interface DriveConnection {
    data object Connected : DriveConnection

    data class NeedsConsent(val consent: PendingIntent) : DriveConnection

    data class Failed(val cause: Throwable) : DriveConnection
}

/**
 * Drive access has not been granted, or has been revoked.
 *
 * Declared here rather than reusing `:core:drive`'s own exception so that `:core:drive` stops
 * at this module: layers above need to tell "not connected" from "went wrong", but they have
 * no business knowing which cloud it is.
 */
class DriveNotConnectedException : Exception("Drive has not been connected")

/**
 * Backup and restore against the user's Drive.
 *
 * Separate from [DocumentsRepository] on purpose: that one owns what a document *is*, and
 * this one owns where copies of it have got to. Nothing in the app needs both, and keeping
 * them apart means the document repository has no opinion about the network.
 */
interface DriveBackupRepository {
    suspend fun connect(): DriveConnection

    /** Uploads anything new or edited since it was last sent. */
    suspend fun backUp(): BackupReport

    /** Brings down anything this device does not already hold, matched on uuid. */
    suspend fun restore(): RestoreReport
}
