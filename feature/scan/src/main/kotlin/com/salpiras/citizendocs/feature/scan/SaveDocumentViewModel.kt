package com.salpiras.citizendocs.feature.scan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.salpiras.citizendocs.core.domain.SaveResult
import com.salpiras.citizendocs.core.domain.SaveScannedDocumentUseCase
import com.salpiras.citizendocs.core.model.TitleValidation
import com.salpiras.citizendocs.core.ui.UiText
import com.salpiras.citizendocs.core.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import javax.inject.Inject
import kotlin.time.Clock

@HiltViewModel
class SaveDocumentViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val saveScannedDocument: SaveScannedDocumentUseCase,
    clock: Clock,
    timeZone: TimeZone,
) : MviViewModel<SaveDocumentUiState, SaveDocumentEvent, SaveDocumentEffect>(
    SaveDocumentUiState(documentDate = clock.todayIn(timeZone)),
) {
    private val route: SaveDocumentDestination = savedStateHandle.toRoute()

    init {
        setState { copy(pageCount = route.pageCount) }
    }

    override fun onEvent(event: SaveDocumentEvent) {
        when (event) {
            is SaveDocumentEvent.TitleChanged ->
                setState { copy(title = event.title, titleError = null) }

            is SaveDocumentEvent.DateSelected ->
                setState { copy(documentDate = event.date, showDatePicker = false) }

            SaveDocumentEvent.DatePickerRequested -> setState { copy(showDatePicker = true) }

            SaveDocumentEvent.DatePickerDismissed -> setState { copy(showDatePicker = false) }

            SaveDocumentEvent.SaveClicked -> save()

            SaveDocumentEvent.CancelClicked -> sendEffect(SaveDocumentEffect.Dismiss)
        }
    }

    private fun save() {
        // Guard against a double tap queueing two inserts of the same scan.
        if (currentState.isSaving) return
        setState { copy(isSaving = true) }

        viewModelScope.launch {
            val result =
                saveScannedDocument(
                    rawTitle = currentState.title,
                    documentDate = currentState.documentDate,
                    sourceUri = route.pdfUri,
                    pageCount = route.pageCount,
                )
            setState { copy(isSaving = false) }

            when (result) {
                is SaveResult.Saved -> sendEffect(SaveDocumentEffect.Saved(currentState.title.trim()))

                is SaveResult.InvalidTitle ->
                    setState { copy(titleError = result.reason.asUiText()) }

                // Surfaced, not swallowed: the old saveFile() printed the stack trace and dismissed
                // the sheet, so a failed write was indistinguishable from a successful one.
                is SaveResult.Failed ->
                    sendEffect(SaveDocumentEffect.ShowMessage(UiText.Res(R.string.scan_save_failed)))
            }
        }
    }
}

internal fun TitleValidation.asUiText(): UiText = when (this) {
    TitleValidation.Blank -> UiText.Res(R.string.scan_title_blank)
    is TitleValidation.TooLong -> UiText.Res(R.string.scan_title_too_long, listOf(max))
    is TitleValidation.Valid -> UiText.Literal("")
}
