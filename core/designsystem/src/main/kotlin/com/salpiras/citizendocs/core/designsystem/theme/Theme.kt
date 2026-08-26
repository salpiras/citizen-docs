package com.salpiras.citizendocs.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import com.salpiras.citizendocs.core.designsystem.motion.LocalReducedMotion
import com.salpiras.citizendocs.core.designsystem.motion.rememberSystemReducedMotion

private val DarkColorScheme =
    darkColorScheme(
        primary = primaryDark,
        onPrimary = onPrimaryDark,
        primaryContainer = primaryContainerDark,
        onPrimaryContainer = onPrimaryContainerDark,
        secondary = secondaryDark,
        onSecondary = onSecondaryDark,
        secondaryContainer = secondaryContainerDark,
        onSecondaryContainer = onSecondaryContainerDark,
        tertiary = tertiaryDark,
        onTertiary = onTertiaryDark,
        tertiaryContainer = tertiaryContainerDark,
        onTertiaryContainer = onTertiaryContainerDark,
        error = errorDark,
        onError = onErrorDark,
        errorContainer = errorContainerDark,
        onErrorContainer = onErrorContainerDark,
        background = backgroundDark,
        onBackground = onBackgroundDark,
        surface = surfaceDark,
        onSurface = onSurfaceDark,
        surfaceVariant = surfaceVariantDark,
        onSurfaceVariant = onSurfaceVariantDark,
        outline = outlineDark,
        outlineVariant = outlineVariantDark,
        scrim = scrimDark,
        inverseSurface = inverseSurfaceDark,
        inverseOnSurface = inverseOnSurfaceDark,
        inversePrimary = inversePrimaryDark,
        surfaceDim = surfaceDimDark,
        surfaceBright = surfaceBrightDark,
        surfaceContainerLowest = surfaceContainerLowestDark,
        surfaceContainerLow = surfaceContainerLowDark,
        surfaceContainer = surfaceContainerDark,
        surfaceContainerHigh = surfaceContainerHighDark,
        surfaceContainerHighest = surfaceContainerHighestDark,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = primaryLight,
        onPrimary = onPrimaryLight,
        primaryContainer = primaryContainerLight,
        onPrimaryContainer = onPrimaryContainerLight,
        secondary = secondaryLight,
        onSecondary = onSecondaryLight,
        secondaryContainer = secondaryContainerLight,
        onSecondaryContainer = onSecondaryContainerLight,
        tertiary = tertiaryLight,
        onTertiary = onTertiaryLight,
        tertiaryContainer = tertiaryContainerLight,
        onTertiaryContainer = onTertiaryContainerLight,
        error = errorLight,
        onError = onErrorLight,
        errorContainer = errorContainerLight,
        onErrorContainer = onErrorContainerLight,
        background = backgroundLight,
        onBackground = onBackgroundLight,
        surface = surfaceLight,
        onSurface = onSurfaceLight,
        surfaceVariant = surfaceVariantLight,
        onSurfaceVariant = onSurfaceVariantLight,
        outline = outlineLight,
        outlineVariant = outlineVariantLight,
        scrim = scrimLight,
        inverseSurface = inverseSurfaceLight,
        inverseOnSurface = inverseOnSurfaceLight,
        inversePrimary = inversePrimaryLight,
        surfaceDim = surfaceDimLight,
        surfaceBright = surfaceBrightLight,
        surfaceContainerLowest = surfaceContainerLowestLight,
        surfaceContainerLow = surfaceContainerLowLight,
        surfaceContainer = surfaceContainerLight,
        surfaceContainerHigh = surfaceContainerHighLight,
        surfaceContainerHighest = surfaceContainerHighestLight,
    )

/**
 * [dynamicColor] defaults to **false**. It used to default to true, which meant that on
 * Android 12+ the wallpaper won and almost nobody ever saw the app's own palette — so the
 * palette was not really the app's identity, it was decoration for old devices. The
 * parameter stays because screenshot tests must pin it, and because turning it back on is
 * then a one-word change.
 *
 * [reducedMotion] follows the same pattern for the same reason: on device it reads the
 * user's setting, and tests pin it so that looping decorative motion cannot stall a capture.
 * See [LocalReducedMotion].
 *
 * Still plain `MaterialTheme`. `MaterialExpressiveTheme` — and with it `MotionScheme`,
 * whose spring specs M3 components would animate themselves with — is `internal` in
 * material3 1.4.0, which is what this build's Compose BOM resolves to. Our own springs live
 * in [com.salpiras.citizendocs.core.designsystem.motion.CitizenDocsMotion] until those APIs
 * go public.
 *
 * System bar handling lives in MainActivity via `enableEdgeToEdge()`; the previous theme
 * imported WindowCompat and friends but never used them.
 */
@Composable
fun CitizenDocsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    reducedMotion: Boolean = rememberSystemReducedMotion(),
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> DarkColorScheme

            else -> LightColorScheme
        }

    // The spot accent is not derived from the scheme, so it is not affected by dynamic
    // colour either. That is deliberate: it is the one colour that always means the same
    // thing, whatever the rest of the palette is doing.
    val accentColors =
        if (darkTheme) {
            AccentColors(accent = accentDark, onAccent = onAccent)
        } else {
            AccentColors(accent = accentLight, onAccent = onAccent)
        }

    CompositionLocalProvider(
        LocalAccentColors provides accentColors,
        LocalReducedMotion provides reducedMotion,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = CitizenDocsShapes,
            typography = CitizenDocsTypography,
            content = content,
        )
    }
}
