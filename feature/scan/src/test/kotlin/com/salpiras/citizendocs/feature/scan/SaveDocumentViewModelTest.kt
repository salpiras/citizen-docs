package com.salpiras.citizendocs.feature.scan

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.salpiras.citizendocs.core.domain.SaveScannedDocumentUseCase
import com.salpiras.citizendocs.core.testing.FakeDocumentsRepository
import com.salpiras.citizendocs.core.testing.FixedClock
import com.salpiras.citizendocs.core.testing.MainDispatcherRule
import com.salpiras.citizendocs.core.ui.UiText
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import kotlin.time.Instant

/**
 * Robolectric, because `SavedStateHandle.toRoute()` decodes through `NavType`, which reads
 * from a real `android.os.Bundle`. Everything else here is plain JVM.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SaveDocumentViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeDocumentsRepository()
    private val clock = FixedClock(Instant.parse("2026-08-18T10:15:30Z"))

    private fun viewModel(pdfUri: String = "content://scan/tmp.pdf", pageCount: Int = 3) = SaveDocumentViewModel(
        // Mirrors what navigation-compose puts in the handle for a @Serializable route.
        savedStateHandle = SavedStateHandle(mapOf("pdfUri" to pdfUri, "pageCount" to pageCount)),
        saveScannedDocument = SaveScannedDocumentUseCase(repository),
        clock = clock,
        timeZone = TimeZone.UTC,
    )

    @Test
    fun `prefills today as the document date`() = runTest {
        assertThat(viewModel().state.value.documentDate).isEqualTo(LocalDate(2026, 8, 18))
    }

    @Test
    fun `exposes the scanned page count`() = runTest {
        assertThat(viewModel(pageCount = 7).state.value.pageCount).isEqualTo(7)
    }

    @Test
    fun `saving a valid document emits Saved and stores it`() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(SaveDocumentEvent.TitleChanged("Tax return 2025"))

        viewModel.effects.test {
            viewModel.onEvent(SaveDocumentEvent.SaveClicked)

            assertThat(awaitItem()).isEqualTo(SaveDocumentEffect.Saved("Tax return 2025"))
        }
        assertThat(repository.current().single().title).isEqualTo("Tax return 2025")
    }

    @Test
    fun `saving a blank title shows an inline error and stores nothing`() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(SaveDocumentEvent.SaveClicked)

        assertThat(viewModel.state.value.titleError).isEqualTo(UiText.Res(R.string.scan_title_blank))
        assertThat(repository.current()).isEmpty()
    }

    @Test
    fun `editing the title clears the error`() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(SaveDocumentEvent.SaveClicked)

        viewModel.onEvent(SaveDocumentEvent.TitleChanged("Passport"))

        assertThat(viewModel.state.value.titleError).isNull()
    }

    @Test
    fun `selecting a date updates the state and closes the picker`() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(SaveDocumentEvent.DatePickerRequested)

        viewModel.onEvent(SaveDocumentEvent.DateSelected(LocalDate(2025, 6, 30)))

        assertThat(viewModel.state.value.documentDate).isEqualTo(LocalDate(2025, 6, 30))
        assertThat(viewModel.state.value.showDatePicker).isFalse()
    }

    // The failure must reach the user; the previous implementation printed the stack trace and
    // dismissed the sheet, which looked identical to success.
    @Test
    fun `a storage failure is reported and the screen stays open`() = runTest {
        repository.addFailure = IOException("disk full")
        val viewModel = viewModel()
        viewModel.onEvent(SaveDocumentEvent.TitleChanged("Tax return 2025"))

        viewModel.effects.test {
            viewModel.onEvent(SaveDocumentEvent.SaveClicked)

            val effect = awaitItem() as SaveDocumentEffect.ShowMessage
            assertThat(effect.message).isEqualTo(UiText.Res(R.string.scan_save_failed))
        }
        assertThat(viewModel.state.value.isSaving).isFalse()
    }

    @Test
    fun `cancelling dismisses without saving`() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(SaveDocumentEvent.TitleChanged("Tax return 2025"))

        viewModel.effects.test {
            viewModel.onEvent(SaveDocumentEvent.CancelClicked)
            assertThat(awaitItem()).isEqualTo(SaveDocumentEffect.Dismiss)
        }
        assertThat(repository.current()).isEmpty()
    }
}
