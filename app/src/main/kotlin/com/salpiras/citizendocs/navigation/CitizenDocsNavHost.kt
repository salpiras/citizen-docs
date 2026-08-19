package com.salpiras.citizendocs.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.salpiras.citizendocs.R
import com.salpiras.citizendocs.core.scanner.DocumentScanner
import com.salpiras.citizendocs.core.scanner.ScanOutcome
import com.salpiras.citizendocs.core.scanner.rememberDocumentScanLauncher
import com.salpiras.citizendocs.feature.documents.DocumentsRoute
import com.salpiras.citizendocs.feature.scan.SaveDocumentDestination
import com.salpiras.citizendocs.feature.scan.SaveDocumentRoute
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data object DocumentsDestination

/**
 * The scan itself is a system activity, not a destination: the FAB launches ML Kit, and only
 * a successful scan navigates onward to the save screen carrying the resulting PDF.
 */
@Composable
fun CitizenDocsNavHost(
    documentScanner: DocumentScanner,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scannerUnavailable = stringResource(R.string.scanner_unavailable)

    val launchScan =
        rememberDocumentScanLauncher(documentScanner) { outcome ->
            when (outcome) {
                is ScanOutcome.Success ->
                    navController.navigate(
                        SaveDocumentDestination(pdfUri = outcome.pdf.uri, pageCount = outcome.pdf.pageCount),
                    )

                // Backing out of the scanner is a normal thing to do, not an error worth reporting.
                ScanOutcome.Cancelled -> Unit

                is ScanOutcome.Failed -> scope.launch { snackbarHostState.showSnackbar(scannerUnavailable) }
            }
        }

    Box(modifier = modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = DocumentsDestination) {
            composable<DocumentsDestination> {
                DocumentsRoute(onScanRequested = launchScan)
            }
            composable<SaveDocumentDestination> {
                SaveDocumentRoute(onFinished = { navController.popBackStack() })
            }
        }

        // Host-level snackbar for failures that happen outside any screen — chiefly the
        // scanner being unavailable because Play services are missing.
        SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}
