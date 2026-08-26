package com.salpiras.citizendocs.feature.documents

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.salpiras.citizendocs.core.domain.DeleteDocumentUseCase
import com.salpiras.citizendocs.core.domain.ExportDocumentsUseCase
import com.salpiras.citizendocs.core.domain.RenameDocumentUseCase
import com.salpiras.citizendocs.core.testing.FakeDocumentsRepository
import com.salpiras.citizendocs.core.testing.FixedClock
import com.salpiras.citizendocs.core.testing.MainDispatcherRule
import com.salpiras.citizendocs.core.testing.TestDocuments
import com.salpiras.citizendocs.core.ui.UiText
import com.salpiras.citizendocs.feature.documents.DocumentsUiState.Content
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import kotlin.time.Instant

// advanceUntilIdle drives the ViewModel's search debounce through virtual time.
@OptIn(ExperimentalCoroutinesApi::class)
class DocumentsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeDocumentsRepository()

    private fun viewModel() = DocumentsViewModel(
        repository = repository,
        renameDocument = RenameDocumentUseCase(repository),
        deleteDocument = DeleteDocumentUseCase(repository),
        exportDocuments = ExportDocumentsUseCase(repository),
        clock = FixedClock(Instant.parse("2026-08-19T10:15:30Z")),
        timeZone = TimeZone.UTC,
    )

    /** Flattens the month sections back to titles, in display order. */
    private fun Content.titles(): List<String> =
        (this as Content.Documents).groups.flatMap { group -> group.documents.map { it.title } }

    @Test
    fun `shows the empty state when there are no documents`() = runTest {
        viewModel().state.test {
            assertThat(awaitItem().content).isEqualTo(Content.Empty)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `shows stored documents in repository order`() = runTest {
        repository.seed(TestDocuments.taxReturn, TestDocuments.passport)
        val viewModel = viewModel()
        advanceUntilIdle()

        assertThat(viewModel.state.value.content.titles())
            .containsExactly("Tax return 2025", "Passport")
            .inOrder()
    }

    @Test
    fun `groups documents by month, newest first`() = runTest {
        repository.seed(TestDocuments.taxReturn, TestDocuments.passport, TestDocuments.tenancyAgreement)
        val viewModel = viewModel()
        advanceUntilIdle()

        val groups = (viewModel.state.value.content as Content.Documents).groups
        assertThat(groups.map { it.key }).containsExactly("2026-01", "2025-06", "2024-11").inOrder()
    }

    @Test
    fun `moves from empty to populated when a document appears`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        assertThat(viewModel.state.value.content).isEqualTo(Content.Empty)

        repository.seed(TestDocuments.taxReturn)
        advanceUntilIdle()

        assertThat(viewModel.state.value.content).isInstanceOf(Content.Documents::class.java)
    }

    @Test
    fun `scan click emits the launch scanner effect`() = runTest {
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onEvent(DocumentsEvent.ScanClicked)
            assertThat(awaitItem()).isEqualTo(DocumentsEffect.LaunchScanner)
        }
    }

    @Test
    fun `tapping a document emits its content uri`() = runTest {
        repository.seed(TestDocuments.taxReturn)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onEvent(DocumentsEvent.DocumentClicked(TestDocuments.taxReturn.id))

            val effect = awaitItem() as DocumentsEffect.OpenDocument
            assertThat(effect.contentUri).isEqualTo("content://test/Tax_return_2025.pdf")
        }
    }

    // --- Search -------------------------------------------------------------------------

    @Test
    fun `the search field updates immediately, before the debounce elapses`() = runTest {
        repository.seed(TestDocuments.taxReturn)
        val viewModel = viewModel()

        viewModel.onEvent(DocumentsEvent.SearchQueryChanged("pass"))

        assertThat(viewModel.state.value.searchQuery).isEqualTo("pass")
    }

    @Test
    fun `searching narrows the list`() = runTest {
        repository.seed(TestDocuments.taxReturn, TestDocuments.passport)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onEvent(DocumentsEvent.SearchQueryChanged("passport"))
        advanceUntilIdle()

        assertThat(viewModel.state.value.content.titles()).containsExactly("Passport")
    }

    @Test
    fun `search results stay grouped by month`() = runTest {
        repository.seed(TestDocuments.taxReturn, TestDocuments.passport)
        val viewModel = viewModel()

        viewModel.onEvent(DocumentsEvent.SearchQueryChanged("passport"))
        advanceUntilIdle()

        val groups = (viewModel.state.value.content as Content.Documents).groups
        assertThat(groups.map { it.key }).containsExactly("2025-06")
    }

    // "You have no documents" is the wrong thing to say to someone with two who mistyped.
    @Test
    fun `no matches is reported separately from having no documents`() = runTest {
        repository.seed(TestDocuments.taxReturn, TestDocuments.passport)
        val viewModel = viewModel()

        viewModel.onEvent(DocumentsEvent.SearchQueryChanged("mortgage"))
        advanceUntilIdle()

        assertThat(viewModel.state.value.content).isEqualTo(Content.NoResults("mortgage"))
    }

    @Test
    fun `clearing the query restores the full list`() = runTest {
        repository.seed(TestDocuments.taxReturn, TestDocuments.passport)
        val viewModel = viewModel()

        viewModel.onEvent(DocumentsEvent.SearchQueryChanged("passport"))
        advanceUntilIdle()
        viewModel.onEvent(DocumentsEvent.SearchQueryChanged(""))
        advanceUntilIdle()

        assertThat(viewModel.state.value.content.titles()).hasSize(2)
    }

    @Test
    fun `closing search clears the query and the results`() = runTest {
        repository.seed(TestDocuments.taxReturn, TestDocuments.passport)
        val viewModel = viewModel()
        viewModel.onEvent(DocumentsEvent.SearchOpened)
        viewModel.onEvent(DocumentsEvent.SearchQueryChanged("passport"))
        advanceUntilIdle()

        viewModel.onEvent(DocumentsEvent.SearchClosed)
        advanceUntilIdle()

        assertThat(viewModel.state.value.isSearchActive).isFalse()
        assertThat(viewModel.state.value.searchQuery).isEmpty()
        assertThat(viewModel.state.value.content.titles()).hasSize(2)
    }

    // --- Grouping -----------------------------------------------------------------------

    @Test
    fun `toggling a group collapses it, and toggling again expands it`() = runTest {
        repository.seed(TestDocuments.taxReturn)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onEvent(DocumentsEvent.GroupToggled("2026-01"))
        assertThat(viewModel.state.value.collapsedGroups).containsExactly("2026-01")

        viewModel.onEvent(DocumentsEvent.GroupToggled("2026-01"))
        assertThat(viewModel.state.value.collapsedGroups).isEmpty()
    }

    // --- Export -------------------------------------------------------------------------

    @Test
    fun `export asks for a destination with a dated file name`() = runTest {
        repository.seed(TestDocuments.taxReturn)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onEvent(DocumentsEvent.ExportClicked)

            val effect = awaitItem() as DocumentsEffect.LaunchExportPicker
            assertThat(effect.suggestedFileName).isEqualTo("citizen-docs-2026-08-19.zip")
        }
    }

    @Test
    fun `export with nothing saved reports it rather than opening a picker`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onEvent(DocumentsEvent.ExportClicked)

            val effect = awaitItem() as DocumentsEffect.ShowMessage
            assertThat(effect.message).isEqualTo(UiText.Res(R.string.documents_export_nothing))
        }
    }

    @Test
    fun `choosing a destination exports every document`() = runTest {
        repository.seed(TestDocuments.taxReturn, TestDocuments.passport)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onEvent(DocumentsEvent.ExportDestinationChosen("content://out.zip"))
        advanceUntilIdle()

        assertThat(repository.exported?.map { it.title })
            .containsExactly("Tax return 2025", "Passport")
    }

    // The whole point of scoping export to the list: a search narrows the archive.
    @Test
    fun `exporting while searching archives only the matches`() = runTest {
        repository.seed(TestDocuments.taxReturn, TestDocuments.passport)
        val viewModel = viewModel()
        viewModel.onEvent(DocumentsEvent.SearchQueryChanged("passport"))
        advanceUntilIdle()

        viewModel.onEvent(DocumentsEvent.ExportDestinationChosen("content://out.zip"))
        advanceUntilIdle()

        assertThat(repository.exported?.map { it.title }).containsExactly("Passport")
    }

    @Test
    fun `a successful export reports how many documents it wrote`() = runTest {
        repository.seed(TestDocuments.taxReturn, TestDocuments.passport)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onEvent(DocumentsEvent.ExportDestinationChosen("content://out.zip"))

            val effect = awaitItem() as DocumentsEffect.ShowMessage
            assertThat(effect.message).isEqualTo(UiText.Res(R.string.documents_export_done, listOf(2)))
        }
        assertThat(viewModel.state.value.isExporting).isFalse()
    }

    @Test
    fun `a failed export is reported and clears the exporting flag`() = runTest {
        repository.seed(TestDocuments.taxReturn)
        repository.exportFailure = IOException("no space left on device")
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onEvent(DocumentsEvent.ExportDestinationChosen("content://out.zip"))

            val effect = awaitItem() as DocumentsEffect.ShowMessage
            assertThat(effect.message).isEqualTo(UiText.Res(R.string.documents_export_failed))
        }
        assertThat(viewModel.state.value.isExporting).isFalse()
    }

    @Test
    fun `cancelling the picker leaves nothing exported`() = runTest {
        repository.seed(TestDocuments.taxReturn)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onEvent(DocumentsEvent.ExportCancelled)
        advanceUntilIdle()

        assertThat(repository.exported).isNull()
        assertThat(viewModel.state.value.isExporting).isFalse()
    }

    // --- Delete and rename ---------------------------------------------------------------

    @Test
    fun `deleting a document removes it and confirms`() = runTest {
        repository.seed(TestDocuments.taxReturn)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onEvent(DocumentsEvent.DeleteClicked(TestDocuments.taxReturn.id))
            assertThat(awaitItem()).isInstanceOf(DocumentsEffect.ShowMessage::class.java)
        }
        assertThat(repository.current()).isEmpty()
    }

    @Test
    fun `a failed delete reports the failure and keeps the document`() = runTest {
        repository.seed(TestDocuments.taxReturn)
        repository.mutationFailure = IOException("read-only filesystem")
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onEvent(DocumentsEvent.DeleteClicked(TestDocuments.taxReturn.id))

            val effect = awaitItem() as DocumentsEffect.ShowMessage
            assertThat(effect.message).isEqualTo(UiText.Res(R.string.documents_delete_failed))
        }
        assertThat(repository.current()).hasSize(1)
    }

    @Test
    fun `rename opens a dialog prefilled with the current title`() = runTest {
        repository.seed(TestDocuments.taxReturn)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onEvent(DocumentsEvent.RenameClicked(TestDocuments.taxReturn.id))

        assertThat(viewModel.state.value.rename?.title).isEqualTo("Tax return 2025")
    }

    @Test
    fun `confirming a rename applies it and closes the dialog`() = runTest {
        repository.seed(TestDocuments.taxReturn)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onEvent(DocumentsEvent.RenameClicked(TestDocuments.taxReturn.id))
        viewModel.onEvent(DocumentsEvent.RenameTitleChanged("Self assessment"))
        viewModel.onEvent(DocumentsEvent.RenameConfirmed)
        advanceUntilIdle()

        assertThat(viewModel.state.value.rename).isNull()
        assertThat(repository.current().single().title).isEqualTo("Self assessment")
    }

    // The dialog must stay open so the user can correct the name, rather than silently
    // discarding the edit.
    @Test
    fun `renaming to a blank title keeps the dialog open with an error`() = runTest {
        repository.seed(TestDocuments.taxReturn)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onEvent(DocumentsEvent.RenameClicked(TestDocuments.taxReturn.id))
        viewModel.onEvent(DocumentsEvent.RenameTitleChanged("  "))
        viewModel.onEvent(DocumentsEvent.RenameConfirmed)
        advanceUntilIdle()

        assertThat(viewModel.state.value.rename?.error)
            .isEqualTo(UiText.Res(R.string.documents_title_blank))
        assertThat(repository.current().single().title).isEqualTo("Tax return 2025")
    }

    @Test
    fun `dismissing the rename dialog discards the edit`() = runTest {
        repository.seed(TestDocuments.taxReturn)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onEvent(DocumentsEvent.RenameClicked(TestDocuments.taxReturn.id))
        viewModel.onEvent(DocumentsEvent.RenameTitleChanged("Discarded"))
        viewModel.onEvent(DocumentsEvent.RenameDismissed)

        assertThat(viewModel.state.value.rename).isNull()
        assertThat(repository.current().single().title).isEqualTo("Tax return 2025")
    }

    // --- Arrival highlight ----------------------------------------------------------------
    //
    // runCurrent rather than advanceUntilIdle: the highlight clears itself on a timer, and
    // advancing all of virtual time would run that timer too, so the assertion would only
    // ever see the cleared state.

    @Test
    fun `highlights a document that arrives after the list has loaded`() = runTest {
        repository.seed(TestDocuments.taxReturn)
        val viewModel = viewModel()
        advanceUntilIdle()

        repository.seed(TestDocuments.taxReturn, TestDocuments.passport)
        runCurrent()

        assertThat(viewModel.state.value.highlighted).isEqualTo(TestDocuments.passport.id)
    }

    @Test
    fun `stops highlighting after a moment`() = runTest {
        repository.seed(TestDocuments.taxReturn)
        val viewModel = viewModel()
        advanceUntilIdle()

        repository.seed(TestDocuments.taxReturn, TestDocuments.passport)
        advanceUntilIdle()

        assertThat(viewModel.state.value.highlighted).isNull()
    }

    // Otherwise opening the app would light up every row the user already had.
    @Test
    fun `highlights nothing on the first load`() = runTest {
        repository.seed(TestDocuments.taxReturn)
        val viewModel = viewModel()
        runCurrent()

        assertThat(viewModel.state.value.highlighted).isNull()
    }

    // Rows coming back because a search was cleared have not "arrived" — they were there all
    // along. This is the case that makes the unchanged-query guard necessary.
    @Test
    fun `highlights nothing when clearing a search brings a row back`() = runTest {
        repository.seed(TestDocuments.taxReturn, TestDocuments.passport)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onEvent(DocumentsEvent.SearchQueryChanged("tax"))
        advanceUntilIdle()
        assertThat(viewModel.state.value.content.titles()).containsExactly("Tax return 2025")

        viewModel.onEvent(DocumentsEvent.SearchQueryChanged(""))
        runCurrent()

        assertThat(viewModel.state.value.highlighted).isNull()
    }
}
