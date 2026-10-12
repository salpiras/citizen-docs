package com.salpiras.citizendocs.feature.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salpiras.citizendocs.core.designsystem.component.CitizenDocsIcons
import com.salpiras.citizendocs.core.designsystem.component.CitizenDocsTopAppBar
import com.salpiras.citizendocs.core.designsystem.motion.CitizenDocsMotion
import com.salpiras.citizendocs.core.designsystem.theme.CitizenDocsTheme
import com.salpiras.citizendocs.core.ui.UiText
import com.salpiras.citizendocs.core.ui.mvi.ObserveEffects
import com.salpiras.citizendocs.core.ui.resolve
import com.salpiras.citizendocs.feature.backup.BackupUiState.Working

@Composable
fun BackupRoute(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: BackupViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // The consent screen does not tell us what the user chose — only that it closed. Asking
    // Drive again afterwards is the only way to find out, and it is silent if they agreed.
    val consentLauncher = rememberLauncherForActivityResult(StartIntentSenderForResult()) {
        viewModel.onEvent(BackupEvent.ConsentFinished)
    }

    ObserveEffects(viewModel.effects) { effect ->
        when (effect) {
            is BackupEffect.LaunchConsent ->
                consentLauncher.launch(IntentSenderRequest.Builder(effect.consent).build())

            is BackupEffect.ShowMessage ->
                snackbarHostState.showSnackbar(effect.message.resolve(context))
        }
    }

    BackupScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Composable
internal fun BackupScreen(
    state: BackupUiState,
    onEvent: (BackupEvent) -> Unit,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            CitizenDocsTopAppBar(
                title = stringResource(R.string.backup_title),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = CitizenDocsIcons.Back,
                            contentDescription = stringResource(R.string.backup_close),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DriveCard(isConnected = state.isConnected)

            if (state.isConnected) {
                Button(
                    onClick = { onEvent(BackupEvent.BackUpClicked) },
                    enabled = !state.isBusy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    BusyLabel(
                        busy = state.working == Working.BackingUp,
                        idleText = stringResource(R.string.backup_now),
                        busyText = stringResource(R.string.backup_in_progress),
                    )
                }

                OutlinedButton(
                    onClick = { onEvent(BackupEvent.RestoreClicked) },
                    enabled = !state.isBusy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    BusyLabel(
                        busy = state.working == Working.Restoring,
                        idleText = stringResource(R.string.backup_restore),
                        busyText = stringResource(R.string.backup_restore_in_progress),
                    )
                }
            } else {
                Button(
                    onClick = { onEvent(BackupEvent.ConnectClicked) },
                    enabled = !state.isBusy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.backup_connect))
                }
            }

            state.lastResult?.let { result ->
                Text(
                    text = result.resolve(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * Cross-fades the label rather than swapping it, so a tap on a button that then takes a few
 * seconds does not read as a glitch.
 */
@Composable
private fun BusyLabel(busy: Boolean, idleText: String, busyText: String) {
    AnimatedContent(
        targetState = busy,
        transitionSpec = {
            fadeIn(CitizenDocsMotion.effectsDefault()) togetherWith fadeOut(CitizenDocsMotion.effectsFast())
        },
        label = "busyLabel",
    ) { isBusy ->
        if (isBusy) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text(text = busyText, style = MaterialTheme.typography.labelSmall)
            }
        } else {
            Text(idleText)
        }
    }
}

@Composable
private fun DriveCard(isConnected: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .clip(MaterialTheme.shapes.small)
                .background(
                    if (isConnected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                ),
        ) {
            Icon(
                imageVector = CitizenDocsIcons.Backup,
                contentDescription = null,
                tint = if (isConnected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                },
                modifier = Modifier.size(28.dp),
            )
        }
        Text(
            text = stringResource(
                if (isConnected) R.string.backup_connected_title else R.string.backup_intro_title,
            ),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(
                if (isConnected) R.string.backup_connected_body else R.string.backup_intro_body,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@PreviewLightDark
@Composable
private fun BackupScreenDisconnectedPreview() {
    CitizenDocsTheme {
        BackupScreen(
            state = BackupUiState(),
            onEvent = {},
            onBack = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@PreviewLightDark
@Composable
private fun BackupScreenConnectedPreview() {
    CitizenDocsTheme {
        BackupScreen(
            state = BackupUiState(
                isConnected = true,
                lastResult = UiText.Res(R.string.backup_done, listOf(3, 11)),
            ),
            onEvent = {},
            onBack = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}
