package com.salpiras.citizendocs.feature.documents

import androidx.compose.runtime.Immutable
import com.salpiras.citizendocs.core.model.DocumentId
import com.salpiras.citizendocs.core.ui.DocumentGroup
import com.salpiras.citizendocs.core.ui.UiText
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf

@Immutable
data class DocumentsUiState(
    val content: Content = Content.Loading,
    val rename: RenameState? = null,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    /** Keys of month sections the user has collapsed. Persistent so +/- stay immutable. */
    val collapsedGroups: PersistentSet<String> = persistentSetOf(),
    val isExporting: Boolean = false,
    /**
     * A document that has just been added, for the row to acknowledge briefly.
     *
     * Transient by design: the ViewModel sets it when it sees an insertion and clears it a
     * moment later, so nothing has to remember to switch it off and rotation cannot replay it.
     */
    val highlighted: DocumentId? = null,
) {
    @Immutable
    sealed interface Content {
        data object Loading : Content

        /** Nothing saved at all. */
        data object Empty : Content

        /**
         * Nothing matches the active search. Distinct from [Empty] because "you have no
         * documents" is the wrong thing to say to someone with forty who mistyped.
         */
        data class NoResults(val query: String) : Content

        data class Documents(val groups: ImmutableList<DocumentGroup>) : Content

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

    data object SearchOpened : DocumentsEvent

    data object SearchClosed : DocumentsEvent

    data class SearchQueryChanged(val query: String) : DocumentsEvent

    data class GroupToggled(val key: String) : DocumentsEvent

    data object ExportClicked : DocumentsEvent

    /** The user picked a destination in the system file picker. */
    data class ExportDestinationChosen(val uri: String) : DocumentsEvent

    data object ExportCancelled : DocumentsEvent
}

/** One-shot actions. Deliberately not part of the state, so they don't replay on rotation. */
sealed interface DocumentsEffect {
    data object LaunchScanner : DocumentsEffect

    data class OpenDocument(val contentUri: String) : DocumentsEffect

    data class LaunchExportPicker(val suggestedFileName: String) : DocumentsEffect

    data class ShowMessage(val message: UiText) : DocumentsEffect
}
