package com.salpiras.citizendocs.feature.documents

import androidx.compose.runtime.Immutable
import com.salpiras.citizendocs.core.model.DocumentId
import com.salpiras.citizendocs.core.ui.DocumentUiModel
import com.salpiras.citizendocs.core.ui.UiText
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class DocumentsUiState(val content: Content = Content.Loading, val rename: RenameState? = null) {
    @Immutable
    sealed interface Content {
        data object Loading : Content

        data object Empty : Content

        data class Documents(val documents: ImmutableList<DocumentUiModel>) : Content

        data class Error(val message: UiText) : Content
    }

    @Immutable
    data class RenameState(val id: DocumentId, val title: String, val error: UiText? = null)
}

/** Every interaction the list screen supports. Nothing reaches the ViewModel except these. */
sealed interface DocumentsEvent {
    data object ScanClicked : DocumentsEvent

    data class DocumentClicked(val id: DocumentId) : DocumentsEvent

    data class DeleteClicked(val id: DocumentId) : DocumentsEvent

    data class RenameClicked(val id: DocumentId) : DocumentsEvent

    data class RenameTitleChanged(val title: String) : DocumentsEvent

    data object RenameConfirmed : DocumentsEvent

    data object RenameDismissed : DocumentsEvent
}

/** One-shot actions. Deliberately not part of the state, so they don't replay on rotation. */
sealed interface DocumentsEffect {
    data object LaunchScanner : DocumentsEffect

    data class OpenDocument(val contentUri: String) : DocumentsEffect

    data class ShowMessage(val message: UiText) : DocumentsEffect
}
