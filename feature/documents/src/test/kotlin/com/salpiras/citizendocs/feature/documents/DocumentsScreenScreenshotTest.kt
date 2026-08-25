package com.salpiras.citizendocs.feature.documents

import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.salpiras.citizendocs.core.designsystem.theme.CitizenDocsTheme
import com.salpiras.citizendocs.core.testing.CitizenDocsRoborazziOptions
import com.salpiras.citizendocs.core.ui.UiText
import com.salpiras.citizendocs.feature.documents.DocumentsUiState.Content
import kotlinx.collections.immutable.persistentSetOf
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

    private fun capture(name: String, darkTheme: Boolean, state: DocumentsUiState) {
        // Progress indicators animate forever, so the default auto-advancing clock never
        // reports idle. Freeze it and step a fixed amount for frame-to-frame determinism.
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            CitizenDocsTheme(darkTheme = darkTheme, dynamicColor = false) {
                DocumentsScreen(
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

    private fun populated() = DocumentsUiState(content = Content.Documents(previewGroups()))

    @Test
    fun empty_light() = capture(
        "DocumentsScreen_empty_light",
        darkTheme = false,
        state = DocumentsUiState(content = Content.Empty),
    )

    @Test
    fun empty_dark() = capture(
        "DocumentsScreen_empty_dark",
        darkTheme = true,
        state = DocumentsUiState(content = Content.Empty),
    )

    @Test
    fun grouped_light() = capture("DocumentsScreen_grouped_light", darkTheme = false, state = populated())

    @Test
    fun grouped_dark() = capture("DocumentsScreen_grouped_dark", darkTheme = true, state = populated())

    @Test
    fun collapsedGroup_light() = capture(
        "DocumentsScreen_collapsedGroup_light",
        darkTheme = false,
        state = populated().copy(collapsedGroups = persistentSetOf("2025-06")),
    )

    @Test
    fun searchActive_light() = capture(
        "DocumentsScreen_searchActive_light",
        darkTheme = false,
        state = populated().copy(isSearchActive = true, searchQuery = "tax"),
    )

    @Test
    fun noResults_light() = capture(
        "DocumentsScreen_noResults_light",
        darkTheme = false,
        state = DocumentsUiState(
            content = Content.NoResults("mortgage"),
            isSearchActive = true,
            searchQuery = "mortgage",
        ),
    )

    @Test
    fun loading_light() = capture(
        "DocumentsScreen_loading_light",
        darkTheme = false,
        state = DocumentsUiState(content = Content.Loading),
    )

    @Test
    fun error_light() = capture(
        "DocumentsScreen_error_light",
        darkTheme = false,
        state = DocumentsUiState(
            content = Content.Error(UiText.Res(R.string.documents_load_failed)),
        ),
    )

    // The rename dialog is deliberately not screenshotted. AlertDialog renders into its own
    // window, and under Robolectric that window's scrim animation never reports idle, so the
    // capture times out regardless of the test clock settings. The dialog is covered instead
    // by DocumentsScreenTest (behaviour) and the @PreviewLightDark previews in RenameDialog.kt.
    //
    // The exporting state is likewise behaviour-tested rather than screenshotted.

    private companion object {
        const val SETTLE_MILLIS = 500L
    }
}
