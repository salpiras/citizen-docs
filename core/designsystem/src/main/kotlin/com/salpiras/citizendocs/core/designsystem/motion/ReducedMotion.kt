package com.salpiras.citizendocs.core.designsystem.motion

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * Whether decorative motion should be suppressed.
 *
 * Two callers care. On device it follows the user: turning off *Developer options →
 * Animator duration scale*, or the accessibility toggles that write the same setting, is
 * Android's only system-wide "I don't want animation" signal.
 *
 * In tests it is the difference between a capture and a hang. Roborazzi drives a frozen
 * clock and waits for idle; a `rememberInfiniteTransition` never reports idle at any clock
 * setting, so anything looping must be gated on this rather than on a duration.
 *
 * Gate *decorative* motion only — looping illustrations, shimmer. Functional motion that
 * communicates what just happened (a row leaving the list, a screen arriving) is already
 * scaled to zero by the platform when the user asks for that.
 */
val LocalReducedMotion = staticCompositionLocalOf { false }

/** Reads the platform animator duration scale. `0` means the user has turned animation off. */
@Composable
fun rememberSystemReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}
