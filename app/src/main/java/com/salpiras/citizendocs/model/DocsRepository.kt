package com.salpiras.citizendocs.model

import com.salpiras.citizendocs.model.local.LocalDocsDataSource
import com.salpiras.citizendocs.model.local.db.EntityDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class DocsRepository @Inject constructor(
  private val localDocsDataSource: LocalDocsDataSource
) {

  fun getDocList() : Flow<List<Document>> = flow {
    localDocsDataSource.getAllDocs().collect {
      emit(it.toDocumentData())
    }
  }.flowOn(Dispatchers.IO)

  fun addDocument(doc: Document) = localDocsDataSource.addDocument(doc.toEntity())

  private fun List<EntityDocument>.toDocumentData() = map {
    it.toDocumentData()
  }

  private fun EntityDocument.toDocumentData() =
    Document(
      title = title,
      path = path,
      month = month,
      year = year,
      date = date,
    )

  private fun Document.toEntity() =
    EntityDocument(
      title = title,
      path = path,
      date = date,
      month = month,
      year = year
      )
}
