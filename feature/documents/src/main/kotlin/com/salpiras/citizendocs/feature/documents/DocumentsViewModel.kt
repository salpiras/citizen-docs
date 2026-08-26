package com.salpiras.citizendocs.feature.documents

import androidx.lifecycle.viewModelScope
import com.salpiras.citizendocs.core.data.DocumentsRepository
import com.salpiras.citizendocs.core.domain.DeleteDocumentUseCase
import com.salpiras.citizendocs.core.domain.DeleteResult
import com.salpiras.citizendocs.core.domain.ExportDocumentsUseCase
import com.salpiras.citizendocs.core.domain.ExportResult
import com.salpiras.citizendocs.core.domain.RenameDocumentUseCase
import com.salpiras.citizendocs.core.domain.RenameResult
import com.salpiras.citizendocs.core.model.Document
import com.salpiras.citizendocs.core.model.DocumentId
import com.salpiras.citizendocs.core.model.TitleValidation
import com.salpiras.citizendocs.core.ui.UiText
import com.salpiras.citizendocs.core.ui.groupByMonth
import com.salpiras.citizendocs.core.ui.mvi.MviViewModel
import com.salpiras.citizendocs.feature.documents.DocumentsUiState.Content
import com.salpiras.citizendocs.feature.documents.DocumentsUiState.RenameState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.minus
import kotlinx.collections.immutable.plus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import javax.inject.Inject
import kotlin.time.Clock

