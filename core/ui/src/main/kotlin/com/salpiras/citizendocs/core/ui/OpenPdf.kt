package com.salpiras.citizendocs.core.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

private const val PDF_MIME_TYPE = "application/pdf"

/**
 * Hands a stored PDF to whichever app the user has for the job.
 *
 * FLAG_GRANT_READ_URI_PERMISSION is what makes the FileProvider URI readable by the target,
 * and only for this Intent — the provider itself stays `exported="false"`. Returns false when
 * no viewer is installed so the caller can surface that rather than crash, which is what the
 * bare `startActivity` in the old code would have done.
 */
fun Context.openPdf(contentUri: String): Boolean {
    val intent =
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri.toUri(), PDF_MIME_TYPE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    return try {
        startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
