package com.salpiras.citizendocs.core.testing

import com.salpiras.citizendocs.core.storage.DocumentFileStore

/**
 * Mirrors [com.salpiras.citizendocs.core.storage.LocalDocumentFileStore]'s naming rules
 * (including `_1` collision suffixes) without touching a filesystem, so repository tests
 * can assert that a rolled-back save leaves no file behind.
 */
class InMemoryDocumentFileStore : DocumentFileStore {
    val files = linkedMapOf<String, Long>()

    var persistFailure: Throwable? = null

    override suspend fun persist(sourceUri: String, desiredStem: String): String {
        persistFailure?.let { throw it }
        val name = uniqueName(desiredStem)
        files[name] = DEFAULT_SIZE
        return name
    }

    override suspend fun rename(fileName: String, desiredStem: String): String {
        val size = files.remove(fileName) ?: return fileName
        val name = uniqueName(desiredStem)
        files[name] = size
        return name
    }

    override suspend fun delete(fileName: String): Boolean = files.remove(fileName) != null

    override suspend fun sizeOf(fileName: String): Long = files[fileName] ?: 0L

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
        const val DEFAULT_SIZE = 1_024L
    }
}
