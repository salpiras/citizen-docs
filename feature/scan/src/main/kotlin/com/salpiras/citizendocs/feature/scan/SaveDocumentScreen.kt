package com.salpiras.citizendocs.feature.scan

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salpiras.citizendocs.core.designsystem.component.CitizenDocsIcons
import com.salpiras.citizendocs.core.designsystem.component.CitizenDocsTopAppBar
import com.salpiras.citizendocs.core.designsystem.motion.CitizenDocsMotion
import com.salpiras.citizendocs.core.designsystem.theme.CitizenDocsTheme
import com.salpiras.citizendocs.core.ui.mvi.ObserveEffects
import com.salpiras.citizendocs.core.ui.resolve
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.time.Instant

@Composable
fun SaveDocumentRoute(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SaveDocumentViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    ObserveEffects(viewModel.effects) { effect ->
        when (effect) {
            is SaveDocumentEffect.Saved -> onFinished()

            SaveDocumentEffect.Dismiss -> onFinished()

            is SaveDocumentEffect.ShowMessage ->
                snackbarHostState.showSnackbar(effect.message.resolve(context))
        }
    }

    SaveDocumentScreen(
        state = state,
        onEvent = viewModel::onEvent,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SaveDocumentScreen(
    state: SaveDocumentUiState,
    onEvent: (SaveDocumentEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            CitizenDocsTopAppBar(
                title = stringResource(R.string.scan_save_title),
                navigationIcon = {
                    IconButton(onClick = { onEvent(SaveDocumentEvent.CancelClicked) }) {
                        Icon(
                            imageVector = CitizenDocsIcons.Close,
                            contentDescription = stringResource(R.string.scan_cancel),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(padding)
                // imePadding must precede verticalScroll, otherwise the scroll container is sized
                // before the keyboard inset is applied and the focused field ends up behind the IME.
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // The same tile-and-row shape the list uses, so what you are about to save looks
            // like what you will find afterwards.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(12.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Icon(
                        imageVector = CitizenDocsIcons.Document,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Text(
                    text = pluralStringResource(R.plurals.scan_pages_scanned, state.pageCount, state.pageCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedTextField(
                value = state.title,
                onValueChange = { onEvent(SaveDocumentEvent.TitleChanged(it)) },
                label = { Text(stringResource(R.string.scan_title_label)) },
                isError = state.titleError != null,
                supportingText = state.titleError?.let { error -> { Text(error.resolve()) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedButton(
                onClick = { onEvent(SaveDocumentEvent.DatePickerRequested) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(
                        R.string.scan_date_label,
                        state.documentDate
                            .toJavaLocalDate()
                            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                    ),
                )
            }

            Button(
                onClick = { onEvent(SaveDocumentEvent.SaveClicked) },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Cross-fade rather than a bare `if`. Swapping the label for a spinner
                // outright made the button flicker at the exact moment the user is waiting
                // to see whether the tap registered.
                AnimatedContent(
                    targetState = state.isSaving,
                    transitionSpec = {
                        fadeIn(CitizenDocsMotion.effectsDefault()) togetherWith
                            fadeOut(CitizenDocsMotion.effectsFast())
                    },
                    label = "saveButton",
                ) { saving ->
                    if (saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(stringResource(R.string.scan_save))
                    }
                }
            }
        }
    }

    if (state.showDatePicker) {
        DocumentDatePickerDialog(
            selected = state.documentDate,
            onDateSelected = { onEvent(SaveDocumentEvent.DateSelected(it)) },
            onDismiss = { onEvent(SaveDocumentEvent.DatePickerDismissed) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentDatePickerDialog(selected: LocalDate, onDateSelected: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val datePickerState =
        rememberDatePickerState(
            initialSelectedDateMillis = selected.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
        )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        // The picker reports UTC midnight; read it back in UTC so the date the user
                        // tapped is the date that gets stored, regardless of device time zone.
                        onDateSelected(
                            Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date,
                        )
                    } ?: onDismiss()
                },
            ) {
                Text(stringResource(R.string.scan_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.scan_cancel)) }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

@PreviewLightDark
@Composable
private fun SaveDocumentScreenPreview() {
    CitizenDocsTheme(dynamicColor = false) {
        SaveDocumentScreen(
            state =
            SaveDocumentUiState(
                title = "Tax return 2025",
                documentDate = LocalDate(2026, 1, 12),
                pageCount = 3,
            ),
            onEvent = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}
