package com.salpiras.citizendocs.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.salpiras.citizendocs.core.model.Document
import com.salpiras.citizendocs.core.model.DocumentId
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

@Entity(
    tableName = "documents",
    indices = [
        // The store guarantees unique names; the index makes the database enforce it too.
        Index(value = ["file_name"], unique = true),
        Index(value = ["document_date"]),
    ],
)
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    @ColumnInfo(name = "document_date") val documentDate: LocalDate,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "page_count") val pageCount: Int,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
)

fun DocumentEntity.asExternalModel() = Document(
    id = DocumentId(id),
    title = title,
    documentDate = documentDate,
    createdAt = createdAt,
    fileName = fileName,
    pageCount = pageCount,
    sizeBytes = sizeBytes,
)
