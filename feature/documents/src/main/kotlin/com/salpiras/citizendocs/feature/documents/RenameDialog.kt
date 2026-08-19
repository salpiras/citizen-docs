package com.salpiras.citizendocs.feature.documents

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.salpiras.citizendocs.core.designsystem.theme.CitizenDocsTheme
import com.salpiras.citizendocs.core.model.DocumentId
import com.salpiras.citizendocs.core.ui.UiText
import com.salpiras.citizendocs.core.ui.resolve
import com.salpiras.citizendocs.feature.documents.DocumentsUiState.RenameState

/**
 * The edited title lives in [RenameState] inside the ViewModel, not in `remember`, so it
 * survives rotation and process death without any `rememberSaveable` here.
 */
@Composable
internal fun RenameDialog(state: RenameState, onEvent: (DocumentsEvent) -> Unit) {
    AlertDialog(
        onDismissRequest = { onEvent(DocumentsEvent.RenameDismissed) },
        title = { Text(stringResource(R.string.documents_rename)) },
        text = {
            OutlinedTextField(
                value = state.title,
                onValueChange = { onEvent(DocumentsEvent.RenameTitleChanged(it)) },
                label = { Text(stringResource(R.string.documents_title_label)) },
                isError = state.error != null,
                supportingText = state.error?.let { error -> { Text(error.resolve()) } },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onEvent(DocumentsEvent.RenameConfirmed) }) {
                Text(stringResource(R.string.documents_save))
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(DocumentsEvent.RenameDismissed) }) {
                Text(stringResource(R.string.documents_cancel))
            }
        },
    )
}

@PreviewLightDark
@Composable
private fun RenameDialogPreview() {
    CitizenDocsTheme(dynamicColor = false) {
        RenameDialog(
            state = RenameState(id = DocumentId(1), title = "Tax return 2025"),
            onEvent = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun RenameDialogErrorPreview() {
    CitizenDocsTheme(dynamicColor = false) {
        RenameDialog(
            state =
            RenameState(
                id = DocumentId(1),
                title = "",
                error = UiText.Res(R.string.documents_title_blank),
            ),
            onEvent = {},
        )
    }
}
