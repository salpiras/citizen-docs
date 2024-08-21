package com.salpiras.citizendocs.model.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.salpiras.citizendocs.model.local.db.EntityDocument
import kotlinx.coroutines.flow.Flow

@Dao
interface DocsDao {

  @Query("SELECT * FROM docs")
  fun getAllDocs() : Flow<List<EntityDocument>>

  @Insert
  fun addDocument(vararg document: EntityDocument)

}