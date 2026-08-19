package com.salpiras.citizendocs.feature.documents

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.salpiras.citizendocs.core.domain.DeleteDocumentUseCase
import com.salpiras.citizendocs.core.domain.RenameDocumentUseCase
import com.salpiras.citizendocs.core.testing.FakeDocumentsRepository
import com.salpiras.citizendocs.core.testing.MainDispatcherRule
import com.salpiras.citizendocs.core.testing.TestDocuments
import com.salpiras.citizendocs.core.ui.UiText
import com.salpiras.citizendocs.feature.documents.DocumentsUiState.Content
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class DocumentsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeDocumentsRepository()

    private fun viewModel() = DocumentsViewModel(
        repository = repository,
        renameDocument = RenameDocumentUseCase(repository),
        deleteDocument = DeleteDocumentUseCase(repository),
    )

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

        viewModel().state.test {
            val content = awaitItem().content as Content.Documents
            assertThat(content.documents.map { it.title })
                .containsExactly("Tax return 2025", "Passport")
                .inOrder()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `moves from empty to populated when a document appears`() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            assertThat(awaitItem().content).isEqualTo(Content.Empty)

            repository.seed(TestDocuments.taxReturn)

            assertThat(awaitItem().content).isInstanceOf(Content.Documents::class.java)
            cancelAndIgnoreRemainingEvents()
        }
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

        viewModel.effects.test {
            viewModel.onEvent(DocumentsEvent.DocumentClicked(TestDocuments.taxReturn.id))

            val effect = awaitItem() as DocumentsEffect.OpenDocument
            assertThat(effect.contentUri).isEqualTo("content://test/Tax_return_2025.pdf")
        }
    }

    @Test
    fun `deleting a document removes it and confirms`() = runTest {
        repository.seed(TestDocuments.taxReturn)
        val viewModel = viewModel()

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

        viewModel.onEvent(DocumentsEvent.RenameClicked(TestDocuments.taxReturn.id))

        val rename = viewModel.state.value.rename
        assertThat(rename).isNotNull()
        assertThat(rename!!.title).isEqualTo("Tax return 2025")
    }

    @Test
    fun `confirming a rename applies it and closes the dialog`() = runTest {
        repository.seed(TestDocuments.taxReturn)
        val viewModel = viewModel()

        viewModel.onEvent(DocumentsEvent.RenameClicked(TestDocuments.taxReturn.id))
        viewModel.onEvent(DocumentsEvent.RenameTitleChanged("Self assessment"))
        viewModel.onEvent(DocumentsEvent.RenameConfirmed)

        assertThat(viewModel.state.value.rename).isNull()
        assertThat(repository.current().single().title).isEqualTo("Self assessment")
    }

    // The dialog must stay open so the user can correct the name, rather than silently
    // discarding the edit.
    @Test
    fun `renaming to a blank title keeps the dialog open with an error`() = runTest {
        repository.seed(TestDocuments.taxReturn)
        val viewModel = viewModel()

        viewModel.onEvent(DocumentsEvent.RenameClicked(TestDocuments.taxReturn.id))
        viewModel.onEvent(DocumentsEvent.RenameTitleChanged("  "))
        viewModel.onEvent(DocumentsEvent.RenameConfirmed)

        val rename = viewModel.state.value.rename
        assertThat(rename).isNotNull()
        assertThat(rename!!.error).isEqualTo(UiText.Res(R.string.documents_title_blank))
        assertThat(repository.current().single().title).isEqualTo("Tax return 2025")
    }

    @Test
    fun `editing the title clears a previous error`() = runTest {
        repository.seed(TestDocuments.taxReturn)
        val viewModel = viewModel()

        viewModel.onEvent(DocumentsEvent.RenameClicked(TestDocuments.taxReturn.id))
        viewModel.onEvent(DocumentsEvent.RenameTitleChanged(""))
        viewModel.onEvent(DocumentsEvent.RenameConfirmed)
        viewModel.onEvent(DocumentsEvent.RenameTitleChanged("Something"))

        assertThat(
            viewModel.state.value.rename!!
                .error,
        ).isNull()
    }

    @Test
    fun `dismissing the rename dialog discards the edit`() = runTest {
        repository.seed(TestDocuments.taxReturn)
        val viewModel = viewModel()

        viewModel.onEvent(DocumentsEvent.RenameClicked(TestDocuments.taxReturn.id))
        viewModel.onEvent(DocumentsEvent.RenameTitleChanged("Discarded"))
        viewModel.onEvent(DocumentsEvent.RenameDismissed)

        assertThat(viewModel.state.value.rename).isNull()
        assertThat(repository.current().single().title).isEqualTo("Tax return 2025")
    }
}
