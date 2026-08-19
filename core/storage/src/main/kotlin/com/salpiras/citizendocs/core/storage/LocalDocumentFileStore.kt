package com.salpiras.citizendocs.core.storage

import android.content.Context
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.salpiras.citizendocs.core.common.CitizenDispatcher
import com.salpiras.citizendocs.core.common.Dispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LocalDocumentFileStore
@Inject
constructor(
    @param:ApplicationContext private val context: Context,
    @param:Dispatcher(CitizenDispatcher.IO) private val io: CoroutineDispatcher,
) : DocumentFileStore {
    // mkdirs() on first access — the previous DocumentService never created the directory,
    // so the very first save of each year failed with NoSuchFileException.
    private val root: File by lazy { File(context.filesDir, DOCUMENTS_DIR).apply { mkdirs() } }

    override suspend fun persist(sourceUri: String, desiredStem: String): String = withContext(io) {
        val target = root.uniqueFile(desiredStem)
        try {
            context.contentResolver
                .openInputStream(sourceUri.toUri())
                ?.use { input -> target.outputStream().use(input::copyTo) }
                ?: throw DocumentStoreException.SourceUnreadable(sourceUri)
        } catch (e: IOException) {
            // Don't leave a truncated PDF behind if the copy dies part-way through.
            target.delete()
            if (e is DocumentStoreException) throw e
            throw DocumentStoreException.WriteFailed(target.name, e)
        }
        target.name
    }

    override suspend fun rename(fileName: String, desiredStem: String): String = withContext(io) {
        val current = File(root, fileName)
        if (!current.exists()) return@withContext fileName

        val target = root.uniqueFile(desiredStem)
        if (target.name == fileName) return@withContext fileName
        if (current.renameTo(target)) target.name else fileName
    }

    override suspend fun delete(fileName: String): Boolean = withContext(io) {
        File(root, fileName).delete()
    }

    override suspend fun sizeOf(fileName: String): Long = withContext(io) {
        File(root, fileName).length()
    }

    override fun contentUri(fileName: String): String = FileProvider
        .getUriForFile(context, "${context.packageName}$AUTHORITY_SUFFIX", File(root, fileName))
        .toString()

    /**
     * Appends `_1`, `_2`, … until the name is free. Two documents may legitimately share a
     * title; the old implementation used `copyTo` without `overwrite` and threw
     * FileAlreadyExistsException the second time.
     */
    private fun File.uniqueFile(stem: String): File {
        var candidate = File(this, "$stem.$EXTENSION")
        var counter = 1
        while (candidate.exists()) {
            candidate = File(this, "${stem}_${counter++}.$EXTENSION")
        }
        return candidate
    }

    private companion object {
        const val DOCUMENTS_DIR = "documents"
        const val EXTENSION = "pdf"
        const val AUTHORITY_SUFFIX = ".fileprovider"
    }
}
