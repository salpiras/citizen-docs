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
    /** This device's row id. Not meaningful anywhere else — see [uuid]. */
    val id: DocumentId,
    /** Stable across devices. See [newDocumentUuid]. */
    val uuid: String,
    val title: String,
    val documentDate: LocalDate,
    val createdAt: Instant,
    /**
     * When the user last changed something about this document — currently only a rename.
     *
     * Distinct from [createdAt], which never moves. This is what lets a future backup upload
     * only what has actually changed instead of re-sending the whole library.
     */
    val updatedAt: Instant,
    val fileName: String,
    val pageCount: Int,
    val sizeBytes: Long,
)
