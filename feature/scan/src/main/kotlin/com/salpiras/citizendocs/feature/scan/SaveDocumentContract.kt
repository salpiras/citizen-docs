package com.salpiras.citizendocs.feature.scan

import androidx.compose.runtime.Immutable
import com.salpiras.citizendocs.core.ui.UiText
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * Type-safe navigation argument carrying the scan result.
 *
 * Only two primitives cross the boundary, so the destination survives process death — the
 * old flow kept the scanned Uri in a ViewModel field and showed a bottom sheet over the list.
 */
@Serializable
data class SaveDocumentDestination(val pdfUri: String, val pageCount: Int)

@Immutable
data class SaveDocumentUiState(
    val title: String = "",
    val documentDate: LocalDate,
    val pageCount: Int = 0,
    val titleError: UiText? = null,
    val isSaving: Boolean = false,
    val showDatePicker: Boolean = false,
)

sealed interface SaveDocumentEvent {
    data class TitleChanged(val title: String) : SaveDocumentEvent

    data class DateSelected(val date: LocalDate) : SaveDocumentEvent

    data object DatePickerRequested : SaveDocumentEvent

    data object DatePickerDismissed : SaveDocumentEvent

    data object SaveClicked : SaveDocumentEvent

    data object CancelClicked : SaveDocumentEvent
}

sealed interface SaveDocumentEffect {
    data class Saved(val title: String) : SaveDocumentEffect

    data object Dismiss : SaveDocumentEffect

    data class ShowMessage(val message: UiText) : SaveDocumentEffect
}
