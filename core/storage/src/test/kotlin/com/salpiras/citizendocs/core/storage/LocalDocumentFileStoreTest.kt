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

/**
 * Exercises the real file operations against Robolectric's temporary filesDir, because the
 * bugs being guarded against here (missing mkdirs, name collisions) are filesystem bugs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class LocalDocumentFileStoreTest {
    private lateinit var context: Context
    private lateinit var store: LocalDocumentFileStore
    private lateinit var source: File

    private val documentsDir get() = File(context.filesDir, "documents")

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        store = LocalDocumentFileStore(context, UnconfinedTestDispatcher())
        source = File(context.cacheDir, "scan.pdf").apply { writeText("%PDF-1.4 fake") }
    }

    private fun sourceUri() = "file://${source.absolutePath}"

    // The old DocumentService never called mkdirs(), so the first save into a new directory
    // failed outright.
    @Test
    fun `creates the documents directory on first use`() = runTest {
        assertThat(documentsDir.exists()).isFalse()

        store.persist(sourceUri(), "Tax_return_2025")

        assertThat(documentsDir.exists()).isTrue()
    }

    @Test
    fun `persists the file under the requested stem`() = runTest {
        val name = store.persist(sourceUri(), "Tax_return_2025")

        assertThat(name).isEqualTo("Tax_return_2025.pdf")
        assertThat(File(documentsDir, name).readText()).isEqualTo("%PDF-1.4 fake")
    }

    // Previously this threw FileAlreadyExistsException — two documents may legitimately
    // share a title.
    @Test
    fun `suffixes the name when one already exists`() = runTest {
        val first = store.persist(sourceUri(), "Tax_return_2025")
        val second = store.persist(sourceUri(), "Tax_return_2025")
        val third = store.persist(sourceUri(), "Tax_return_2025")

        assertThat(listOf(first, second, third))
            .containsExactly(
                "Tax_return_2025.pdf",
                "Tax_return_2025_1.pdf",
                "Tax_return_2025_2.pdf",
            ).inOrder()
    }

    @Test
    fun `fails cleanly when the source cannot be read`() = runTest {
        val missing = "file://${File(context.cacheDir, "nope.pdf").absolutePath}"

        val result = runCatching { store.persist(missing, "Whatever") }

        assertThat(result.isFailure).isTrue()
        // No half-written file left behind.
        assertThat(documentsDir.listFiles().orEmpty()).isEmpty()
    }

    @Test
    fun `reports the stored size`() = runTest {
        val name = store.persist(sourceUri(), "Tax_return_2025")

        assertThat(store.sizeOf(name)).isEqualTo("%PDF-1.4 fake".length.toLong())
    }

    @Test
    fun `renames a stored file`() = runTest {
        val name = store.persist(sourceUri(), "Tax_return_2025")

        val renamed = store.rename(name, "Self_assessment")

        assertThat(renamed).isEqualTo("Self_assessment.pdf")
        assertThat(File(documentsDir, "Self_assessment.pdf").exists()).isTrue()
        assertThat(File(documentsDir, name).exists()).isFalse()
    }

    @Test
    fun `renaming a missing file is a no-op`() = runTest {
        assertThat(store.rename("ghost.pdf", "Anything")).isEqualTo("ghost.pdf")
    }

    @Test
    fun `deletes a stored file`() = runTest {
        val name = store.persist(sourceUri(), "Tax_return_2025")

        assertThat(store.delete(name)).isTrue()
        assertThat(store.delete(name)).isFalse()
    }
}
