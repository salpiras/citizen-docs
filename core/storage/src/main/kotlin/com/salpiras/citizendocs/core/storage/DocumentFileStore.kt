package com.salpiras.citizendocs.core.storage

import java.io.IOException

/**
 * Owns the PDF files on disk. Callers work in terms of *relative* file names; the store is
 * the only thing that knows the absolute root, which is what keeps absolute paths out of
 * the database.
 */
interface DocumentFileStore {
    /** Copies [sourceUri] into the store under a unique name derived from [desiredStem]. */
    suspend fun persist(sourceUri: String, desiredStem: String): String

    /** Renames [fileName] to a unique name derived from [desiredStem]; returns the new name. */
    suspend fun rename(fileName: String, desiredStem: String): String

    suspend fun delete(fileName: String): Boolean

    suspend fun sizeOf(fileName: String): Long

    /** A `content://` URI another app may read, granted per-Intent. */
    fun contentUri(fileName: String): String
}

sealed class DocumentStoreException(message: String, cause: Throwable? = null) : IOException(message, cause) {
    class SourceUnreadable(uri: String) : DocumentStoreException("Could not open the scanned document at $uri")

    class WriteFailed(fileName: String, cause: Throwable) :
        DocumentStoreException("Could not write $fileName into the document store", cause)
}
