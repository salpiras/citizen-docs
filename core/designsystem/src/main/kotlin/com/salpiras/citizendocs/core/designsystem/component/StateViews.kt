package com.salpiras.citizendocs.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.salpiras.citizendocs.core.designsystem.motion.LocalReducedMotion
import com.salpiras.citizendocs.core.designsystem.theme.CitizenDocsTheme

// The three whole-screen states every list-style screen needs. Content-agnostic on purpose:
// callers pass already-resolved strings so this module never depends on feature resources.

/**
 * A skeleton of the list that is about to arrive, rather than a spinner.
 *
 * A spinner says "wait"; a skeleton says "here is the shape of what you are waiting for",
 * and because the placeholder rows are the same height as real ones the content does not
 * jump when it lands.
 *
 * [contentDescription] is not drawn — a skeleton with a caption is neither one thing nor the
 * other. It becomes the container's accessible name instead, as a polite live region, so
 * TalkBack announces the wait exactly once.
 */
@Composable
fun LoadingState(contentDescription: String, modifier: Modifier = Modifier) {
    val shimmer = rememberShimmerProgress()

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics {
                this.contentDescription = contentDescription
                liveRegion = LiveRegionMode.Polite
            },
    ) {
        repeat(PLACEHOLDER_ROWS) { PlaceholderRow(shimmer) }
    }
}

@Composable
private fun PlaceholderRow(shimmer: State<Float>?) {
    val base = MaterialTheme.colorScheme.surfaceContainerLow
    val block = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlight = MaterialTheme.colorScheme.surfaceContainerHighest

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(base)
            .padding(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(MaterialTheme.shapes.small)
                .shimmer(shimmer, block, highlight),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(TITLE_WIDTH_FRACTION)
                    .height(14.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .shimmer(shimmer, block, highlight),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(META_WIDTH_FRACTION)
                    .height(10.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .shimmer(shimmer, block, highlight),
            )
        }
    }
}

/**
 * Paints the placeholder fill and, when motion is allowed, a highlight band sweeping across
 * it.
 *
 * Everything here happens in the draw phase. [progress] is captured as a [State] and only
 * dereferenced inside `onDrawBehind`, so a moving band costs redraws and not recompositions,
 * and the gradient is built once per size change rather than once per frame.
 */
private fun Modifier.shimmer(progress: State<Float>?, base: Color, highlight: Color): Modifier = drawWithCache {
    val band =
        Brush.linearGradient(
            colors = listOf(Color.Transparent, highlight, Color.Transparent),
            start = Offset.Zero,
            end = Offset(size.width, 0f),
        )

    onDrawBehind {
        drawRect(base)
        progress?.let {
            // -1 → 1 of the row's own width, so the band enters from the left and leaves
            // to the right.
            translate(left = (it.value * 2f - 1f) * size.width) { drawRect(band) }
        }
    }
}

/** `null` when the user has asked for no animation — see [LocalReducedMotion]. */
@Composable
private fun rememberShimmerProgress(): State<Float>? {
    if (LocalReducedMotion.current) return null

    val transition = rememberInfiniteTransition(label = "shimmer")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            // Linear, and restarting rather than reversing: a sweep that eases or runs
            // backwards reads as something being dragged, not as a surface catching light.
            animation = tween(durationMillis = SHIMMER_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerProgress",
    )
}

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier) {
    val drift = rememberDrift()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            // The heading below already conveys the meaning; a duplicate announcement is noise.
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(72.dp)
                // Read inside the lambda, so the drift is a draw-phase transform. An empty
                // screen has nothing else to do, but an infinite animation that recomposed
                // would keep the whole subtree awake for no reason.
                .graphicsLayer {
                    translationY = drift?.value ?: 0f
                    rotationZ = (drift?.value ?: 0f) / DRIFT_ROTATION_DIVISOR
                },
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 24.dp),
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** A sheet of paper settling. Slow and small enough to be noticed only if you look for it. */
@Composable
private fun rememberDrift(): State<Float>? {
    if (LocalReducedMotion.current) return null

    val transition = rememberInfiniteTransition(label = "drift")
    return transition.animateFloat(
        initialValue = -DRIFT_PIXELS,
        targetValue = DRIFT_PIXELS,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = DRIFT_MILLIS),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "driftOffset",
    )
}

@Composable
fun ErrorState(icon: ImageVector, title: String, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .size(56.dp)
                .clearAndSetSemantics {},
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

private const val PLACEHOLDER_ROWS = 4
private const val TITLE_WIDTH_FRACTION = 0.55f
private const val META_WIDTH_FRACTION = 0.8f
private const val SHIMMER_MILLIS = 1400
private const val DRIFT_MILLIS = 2600
private const val DRIFT_PIXELS = 10f
private const val DRIFT_ROTATION_DIVISOR = 4f

@PreviewLightDark
@Composable
private fun LoadingStatePreview() {
    CitizenDocsTheme {
        LoadingState(contentDescription = "Loading your documents…")
    }
}

@PreviewLightDark
@Composable
private fun EmptyStatePreview() {
    CitizenDocsTheme {
        EmptyState(
            icon = CitizenDocsIcons.DocumentScanner,
            title = "No documents yet",
            body = "Tap the scan button to file your first document.",
        )
    }
}

@PreviewLightDark
@Composable
private fun ErrorStatePreview() {
    CitizenDocsTheme {
        ErrorState(
            icon = CitizenDocsIcons.Error,
            title = "Something went wrong",
            message = "We couldn't load your documents.",
        )
    }
}
