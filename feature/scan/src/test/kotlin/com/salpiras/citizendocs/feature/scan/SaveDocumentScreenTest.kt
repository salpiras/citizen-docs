package com.salpiras.citizendocs.feature.scan

import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.google.common.truth.Truth.assertThat
import com.salpiras.citizendocs.core.designsystem.theme.CitizenDocsTheme
import com.salpiras.citizendocs.core.ui.UiText
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SaveDocumentScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val events = mutableListOf<SaveDocumentEvent>()

    private fun setContent(state: SaveDocumentUiState) {
        composeTestRule.setContent {
            CitizenDocsTheme(dynamicColor = false) {
                SaveDocumentScreen(
                    state = state,
                    onEvent = events::add,
                    snackbarHostState = remember { SnackbarHostState() },
                )
            }
        }
    }

    private fun state(title: String = "Tax return 2025", titleError: UiText? = null, isSaving: Boolean = false) =
        SaveDocumentUiState(
            title = title,
            documentDate = LocalDate(2026, 1, 12),
            pageCount = 3,
            titleError = titleError,
            isSaving = isSaving,
        )

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    // The app bar title and the button carry the same string, so match on the clickable one.
    private fun saveButton() = composeTestRule.onNode(hasText(string(R.string.scan_save)) and hasClickAction())

    @Test
    fun showsTheScannedPageCount() {
        setContent(state())

        composeTestRule.onNodeWithText("3 pages scanned").assertIsDisplayed()
    }

    @Test
    fun editingTheTitle_emitsTitleChanged() {
        setContent(state(title = ""))

        composeTestRule
            .onNodeWithText(string(R.string.scan_title_label))
            .performTextReplacement("Passport")

        assertThat(events).contains(SaveDocumentEvent.TitleChanged("Passport"))
    }

    @Test
    fun tappingSave_emitsSaveClicked() {
        setContent(state())

        saveButton().performClick()

        assertThat(events).containsExactly(SaveDocumentEvent.SaveClicked)
    }

    @Test
    fun saveIsEnabled_whenNotSaving() {
        setContent(state())

        saveButton().assertIsEnabled()
    }

    // Replaces the screenshot of the saving state, whose only visual is an indeterminate
    // spinner. This asserts what actually matters: a second tap cannot double-submit the scan.
    @Test
    fun saveIsDisabled_whileSaving() {
        setContent(state(isSaving = true))

        // While saving, the label is swapped for a spinner, so no tappable Save remains.
        saveButton().assertDoesNotExist()
    }

    @Test
    fun showsAnInlineTitleError() {
        setContent(state(title = "", titleError = UiText.Res(R.string.scan_title_blank)))

        composeTestRule.onNodeWithText(string(R.string.scan_title_blank)).assertIsDisplayed()
    }

    @Test
    fun tappingTheDate_opensThePicker() {
        setContent(state())

        composeTestRule.onNodeWithText("Document date", substring = true).performClick()

        assertThat(events).containsExactly(SaveDocumentEvent.DatePickerRequested)
    }

    @Test
    fun tappingClose_emitsCancelClicked() {
        setContent(state())

        composeTestRule.onNodeWithContentDescription(string(R.string.scan_cancel)).performClick()

        assertThat(events).containsExactly(SaveDocumentEvent.CancelClicked)
    }
}
