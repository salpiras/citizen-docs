package com.salpiras.citizendocs.core.domain

import com.salpiras.citizendocs.core.data.DocumentsRepository
import com.salpiras.citizendocs.core.model.Document
import javax.inject.Inject

sealed interface ExportResult {
    data class Exported(val documentCount: Int, val bytesWritten: Long) : ExportResult

    data object NothingToExport : ExportResult

    data class Failed(val cause: Throwable) : ExportResult
}

class ExportDocumentsUseCase @Inject constructor(private val repository: DocumentsRepository) {
    suspend operator fun invoke(documents: List<Document>, destinationUri: String): ExportResult = when {
        // An empty archive would look like a successful backup of nothing, which is the
        // worst possible outcome for a feature whose whole job is not losing documents.
        documents.isEmpty() -> ExportResult.NothingToExport

        else -> runCatching { repository.export(documents, destinationUri) }
            .fold(
                onSuccess = { ExportResult.Exported(documentCount = documents.size, bytesWritten = it) },
                onFailure = ExportResult::Failed,
            )
    }
}
