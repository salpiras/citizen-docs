package com.salpiras.citizendocs

import com.salpiras.citizendocs.model.DocsRepository
import com.salpiras.citizendocs.model.Document
import com.salpiras.citizendocs.model.local.LocalDocsDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import org.junit.Test

import org.junit.Assert.*

class FakeEmptyDocsRepository(dataSource: LocalDocsDataSource, dispatcher: CoroutineDispatcher)
  : DocsRepository {
  override fun getDocList(): Flow<List<Document>> {

  }

  override fun addDocument(doc: Document) {

  }
}

class GetAllDocsUseCaseTest {
  @Test
  fun `Empty repository`() {

    assertEquals(4, 2 + 2)
  }
}