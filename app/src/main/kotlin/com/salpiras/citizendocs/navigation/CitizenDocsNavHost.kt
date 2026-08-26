package com.salpiras.citizendocs.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import com.salpiras.citizendocs.core.designsystem.motion.CitizenDocsMotion
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
        NavHost(
            navController = navController,
            startDestination = DocumentsDestination,
            // Forward: the arriving screen slides in over a list that recedes slightly, so
            // the list reads as staying put underneath rather than being replaced. Back
            // reverses it exactly. The scale is small on purpose — at 0.96 it registers as
            // depth, and any deeper starts to look like the screen is falling away.
            enterTransition = {
                slideInHorizontally(CitizenDocsMotion.offsetSlow()) { it / SLIDE_FRACTION } +
                    fadeIn(CitizenDocsMotion.effectsDefault())
            },
            exitTransition = {
                scaleOut(CitizenDocsMotion.effectsDefault(), targetScale = RECEDED_SCALE) +
                    fadeOut(CitizenDocsMotion.effectsFast())
            },
            popEnterTransition = {
                scaleIn(CitizenDocsMotion.effectsDefault(), initialScale = RECEDED_SCALE) +
                    fadeIn(CitizenDocsMotion.effectsDefault())
            },
            popExitTransition = {
                slideOutHorizontally(CitizenDocsMotion.offsetSlow()) { it / SLIDE_FRACTION } +
                    fadeOut(CitizenDocsMotion.effectsFast())
            },
        ) {
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

// A third of the screen's width, not the whole of it: a full-width slide has to travel so
// far that it either overshoots the eye or has to be slowed down until it drags.
private const val SLIDE_FRACTION = 3
private const val RECEDED_SCALE = 0.96f
