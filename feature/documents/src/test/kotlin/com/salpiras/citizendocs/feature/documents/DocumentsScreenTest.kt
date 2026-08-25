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
import com.salpiras.citizendocs.core.ui.DocumentGroup
import com.salpiras.citizendocs.core.ui.DocumentUiModel
import com.salpiras.citizendocs.feature.documents.DocumentsUiState.Content
import com.salpiras.citizendocs.feature.documents.DocumentsUiState.RenameState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
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

    private val groups = persistentListOf(
        DocumentGroup(
            key = "2026-01",
            label = "January 2026",
            documents = persistentListOf(
                DocumentUiModel(DocumentId(1), "Tax return 2025", "12 Jan 2026", 3, 248_000),
            ),
        ),
        DocumentGroup(
            key = "2025-06",
            label = "June 2025",
            documents = persistentListOf(
                DocumentUiModel(DocumentId(2), "Passport", "30 Jun 2025", 1, 96_500),
            ),
        ),
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

    private fun populated(
        isSearchActive: Boolean = false,
        searchQuery: String = "",
        collapsed: Set<String> = emptySet(),
    ) = DocumentsUiState(
        content = Content.Documents(groups),
        isSearchActive = isSearchActive,
        searchQuery = searchQuery,
        collapsedGroups = persistentSetOf<String>().addingAll(collapsed),
    )

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    @Test
    fun emptyState_showsGuidance() {
        setContent(DocumentsUiState(content = Content.Empty))

        composeTestRule.onNodeWithText(string(R.string.documents_empty_title)).assertIsDisplayed()
    }

    @Test
    fun populatedState_showsEveryDocumentTitle() {
        setContent(populated())

        composeTestRule.onNodeWithText("Tax return 2025", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Passport", substring = true).assertIsDisplayed()
    }

    // Guards the bug where supportingContent returned a String instead of emitting a Text,
    // so the date silently never rendered.
    @Test
    fun populatedState_showsTheDocumentDate() {
        setContent(populated())

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
        setContent(populated())

        composeTestRule.onNodeWithText("Tax return 2025", substring = true).performClick()

        assertThat(events).containsExactly(DocumentsEvent.DocumentClicked(DocumentId(1)))
    }

    @Test
    fun overflowMenu_offersRenameAndDelete() {
        setContent(populated())

        composeTestRule
            .onNodeWithContentDescription(string(R.string.documents_more_options).format("Tax return 2025"))
            .performClick()

        composeTestRule.onNodeWithText(string(R.string.documents_rename)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.documents_delete)).assertIsDisplayed()
    }

    @Test
    fun renameDialog_editingTitleEmitsTheChange() {
        setContent(
            populated().copy(rename = RenameState(id = DocumentId(1), title = "Tax return 2025")),
        )

        composeTestRule
            .onNodeWithText(string(R.string.documents_title_label))
            .performTextReplacement("Self assessment")

        assertThat(events).contains(DocumentsEvent.RenameTitleChanged("Self assessment"))
    }

    @Test
    fun renameDialog_confirmEmitsRenameConfirmed() {
        setContent(
            populated().copy(rename = RenameState(id = DocumentId(1), title = "Tax return 2025")),
        )

        composeTestRule.onNodeWithText(string(R.string.documents_save)).performClick()

        assertThat(events).containsExactly(DocumentsEvent.RenameConfirmed)
    }

    // --- Grouping -----------------------------------------------------------------------

    @Test
    fun monthHeadersAreShown() {
        setContent(populated())

        composeTestRule.onNodeWithText("January 2026").assertIsDisplayed()
        composeTestRule.onNodeWithText("June 2025").assertIsDisplayed()
    }

    @Test
    fun tappingAMonthHeader_emitsGroupToggled() {
        setContent(populated())

        composeTestRule
            .onNodeWithContentDescription(string(R.string.documents_group_collapse).format("January 2026"))
            .performClick()

        assertThat(events).containsExactly(DocumentsEvent.GroupToggled("2026-01"))
    }

    @Test
    fun aCollapsedGroupHidesItsDocumentsButKeepsItsHeader() {
        setContent(populated(collapsed = setOf("2026-01")))

        composeTestRule.onNodeWithText("January 2026").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tax return 2025", substring = true).assertDoesNotExist()
        // The other month is untouched.
        composeTestRule.onNodeWithText("Passport", substring = true).assertIsDisplayed()
    }

    // --- Search -------------------------------------------------------------------------

    @Test
    fun tappingSearch_emitsSearchOpened() {
        setContent(populated())

        composeTestRule.onNodeWithContentDescription(string(R.string.documents_search)).performClick()

        assertThat(events).containsExactly(DocumentsEvent.SearchOpened)
    }

    @Test
    fun typingInSearch_emitsTheQuery() {
        setContent(populated(isSearchActive = true))

        composeTestRule
            .onNodeWithText(string(R.string.documents_search_hint))
            .performTextReplacement("passport")

        assertThat(events).contains(DocumentsEvent.SearchQueryChanged("passport"))
    }

    @Test
    fun clearingSearch_emitsAnEmptyQuery() {
        setContent(populated(isSearchActive = true, searchQuery = "passport"))

        composeTestRule.onNodeWithContentDescription(string(R.string.documents_search_clear)).performClick()

        assertThat(events).containsExactly(DocumentsEvent.SearchQueryChanged(""))
    }

    @Test
    fun closingSearch_emitsSearchClosed() {
        setContent(populated(isSearchActive = true, searchQuery = "passport"))

        composeTestRule.onNodeWithContentDescription(string(R.string.documents_search_close)).performClick()

        assertThat(events).containsExactly(DocumentsEvent.SearchClosed)
    }

    @Test
    fun noResults_namesTheQueryRatherThanClaimingThereAreNoDocuments() {
        setContent(DocumentsUiState(content = Content.NoResults("mortgage"), isSearchActive = true))

        composeTestRule.onNodeWithText(string(R.string.documents_no_results_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText("mortgage", substring = true).assertIsDisplayed()
    }

    // --- Export -------------------------------------------------------------------------

    @Test
    fun exportMenuItem_emitsExportClicked() {
        setContent(populated())

        composeTestRule
            .onNodeWithContentDescription(string(R.string.documents_more_options_screen))
            .performClick()
        composeTestRule.onNodeWithText(string(R.string.documents_export)).performClick()

        assertThat(events).containsExactly(DocumentsEvent.ExportClicked)
    }

    // Replaces a screenshot of the exporting state: an in-flight spinner is not a stable
    // golden, but "you cannot start a second export" is exactly what must hold.
    @Test
    fun exportIsUnavailableWhileAnExportIsRunning() {
        setContent(populated().copy(isExporting = true))

        composeTestRule
            .onNodeWithContentDescription(string(R.string.documents_more_options_screen))
            .performClick()
        composeTestRule.onNodeWithText(string(R.string.documents_exporting)).performClick()

        assertThat(events).isEmpty()
    }
}
