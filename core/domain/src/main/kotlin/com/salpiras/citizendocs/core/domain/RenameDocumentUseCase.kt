package com.salpiras.citizendocs.core.domain

import com.salpiras.citizendocs.core.data.DocumentsRepository
import com.salpiras.citizendocs.core.model.DocumentId
import com.salpiras.citizendocs.core.model.DocumentTitle
import com.salpiras.citizendocs.core.model.TitleValidation
import javax.inject.Inject

sealed interface RenameResult {
    data object Renamed : RenameResult

    data class InvalidTitle(val reason: TitleValidation) : RenameResult

    data class Failed(val cause: Throwable) : RenameResult
}

class RenameDocumentUseCase
@Inject
constructor(private val repository: DocumentsRepository) {
    suspend operator fun invoke(id: DocumentId, rawTitle: String): RenameResult =
        when (val validation = DocumentTitle.validate(rawTitle)) {
            TitleValidation.Blank -> RenameResult.InvalidTitle(TitleValidation.Blank)

            is TitleValidation.TooLong -> RenameResult.InvalidTitle(validation)

            is TitleValidation.Valid ->
                runCatching { repository.rename(id, validation.title) }
                    .fold(
                        onSuccess = { RenameResult.Renamed },
                        onFailure = RenameResult::Failed,
                    )
        }
}
