package com.salpiras.citizendocs.core.scanner

import android.app.Activity
import android.content.Intent
import android.content.IntentSender

data class ScannedPdf(val uri: String, val pageCount: Int)

sealed interface ScanOutcome {
    data class Success(val pdf: ScannedPdf) : ScanOutcome

    data object Cancelled : ScanOutcome

    /** Most commonly: Google Play services missing or too old to serve the scanner module. */
    data class Failed(val cause: Throwable) : ScanOutcome
}

/**
 * Wraps ML Kit so nothing above this module imports `com.google.mlkit`, and so `Uri` and
 * `Activity` stop leaking into ViewModels — the old DocsScannerViewModel exposed raw
 * `GmsDocumentScannerOptions` straight to the composable.
 */
interface DocumentScanner {
    suspend fun startScanIntent(activity: Activity): IntentSender

    fun outcomeFrom(resultCode: Int, data: Intent?): ScanOutcome
}
