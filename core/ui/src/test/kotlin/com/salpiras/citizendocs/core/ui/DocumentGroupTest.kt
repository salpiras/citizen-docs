package com.salpiras.citizendocs.core.ui

import com.google.common.truth.Truth.assertThat
import com.salpiras.citizendocs.core.model.Document
import com.salpiras.citizendocs.core.model.DocumentId
import kotlinx.datetime.LocalDate
import org.junit.Test
import kotlin.time.Instant

class DocumentGroupTest {
    private var nextId = 1L

    private fun document(date: LocalDate, title: String = "Document") = Document(
        id = DocumentId(nextId++),
        title = title,
        documentDate = date,
        createdAt = Instant.parse("2026-01-12T09:30:00Z"),
        fileName = "$title.pdf",
        pageCount = 1,
        sizeBytes = 1_024,
    )

    @Test
    fun `an empty list produces no groups`() {
        assertThat(emptyList<Document>().groupByMonth()).isEmpty()
    }

    @Test
    fun `documents in the same month share one group`() {
        val groups = listOf(
            document(LocalDate(2026, 1, 20)),
            document(LocalDate(2026, 1, 3)),
        ).groupByMonth()

        assertThat(groups).hasSize(1)
        assertThat(groups.single().documents).hasSize(2)
    }

    @Test
    fun `different months are separate groups, newest first`() {
        // Already ordered newest-first by the repository; grouping must preserve that.
        val groups = listOf(
            document(LocalDate(2026, 1, 20)),
            document(LocalDate(2025, 12, 31)),
            document(LocalDate(2025, 6, 1)),
        ).groupByMonth()

        assertThat(groups.map { it.key }).containsExactly("2026-01", "2025-12", "2025-06").inOrder()
    }

    @Test
    fun `the same month in different years does not merge`() {
        val groups = listOf(
            document(LocalDate(2026, 6, 1)),
            document(LocalDate(2025, 6, 1)),
        ).groupByMonth()

        assertThat(groups.map { it.key }).containsExactly("2026-06", "2025-06").inOrder()
    }

    @Test
    fun `keys are zero-padded so they sort lexicographically`() {
        val groups = listOf(document(LocalDate(2026, 3, 9))).groupByMonth()

        assertThat(groups.single().key).isEqualTo("2026-03")
    }

    @Test
    fun `documents keep their order inside a group`() {
        val groups = listOf(
            document(LocalDate(2026, 1, 20), "Newest"),
            document(LocalDate(2026, 1, 3), "Oldest"),
        ).groupByMonth()

        assertThat(groups.single().documents.map { it.title })
            .containsExactly("Newest", "Oldest")
            .inOrder()
    }

    @Test
    fun `the label names the month and year`() {
        val label = listOf(document(LocalDate(2026, 1, 12))).groupByMonth().single().label

        // Locale-dependent month name, so assert on the parts that must always be present.
        assertThat(label).contains("2026")
        assertThat(label).isNotEmpty()
    }
}
