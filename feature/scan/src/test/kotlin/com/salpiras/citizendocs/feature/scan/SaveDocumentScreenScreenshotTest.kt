package com.salpiras.citizendocs.feature.scan

import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.salpiras.citizendocs.core.designsystem.theme.CitizenDocsTheme
import com.salpiras.citizendocs.core.testing.CitizenDocsRoborazziOptions
import com.salpiras.citizendocs.core.ui.UiText
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-normal-long-notround-any-420dpi-keyshidden-nonav")
class SaveDocumentScreenScreenshotTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun capture(name: String, darkTheme: Boolean, state: SaveDocumentUiState) {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            CitizenDocsTheme(darkTheme = darkTheme, dynamicColor = false) {
                SaveDocumentScreen(
                    state = state,
                    onEvent = {},
                    snackbarHostState = remember { SnackbarHostState() },
                )
            }
        }
        composeTestRule.mainClock.advanceTimeBy(SETTLE_MILLIS)
        composeTestRule
            .onRoot()
            .captureRoboImage("src/test/screenshots/$name.png", CitizenDocsRoborazziOptions)
    }

    private fun state(title: String = "Tax return 2025", titleError: UiText? = null, isSaving: Boolean = false) =
        SaveDocumentUiState(
            title = title,
            documentDate = LocalDate(2026, 1, 12),
            pageCount = 3,
            titleError = titleError,
            isSaving = isSaving,
        )

    @Test
    fun filled_light() = capture("SaveDocumentScreen_filled_light", darkTheme = false, state = state())

    @Test
    fun filled_dark() = capture("SaveDocumentScreen_filled_dark", darkTheme = true, state = state())

    @Test
    fun blankTitleError_light() = capture(
        "SaveDocumentScreen_titleError_light",
        darkTheme = false,
        state = state(title = "", titleError = UiText.Res(R.string.scan_title_blank)),
    )

    // The in-flight saving state is deliberately not screenshotted. Its only visual is a
    // CircularProgressIndicator, whose rotation phase is not reproducible across machines
    // even with a frozen test clock — the golden would be a coin flip. SaveDocumentScreenTest
    // asserts the observable behaviour instead: the Save button is disabled while saving.

    private companion object {
        const val SETTLE_MILLIS = 500L
    }
}
