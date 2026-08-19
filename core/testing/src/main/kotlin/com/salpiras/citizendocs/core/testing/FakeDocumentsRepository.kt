package com.salpiras.citizendocs.core.testing

import com.salpiras.citizendocs.core.data.DocumentDraft
import com.salpiras.citizendocs.core.data.DocumentsRepository
import com.salpiras.citizendocs.core.model.Document
import com.salpiras.citizendocs.core.model.DocumentId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.time.Instant

/**
 * In-memory stand-in for the real repository.
 *
 * A hand-written fake rather than a mock: it enforces the same invariants as the real thing
 * (ids increment, renames land, deletes remove), so a test that passes against it is
 * testing behaviour and not a recorded call sequence.
 */
class FakeDocumentsRepository : DocumentsRepository {
    private val documents = MutableStateFlow<List<Document>>(emptyList())

    /** Set to make the next [add] throw, for exercising failure paths. */
    var addFailure: Throwable? = null

    /** Set to make the next [rename] or [delete] throw. */
    var mutationFailure: Throwable? = null

    private var nextId = 1L

    fun seed(vararg docs: Document) {
        documents.value = docs.toList()
        nextId = (docs.maxOfOrNull { it.id.value } ?: 0L) + 1
    }

    fun current(): List<Document> = documents.value

    override fun observeDocuments(): Flow<List<Document>> = documents.asStateFlow()

    override fun observeDocument(id: DocumentId): Flow<Document?> =
        documents.map { docs -> docs.firstOrNull { it.id == id } }

    override suspend fun add(draft: DocumentDraft): DocumentId {
        addFailure?.let { throw it }
        val id = DocumentId(nextId++)
        documents.update { docs ->
            docs +
                Document(
                    id = id,
                    title = draft.title,
                    documentDate = draft.documentDate,
                    createdAt = Instant.fromEpochMilliseconds(0),
                    fileName = "${draft.title}.pdf",
                    pageCount = draft.pageCount,
                    sizeBytes = 1_024,
                )
        }
        return id
    }

    override suspend fun rename(id: DocumentId, title: String) {
        mutationFailure?.let { throw it }
        documents.update { docs ->
            docs.map { if (it.id == id) it.copy(title = title) else it }
        }
    }

    override suspend fun delete(id: DocumentId) {
        mutationFailure?.let { throw it }
        documents.update { docs -> docs.filterNot { it.id == id } }
    }

    override fun contentUri(document: Document): String = "content://test/${document.fileName}"
}
