package com.salpiras.citizendocs.feature.documents

import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.salpiras.citizendocs.core.designsystem.theme.CitizenDocsTheme
import com.salpiras.citizendocs.core.model.DocumentId
import com.salpiras.citizendocs.core.testing.CitizenDocsRoborazziOptions
import com.salpiras.citizendocs.core.ui.DocumentUiModel
import com.salpiras.citizendocs.core.ui.UiText
import com.salpiras.citizendocs.feature.documents.DocumentsUiState.Content
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Goldens live in `src/test/screenshots` next to the code they cover.
 *
 * `dynamicColor = false` is essential: with it on, the palette would follow the host
 * wallpaper and every golden would be machine-dependent. See [CitizenDocsRoborazziOptions]
 * for why comparison is not pixel-exact.
 *
 * Record with `./gradlew recordRoborazziDebug`, verify with `./gradlew verifyRoborazziDebug`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-normal-long-notround-any-420dpi-keyshidden-nonav")
class DocumentsScreenScreenshotTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val documents =
        persistentListOf(
            DocumentUiModel(DocumentId(1), "Tax return 2025", "12 Jan 2026", 3, 248_000),
            DocumentUiModel(DocumentId(2), "Passport", "30 Jun 2025", 1, 96_500),
            DocumentUiModel(DocumentId(3), "Tenancy agreement", "3 Nov 2024", 12, 1_340_000),
        )

    private fun capture(name: String, darkTheme: Boolean, content: @Composable () -> Unit) {
        // Dialog scrims and progress indicators animate forever, so the default auto-advancing
        // clock never reports idle. Freeze it and step a fixed amount so captures are also
        // deterministic frame-to-frame.
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            CitizenDocsTheme(darkTheme = darkTheme, dynamicColor = false) { content() }
        }
        composeTestRule.mainClock.advanceTimeBy(SETTLE_MILLIS)
        composeTestRule
            .onRoot()
            .captureRoboImage("src/test/screenshots/$name.png", CitizenDocsRoborazziOptions)
    }

    private fun screen(state: DocumentsUiState): @Composable () -> Unit = {
        DocumentsScreen(
            state = state,
            onEvent = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }

    @Test
    fun empty_light() = capture(
        "DocumentsScreen_empty_light",
        darkTheme = false,
        content = screen(DocumentsUiState(content = Content.Empty)),
    )

    @Test
    fun empty_dark() = capture(
        "DocumentsScreen_empty_dark",
        darkTheme = true,
        content = screen(DocumentsUiState(content = Content.Empty)),
    )

    @Test
    fun populated_light() = capture(
        "DocumentsScreen_populated_light",
        darkTheme = false,
        content = screen(DocumentsUiState(content = Content.Documents(documents))),
    )

    @Test
    fun populated_dark() = capture(
        "DocumentsScreen_populated_dark",
        darkTheme = true,
        content = screen(DocumentsUiState(content = Content.Documents(documents))),
    )

    @Test
    fun loading_light() = capture(
        "DocumentsScreen_loading_light",
        darkTheme = false,
        content = screen(DocumentsUiState(content = Content.Loading)),
    )

    @Test
    fun error_light() = capture(
        "DocumentsScreen_error_light",
        darkTheme = false,
        content =
        screen(
            DocumentsUiState(content = Content.Error(UiText.Res(R.string.documents_load_failed))),
        ),
    )

    // The rename dialog is deliberately not screenshotted. AlertDialog renders into its own
    // window, and under Robolectric that window's scrim animation never reports idle, so the
    // capture times out regardless of the test clock settings. The dialog is covered instead
    // by DocumentsScreenTest (behaviour) and the @PreviewLightDark previews in RenameDialog.kt.

    private companion object {
        const val SETTLE_MILLIS = 500L
    }
}
