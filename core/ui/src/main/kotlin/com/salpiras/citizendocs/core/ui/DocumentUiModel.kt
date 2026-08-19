package com.salpiras.citizendocs.core.ui

import androidx.compose.runtime.Immutable
import com.salpiras.citizendocs.core.model.Document
import com.salpiras.citizendocs.core.model.DocumentId
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * What a row actually renders. Pre-formatted so the composable does no work during layout
 * and screenshot tests are not at the mercy of the host locale.
 */
@Immutable
data class DocumentUiModel(
    val id: DocumentId,
    val title: String,
    val formattedDate: String,
    val pageCount: Int,
    val sizeBytes: Long,
)

private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

fun Document.asUiModel() = DocumentUiModel(
    id = id,
    title = title,
    formattedDate = documentDate.format(),
    pageCount = pageCount,
    sizeBytes = sizeBytes,
)

private fun LocalDate.format(): String = toJavaLocalDate().format(dateFormatter)
