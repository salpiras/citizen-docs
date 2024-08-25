package com.salpiras.citizendocs.model

import com.salpiras.citizendocs.di.DispatcherIO
import com.salpiras.citizendocs.model.local.LocalDocsDataSource
import com.salpiras.citizendocs.model.local.db.EntityDocument
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

interface DocsRepository {
  fun getDocList() : Flow<List<Document>>
  fun addDocument(doc: Document)
}

class DocsRepositoryImpl @Inject constructor(
  private val localDocsDataSource: LocalDocsDataSource,
  @DispatcherIO private val dispatcher: CoroutineDispatcher
) : DocsRepository {

  override fun getDocList() : Flow<List<Document>> = flow {
    localDocsDataSource.getAllDocs().collect {
      emit(it.toDocumentData())
    }
  }.flowOn(dispatcher)

  override fun addDocument(doc: Document) = localDocsDataSource.addDocument(doc.toEntity())

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
