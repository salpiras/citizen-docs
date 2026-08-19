package com.salpiras.citizendocs.core.domain

import com.salpiras.citizendocs.core.data.DocumentDraft
import com.salpiras.citizendocs.core.data.DocumentsRepository
import com.salpiras.citizendocs.core.model.DocumentId
import com.salpiras.citizendocs.core.model.DocumentTitle
import com.salpiras.citizendocs.core.model.TitleValidation
import kotlinx.datetime.LocalDate
import javax.inject.Inject

sealed interface SaveResult {
    data class Saved(val id: DocumentId) : SaveResult

    data class InvalidTitle(val reason: TitleValidation) : SaveResult

    data class Failed(val cause: Throwable) : SaveResult
}

/**
 * Validate, then persist. Every outcome is a value the caller must handle — the old
 * DocsScannerViewModel caught Exception, called printStackTrace(), and then dismissed the
 * sheet, so a failed save looked exactly like a successful one.
 */
class SaveScannedDocumentUseCase
@Inject
constructor(private val repository: DocumentsRepository) {
    suspend operator fun invoke(
        rawTitle: String,
        documentDate: LocalDate,
        sourceUri: String,
        pageCount: Int,
    ): SaveResult = when (val validation = DocumentTitle.validate(rawTitle)) {
        TitleValidation.Blank -> SaveResult.InvalidTitle(TitleValidation.Blank)

        is TitleValidation.TooLong -> SaveResult.InvalidTitle(validation)

        is TitleValidation.Valid ->
            runCatching {
                repository.add(
                    DocumentDraft(
                        title = validation.title,
                        documentDate = documentDate,
                        sourceUri = sourceUri,
                        pageCount = pageCount,
                    ),
                )
            }.fold(
                onSuccess = SaveResult::Saved,
                onFailure = SaveResult::Failed,
            )
    }
}
