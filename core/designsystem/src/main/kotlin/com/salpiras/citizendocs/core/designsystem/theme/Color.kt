package com.salpiras.citizendocs.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// Hand-authored, not a Material Theme Builder export.
//
// "Paper and ink": a warm sienna primary with sage and brass in support, over a paper-white
// ground. The previous palette was the tool's default blue over #F7F9FF — a *blue*-tinted
// white — with a blue-black dark surface, which is what made the app read as an enterprise
// document system rather than somebody's box of paperwork.
//
// Every neutral here carries the same warm hue as the accents, so surfaces read as paper
// stock in light and as dimmed paper in dark, never as grey chrome.

internal val primaryLight = Color(0xFFA8481C)
internal val onPrimaryLight = Color(0xFFFFFFFF)
internal val primaryContainerLight = Color(0xFFFFDBC8)
internal val onPrimaryContainerLight = Color(0xFF3A1200)
internal val secondaryLight = Color(0xFF4C6353)
internal val onSecondaryLight = Color(0xFFFFFFFF)
internal val secondaryContainerLight = Color(0xFFD0E8D7)
internal val onSecondaryContainerLight = Color(0xFF0A2015)
internal val tertiaryLight = Color(0xFF7A5B12)
internal val onTertiaryLight = Color(0xFFFFFFFF)
internal val tertiaryContainerLight = Color(0xFFFBE2A4)
internal val onTertiaryContainerLight = Color(0xFF261A00)
internal val errorLight = Color(0xFFB3261E)
internal val onErrorLight = Color(0xFFFFFFFF)
internal val errorContainerLight = Color(0xFFF9DEDC)
internal val onErrorContainerLight = Color(0xFF410E0B)

// The light ground is a shade deeper than plain paper so that `surfaceContainerLow` — the
// role every card uses — reads as a sheet lying *on* it. With the ground at #FBF6EF the two
// were close enough that cards dissolved into the background in light mode, and the only
// alternatives were a per-mode conditional inside the card or a hairline outline on every
// one of them. Moving one value keeps components role-based and free of `isSystemInDarkTheme`.
internal val backgroundLight = Color(0xFFF7F1E7)
internal val onBackgroundLight = Color(0xFF241C15)
internal val surfaceLight = Color(0xFFF7F1E7)
internal val onSurfaceLight = Color(0xFF241C15)
internal val surfaceVariantLight = Color(0xFFE7DCD1)
internal val onSurfaceVariantLight = Color(0xFF57493D)
internal val outlineLight = Color(0xFF897768)
internal val outlineVariantLight = Color(0xFFD8C9B8)
internal val scrimLight = Color(0xFF000000)
internal val inverseSurfaceLight = Color(0xFF392F27)
internal val inverseOnSurfaceLight = Color(0xFFF5EDE3)
internal val inversePrimaryLight = Color(0xFFFFB491)
internal val surfaceDimLight = Color(0xFFDDD3C4)
internal val surfaceBrightLight = Color(0xFFFFFCF7)
internal val surfaceContainerLowestLight = Color(0xFFFFFFFF)
internal val surfaceContainerLowLight = Color(0xFFFFFCF7)
internal val surfaceContainerLight = Color(0xFFF1EADE)
internal val surfaceContainerHighLight = Color(0xFFEBE3D5)
internal val surfaceContainerHighestLight = Color(0xFFE5DCCB)

internal val primaryDark = Color(0xFFFFB491)
internal val onPrimaryDark = Color(0xFF5A1F00)
internal val primaryContainerDark = Color(0xFF843209)
internal val onPrimaryContainerDark = Color(0xFFFFDBC8)
internal val secondaryDark = Color(0xFFB4CCBB)
internal val onSecondaryDark = Color(0xFF1F3527)
internal val secondaryContainerDark = Color(0xFF34493C)
internal val onSecondaryContainerDark = Color(0xFFD0E8D7)
internal val tertiaryDark = Color(0xFFDDC37A)
internal val onTertiaryDark = Color(0xFF3C2E00)
internal val tertiaryContainerDark = Color(0xFF5C4400)
internal val onTertiaryContainerDark = Color(0xFFFBE2A4)
internal val errorDark = Color(0xFFF2B8B5)
internal val onErrorDark = Color(0xFF601410)
internal val errorContainerDark = Color(0xFF8C1D18)
internal val onErrorContainerDark = Color(0xFFF9DEDC)
internal val backgroundDark = Color(0xFF141110)
internal val onBackgroundDark = Color(0xFFEFE2D7)
internal val surfaceDark = Color(0xFF141110)
internal val onSurfaceDark = Color(0xFFEFE2D7)
internal val surfaceVariantDark = Color(0xFF52443B)
internal val onSurfaceVariantDark = Color(0xFFD6C4B4)
internal val outlineDark = Color(0xFF9E8A79)
internal val outlineVariantDark = Color(0xFF52443B)
internal val scrimDark = Color(0xFF000000)
internal val inverseSurfaceDark = Color(0xFFEFE2D7)
internal val inverseOnSurfaceDark = Color(0xFF392F27)
internal val inversePrimaryDark = Color(0xFFA8481C)
internal val surfaceDimDark = Color(0xFF141110)
internal val surfaceBrightDark = Color(0xFF3B3430)
internal val surfaceContainerLowestDark = Color(0xFF0E0C0B)
internal val surfaceContainerLowDark = Color(0xFF1D1917)
internal val surfaceContainerDark = Color(0xFF211D1A)
internal val surfaceContainerHighDark = Color(0xFF2C2622)
internal val surfaceContainerHighestDark = Color(0xFF373029)

// The spot accent sits deliberately *outside* the tonal scheme. It exists only for moments
// of motion — a save landing, a search match — so keeping it out of ColorScheme is what
// stops it drifting into ordinary chrome. See AccentColors.kt.
internal val accentLight = Color(0xFFF0A020)
internal val accentDark = Color(0xFFFFC24D)
internal val onAccent = Color(0xFF412402)
