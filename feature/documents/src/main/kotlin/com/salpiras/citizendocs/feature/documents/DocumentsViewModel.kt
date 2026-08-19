package com.salpiras.citizendocs.feature.documents

import androidx.lifecycle.viewModelScope
import com.salpiras.citizendocs.core.data.DocumentsRepository
import com.salpiras.citizendocs.core.domain.DeleteDocumentUseCase
import com.salpiras.citizendocs.core.domain.DeleteResult
import com.salpiras.citizendocs.core.domain.RenameDocumentUseCase
import com.salpiras.citizendocs.core.domain.RenameResult
import com.salpiras.citizendocs.core.model.Document
import com.salpiras.citizendocs.core.model.DocumentId
import com.salpiras.citizendocs.core.model.TitleValidation
import com.salpiras.citizendocs.core.ui.UiText
import com.salpiras.citizendocs.core.ui.asUiModel
import com.salpiras.citizendocs.core.ui.mvi.MviViewModel
import com.salpiras.citizendocs.feature.documents.DocumentsUiState.Content
import com.salpiras.citizendocs.feature.documents.DocumentsUiState.RenameState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DocumentsViewModel
@Inject
constructor(
    private val repository: DocumentsRepository,
    private val renameDocument: RenameDocumentUseCase,
    private val deleteDocument: DeleteDocumentUseCase,
) : MviViewModel<DocumentsUiState, DocumentsEvent, DocumentsEffect>(DocumentsUiState()) {
    // The domain objects behind the current UI models, so click handlers can resolve an id
    // back to a Document without the UI ever carrying one.
    private var documents: List<Document> = emptyList()

    init {
        repository
            .observeDocuments()
            .onEach { docs ->
                documents = docs
                setState {
                    copy(
                        content =
                        if (docs.isEmpty()) {
                            Content.Empty
                        } else {
                            Content.Documents(docs.map(Document::asUiModel).toImmutableList())
                        },
                    )
                }
            }.catch {
                setState { copy(content = Content.Error(UiText.Res(R.string.documents_load_failed))) }
            }.launchIn(viewModelScope)
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

    private fun delete(id: DocumentId) {
        val title = documents.firstOrNull { it.id == id }?.title ?: return
        viewModelScope.launch {
            val message =
                when (deleteDocument(id)) {
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
}

internal fun TitleValidation.asUiText(): UiText = when (this) {
    TitleValidation.Blank -> UiText.Res(R.string.documents_title_blank)
    is TitleValidation.TooLong -> UiText.Res(R.string.documents_title_too_long, listOf(max))
    is TitleValidation.Valid -> UiText.Literal("")
}
