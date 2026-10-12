package com.salpiras.citizendocs.feature.backup

import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.salpiras.citizendocs.core.designsystem.theme.CitizenDocsTheme
import com.salpiras.citizendocs.core.testing.CitizenDocsRoborazziOptions
import com.salpiras.citizendocs.core.ui.UiText
import com.salpiras.citizendocs.feature.backup.BackupUiState.Working
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-normal-long-notround-any-420dpi-keyshidden-nonav")
class BackupScreenScreenshotTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun capture(name: String, darkTheme: Boolean, state: BackupUiState) {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            CitizenDocsTheme(darkTheme = darkTheme, dynamicColor = false, reducedMotion = true) {
                BackupScreen(
                    state = state,
                    onEvent = {},
                    onBack = {},
                    snackbarHostState = remember { SnackbarHostState() },
                )
            }
        }
        composeTestRule.mainClock.advanceTimeBy(SETTLE_MILLIS)
        composeTestRule
            .onRoot()
            .captureRoboImage("src/test/screenshots/$name.png", CitizenDocsRoborazziOptions)
    }

    @Test
    fun disconnected_light() = capture("BackupScreen_disconnected_light", false, BackupUiState())

    @Test
    fun disconnected_dark() = capture("BackupScreen_disconnected_dark", true, BackupUiState())

    @Test
    fun connected_light() = capture(
        "BackupScreen_connected_light",
        darkTheme = false,
        state = BackupUiState(
            isConnected = true,
            lastResult = UiText.Res(R.string.backup_done, listOf(3, 11)),
        ),
    )

    @Test
    fun connected_dark() = capture(
        "BackupScreen_connected_dark",
        darkTheme = true,
        state = BackupUiState(
            isConnected = true,
            lastResult = UiText.Res(R.string.backup_done, listOf(3, 11)),
        ),
    )

    // The in-flight spinner has no reproducible rotation phase across machines even with a
    // frozen clock, so the working states are covered by BackupScreenTest instead.

    private companion object {
        const val SETTLE_MILLIS = 500L
    }
}
