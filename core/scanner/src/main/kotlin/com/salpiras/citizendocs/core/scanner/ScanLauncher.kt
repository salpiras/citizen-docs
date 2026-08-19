package com.salpiras.citizendocs.core.scanner

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

/**
 * Returns a callback that starts the system document scanner and reports the [ScanOutcome].
 *
 * Hides the activity-result plumbing from feature modules, and resolves the Activity by
 * unwrapping ContextWrappers rather than the unchecked `LocalContext.current as Activity`
 * cast the old list screen used — that cast crashes under any wrapped context.
 */
@Composable
fun rememberDocumentScanLauncher(scanner: DocumentScanner, onOutcome: (ScanOutcome) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnOutcome by rememberUpdatedState(onOutcome)

    val launcher =
        rememberLauncherForActivityResult(StartIntentSenderForResult()) { result ->
            currentOnOutcome(scanner.outcomeFrom(result.resultCode, result.data))
        }

    return remember(launcher, context, scanner) {
        {
            val activity = context.findActivity()
            if (activity == null) {
                currentOnOutcome(ScanOutcome.Failed(IllegalStateException("No Activity in context")))
            } else {
                scope.launch {
                    runCatching { scanner.startScanIntent(activity) }
                        .onSuccess { launcher.launch(IntentSenderRequest.Builder(it).build()) }
                        .onFailure { currentOnOutcome(ScanOutcome.Failed(it)) }
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