// flatMapLatest is @ExperimentalCoroutinesApi; the lambda form of debounce is @FlowPreview.
// Opted in here rather than project-wide, so each use is a deliberate choice.
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class DocumentsViewModel @Inject constructor(
    private val repository: DocumentsRepository,
    private val renameDocument: RenameDocumentUseCase,
    private val deleteDocument: DeleteDocumentUseCase,
    private val exportDocuments: ExportDocumentsUseCase,
    private val clock: Clock,
    private val timeZone: TimeZone,
) : MviViewModel<DocumentsUiState, DocumentsEvent, DocumentsEffect>(DocumentsUiState()) {

    // The domain objects behind the current UI models, so click handlers can resolve an id
    // back to a Document, and so export can archive exactly what the list is showing.
    private var documents: List<Document> = emptyList()

    /** Drives the query the database is actually running, separate from the text in the field. */
    private val query = MutableStateFlow("")

    // What the previous emission contained, so an insertion can be told apart from a
    // re-query. Null until the first emission arrives.
    private var knownIds: Set<DocumentId>? = null
    private var lastQuery: String? = null
    private var highlightJob: Job? = null

    init {
        query
            // Clearing feels instant; typing doesn't re-query on every keystroke.
            .debounce { if (it.isEmpty()) 0L else SEARCH_DEBOUNCE_MS }
            .flatMapLatest { repository.observeDocuments(it) }
            .onEach { docs ->
                val currentQuery = query.value
                val ids = docs.mapTo(mutableSetOf(), Document::id)
                val arrived = newlyArrived(ids, currentQuery)

                knownIds = ids
                lastQuery = currentQuery
                documents = docs
                setState { copy(content = contentFor(docs, currentQuery)) }

                arrived?.let(::highlight)
            }.catch {
                setState { copy(content = Content.Error(UiText.Res(R.string.documents_load_failed))) }
            }.launchIn(viewModelScope)
    }

    /**
     * The one id that appeared since the last emission, or null if this was not an insertion.
     *
     * Two cases have to be excluded or the list would light up for no reason. The first
     * emission has nothing to compare against, so every document would count as new. And a
     * changed query re-emits a different subset of the library — clearing a search brings
     * rows *back*, which is not the same thing as a document arriving. Requiring exactly one
     * new id under an unchanged query leaves only the case this is for: a scan just landed.
     */
    private fun newlyArrived(ids: Set<DocumentId>, currentQuery: String): DocumentId? {
        val previous = knownIds ?: return null
        if (lastQuery != currentQuery) return null
        return (ids - previous).singleOrNull()
    }

    private fun highlight(id: DocumentId) {
        // A second scan saved before the first has faded replaces it rather than racing it.
        highlightJob?.cancel()
        highlightJob = viewModelScope.launch {
            setState { copy(highlighted = id) }
            delay(HIGHLIGHT_MS)
            setState { copy(highlighted = null) }
        }
    }

    private fun contentFor(documents: List<Document>, query: String): Content = when {
        documents.isNotEmpty() -> Content.Documents(documents.groupByMonth())
        query.isNotEmpty() -> Content.NoResults(query)
        else -> Content.Empty
    }

    override fun onEvent(event: DocumentsEvent) {
        when (event) {
            DocumentsEvent.ScanClicked -> sendEffect(DocumentsEffect.LaunchScanner)

            is DocumentsEvent.DocumentClicked -> openDocument(event.id)

            is DocumentsEvent.DeleteClicked -> delete(event.id)

            is DocumentsEvent.RenameClicked -> startRename(event.id)

            is DocumentsEvent.RenameTitleChanged ->
                setState { copy(rename = rename?.copy(title = event.title, error = null)) }

            DocumentsEvent.RenameConfirmed -> confirmRename()

            DocumentsEvent.RenameDismissed -> setState { copy(rename = null) }

            DocumentsEvent.SearchOpened -> setState { copy(isSearchActive = true) }

            DocumentsEvent.SearchClosed -> closeSearch()

            is DocumentsEvent.SearchQueryChanged -> changeQuery(event.query)

            is DocumentsEvent.GroupToggled -> toggleGroup(event.key)

            DocumentsEvent.ExportClicked -> requestExport()

            is DocumentsEvent.ExportDestinationChosen -> export(event.uri)

            DocumentsEvent.ExportCancelled -> setState { copy(isExporting = false) }
        }
    }

    private fun openDocument(id: DocumentId) {
        val document = documents.firstOrNull { it.id == id } ?: return
        sendEffect(DocumentsEffect.OpenDocument(repository.contentUri(document)))
    }

    private fun startRename(id: DocumentId) {
        val document = documents.firstOrNull { it.id == id } ?: return
        setState { copy(rename = RenameState(id = id, title = document.title)) }
    }

    private fun changeQuery(newQuery: String) {
        // The field updates immediately so typing never feels laggy; the debounced flow
        // decides when the database is actually asked.
        setState { copy(searchQuery = newQuery) }
        query.value = newQuery
    }

    private fun closeSearch() {
        setState { copy(isSearchActive = false, searchQuery = "") }
        query.value = ""
    }

    private fun toggleGroup(key: String) = setState {
        copy(collapsedGroups = if (key in collapsedGroups) collapsedGroups - key else collapsedGroups + key)
    }

    private fun requestExport() {
        if (documents.isEmpty()) {
            sendEffect(DocumentsEffect.ShowMessage(UiText.Res(R.string.documents_export_nothing)))
            return
        }
        val today = clock.todayIn(timeZone)
        sendEffect(DocumentsEffect.LaunchExportPicker("citizen-docs-$today.zip"))
    }

    private fun export(destinationUri: String) {
        if (currentState.isExporting) return
        setState { copy(isExporting = true) }

        // Snapshot what the list shows now, so an active search narrows the export and a
        // concurrent database emission can't change the archive mid-write.
        val toExport = documents
        viewModelScope.launch {
            val result = exportDocuments(toExport, destinationUri)
            setState { copy(isExporting = false) }

            val message = when (result) {
                is ExportResult.Exported -> UiText.Res(R.string.documents_export_done, listOf(result.documentCount))
                ExportResult.NothingToExport -> UiText.Res(R.string.documents_export_nothing)
                is ExportResult.Failed -> UiText.Res(R.string.documents_export_failed)
            }
            sendEffect(DocumentsEffect.ShowMessage(message))
        }
    }

    private fun delete(id: DocumentId) {
        val title = documents.firstOrNull { it.id == id }?.title ?: return
        viewModelScope.launch {
            val message = when (deleteDocument(id)) {
                DeleteResult.Deleted -> UiText.Res(R.string.documents_deleted, listOf(title))
                is DeleteResult.Failed -> UiText.Res(R.string.documents_delete_failed)
            }
            sendEffect(DocumentsEffect.ShowMessage(message))
        }
    }

    private fun confirmRename() {
        val rename = currentState.rename ?: return
        viewModelScope.launch {
            when (val result = renameDocument(rename.id, rename.title)) {
                RenameResult.Renamed -> setState { copy(rename = null) }

                // Keep the dialog open and explain why, rather than silently discarding the edit.
                is RenameResult.InvalidTitle ->
                    setState { copy(rename = rename.copy(error = result.reason.asUiText())) }

                is RenameResult.Failed ->
                    sendEffect(DocumentsEffect.ShowMessage(UiText.Res(R.string.documents_rename_failed)))
            }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 250L

        /** Long enough to notice on a list you are already looking at, short of a distraction. */
        const val HIGHLIGHT_MS = 1_600L
    }
}

internal fun TitleValidation.asUiText(): UiText = when (this) {
    TitleValidation.Blank -> UiText.Res(R.string.documents_title_blank)
    is TitleValidation.TooLong -> UiText.Res(R.string.documents_title_too_long, listOf(max))
    is TitleValidation.Valid -> UiText.Literal("")
}
