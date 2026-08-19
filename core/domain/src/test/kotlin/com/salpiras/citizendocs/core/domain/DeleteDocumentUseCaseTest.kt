package com.salpiras.citizendocs.core.domain

import com.google.common.truth.Truth.assertThat
import com.salpiras.citizendocs.core.testing.FakeDocumentsRepository
import com.salpiras.citizendocs.core.testing.TestDocuments
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

class DeleteDocumentUseCaseTest {
    private val repository = FakeDocumentsRepository()
    private val deleteDocument = DeleteDocumentUseCase(repository)

    @Test
    fun `deletes a document`() = runTest {
        repository.seed(TestDocuments.taxReturn, TestDocuments.passport)

        val result = deleteDocument(TestDocuments.taxReturn.id)

        assertThat(result).isEqualTo(DeleteResult.Deleted)
        assertThat(repository.current().map { it.title }).containsExactly("Passport")
    }

    @Test
    fun `reports a repository failure`() = runTest {
        repository.seed(TestDocuments.taxReturn)
        repository.mutationFailure = IOException("read-only filesystem")

        val result = deleteDocument(TestDocuments.taxReturn.id)

        assertThat(result).isInstanceOf(DeleteResult.Failed::class.java)
        assertThat(repository.current()).hasSize(1)
    }
}
