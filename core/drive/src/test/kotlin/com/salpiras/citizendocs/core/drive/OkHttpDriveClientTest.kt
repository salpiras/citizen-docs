package com.salpiras.citizendocs.core.drive

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream

/**
 * The client against a real HTTP server, on the JVM.
 *
 * MockWebServer rather than a mocked `Call.Factory` is the point: it exercises the actual
 * request shapes — query strings, the `multipart/related` body, pagination — which is where
 * a REST client's bugs live. A mock would only ever confirm that the code calls the methods
 * it calls.
 */
class OkHttpDriveClientTest {
    private val server = MockWebServer()
    private lateinit var client: OkHttpDriveClient

    private val auth = FakeDriveAuth()

    @Before
    fun setUp() {
        server.start()
        client = OkHttpDriveClient(
            callFactory = OkHttpClient.Builder().addInterceptor(DriveAuthInterceptor(auth)).build(),
            baseUrl = server.url("/"),
            io = UnconfinedTestDispatcher(),
        )
    }

    @After
    fun tearDown() = server.close()

    private fun enqueueJson(body: String, code: Int = 200) =
        server.enqueue(MockResponse.Builder().code(code).body(body).build())

    // --- Folders --------------------------------------------------------------------

    @Test
    fun `findFolder asks only for folders that are not in the bin`() = runTest {
        enqueueJson("""{"files":[{"id":"folder-1","name":"CitizenDocs"}]}""")

        val folder = client.findFolder("CitizenDocs")

        assertThat(folder).isEqualTo(DriveFile(id = "folder-1", name = "CitizenDocs"))
        val query = server.takeRequest().url.queryParameter("q")!!
        assertThat(query).contains("name = 'CitizenDocs'")
        assertThat(query).contains("mimeType = 'application/vnd.google-apps.folder'")
        assertThat(query).contains("trashed = false")
        assertThat(query).contains("'root' in parents")
    }

    @Test
    fun `findFolder returns null when Drive has none`() = runTest {
        enqueueJson("""{"files":[]}""")

        assertThat(client.findFolder("CitizenDocs")).isNull()
    }

    @Test
    fun `ensureFolder creates the folder only when it is missing`() = runTest {
        enqueueJson("""{"files":[]}""")
        enqueueJson("""{"id":"made","name":"2026"}""")

        val folder = client.ensureFolder("2026", parentId = "root-folder")

        assertThat(folder.id).isEqualTo("made")
        server.takeRequest()
        val create = server.takeRequest()
        assertThat(create.method).isEqualTo("POST")
        val body = create.body!!.utf8()
        assertThat(body).contains("\"mimeType\":\"application/vnd.google-apps.folder\"")
        assertThat(body).contains("\"parents\":[\"root-folder\"]")
    }

    @Test
    fun `a name containing a quote cannot break out of the query`() = runTest {
        enqueueJson("""{"files":[]}""")

        client.findFolder("Mum's papers")

        assertThat(server.takeRequest().url.queryParameter("q")).contains("""name = 'Mum\'s papers'""")
    }

    // --- Listing --------------------------------------------------------------------

    /**
     * The failure this guards against is the quiet one: a restore that stops at the first
     * page looks like it worked and simply leaves the rest of the library behind.
     */
    @Test
    fun `listFiles follows every page`() = runTest {
        enqueueJson("""{"files":[{"id":"a","name":"A.pdf"}],"nextPageToken":"page-2"}""")
        enqueueJson("""{"files":[{"id":"b","name":"B.pdf"}],"nextPageToken":"page-3"}""")
        enqueueJson("""{"files":[{"id":"c","name":"C.pdf"}]}""")

        val files = client.listFiles("folder-1")

        assertThat(files.map { it.id }).containsExactly("a", "b", "c").inOrder()
        assertThat(server.takeRequest().url.queryParameter("pageToken")).isNull()
        assertThat(server.takeRequest().url.queryParameter("pageToken")).isEqualTo("page-2")
        assertThat(server.takeRequest().url.queryParameter("pageToken")).isEqualTo("page-3")
    }

    @Test
    fun `listFiles reads app properties back`() = runTest {
        enqueueJson(
            """{"files":[{"id":"a","name":"Tax return 2025.pdf","appProperties":{"u":"abc","p":"3"}}]}""",
        )

        val file = client.listFiles("folder-1").single()

        assertThat(file.name).isEqualTo("Tax return 2025.pdf")
        assertThat(file.appProperties).containsExactly("u", "abc", "p", "3")
    }

    // --- Upload and download --------------------------------------------------------

