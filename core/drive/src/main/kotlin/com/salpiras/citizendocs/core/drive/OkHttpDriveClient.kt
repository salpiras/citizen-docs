package com.salpiras.citizendocs.core.drive

import com.salpiras.citizendocs.core.common.CitizenDispatcher
import com.salpiras.citizendocs.core.common.Dispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.BufferedSink
import okio.source
import java.io.IOException
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [DriveClient] over the Drive REST v3 endpoints, five of them.
 *
 * Calls are synchronous OkHttp on an injected IO dispatcher rather than enqueued: a backup
 * is already a sequential walk over documents, so callbacks would buy nothing and cost the
 * ability to read the code top to bottom.
 */
@Singleton
internal class OkHttpDriveClient
@Inject
constructor(
    private val callFactory: okhttp3.Call.Factory,
    @param:DriveBaseUrl private val baseUrl: HttpUrl,
    @param:Dispatcher(CitizenDispatcher.IO) private val io: CoroutineDispatcher,
) : DriveClient {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun findFolder(name: String, parentId: String?): DriveFile? = withContext(io) {
        val query = buildString {
            append("name = '").append(name.escapedForQuery()).append("'")
            append(" and mimeType = '").append(FOLDER_MIME_TYPE).append("'")
            append(" and trashed = false")
            append(" and '").append(parentId ?: ROOT).append("' in parents")
        }
        listPages(query).firstOrNull()
    }

    override suspend fun createFolder(name: String, parentId: String?): DriveFile = withContext(io) {
        val metadata = FileMetadata(
            name = name,
            mimeType = FOLDER_MIME_TYPE,
            parents = listOf(parentId ?: ROOT),
        )
        val request = Request
            .Builder()
            .url(filesUrl().addFields().build())
            .post(json.encodeToString(metadata).toRequestBody(JSON_MEDIA_TYPE))
            .build()

        request.readFile()
    }

    override suspend fun listFiles(parentId: String): List<DriveFile> = withContext(io) {
        listPages(
            "'${parentId.escapedForQuery()}' in parents" +
                " and trashed = false" +
                " and mimeType != '$FOLDER_MIME_TYPE'",
        )
    }

    override suspend fun listAllFiles(): List<DriveFile> = withContext(io) {
        // No parent clause at all. The scope is the filter: `drive.file` only ever returns
        // files this app created, so this is the whole library and nothing of the user's own.
        listPages("trashed = false and mimeType != '$FOLDER_MIME_TYPE'")
    }

    override suspend fun upload(
        name: String,
        parentId: String,
        appProperties: Map<String, String>,
        content: DriveContent,
    ): DriveFile = withContext(io) {
        val metadata = FileMetadata(name = name, parents = listOf(parentId), appProperties = appProperties)
        val request = Request
            .Builder()
            .url(uploadUrl().addFields().build())
            .post(multipartBody(metadata, content))
            .build()

        request.readFile()
    }

    override suspend fun update(
        fileId: String,
        name: String,
        appProperties: Map<String, String>,
        content: DriveContent,
    ): DriveFile = withContext(io) {
        // No `parents` on an update: Drive rejects the field here, and the file is not
        // moving anyway — only its name, metadata and bytes change.
        val metadata = FileMetadata(name = name, appProperties = appProperties)
        val request = Request
            .Builder()
            .url(uploadUrl().addPathSegment(fileId).addFields().build())
            .patch(multipartBody(metadata, content))
            .build()

        request.readFile()
    }

    override suspend fun download(fileId: String, destination: OutputStream): Unit = withContext(io) {
        val request = Request
            .Builder()
            .url(filesUrl().addPathSegment(fileId).addQueryParameter("alt", "media").build())
            .get()
            .build()

        request.execute().use { response ->
            response.requireSuccess()
            // Streamed rather than read into memory: the caller is writing this straight to
            // disk, and a document library has no particular upper bound.
            response.body.byteStream().use { it.copyTo(destination) }
        }
    }

    /**
     * Walks every page of a query.
     *
     * Drive pages results and will not tell you it has done so except through
     * `nextPageToken`. Reading only the first page would silently cap a restore at Drive's
     * default page size — the failure mode being a restore that looks like it worked and
     * quietly leaves documents behind.
     */
    private fun listPages(query: String): List<DriveFile> {
        val files = mutableListOf<DriveFile>()
        var pageToken: String? = null

        do {
            val url = filesUrl()
                .addQueryParameter("q", query)
                .addQueryParameter("spaces", "drive")
                .addQueryParameter("pageSize", PAGE_SIZE.toString())
                .addQueryParameter("fields", "nextPageToken,files($FILE_FIELDS)")
                .apply { pageToken?.let { addQueryParameter("pageToken", it) } }
                .build()

            val page = Request.Builder().url(url).get().build().execute().use { response ->
                response.requireSuccess()
                json.decodeFromString<FileList>(response.body.string())
            }

            files += page.files.map(FileResource::asDriveFile)
            pageToken = page.nextPageToken
        } while (pageToken != null)

        return files
    }

    private fun multipartBody(metadata: FileMetadata, content: DriveContent): MultipartBody = MultipartBody
        .Builder()
        // `related`, not the `form-data` Retrofit would give you: Drive's upload is an
        // ordered pair of metadata-then-bytes, and the parts carry no names.
        .setType(RELATED_MEDIA_TYPE)
        .addPart(json.encodeToString(metadata).toRequestBody(JSON_MEDIA_TYPE))
        .addPart(content.asRequestBody())
        .build()

    private fun filesUrl() = baseUrl.newBuilder().addPathSegments("drive/v3/files")

    private fun uploadUrl() = baseUrl
        .newBuilder()
        .addPathSegments("upload/drive/v3/files")
        .addQueryParameter("uploadType", "multipart")

    private fun HttpUrl.Builder.addFields() = addQueryParameter("fields", FILE_FIELDS)

    private fun Request.readFile(): DriveFile = execute().use { response ->
        response.requireSuccess()
        json.decodeFromString<FileResource>(response.body.string()).asDriveFile()
    }

    private fun Request.execute(): Response = try {
        callFactory.newCall(this).execute()
    } catch (e: DriveUnauthorizedException) {
        // Thrown by the auth interceptor. It is not a transport failure and must not be
        // flattened into one, or the UI would offer "retry" where it should offer "connect".
        throw e
    } catch (e: IOException) {
        throw DriveException("Could not reach Drive", cause = e)
    }

    private fun Response.requireSuccess() {
        if (isSuccessful) return

        // Read a little of the body: Drive explains itself in there, and a bare status code
        // turns every support question into a guess.
        val detail = runCatching { body.string().take(ERROR_BODY_CHARS) }.getOrDefault("")
        close()
        throw DriveException(
            message = "Drive returned $code${if (detail.isBlank()) "" else ": $detail"}",
            statusCode = code,
        )
    }

    private companion object {
        const val FOLDER_MIME_TYPE = "application/vnd.google-apps.folder"
        const val ROOT = "root"
        const val FILE_FIELDS = "id,name,appProperties"
        const val PAGE_SIZE = 100
        const val ERROR_BODY_CHARS = 500
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        val RELATED_MEDIA_TYPE = "multipart/related".toMediaType()
    }
}

/**
 * Drive's query language delimits strings with single quotes and escapes them with a
 * backslash. Folder names here are a product name and zero-padded numbers, so this will
 * likely never fire — but a query built by concatenation without it is an injection waiting
 * for the first unusual name.
 */
private fun String.escapedForQuery(): String = replace("\\", "\\\\").replace("'", "\\'")

private fun DriveContent.asRequestBody(): RequestBody = object : RequestBody() {
    override fun contentType() = "application/pdf".toMediaType()

    override fun writeTo(sink: BufferedSink) {
        // Opened here, at write time, so a retried request reads the file again instead of
        // replaying an exhausted stream.
        open().use { input -> sink.writeAll(input.source()) }
    }
}
