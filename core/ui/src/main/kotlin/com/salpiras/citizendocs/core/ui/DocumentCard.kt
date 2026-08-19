package com.salpiras.citizendocs.core.ui

import android.text.format.Formatter
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.salpiras.citizendocs.core.designsystem.component.CitizenDocsIcons
import com.salpiras.citizendocs.core.designsystem.theme.CitizenDocsTheme
import com.salpiras.citizendocs.core.model.DocumentId

@Composable
fun DocumentCard(
    document: DocumentUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    val context = LocalContext.current
    val pages = pluralStringResource(R.plurals.core_ui_page_count, document.pageCount, document.pageCount)
    val size = Formatter.formatShortFileSize(context, document.sizeBytes)

    // No clearAndSetSemantics here. A clickable Card already merges its non-clickable
    // descendants into one node, so TalkBack reads the title and subtitle together, while the
    // overflow button stays independently focusable. Collapsing the whole subtree into a
    // single description would have made rename and delete unreachable via TalkBack.
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            headlineContent = { Text(document.title) },
            // The old card passed `{ element.date.toString() }` here, which returns a String
            // rather than emitting a composable — so the date never rendered at all.
            supportingContent = { Text("${document.formattedDate} · $pages · $size") },
            leadingContent = {
                // A PDF can't be decoded by Coil; the old AsyncImage always fell back to the
                // launcher background. A typed icon is honest and costs nothing.
                Icon(
                    imageVector = CitizenDocsIcons.Document,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp),
                )
            },
            trailingContent = { Row { trailing() } },
        )
    }
}

@PreviewLightDark
@Composable
private fun DocumentCardPreview() {
    CitizenDocsTheme(dynamicColor = false) {
        DocumentCard(
            document =
            DocumentUiModel(
                id = DocumentId(1),
                title = "Tax return 2025",
                formattedDate = "12 Jan 2026",
                pageCount = 3,
                sizeBytes = 248_000,
            ),
            onClick = {},
        )
    }
}
