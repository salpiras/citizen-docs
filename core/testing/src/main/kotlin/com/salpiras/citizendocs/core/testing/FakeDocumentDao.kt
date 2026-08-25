package com.salpiras.citizendocs.core.testing

import com.salpiras.citizendocs.core.database.DocumentDao
import com.salpiras.citizendocs.core.database.DocumentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * A DAO that keeps rows in memory but preserves the two behaviours the repository depends
 * on: the sort order of [observeAll], and the ability to fail an insert.
 */
class FakeDocumentDao : DocumentDao {
    private val rows = MutableStateFlow<List<DocumentEntity>>(emptyList())

    /** Set to simulate a constraint violation on the next insert. */
    var insertFailure: Throwable? = null

    private var nextId = 1L

    override fun observeAll(): Flow<List<DocumentEntity>> = observeMatching("")

    override fun observeMatching(query: String): Flow<List<DocumentEntity>> = rows.map { list ->
        list
            .filter { query.isEmpty() || it.title.contains(query, ignoreCase = true) }
            .sortedWith(compareByDescending<DocumentEntity> { it.documentDate }.thenByDescending { it.id })
    }

    override fun observeById(id: Long): Flow<DocumentEntity?> = rows.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun findById(id: Long): DocumentEntity? = rows.value.firstOrNull { it.id == id }

    override suspend fun insert(entity: DocumentEntity): Long {
        insertFailure?.let { throw it }
        val id = nextId++
        rows.update { it + entity.copy(id = id) }
        return id
    }

    override suspend fun updateTitle(id: Long, title: String): Int = update(id) { it.copy(title = title) }

    override suspend fun updateFileName(id: Long, fileName: String): Int = update(id) { it.copy(fileName = fileName) }

    override suspend fun deleteById(id: Long): Int {
        val existed = rows.value.any { it.id == id }
        rows.update { list -> list.filterNot { it.id == id } }
        return if (existed) 1 else 0
    }

    private fun update(id: Long, transform: (DocumentEntity) -> DocumentEntity): Int {
        var affected = 0
        rows.update { list ->
            list.map {
                if (it.id == id) {
                    affected = 1
                    transform(it)
                } else {
                    it
                }
            }
        }
        return affected
    }
}
