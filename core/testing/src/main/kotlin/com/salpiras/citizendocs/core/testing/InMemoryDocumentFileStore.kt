package com.salpiras.citizendocs.core.testing

import com.salpiras.citizendocs.core.storage.DocumentFileStore
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Mirrors [com.salpiras.citizendocs.core.storage.LocalDocumentFileStore]'s naming rules
 * (including `_1` collision suffixes) without touching a filesystem, so repository tests
 * can assert that a rolled-back save leaves no file behind.
 *
 * Holds real bytes rather than just sizes. Backup reads files back out and restore writes
 * them in, so a store that only remembered how big things were could not tell a test
 * whether the right document went up or came down.
 */
class InMemoryDocumentFileStore : DocumentFileStore {
    val files = linkedMapOf<String, ByteArray>()

    var persistFailure: Throwable? = null

    override suspend fun persist(sourceUri: String, desiredStem: String): String {
        persistFailure?.let { throw it }
        val name = uniqueName(desiredStem)
        files[name] = DEFAULT_CONTENT.toByteArray()
        return name
    }

    override suspend fun persistStream(desiredStem: String, write: suspend (OutputStream) -> Unit): String {
        persistFailure?.let { throw it }
        val name = uniqueName(desiredStem)
        val buffer = ByteArrayOutputStream()
        try {
            write(buffer)
        } catch (e: Throwable) {
            // The real store deletes the half-written file; here there is nothing to delete
            // yet, which is the same outcome — no entry appears in `files`.
            throw e
        }
        files[name] = buffer.toByteArray()
        return name
    }

    override fun openStream(fileName: String): InputStream =
        files[fileName]?.inputStream() ?: error("No such file: $fileName")

    override suspend fun rename(fileName: String, desiredStem: String): String {
        val content = files.remove(fileName) ?: return fileName
        val name = uniqueName(desiredStem)
        files[name] = content
        return name
    }

    override suspend fun delete(fileName: String): Boolean = files.remove(fileName) != null

    override suspend fun sizeOf(fileName: String): Long = files[fileName]?.size?.toLong() ?: 0L

    override fun contentUri(fileName: String): String = "content://test/$fileName"

    private fun uniqueName(stem: String): String {
        var candidate = "$stem.pdf"
        var counter = 1
        while (files.containsKey(candidate)) {
            candidate = "${stem}_${counter++}.pdf"
        }
        return candidate
    }

    private companion object {
        const val DEFAULT_CONTENT = "%PDF-1.4 scanned"
    }
}
