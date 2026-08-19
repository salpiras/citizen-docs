package com.salpiras.citizendocs.core.domain

import com.google.common.truth.Truth.assertThat
import com.salpiras.citizendocs.core.model.TitleValidation
import com.salpiras.citizendocs.core.testing.FakeDocumentsRepository
import com.salpiras.citizendocs.core.testing.TestDocuments
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

class RenameDocumentUseCaseTest {
    private val repository = FakeDocumentsRepository()
    private val renameDocument = RenameDocumentUseCase(repository)

    @Test
    fun `renames a document`() = runTest {
        repository.seed(TestDocuments.taxReturn)

        val result = renameDocument(TestDocuments.taxReturn.id, "Self assessment")

        assertThat(result).isEqualTo(RenameResult.Renamed)
        assertThat(repository.current().single().title).isEqualTo("Self assessment")
    }

    @Test
    fun `rejects a blank title and leaves the document untouched`() = runTest {
        repository.seed(TestDocuments.taxReturn)

        val result = renameDocument(TestDocuments.taxReturn.id, "")

        assertThat(result).isEqualTo(RenameResult.InvalidTitle(TitleValidation.Blank))
        assertThat(repository.current().single().title).isEqualTo("Tax return 2025")
    }

    @Test
    fun `reports a repository failure`() = runTest {
        repository.seed(TestDocuments.taxReturn)
        repository.mutationFailure = IOException("read-only filesystem")

        val result = renameDocument(TestDocuments.taxReturn.id, "Self assessment")

        assertThat(result).isInstanceOf(RenameResult.Failed::class.java)
    }
}
