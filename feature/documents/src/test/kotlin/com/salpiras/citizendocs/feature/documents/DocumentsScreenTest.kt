package com.salpiras.citizendocs.feature.documents

import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.google.common.truth.Truth.assertThat
import com.salpiras.citizendocs.core.designsystem.theme.CitizenDocsTheme
import com.salpiras.citizendocs.core.model.DocumentId
import com.salpiras.citizendocs.core.ui.DocumentUiModel
import com.salpiras.citizendocs.feature.documents.DocumentsUiState.Content
import com.salpiras.citizendocs.feature.documents.DocumentsUiState.RenameState
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Behaviour tests against the stateless screen: given a state, does it render the right
 * thing, and do interactions emit the right events? Robolectric means these run on the JVM
 * with the rest of the unit tests — no emulator.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DocumentsScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val events = mutableListOf<DocumentsEvent>()

    private val documents =
        persistentListOf(
            DocumentUiModel(DocumentId(1), "Tax return 2025", "12 Jan 2026", 3, 248_000),
            DocumentUiModel(DocumentId(2), "Passport", "30 Jun 2025", 1, 96_500),
        )

    private fun setContent(state: DocumentsUiState) {
        composeTestRule.setContent {
            CitizenDocsTheme(dynamicColor = false) {
                DocumentsScreen(
                    state = state,
                    onEvent = events::add,
                    snackbarHostState = remember { SnackbarHostState() },
                )
            }
        }
    }

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    @Test
    fun emptyState_showsGuidance() {
        setContent(DocumentsUiState(content = Content.Empty))

        composeTestRule.onNodeWithText(string(R.string.documents_empty_title)).assertIsDisplayed()
    }

    @Test
    fun populatedState_showsEveryDocumentTitle() {
        setContent(DocumentsUiState(content = Content.Documents(documents)))

        composeTestRule.onNodeWithText("Tax return 2025", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Passport", substring = true).assertIsDisplayed()
    }

    // Guards the bug where supportingContent returned a String instead of emitting a Text,
    // so the date silently never rendered.
    @Test
    fun populatedState_showsTheDocumentDate() {
        setContent(DocumentsUiState(content = Content.Documents(documents)))

        composeTestRule.onNodeWithText("12 Jan 2026", substring = true).assertIsDisplayed()
    }

    @Test
    fun tappingTheFab_emitsScanClicked() {
        setContent(DocumentsUiState(content = Content.Empty))

        composeTestRule.onNodeWithContentDescription(string(R.string.documents_scan_action)).performClick()

        assertThat(events).containsExactly(DocumentsEvent.ScanClicked)
    }

    @Test
    fun tappingARow_emitsDocumentClickedWithThatId() {
        setContent(DocumentsUiState(content = Content.Documents(documents)))

        composeTestRule.onNodeWithText("Tax return 2025", substring = true).performClick()

        assertThat(events).containsExactly(DocumentsEvent.DocumentClicked(DocumentId(1)))
    }

    @Test
    fun overflowMenu_offersRenameAndDelete() {
        setContent(DocumentsUiState(content = Content.Documents(documents)))

        composeTestRule
            .onNodeWithContentDescription(string(R.string.documents_more_options).format("Tax return 2025"))
            .performClick()

        composeTestRule.onNodeWithText(string(R.string.documents_rename)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.documents_delete)).assertIsDisplayed()
    }

    @Test
    fun renameDialog_editingTitleEmitsTheChange() {
        setContent(
            DocumentsUiState(
                content = Content.Documents(documents),
                rename = RenameState(id = DocumentId(1), title = "Tax return 2025"),
            ),
        )

        composeTestRule
            .onNodeWithText(string(R.string.documents_title_label))
            .performTextReplacement("Self assessment")

        assertThat(events).contains(DocumentsEvent.RenameTitleChanged("Self assessment"))
    }

    @Test
    fun renameDialog_confirmEmitsRenameConfirmed() {
        setContent(
            DocumentsUiState(
                content = Content.Documents(documents),
                rename = RenameState(id = DocumentId(1), title = "Tax return 2025"),
            ),
        )

        composeTestRule.onNodeWithText(string(R.string.documents_save)).performClick()

        assertThat(events).containsExactly(DocumentsEvent.RenameConfirmed)
    }
}
