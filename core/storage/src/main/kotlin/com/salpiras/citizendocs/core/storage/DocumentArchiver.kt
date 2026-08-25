package com.salpiras.citizendocs.core.storage

/**
 * One file in the archive.
 *
 * @param pathInZip where it lands inside the archive, e.g. "2026/01/Tax_return_2025.pdf".
 * @param fileName the stored file to read, relative to the document store root.
 */
data class ArchiveEntry(val pathInZip: String, val fileName: String)

interface DocumentArchiver {
    /**
     * Streams [entries] into a zip at [destinationUri], which must be writable — in practice a
     * URI handed back by the system file picker. Returns the number of bytes written.
     */
    suspend fun writeZip(destinationUri: String, entries: List<ArchiveEntry>): Long
}
