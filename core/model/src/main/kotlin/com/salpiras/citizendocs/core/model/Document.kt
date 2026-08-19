package com.salpiras.citizendocs.core.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

@JvmInline
value class DocumentId(val value: Long)

/**
 * A document the user has scanned and filed.
 *
 * [fileName] is relative to the document store root — never an absolute path — so a record
 * stays valid if the app's data directory moves. Resolve it through `DocumentFileStore`.
 */
data class Document(
    val id: DocumentId,
    val title: String,
    val documentDate: LocalDate,
    val createdAt: Instant,
    val fileName: String,
    val pageCount: Int,
    val sizeBytes: Long,
)