    @Test
    fun `upload sends metadata and bytes as one related body`() = runTest {
        enqueueJson("""{"id":"uploaded","name":"Tax return 2025.pdf"}""")

        val file = client.upload(
            name = "Tax return 2025.pdf",
            parentId = "month-folder",
            appProperties = mapOf("u" to "abc"),
            content = { "%PDF-1.4 fake".byteInputStream() },
        )

        assertThat(file.id).isEqualTo("uploaded")
        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("POST")
        assertThat(request.url.queryParameter("uploadType")).isEqualTo("multipart")
        assertThat(request.headers["Content-Type"]).startsWith("multipart/related")

        val body = request.body!!.utf8()
        // Title in the file name, not in appProperties, which cannot hold 80 characters.
        assertThat(body).contains("\"name\":\"Tax return 2025.pdf\"")
        assertThat(body).contains("\"appProperties\":{\"u\":\"abc\"}")
        assertThat(body).contains("%PDF-1.4 fake")
    }

    @Test
    fun `update patches in place and does not try to move the file`() = runTest {
        enqueueJson("""{"id":"existing","name":"Self assessment.pdf"}""")

        client.update(
            fileId = "existing",
            name = "Self assessment.pdf",
            appProperties = mapOf("u" to "abc"),
            content = { "%PDF-1.4 fake".byteInputStream() },
        )

        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("PATCH")
        assertThat(request.url.encodedPath).endsWith("/upload/drive/v3/files/existing")
        // Drive rejects `parents` on an update, and the file is not moving anyway.
        assertThat(request.body!!.utf8()).doesNotContain("parents")
    }

    @Test
    fun `download streams the bytes out`() = runTest {
        server.enqueue(MockResponse.Builder().code(200).body("%PDF-1.4 contents").build())
        val destination = ByteArrayOutputStream()

        client.download("file-1", destination)

        assertThat(destination.toString()).isEqualTo("%PDF-1.4 contents")
        assertThat(server.takeRequest().url.queryParameter("alt")).isEqualTo("media")
    }

    // --- Failures -------------------------------------------------------------------

    @Test
    fun `a Drive error carries the status and what Drive said`() = runTest {
        enqueueJson("""{"error":{"message":"Rate limit exceeded"}}""", code = 403)

        val thrown = runCatching { client.findFolder("CitizenDocs") }.exceptionOrNull()

        assertThat(thrown).isInstanceOf(DriveException::class.java)
        assertThat(thrown).hasMessageThat().contains("403")
        assertThat(thrown).hasMessageThat().contains("Rate limit exceeded")
    }

    // --- Authorization --------------------------------------------------------------

    @Test
    fun `a rejected token is refreshed and the request retried once`() = runTest {
        enqueueJson("", code = 401)
        enqueueJson("""{"files":[{"id":"folder-1","name":"CitizenDocs"}]}""")

        val folder = client.findFolder("CitizenDocs")

        assertThat(folder?.id).isEqualTo("folder-1")
        assertThat(server.takeRequest().headers["Authorization"]).isEqualTo("Bearer token-1")
        assertThat(server.takeRequest().headers["Authorization"]).isEqualTo("Bearer token-2")
        assertThat(auth.refreshes).isEqualTo(1)
    }

    @Test
    fun `a second rejection is not retried again`() = runTest {
        enqueueJson("", code = 401)
        enqueueJson("", code = 401)

        val thrown = runCatching { client.findFolder("CitizenDocs") }.exceptionOrNull()

        // The second 401 surfaces as an ordinary Drive error rather than looping.
        assertThat(thrown).isInstanceOf(DriveException::class.java)
        assertThat(auth.refreshes).isEqualTo(1)
    }

    @Test
    fun `no consent is not reported as a transport failure`() = runTest {
        auth.token = null

        val thrown = runCatching { client.findFolder("CitizenDocs") }.exceptionOrNull()

        // The UI has to offer "connect", not "retry", so this must not collapse into
        // DriveException the way a network error does.
        assertThat(thrown).isInstanceOf(DriveUnauthorizedException::class.java)
    }
}

/** Hands out `token-1`, then `token-2`, and counts refreshes. */
private class FakeDriveAuth : DriveAuth {
    var token: String? = "token-1"
    var refreshes = 0

    override suspend fun accessToken(forceRefresh: Boolean): String? {
        if (forceRefresh && token != null) {
            refreshes++
            token = "token-${refreshes + 1}"
        }
        return token
    }

    override suspend fun authorize() = DriveAuthorization.Granted
}
