package com.salpiras.citizendocs.core.drive

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * A file or folder in the user's Drive, reduced to the three things this app uses.
 *
 * Deliberately not Drive's own `File` model, which has upwards of fifty fields: everything
 * above this module should be unable to depend on a Drive concept that is not one of these.
 */
data class DriveFile(
    val id: String,
    /**
     * For a document, the user's real title — not the slugified on-disk name.
     *
     * The title cannot ride in [appProperties]: those allow 124 UTF-8 bytes for key *and*
     * value together, and an 80-character title can run to 320. Putting it in the file name
     * is also what makes the Drive folder worth opening.
     */
    val name: String,
    /**
     * App-private metadata, invisible in the Drive UI. Keys are single letters because the
     * byte budget is shared with the value.
     *
     * This is why there is no manifest file. Metadata that travels with the file it
     * describes cannot drift out of step with it, and under `drive.file` the app only ever
     * sees its own files — so listing them *is* the manifest.
     */
    val appProperties: Map<String, String> = emptyMap(),
)

/**
 * Bytes to upload, opened only when the request is actually sent.
 *
 * A lambda rather than an `InputStream` so a retry can open the file a second time, and so
 * a multi-megabyte PDF is never held in memory waiting for its turn.
 */
fun interface DriveContent {
    fun open(): InputStream
}

/**
 * The slice of Drive this app needs: find or make a folder, list it, move PDFs in and out.
 *
 * Every method can throw [DriveException] or [DriveUnauthorizedException], and nothing else
 * that callers are expected to catch.
 */
interface DriveClient {
    /** The folder with this name under [parentId], or null. Trashed folders do not count. */
    suspend fun findFolder(name: String, parentId: String? = null): DriveFile?

    suspend fun createFolder(name: String, parentId: String? = null): DriveFile

    /** Finds the folder or makes it. The common case, and the reason [findFolder] exists. */
    suspend fun ensureFolder(name: String, parentId: String? = null): DriveFile =
        findFolder(name, parentId) ?: createFolder(name, parentId)

    /** Non-folder children of [parentId], with their [DriveFile.appProperties] populated. */
    suspend fun listFiles(parentId: String): List<DriveFile>

    /**
     * Every non-folder file this app has ever created, wherever it now sits.
     *
     * Under `drive.file` that set is exactly this app's documents and nothing else, which is
     * what makes a restore able to skip walking the year/month tree. It also means a restore
     * still finds everything after the user has reorganised their Drive, which they are
     * entitled to do and which a folder-walk would silently fail at.
     */
    suspend fun listAllFiles(): List<DriveFile>

    suspend fun upload(
        name: String,
        parentId: String,
        appProperties: Map<String, String>,
        content: DriveContent,
    ): DriveFile

    /** Replaces an existing file's name, metadata and bytes, keeping its id. */
    suspend fun update(
        fileId: String,
        name: String,
        appProperties: Map<String, String>,
        content: DriveContent,
    ): DriveFile

    /** Streams [fileId] into [destination]. The caller owns and closes the stream. */
    suspend fun download(fileId: String, destination: OutputStream)
}

/**
 * Drive said no, or the network did.
 *
 * [statusCode] is null when the request never got an answer. It is carried because callers
 * genuinely need to tell one failure from another: a 404 on an update means the user deleted
 * the file in Drive and the right response is to upload it afresh, whereas treating that as a
 * generic error would break every backup from then on.
 */
class DriveException(message: String, val statusCode: Int? = null, cause: Throwable? = null) :
    IOException(message, cause)
