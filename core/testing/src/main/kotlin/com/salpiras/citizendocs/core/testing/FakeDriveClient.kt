package com.salpiras.citizendocs.core.testing

import com.salpiras.citizendocs.core.drive.DriveClient
import com.salpiras.citizendocs.core.drive.DriveContent
import com.salpiras.citizendocs.core.drive.DriveException
import com.salpiras.citizendocs.core.drive.DriveFile
import java.io.OutputStream

/**
 * A Drive that lives in a map.
 *
 * Enforces the behaviours the backup code actually leans on: ids are unique and stable,
 * a folder created twice is the same folder, an update to a file that is not there fails
 * with a 404, and `listAllFiles` returns files wherever they were put — which is what lets
 * a restore test prove it does not depend on the folder tree.
 */
class FakeDriveClient : DriveClient {
    data class Stored(
        val file: DriveFile,
        val parentId: String?,
        val isFolder: Boolean,
        val content: ByteArray = ByteArray(0),
    )

    val stored = linkedMapOf<String, Stored>()

    /** Set to make the next call of any kind fail. */
    var failure: Throwable? = null

    /** Counts uploads and updates separately, so a test can prove nothing was re-sent. */
    var uploads = 0
        private set
    var updates = 0
        private set

    private var nextId = 1

    private fun newId() = "drive-${nextId++}"

    private fun check() = failure?.let { throw it }

    override suspend fun findFolder(name: String, parentId: String?): DriveFile? {
        check()
        return stored.values
            .firstOrNull { it.isFolder && it.file.name == name && it.parentId == parentId }
            ?.file
    }

    override suspend fun createFolder(name: String, parentId: String?): DriveFile {
        check()
        val file = DriveFile(id = newId(), name = name)
        stored[file.id] = Stored(file = file, parentId = parentId, isFolder = true)
        return file
    }

    override suspend fun listFiles(parentId: String): List<DriveFile> {
        check()
        return stored.values.filter { !it.isFolder && it.parentId == parentId }.map { it.file }
    }

    override suspend fun listAllFiles(): List<DriveFile> {
        check()
        return stored.values.filter { !it.isFolder }.map { it.file }
    }

    override suspend fun upload(
        name: String,
        parentId: String,
        appProperties: Map<String, String>,
        content: DriveContent,
    ): DriveFile {
        check()
        uploads++
        val file = DriveFile(id = newId(), name = name, appProperties = appProperties)
        stored[file.id] = Stored(
            file = file,
            parentId = parentId,
            isFolder = false,
            content = content.open().use { it.readBytes() },
        )
        return file
    }

    override suspend fun update(
        fileId: String,
        name: String,
        appProperties: Map<String, String>,
        content: DriveContent,
    ): DriveFile {
        check()
        val existing = stored[fileId]
            ?: throw DriveException("Drive returned 404: no such file", statusCode = 404)

        updates++
        val file = existing.file.copy(name = name, appProperties = appProperties)
        stored[fileId] = existing.copy(file = file, content = content.open().use { it.readBytes() })
        return file
    }

    override suspend fun download(fileId: String, destination: OutputStream) {
        check()
        val existing = stored[fileId]
            ?: throw DriveException("Drive returned 404: no such file", statusCode = 404)
        destination.write(existing.content)
    }

    /** Puts a document in Drive as though another device had backed it up. */
    fun seedDocument(
        name: String,
        appProperties: Map<String, String>,
        content: String = "%PDF-1.4 from another device",
        parentId: String = "someone-elses-folder",
    ): DriveFile {
        val file = DriveFile(id = newId(), name = name, appProperties = appProperties)
        stored[file.id] = Stored(
            file = file,
            parentId = parentId,
            isFolder = false,
            content = content.toByteArray(),
        )
        return file
    }
}
