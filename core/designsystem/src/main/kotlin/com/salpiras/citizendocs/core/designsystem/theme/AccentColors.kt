package com.salpiras.citizendocs.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colour roles that have no home in [androidx.compose.material3.ColorScheme].
 *
 * M3's scheme has no slot for "the one colour reserved for a moment of motion", and
 * borrowing `tertiary` for it would mean the same colour turning up on ordinary chrome.
 * A separate holder behind a static local is the documented way to extend the scheme.
 *
 * Static rather than dynamic: these change only when the theme itself changes, so a static
 * local avoids making every reader of [LocalAccentColors] a recomposition scope.
 */
@Immutable
data class AccentColors(
    /** The spot colour. Save confirmations, search-match highlights — nothing routine. */
    val accent: Color,
    val onAccent: Color,
)

val LocalAccentColors =
    staticCompositionLocalOf {
        // A magenta default makes an un-themed usage obvious on screen rather than silently
        // rendering something plausible.
        AccentColors(accent = Color.Magenta, onAccent = Color.White)
    }
