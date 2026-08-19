package com.salpiras.citizendocs.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY document_date DESC, id DESC")
    fun observeAll(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id")
    fun observeById(id: Long): Flow<DocumentEntity?>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun findById(id: Long): DocumentEntity?

    // ABORT, not IGNORE: a failed insert must surface so the repository can roll the file back.
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: DocumentEntity): Long

    @Query("UPDATE documents SET title = :title WHERE id = :id")
    suspend fun updateTitle(id: Long, title: String): Int

    @Query("UPDATE documents SET file_name = :fileName WHERE id = :id")
    suspend fun updateFileName(id: Long, fileName: String): Int

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
