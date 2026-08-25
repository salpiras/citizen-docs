package com.salpiras.citizendocs.core.testing

import com.salpiras.citizendocs.core.storage.ArchiveEntry
import com.salpiras.citizendocs.core.storage.DocumentArchiver

/**
 * Captures what it was asked to archive, so repository and use-case tests can assert on the
 * *structure* of the zip — which paths, in which order — without touching a filesystem.
 * The real zip round-trip is covered by ZipDocumentArchiverTest.
 */
class RecordingDocumentArchiver : DocumentArchiver {
    var destinationUri: String? = null
        private set

    var entries: List<ArchiveEntry> = emptyList()
        private set

    var failure: Throwable? = null

    var bytesPerEntry: Long = 1_024

    override suspend fun writeZip(destinationUri: String, entries: List<ArchiveEntry>): Long {
        failure?.let { throw it }
        this.destinationUri = destinationUri
        this.entries = entries
        return entries.size * bytesPerEntry
    }
}
