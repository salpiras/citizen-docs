package com.salpiras.citizendocs.core.ui

import android.text.format.Formatter
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.salpiras.citizendocs.core.designsystem.component.CitizenDocsIcons
import com.salpiras.citizendocs.core.designsystem.motion.CitizenDocsMotion
import com.salpiras.citizendocs.core.designsystem.theme.CitizenDocsTheme
import com.salpiras.citizendocs.core.designsystem.theme.LocalAccentColors
import com.salpiras.citizendocs.core.model.DocumentId
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance

@Composable
fun DocumentCard(
    document: DocumentUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    trailing: @Composable () -> Unit = {},
) {
    val context = LocalContext.current
    val pages = pluralStringResource(R.plurals.core_ui_page_count, document.pageCount, document.pageCount)
    val size = Formatter.formatShortFileSize(context, document.sizeBytes)
    val tile = document.tileColors()

    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = remember { Animatable(1f) }

    // Deliberately not `collectIsPressedAsState()`. That would make every press a
    // recomposition of this card. Driving an Animatable from the interaction flow and
    // reading it inside the graphicsLayer lambda instead keeps the whole press entirely in
    // the draw phase — no recomposition, no relayout.
    //
    // collectLatest, so a release arriving mid-press cancels the press animation rather than
    // queueing behind it. Animatable keeps its current value when cancelled, so the release
    // springs back from wherever the press had got to.
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.filterIsInstance<PressInteraction>().collectLatest { interaction ->
            val target = if (interaction is PressInteraction.Press) PRESSED_SCALE else 1f
            pressScale.animateTo(target, CitizenDocsMotion.spatialFast())
        }
    }

    // The one place the spot accent appears: a wash over the row that has just arrived,
    // fading in and back out. Same discipline as the press — an Animatable read inside the
    // draw lambda, so acknowledging a save costs the list nothing.
    val accent = LocalAccentColors.current.accent
    val wash = remember { Animatable(0f) }
    LaunchedEffect(highlighted) {
        wash.animateTo(
            targetValue = if (highlighted) 1f else 0f,
            animationSpec = CitizenDocsMotion.effectsDefault(),
        )
    }

    // No clearAndSetSemantics here. A clickable Card already merges its non-clickable
    // descendants into one node, so TalkBack reads the title and subtitle together, while the
    // overflow button stays independently focusable. Collapsing the whole subtree into a
    // single description would have made rename and delete unreachable via TalkBack.
    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale.value
                scaleY = pressScale.value
            }
            // Drawn over the finished card, and inside the Card's own clip, so the wash
            // takes the card's corners without any geometry of its own.
            .drawWithContent {
                drawContent()
                val alpha = wash.value
                if (alpha > 0f) drawRect(color = accent, alpha = alpha * WASH_ALPHA)
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(tile.container),
            ) {
                // A PDF can't be decoded by Coil; the old card's AsyncImage always fell back
                // to the launcher background. A typed icon is honest and costs nothing.
                Icon(
                    imageVector = CitizenDocsIcons.Document,
                    contentDescription = null,
                    tint = tile.onContainer,
                    modifier = Modifier.size(24.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // The old card passed `{ element.date.toString() }` to supportingContent,
                // which returns a String rather than emitting a composable — so the date
                // never rendered at all.
                Text(
                    text = "${document.formattedDate} · $pages · $size",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            trailing()
        }
    }
}

private data class TileColors(val container: Color, val onContainer: Color)

/**
 * Rotates the icon tile through the three container roles so a long list has some rhythm
 * instead of forty identical rows.
 *
 * Keyed on the id rather than the list index, so a document keeps its colour when the list
 * is filtered or regrouped — an index-keyed tint would reshuffle every tile on every search
 * keystroke. `Long.mod` rather than `%` because the latter can return a negative index.
 */
@Composable
private fun DocumentUiModel.tileColors(): TileColors {
    val scheme = MaterialTheme.colorScheme
    return when (id.value.mod(TILE_VARIANTS)) {
        0 -> TileColors(scheme.primaryContainer, scheme.onPrimaryContainer)
        1 -> TileColors(scheme.secondaryContainer, scheme.onSecondaryContainer)
        else -> TileColors(scheme.tertiaryContainer, scheme.onTertiaryContainer)
    }
}

private const val TILE_VARIANTS = 3
private const val PRESSED_SCALE = 0.97f

// Enough to register as a highlighter mark; any more and the title underneath goes muddy.
private const val WASH_ALPHA = 0.18f

@PreviewLightDark
@Composable
private fun DocumentCardPreview() {
    CitizenDocsTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
        ) {
            // Three ids, so the preview shows all three tile tints.
            persistentListOf(
                DocumentUiModel(DocumentId(3), "Tax return 2025", "12 Jan 2026", 3, 248_000),
                DocumentUiModel(DocumentId(1), "Passport", "30 Jun 2025", 1, 96_500),
                DocumentUiModel(DocumentId(2), "Tenancy agreement", "3 Jun 2025", 12, 1_340_000),
            ).forEach { document ->
                DocumentCard(document = document, onClick = {})
            }
        }
    }
}
