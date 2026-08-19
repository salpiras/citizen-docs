package com.salpiras.citizendocs.core.data

import com.salpiras.citizendocs.core.common.CitizenDispatcher
import com.salpiras.citizendocs.core.common.Dispatcher
import com.salpiras.citizendocs.core.database.DocumentDao
import com.salpiras.citizendocs.core.database.DocumentEntity
import com.salpiras.citizendocs.core.database.asExternalModel
import com.salpiras.citizendocs.core.model.Document
import com.salpiras.citizendocs.core.model.DocumentId
import com.salpiras.citizendocs.core.model.slugify
import com.salpiras.citizendocs.core.storage.DocumentFileStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Clock

/**
 * Coordinates the two stores that together make a document: the row and the file.
 *
 * The interesting part is that these can disagree. Writing the file first and the row second
 * means a failed insert would otherwise orphan a PDF forever, so [add] rolls the file back.
 */
internal class OfflineFirstDocumentsRepository
@Inject
constructor(
    private val dao: DocumentDao,
    private val fileStore: DocumentFileStore,
    private val clock: Clock,
    @param:Dispatcher(CitizenDispatcher.IO) private val io: CoroutineDispatcher,
) : DocumentsRepository {
    // No flowOn here: Room already runs its queries on its own executor and emits off the
    // main thread. The old repository wrapped this in flow { collect { emit } } as well,
    // which added an operator that did nothing.
    override fun observeDocuments(): Flow<List<Document>> =
        dao.observeAll().map { entities -> entities.map(DocumentEntity::asExternalModel) }

    override fun observeDocument(id: DocumentId): Flow<Document?> =
        dao.observeById(id.value).map { it?.asExternalModel() }

    override suspend fun add(draft: DocumentDraft): DocumentId = withContext(io) {
        val fileName = fileStore.persist(draft.sourceUri, slugify(draft.title))
        try {
            val rowId =
                dao.insert(
                    DocumentEntity(
                        title = draft.title,
                        documentDate = draft.documentDate,
                        createdAt = clock.now(),
                        fileName = fileName,
                        pageCount = draft.pageCount,
                        sizeBytes = fileStore.sizeOf(fileName),
                    ),
                )
            DocumentId(rowId)
        } catch (e: Throwable) {
            fileStore.delete(fileName)
            throw e
        }
    }

    override suspend fun rename(id: DocumentId, title: String): Unit = withContext(io) {
        val entity = dao.findById(id.value) ?: return@withContext
        dao.updateTitle(id.value, title)
        // Keep the file name in step with the title so the on-disk store stays browsable.
        val newFileName = fileStore.rename(entity.fileName, slugify(title))
        if (newFileName != entity.fileName) dao.updateFileName(id.value, newFileName)
    }

    override suspend fun delete(id: DocumentId): Unit = withContext(io) {
        val entity = dao.findById(id.value) ?: return@withContext
        dao.deleteById(id.value)
        fileStore.delete(entity.fileName)
    }

    override fun contentUri(document: Document): String = fileStore.contentUri(document.fileName)
}
