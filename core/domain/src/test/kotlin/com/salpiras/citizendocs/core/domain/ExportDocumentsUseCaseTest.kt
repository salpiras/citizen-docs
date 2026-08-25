package com.salpiras.citizendocs.core.domain

import com.google.common.truth.Truth.assertThat
import com.salpiras.citizendocs.core.testing.FakeDocumentsRepository
import com.salpiras.citizendocs.core.testing.TestDocuments
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

class ExportDocumentsUseCaseTest {
    private val repository = FakeDocumentsRepository()
    private val exportDocuments = ExportDocumentsUseCase(repository)

    private val destination = "content://downloads/citizen-docs.zip"

    @Test
    fun `exports the documents it is given`() = runTest {
        val result = exportDocuments(listOf(TestDocuments.taxReturn, TestDocuments.passport), destination)

        assertThat(result).isInstanceOf(ExportResult.Exported::class.java)
        assertThat((result as ExportResult.Exported).documentCount).isEqualTo(2)
        assertThat(repository.exported?.map { it.title })
            .containsExactly("Tax return 2025", "Passport")
    }

    @Test
    fun `passes the chosen destination through`() = runTest {
        exportDocuments(listOf(TestDocuments.taxReturn), destination)

        assertThat(repository.exportDestination).isEqualTo(destination)
    }

    @Test
    fun `reports the bytes written`() = runTest {
        val result = exportDocuments(listOf(TestDocuments.taxReturn), destination)

        assertThat((result as ExportResult.Exported).bytesWritten)
            .isEqualTo(TestDocuments.taxReturn.sizeBytes)
    }

    // An empty archive would look like a successful backup of nothing.
    @Test
    fun `refuses to write an empty archive`() = runTest {
        val result = exportDocuments(emptyList(), destination)

        assertThat(result).isEqualTo(ExportResult.NothingToExport)
        assertThat(repository.exported).isNull()
    }

    @Test
    fun `reports a storage failure instead of throwing`() = runTest {
        repository.exportFailure = IOException("no space left on device")

        val result = exportDocuments(listOf(TestDocuments.taxReturn), destination)

        assertThat(result).isInstanceOf(ExportResult.Failed::class.java)
        assertThat((result as ExportResult.Failed).cause).isInstanceOf(IOException::class.java)
    }
}
