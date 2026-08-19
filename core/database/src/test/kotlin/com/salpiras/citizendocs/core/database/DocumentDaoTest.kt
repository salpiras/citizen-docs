package com.salpiras.citizendocs.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Instant

/**
 * Runs against the real SQLite engine (in memory) rather than a fake, so the schema, the
 * type converters and the unique index are all genuinely exercised.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DocumentDaoTest {
    private lateinit var database: CitizenDocsDatabase
    private lateinit var dao: DocumentDao

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    CitizenDocsDatabase::class.java,
                ).build()
        dao = database.documentDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun entity(title: String, fileName: String, date: LocalDate = LocalDate(2026, 1, 12)) = DocumentEntity(
        title = title,
        documentDate = date,
        createdAt = Instant.parse("2026-01-12T09:30:00Z"),
        fileName = fileName,
        pageCount = 3,
        sizeBytes = 248_000,
    )

    @Test
    fun `inserts and reads a document back with its converted types intact`() = runTest {
        val id = dao.insert(entity("Tax return 2025", "Tax_return_2025.pdf"))

        val stored = dao.findById(id)!!
        assertThat(stored.title).isEqualTo("Tax return 2025")
        assertThat(stored.documentDate).isEqualTo(LocalDate(2026, 1, 12))
        assertThat(stored.createdAt).isEqualTo(Instant.parse("2026-01-12T09:30:00Z"))
    }

    @Test
    fun `orders by document date descending`() = runTest {
        dao.insert(entity("Oldest", "a.pdf", LocalDate(2024, 1, 1)))
        dao.insert(entity("Newest", "b.pdf", LocalDate(2026, 5, 5)))
        dao.insert(entity("Middle", "c.pdf", LocalDate(2025, 3, 3)))

        assertThat(dao.observeAll().first().map { it.title })
            .containsExactly("Newest", "Middle", "Oldest")
            .inOrder()
    }

    @Test
    fun `rejects a duplicate file name`() = runTest {
        dao.insert(entity("First", "same.pdf"))

        val result = runCatching { dao.insert(entity("Second", "same.pdf")) }

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `allows two documents with the same title but different file names`() = runTest {
        dao.insert(entity("Tax return 2025", "Tax_return_2025.pdf"))
        dao.insert(entity("Tax return 2025", "Tax_return_2025_1.pdf"))

        assertThat(dao.observeAll().first()).hasSize(2)
    }

    @Test
    fun `updates the title`() = runTest {
        val id = dao.insert(entity("Tax return 2025", "a.pdf"))

        assertThat(dao.updateTitle(id, "Self assessment")).isEqualTo(1)
        assertThat(dao.findById(id)!!.title).isEqualTo("Self assessment")
    }

    @Test
    fun `deletes by id and reports how many rows went`() = runTest {
        val id = dao.insert(entity("Tax return 2025", "a.pdf"))

        assertThat(dao.deleteById(id)).isEqualTo(1)
        assertThat(dao.deleteById(id)).isEqualTo(0)
        assertThat(dao.observeAll().first()).isEmpty()
    }

    @Test
    fun `observeAll emits again when a row is inserted`() = runTest {
        assertThat(dao.observeAll().first()).isEmpty()

        dao.insert(entity("Tax return 2025", "a.pdf"))

        assertThat(dao.observeAll().first()).hasSize(1)
    }
}
