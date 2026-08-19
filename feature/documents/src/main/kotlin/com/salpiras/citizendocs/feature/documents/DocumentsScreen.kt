package com.salpiras.citizendocs.feature.documents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salpiras.citizendocs.core.designsystem.component.CitizenDocsIcons
import com.salpiras.citizendocs.core.designsystem.component.CitizenDocsTopAppBar
import com.salpiras.citizendocs.core.designsystem.component.EmptyState
import com.salpiras.citizendocs.core.designsystem.component.ErrorState
import com.salpiras.citizendocs.core.designsystem.component.LoadingState
import com.salpiras.citizendocs.core.designsystem.theme.CitizenDocsTheme
import com.salpiras.citizendocs.core.model.DocumentId
import com.salpiras.citizendocs.core.ui.DocumentCard
import com.salpiras.citizendocs.core.ui.DocumentUiModel
import com.salpiras.citizendocs.core.ui.mvi.ObserveEffects
import com.salpiras.citizendocs.core.ui.openPdf
import com.salpiras.citizendocs.core.ui.resolve
import com.salpiras.citizendocs.feature.documents.DocumentsUiState.Content
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * Stateful entry point: owns DI and effect handling. Everything visual lives in the
 * stateless [DocumentsScreen] below, which is what previews and tests drive.
 */
@Composable
fun DocumentsRoute(
    onScanRequested: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DocumentsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val noViewerMessage = stringResource(R.string.documents_no_pdf_viewer)

    ObserveEffects(viewModel.effects) { effect ->
        when (effect) {
            DocumentsEffect.LaunchScanner -> onScanRequested()

            is DocumentsEffect.OpenDocument ->
                if (!context.openPdf(effect.contentUri)) snackbarHostState.showSnackbar(noViewerMessage)

            is DocumentsEffect.ShowMessage ->
                snackbarHostState.showSnackbar(effect.message.resolve(context))
        }
    }

    DocumentsScreen(
        state = state,
        onEvent = viewModel::onEvent,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DocumentsScreen(
    state: DocumentsUiState,
    onEvent: (DocumentsEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { CitizenDocsTopAppBar(title = stringResource(R.string.documents_title)) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { onEvent(DocumentsEvent.ScanClicked) }) {
                Icon(
                    imageVector = CitizenDocsIcons.DocumentScanner,
                    contentDescription = stringResource(R.string.documents_scan_action),
                )
            }
        },
    ) { padding ->
        when (val content = state.content) {
            Content.Loading ->
                LoadingState(
                    contentDescription = stringResource(R.string.documents_loading),
                    modifier = Modifier.padding(padding),
                )

            Content.Empty ->
                EmptyState(
                    icon = CitizenDocsIcons.DocumentScanner,
                    title = stringResource(R.string.documents_empty_title),
                    body = stringResource(R.string.documents_empty_body),
                    modifier = Modifier.padding(padding),
                )

            is Content.Error ->
                ErrorState(
                    icon = CitizenDocsIcons.Error,
                    title = stringResource(R.string.documents_error_title),
                    message = content.message.resolve(),
                    modifier = Modifier.padding(padding),
                )

            is Content.Documents ->
                DocumentList(
                    documents = content.documents,
                    onEvent = onEvent,
                    contentPadding = padding,
                )
        }
    }

    state.rename?.let { rename -> RenameDialog(state = rename, onEvent = onEvent) }
}

@Composable
private fun DocumentList(
    documents: ImmutableList<DocumentUiModel>,
    onEvent: (DocumentsEvent) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        // consumeWindowInsets prevents any nested component double-applying the same insets.
        modifier =
        modifier
            .fillMaxSize()
            .consumeWindowInsets(contentPadding),
        contentPadding =
        PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            // Leaves room for the FAB so the last row is never trapped underneath it.
            bottom = contentPadding.calculateBottomPadding() + 88.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Keyed on the id: the old list keyed on title, so two documents sharing a name
        // crashed the LazyColumn with a duplicate-key exception.
        items(items = documents, key = { it.id.value }) { document ->
            DocumentCard(
                document = document,
                onClick = { onEvent(DocumentsEvent.DocumentClicked(document.id)) },
                trailing = { DocumentOverflowMenu(document = document, onEvent = onEvent) },
            )
        }
    }
}

@Composable
private fun DocumentOverflowMenu(document: DocumentUiModel, onEvent: (DocumentsEvent) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }) {
        Icon(
            imageVector = CitizenDocsIcons.MoreOptions,
            contentDescription = stringResource(R.string.documents_more_options, document.title),
        )
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.documents_rename)) },
            leadingIcon = { Icon(CitizenDocsIcons.Rename, contentDescription = null) },
            onClick = {
                expanded = false
                onEvent(DocumentsEvent.RenameClicked(document.id))
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.documents_delete)) },
            leadingIcon = { Icon(CitizenDocsIcons.Delete, contentDescription = null) },
            onClick = {
                expanded = false
                onEvent(DocumentsEvent.DeleteClicked(document.id))
            },
        )
    }
}

@PreviewLightDark
@Composable
private fun DocumentsScreenEmptyPreview() {
    CitizenDocsTheme(dynamicColor = false) {
        DocumentsScreen(
            state = DocumentsUiState(content = Content.Empty),
            onEvent = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@PreviewLightDark
@Composable
private fun DocumentsScreenPopulatedPreview() {
    CitizenDocsTheme(dynamicColor = false) {
        DocumentsScreen(
            state = DocumentsUiState(content = Content.Documents(previewDocuments())),
            onEvent = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

private fun previewDocuments(): ImmutableList<DocumentUiModel> = persistentListOf(
    DocumentUiModel(DocumentId(1), "Tax return 2025", "12 Jan 2026", 3, 248_000),
    DocumentUiModel(DocumentId(2), "Passport", "30 Jun 2025", 1, 96_500),
    DocumentUiModel(DocumentId(3), "Tenancy agreement", "3 Nov 2024", 12, 1_340_000),
).toImmutableList()
