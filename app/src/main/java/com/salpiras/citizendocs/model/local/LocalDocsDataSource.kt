package com.salpiras.citizendocs.model.local

import com.salpiras.citizendocs.model.local.db.EntityDocument
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LocalDocsDataSource @Inject constructor(private val docsDao: DocsDao) {
  fun getAllDocs() : Flow<List<EntityDocument>> {
    return docsDao.getAllDocs()
  }

  fun addDocument(doc : EntityDocument) = docsDao.addDocument(doc)

}
