package com.salpiras.citizendocs.core.storage

import android.content.Context
import android.provider.DocumentsContract
import androidx.core.net.toUri
import com.salpiras.citizendocs.core.common.CitizenDispatcher
import com.salpiras.citizendocs.core.common.Dispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ZipDocumentArchiver @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:Dispatcher(CitizenDispatcher.IO) private val io: CoroutineDispatcher,
) : DocumentArchiver {

    private val root: File by lazy { File(context.filesDir, DOCUMENTS_DIR) }

    override suspend fun writeZip(destinationUri: String, entries: List<ArchiveEntry>): Long = withContext(io) {
        val uri = destinationUri.toUri()
        var bytesWritten = 0L
        try {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                ZipOutputStream(output.buffered()).use { zip ->
                    entries.forEach { entry ->
                        val source = File(root, entry.fileName)
                        if (!source.exists()) return@forEach

                        zip.putNextEntry(ZipEntry(entry.pathInZip))
                        source.inputStream().use { input -> bytesWritten += input.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            } ?: throw DocumentStoreException.SourceUnreadable(destinationUri)
        } catch (e: IOException) {
            // A truncated archive is worse than none: it looks like a valid backup. The
            // picker has already created the file, so remove it before surfacing the error.
            deleteQuietly(destinationUri)
            if (e is DocumentStoreException) throw e
            throw DocumentStoreException.WriteFailed(destinationUri, e)
        }
        bytesWritten
    }

    private fun deleteQuietly(destinationUri: String) {
        // Best effort: the provider may not permit deletion, and the original failure is the
        // one worth reporting either way.
        runCatching {
            DocumentsContract.deleteDocument(context.contentResolver, destinationUri.toUri())
        }
    }
}
