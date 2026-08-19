package com.salpiras.citizendocs.core.data

import com.salpiras.citizendocs.core.model.Document
import com.salpiras.citizendocs.core.model.DocumentId
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/** Everything needed to file a freshly scanned document. */
data class DocumentDraft(val title: String, val documentDate: LocalDate, val sourceUri: String, val pageCount: Int)

interface DocumentsRepository {
    fun observeDocuments(): Flow<List<Document>>

    fun observeDocument(id: DocumentId): Flow<Document?>

    suspend fun add(draft: DocumentDraft): DocumentId

    suspend fun rename(id: DocumentId, title: String)

    suspend fun delete(id: DocumentId)

    fun contentUri(document: Document): String
}
