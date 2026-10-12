package com.salpiras.citizendocs.core.storage

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Owns the PDF files on disk. Callers work in terms of *relative* file names; the store is
 * the only thing that knows the absolute root, which is what keeps absolute paths out of
 * the database.
 */
interface DocumentFileStore {
    /** Copies [sourceUri] into the store under a unique name derived from [desiredStem]. */
    suspend fun persist(sourceUri: String, desiredStem: String): String

    /**
     * Makes a file under a unique name derived from [desiredStem] and lets [write] fill it.
     *
     * For bytes that arrive from somewhere with no URI — a Drive download, say. Writing to a
     * temp file and handing back a `file://` URI would work too, but it would mean a second
     * copy of every document on disk and a temp file to remember to delete.
     *
     * Same guarantee as [persist]: if [write] throws, the partial file is removed rather
     * than left behind looking like a document.
     */
    suspend fun persistStream(desiredStem: String, write: suspend (OutputStream) -> Unit): String

    /**
     * Opens a stored file for reading.
     *
     * The caller closes it. Returned as a stream rather than bytes because an upload should
     * not have to hold a whole PDF in memory to send it.
     */
    fun openStream(fileName: String): InputStream

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
