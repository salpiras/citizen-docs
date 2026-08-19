package com.salpiras.citizendocs.core.domain

import com.google.common.truth.Truth.assertThat
import com.salpiras.citizendocs.core.model.TitleValidation
import com.salpiras.citizendocs.core.testing.FakeDocumentsRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Test
import java.io.IOException

class SaveScannedDocumentUseCaseTest {
    private val repository = FakeDocumentsRepository()
    private val saveScannedDocument = SaveScannedDocumentUseCase(repository)

    private val date = LocalDate(2026, 1, 12)

    @Test
    fun `saves a document with a valid title`() = runTest {
        val result = saveScannedDocument("Tax return 2025", date, "content://scan/tmp.pdf", 3)

        assertThat(result).isInstanceOf(SaveResult.Saved::class.java)
        assertThat(repository.current()).hasSize(1)
    }

    @Test
    fun `trims the title before saving`() = runTest {
        saveScannedDocument("  Passport  ", date, "content://scan/tmp.pdf", 1)

        assertThat(repository.current().single().title).isEqualTo("Passport")
    }

    @Test
    fun `rejects a blank title without touching the repository`() = runTest {
        val result = saveScannedDocument("   ", date, "content://scan/tmp.pdf", 1)

        assertThat(result).isEqualTo(SaveResult.InvalidTitle(TitleValidation.Blank))
        assertThat(repository.current()).isEmpty()
    }

    @Test
    fun `rejects an over-long title`() = runTest {
        val result = saveScannedDocument("a".repeat(200), date, "content://scan/tmp.pdf", 1)

        assertThat(result).isInstanceOf(SaveResult.InvalidTitle::class.java)
        assertThat(repository.current()).isEmpty()
    }

    // A storage failure must come back as a value the UI can react to, not as an exception
    // that escapes and gets swallowed somewhere up the stack.
    @Test
    fun `reports a repository failure instead of throwing`() = runTest {
        repository.addFailure = IOException("disk full")

        val result = saveScannedDocument("Tax return 2025", date, "content://scan/tmp.pdf", 3)

        assertThat(result).isInstanceOf(SaveResult.Failed::class.java)
        assertThat((result as SaveResult.Failed).cause).isInstanceOf(IOException::class.java)
    }
}
