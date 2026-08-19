package com.salpiras.citizendocs.core.scanner

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class MlKitDocumentScanner
@Inject
constructor() : DocumentScanner {
    private val client by lazy {
        GmsDocumentScanning.getClient(
            GmsDocumentScannerOptions
                .Builder()
                .setGalleryImportAllowed(true)
                .setPageLimit(MAX_PAGES)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .build(),
        )
    }

    override suspend fun startScanIntent(activity: Activity): IntentSender = client.getStartScanIntent(activity).await()

    override fun outcomeFrom(resultCode: Int, data: Intent?): ScanOutcome = when {
        resultCode != Activity.RESULT_OK -> ScanOutcome.Cancelled

        else ->
            GmsDocumentScanningResult
                .fromActivityResultIntent(data)
                ?.pdf
                ?.let { ScanOutcome.Success(ScannedPdf(uri = it.uri.toString(), pageCount = it.pageCount)) }
                ?: ScanOutcome.Failed(IllegalStateException("Scanner returned no PDF"))
    }

    private companion object {
        const val MAX_PAGES = 10
    }
}
