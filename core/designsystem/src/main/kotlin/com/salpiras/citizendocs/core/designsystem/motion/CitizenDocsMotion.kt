package com.salpiras.citizendocs.core.designsystem.motion

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.IntOffset

/**
 * The app's motion vocabulary, in one place, so that a transition written in `:app` and a
 * chevron written in `:feature:documents` move like they belong to the same product.
 *
 * Springs rather than durations, throughout. A spring is defined by where it is going, not
 * by how long it has left, so an animation interrupted half-way — the user collapsing a
 * section and immediately expanding it again — continues from its current velocity instead
 * of snapping and restarting.
 *
 * M3 ships exactly this idea as `MotionScheme`, but in material3 1.4.0 both it and
 * `MaterialExpressiveTheme` are `internal`, so there is nothing to read from the theme yet.
 * When the expressive APIs go public these become one-line delegations to
 * `MaterialTheme.motionScheme` and every call site stays as it is.
 *
 * The split matters:
 * - **Spatial** — anything that moves or resizes. Slightly under-damped, so movement has a
 *   little weight at the end.
 * - **Effects** — anything that fades or changes colour. Critically damped, always: an
 *   overshoot on alpha or colour reads as a rendering bug rather than as liveliness.
 */
object CitizenDocsMotion {
    fun <T> spatialDefault(): FiniteAnimationSpec<T> =
        spring(dampingRatio = SPATIAL_DAMPING, stiffness = Spring.StiffnessMediumLow)

    /** For small, frequent movements — a chevron, a press. Fast enough to feel direct. */
    fun <T> spatialFast(): FiniteAnimationSpec<T> =
        spring(dampingRatio = SPATIAL_DAMPING, stiffness = Spring.StiffnessMedium)

    /** For whole-screen movement, where a slower settle reads as deliberate. */
    fun <T> spatialSlow(): FiniteAnimationSpec<T> =
        spring(dampingRatio = SPATIAL_DAMPING, stiffness = Spring.StiffnessVeryLow)

    fun <T> effectsDefault(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)

    fun <T> effectsFast(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)

    /**
     * The spatial specs specialised for `IntOffset` — list-item placement, screen slides.
     *
     * Worth having separately because the generic versions leave `visibilityThreshold` null,
     * which makes a spring run until it is within 0.01 of its target. For a pixel offset
     * that is a hundredth of a pixel, so it burns frames animating something nobody can see.
     * `IntOffset`'s own threshold stops it at half a pixel instead.
     */
    fun offsetDefault(): FiniteAnimationSpec<IntOffset> = offsetSpring(Spring.StiffnessMediumLow)

    fun offsetSlow(): FiniteAnimationSpec<IntOffset> = offsetSpring(Spring.StiffnessVeryLow)

    private fun offsetSpring(stiffness: Float): FiniteAnimationSpec<IntOffset> = spring(
        dampingRatio = SPATIAL_DAMPING,
        stiffness = stiffness,
        visibilityThreshold = IntOffset.VisibilityThreshold,
    )
}

// Just short of critical damping: enough to look alive, not enough to look like a toy.
private const val SPATIAL_DAMPING = 0.85f
