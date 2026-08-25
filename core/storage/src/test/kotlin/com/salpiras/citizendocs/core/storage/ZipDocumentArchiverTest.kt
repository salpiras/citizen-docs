package com.salpiras.citizendocs.core.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.zip.ZipInputStream

/**
 * Writes real archives and reads them back, because the things that break here — entry paths,
 * truncated streams, a destination that can't be opened — are all filesystem behaviour.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ZipDocumentArchiverTest {
    private lateinit var context: Context
    private lateinit var archiver: ZipDocumentArchiver

    private val documentsDir get() = File(context.filesDir, "documents")

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        archiver = ZipDocumentArchiver(context, UnconfinedTestDispatcher())
        documentsDir.mkdirs()
    }

    private fun storeFile(name: String, contents: String) {
        File(documentsDir, name).writeText(contents)
    }

    private fun destination(name: String = "out.zip"): String {
        val file = File(context.cacheDir, name)
        file.delete()
        return "file://${file.absolutePath}"
    }

    private fun readZip(uri: String): Map<String, String> {
        val entries = mutableMapOf<String, String>()
        ZipInputStream(File(uri.removePrefix("file://")).inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entries[entry.name] = zip.readBytes().decodeToString()
                entry = zip.nextEntry
            }
        }
        return entries
    }

    @Test
    fun `writes each entry at its requested path`() = runTest {
        storeFile("Tax_return_2025.pdf", "tax")
        storeFile("Passport.pdf", "passport")
        val uri = destination()

        archiver.writeZip(
            uri,
            listOf(
                ArchiveEntry("2026/01/Tax_return_2025.pdf", "Tax_return_2025.pdf"),
                ArchiveEntry("2025/06/Passport.pdf", "Passport.pdf"),
            ),
        )

        assertThat(readZip(uri).keys)
            .containsExactly("2026/01/Tax_return_2025.pdf", "2025/06/Passport.pdf")
    }

    @Test
    fun `entry contents round-trip`() = runTest {
        storeFile("Tax_return_2025.pdf", "%PDF-1.4 tax return")
        val uri = destination()

        archiver.writeZip(uri, listOf(ArchiveEntry("2026/01/Tax_return_2025.pdf", "Tax_return_2025.pdf")))

        assertThat(readZip(uri)["2026/01/Tax_return_2025.pdf"]).isEqualTo("%PDF-1.4 tax return")
    }

    @Test
    fun `reports the bytes written`() = runTest {
        storeFile("Tax_return_2025.pdf", "1234567890")
        val uri = destination()

        val written = archiver.writeZip(uri, listOf(ArchiveEntry("a.pdf", "Tax_return_2025.pdf")))

        assertThat(written).isEqualTo(10)
    }

    // A document row whose file vanished must not abort the whole backup.
    @Test
    fun `skips entries whose stored file is missing`() = runTest {
        storeFile("Passport.pdf", "passport")
        val uri = destination()

        archiver.writeZip(
            uri,
            listOf(
                ArchiveEntry("2026/01/Gone.pdf", "Gone.pdf"),
                ArchiveEntry("2025/06/Passport.pdf", "Passport.pdf"),
            ),
        )

        assertThat(readZip(uri).keys).containsExactly("2025/06/Passport.pdf")
    }

    @Test
    fun `an empty entry list still produces a readable archive`() = runTest {
        val uri = destination()

        archiver.writeZip(uri, emptyList())

        assertThat(readZip(uri)).isEmpty()
    }

    @Test
    fun `fails when the destination cannot be opened`() = runTest {
        storeFile("Passport.pdf", "passport")

        val result = runCatching {
            archiver.writeZip(
                "file:///nope/missing-directory/out.zip",
                listOf(ArchiveEntry("Passport.pdf", "Passport.pdf")),
            )
        }

        assertThat(result.isFailure).isTrue()
    }
}
