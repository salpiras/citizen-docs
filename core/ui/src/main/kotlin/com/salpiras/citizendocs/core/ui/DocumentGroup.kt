package com.salpiras.citizendocs.core.ui

import androidx.compose.runtime.Immutable
import com.salpiras.citizendocs.core.model.Document
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import java.time.format.DateTimeFormatter

/**
 * A month's worth of documents, as one collapsible section of the list.
 *
 * [label] is pre-formatted here rather than in the composable, exactly as [DocumentUiModel]
 * pre-formats its date: it keeps locale work out of layout, and it keeps screenshot goldens
 * from depending on the host's locale.
 */
@Immutable
data class DocumentGroup(
    /** Sort- and identity-stable, e.g. "2026-01". Used as the LazyColumn key. */
    val key: String,
    /** Human-facing, e.g. "January 2026". */
    val label: String,
    val documents: ImmutableList<DocumentUiModel>,
)

// LLLL is the standalone month form — "January", not the genitive some locales use when a
// month appears alongside a day number.
private val monthLabelFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("LLLL yyyy")

/**
 * Splits documents into month sections.
 *
 * The repository already orders by document date descending, and [groupBy] preserves
 * encounter order, so both the groups and the documents inside them come out newest-first
 * without a second sort.
 */
fun List<Document>.groupByMonth(): ImmutableList<DocumentGroup> = groupBy { document ->
    // Via java.time: kotlinx-datetime deprecated `monthNumber`, and its `Month` type differs
    // between the common and JVM declarations, so this is the unambiguous accessor.
    val date = document.documentDate.toJavaLocalDate()
    date.year to date.monthValue
}.map { (yearMonth, documents) ->
    val (year, month) = yearMonth
    DocumentGroup(
        key = "%04d-%02d".format(year, month),
        label = LocalDate(year, month, 1).toJavaLocalDate().format(monthLabelFormatter),
        documents = documents.map(Document::asUiModel).toImmutableList(),
    )
}.toImmutableList()
