package com.salpiras.citizendocs.core.data

import com.google.common.truth.Truth.assertThat
import com.salpiras.citizendocs.core.model.DocumentId
import com.salpiras.citizendocs.core.testing.FakeDocumentDao
import com.salpiras.citizendocs.core.testing.FixedClock
import com.salpiras.citizendocs.core.testing.InMemoryDocumentFileStore
import com.salpiras.citizendocs.core.testing.RecordingDocumentArchiver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Test
import java.io.IOException
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineFirstDocumentsRepositoryTest {
    private val dao = FakeDocumentDao()
    private val fileStore = InMemoryDocumentFileStore()
    private val clock = FixedClock(Instant.parse("2026-08-18T10:15:30Z"))

    private val archiver = RecordingDocumentArchiver()

    private val repository =
        OfflineFirstDocumentsRepository(
            dao = dao,
            fileStore = fileStore,
            archiver = archiver,
            clock = clock,
            io = UnconfinedTestDispatcher(),
        )

    private val draft =
        DocumentDraft(
            title = "Tax return 2025",
            documentDate = LocalDate(2026, 1, 12),
            sourceUri = "content://scan/tmp.pdf",
            pageCount = 3,
        )

    @Test
    fun `add stores both the file and the row`() = runTest {
        val id = repository.add(draft)

        val stored = repository.observeDocument(id).first()
        assertThat(stored).isNotNull()
        assertThat(stored!!.title).isEqualTo("Tax return 2025")
        assertThat(fileStore.files.keys).containsExactly("Tax_return_2025.pdf")
    }

    @Test
    fun `add stamps createdAt from the injected clock`() = runTest {
        val id = repository.add(draft)

        assertThat(repository.observeDocument(id).first()!!.createdAt)
            .isEqualTo(Instant.parse("2026-08-18T10:15:30Z"))
    }

    @Test
    fun `add stores a file name relative to the store root, never an absolute path`() = runTest {
        val id = repository.add(draft)

        val fileName = repository.observeDocument(id).first()!!.fileName
        assertThat(fileName).doesNotContain("/")
    }

    // The file is written before the row exists, so a failed insert would otherwise leave a
    // PDF on disk that nothing references and nothing can ever delete.
    @Test
    fun `add deletes the persisted file when the row insert fails`() = runTest {
        dao.insertFailure = IOException("UNIQUE constraint failed")

        val result = runCatching { repository.add(draft) }

        assertThat(result.exceptionOrNull()).isInstanceOf(IOException::class.java)
        assertThat(fileStore.files).isEmpty()
    }

    @Test
    fun `two documents with the same title both survive`() = runTest {
        repository.add(draft)
        repository.add(draft)

        assertThat(repository.observeDocuments().first()).hasSize(2)
        assertThat(fileStore.files.keys)
            .containsExactly("Tax_return_2025.pdf", "Tax_return_2025_1.pdf")
    }

    @Test
    fun `delete removes the row and the file together`() = runTest {
        val id = repository.add(draft)

        repository.delete(id)

        assertThat(repository.observeDocuments().first()).isEmpty()
        assertThat(fileStore.files).isEmpty()
    }

    @Test
    fun `delete of an unknown id is a no-op`() = runTest {
        repository.add(draft)

        repository.delete(DocumentId(999))

        assertThat(repository.observeDocuments().first()).hasSize(1)
        assertThat(fileStore.files).hasSize(1)
    }

    @Test
    fun `rename updates the title and the file name`() = runTest {
        val id = repository.add(draft)

        repository.rename(id, "Self assessment")

        val renamed = repository.observeDocument(id).first()!!
        assertThat(renamed.title).isEqualTo("Self assessment")
        assertThat(renamed.fileName).isEqualTo("Self_assessment.pdf")
        assertThat(fileStore.files.keys).containsExactly("Self_assessment.pdf")
    }

    @Test
    fun `search filters by title substring`() = runTest {
        repository.add(draft.copy(title = "Tax return 2025"))
        repository.add(draft.copy(title = "Passport"))

        assertThat(repository.observeDocuments("pass").first().map { it.title })
            .containsExactly("Passport")
    }

    @Test
    fun `an empty search query returns everything`() = runTest {
        repository.add(draft.copy(title = "Tax return 2025"))
        repository.add(draft.copy(title = "Passport"))

        assertThat(repository.observeDocuments("").first()).hasSize(2)
    }

    // The zip's folders mirror the month sections the list shows.
    @Test
    fun `export folders entries by document year and month`() = runTest {
        val id = repository.add(draft.copy(documentDate = LocalDate(2026, 1, 12)))
        val document = repository.observeDocument(id).first()!!

        repository.export(listOf(document), "content://downloads/out.zip")

        assertThat(archiver.entries.map { it.pathInZip })
            .containsExactly("2026/01/Tax_return_2025.pdf")
    }

    @Test
    fun `export zero-pads the month`() = runTest {
        val id = repository.add(draft.copy(title = "Bill", documentDate = LocalDate(2025, 9, 3)))
        val document = repository.observeDocument(id).first()!!

        repository.export(listOf(document), "content://downloads/out.zip")

        assertThat(archiver.entries.single().pathInZip).isEqualTo("2025/09/Bill.pdf")
    }

    @Test
    fun `export passes the destination to the archiver`() = runTest {
        val id = repository.add(draft)
        val document = repository.observeDocument(id).first()!!

        repository.export(listOf(document), "content://downloads/out.zip")

        assertThat(archiver.destinationUri).isEqualTo("content://downloads/out.zip")
    }

    @Test
    fun `documents are ordered by document date, newest first`() = runTest {
        repository.add(draft.copy(title = "Older", documentDate = LocalDate(2024, 1, 1)))
        repository.add(draft.copy(title = "Newer", documentDate = LocalDate(2026, 5, 5)))

        assertThat(repository.observeDocuments().first().map { it.title })
            .containsExactly("Newer", "Older")
            .inOrder()
    }
}
