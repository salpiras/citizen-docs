package com.salpiras.citizendocs.core.domain

import com.salpiras.citizendocs.core.data.DocumentsRepository
import com.salpiras.citizendocs.core.model.DocumentId
import javax.inject.Inject

sealed interface DeleteResult {
    data object Deleted : DeleteResult

    data class Failed(val cause: Throwable) : DeleteResult
}

class DeleteDocumentUseCase
@Inject
constructor(private val repository: DocumentsRepository) {
    suspend operator fun invoke(id: DocumentId): DeleteResult = runCatching { repository.delete(id) }
        .fold(
            onSuccess = { DeleteResult.Deleted },
            onFailure = DeleteResult::Failed,
        )
}
