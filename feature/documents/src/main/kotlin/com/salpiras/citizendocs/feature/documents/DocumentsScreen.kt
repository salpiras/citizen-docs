package com.salpiras.citizendocs.feature.documents

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
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
import com.salpiras.citizendocs.core.ui.DocumentGroup
import com.salpiras.citizendocs.core.ui.DocumentUiModel
import com.salpiras.citizendocs.core.ui.mvi.ObserveEffects
import com.salpiras.citizendocs.core.ui.openPdf
import com.salpiras.citizendocs.core.ui.resolve
import com.salpiras.citizendocs.feature.documents.DocumentsUiState.Content
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
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

    // The system picker creates the file and hands back a writable URI; a null result means
    // the user backed out, which is not an error.
    val exportLauncher = rememberLauncherForActivityResult(CreateDocument(ZIP_MIME_TYPE)) { uri ->
        viewModel.onEvent(
            uri?.let { DocumentsEvent.ExportDestinationChosen(it.toString()) }
                ?: DocumentsEvent.ExportCancelled,
        )
    }

    ObserveEffects(viewModel.effects) { effect ->
        when (effect) {
            DocumentsEffect.LaunchScanner -> onScanRequested()

            is DocumentsEffect.OpenDocument ->
                if (!context.openPdf(effect.contentUri)) snackbarHostState.showSnackbar(noViewerMessage)

            is DocumentsEffect.LaunchExportPicker -> exportLauncher.launch(effect.suggestedFileName)

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

@Composable
internal fun DocumentsScreen(
    state: DocumentsUiState,
    onEvent: (DocumentsEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    // Search is a mode, so back should leave it before leaving the screen.
    BackHandler(enabled = state.isSearchActive) { onEvent(DocumentsEvent.SearchClosed) }

    Scaffold(
        modifier = modifier,
        topBar = {
            if (state.isSearchActive) {
                SearchAppBar(query = state.searchQuery, onEvent = onEvent)
            } else {
                DocumentsAppBar(isExporting = state.isExporting, onEvent = onEvent)
            }
        },
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

            is Content.NoResults ->
                EmptyState(
                    icon = CitizenDocsIcons.Search,
                    title = stringResource(R.string.documents_no_results_title),
                    body = stringResource(R.string.documents_no_results_body, content.query),
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
                    groups = content.groups,
                    collapsedGroups = state.collapsedGroups,
                    onEvent = onEvent,
                    contentPadding = padding,
                )
        }
    }

    state.rename?.let { rename -> RenameDialog(state = rename, onEvent = onEvent) }
}

@Composable
private fun DocumentsAppBar(isExporting: Boolean, onEvent: (DocumentsEvent) -> Unit) {
    var menuExpanded by rememberSaveable { mutableStateOf(false) }

    CitizenDocsTopAppBar(
        title = stringResource(R.string.documents_title),
        actions = {
            IconButton(onClick = { onEvent(DocumentsEvent.SearchOpened) }) {
                Icon(
                    imageVector = CitizenDocsIcons.Search,
                    contentDescription = stringResource(R.string.documents_search),
                )
            }
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = CitizenDocsIcons.MoreOptions,
                    contentDescription = stringResource(R.string.documents_more_options_screen),
                )
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                if (isExporting) R.string.documents_exporting else R.string.documents_export,
                            ),
                        )
                    },
                    leadingIcon = { Icon(CitizenDocsIcons.Export, contentDescription = null) },
                    // An export already in flight must not be startable a second time.
                    enabled = !isExporting,
                    onClick = {
                        menuExpanded = false
                        onEvent(DocumentsEvent.ExportClicked)
                    },
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchAppBar(query: String, onEvent: (DocumentsEvent) -> Unit) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    CitizenDocsTopAppBar(
        navigationIcon = {
            IconButton(onClick = { onEvent(DocumentsEvent.SearchClosed) }) {
                Icon(
                    imageVector = CitizenDocsIcons.Back,
                    contentDescription = stringResource(R.string.documents_search_close),
                )
            }
        },
        titleContent = {
            TextField(
                value = query,
                onValueChange = { onEvent(DocumentsEvent.SearchQueryChanged(it)) },
                placeholder = { Text(stringResource(R.string.documents_search_hint)) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onEvent(DocumentsEvent.SearchQueryChanged("")) }) {
                            Icon(
                                imageVector = CitizenDocsIcons.Close,
                                contentDescription = stringResource(R.string.documents_search_clear),
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DocumentList(
    groups: ImmutableList<DocumentGroup>,
    collapsedGroups: PersistentSet<String>,
    onEvent: (DocumentsEvent) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        // consumeWindowInsets prevents any nested component double-applying the same insets.
        modifier = modifier
            .fillMaxSize()
            .consumeWindowInsets(contentPadding),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            // Leaves room for the FAB so the last row is never trapped underneath it.
            bottom = contentPadding.calculateBottomPadding() + 88.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        groups.forEach { group ->
            val collapsed = group.key in collapsedGroups

            stickyHeader(key = group.key) {
                MonthHeader(
                    group = group,
                    collapsed = collapsed,
                    onClick = { onEvent(DocumentsEvent.GroupToggled(group.key)) },
                )
            }

            if (!collapsed) {
                // Keyed on the id: the old list keyed on title, so two documents sharing a
                // name crashed the LazyColumn with a duplicate-key exception.
                items(items = group.documents, key = { it.id.value }) { document ->
                    DocumentCard(
                        document = document,
                        onClick = { onEvent(DocumentsEvent.DocumentClicked(document.id)) },
                        trailing = { DocumentOverflowMenu(document = document, onEvent = onEvent) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(group: DocumentGroup, collapsed: Boolean, onClick: () -> Unit) {
    val toggleDescription = stringResource(
        if (collapsed) R.string.documents_group_expand else R.string.documents_group_collapse,
        group.label,
    )

    // Opaque, because a sticky header scrolls over the rows beneath it.
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        ) {
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = if (collapsed) CitizenDocsIcons.Expand else CitizenDocsIcons.Collapse,
                    contentDescription = toggleDescription,
                )
            }
            Text(text = group.label, style = MaterialTheme.typography.titleMedium)
            Text(
                text = pluralStringResource(
                    R.plurals.documents_group_count,
                    group.documents.size,
                    group.documents.size,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
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

private const val ZIP_MIME_TYPE = "application/zip"

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
            state = DocumentsUiState(content = Content.Documents(previewGroups())),
            onEvent = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@PreviewLightDark
@Composable
private fun DocumentsScreenCollapsedPreview() {
    CitizenDocsTheme(dynamicColor = false) {
        DocumentsScreen(
            state = DocumentsUiState(
                content = Content.Documents(previewGroups()),
                collapsedGroups = persistentSetOf("2025-06"),
            ),
            onEvent = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

internal fun previewGroups(): ImmutableList<DocumentGroup> = persistentListOf(
    DocumentGroup(
        key = "2026-01",
        label = "January 2026",
        documents = persistentListOf(
            DocumentUiModel(DocumentId(1), "Tax return 2025", "12 Jan 2026", 3, 248_000),
        ),
    ),
    DocumentGroup(
        key = "2025-06",
        label = "June 2025",
        documents = persistentListOf(
            DocumentUiModel(DocumentId(2), "Passport", "30 Jun 2025", 1, 96_500),
            DocumentUiModel(DocumentId(3), "Tenancy agreement", "3 Jun 2025", 12, 1_340_000),
        ),
    ),
).toImmutableList()
