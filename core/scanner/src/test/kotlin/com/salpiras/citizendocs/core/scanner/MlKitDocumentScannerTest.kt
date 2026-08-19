package com.salpiras.citizendocs.core.scanner

import android.app.Activity
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers the result-mapping branches that don't need Play services present. The success
 * path goes through ML Kit's static result parser and is exercised on device instead.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MlKitDocumentScannerTest {
    private val scanner = MlKitDocumentScanner()

    @Test
    fun `backing out of the scanner is reported as cancelled, not an error`() {
        assertThat(scanner.outcomeFrom(Activity.RESULT_CANCELED, null))
            .isEqualTo(ScanOutcome.Cancelled)
    }

    @Test
    fun `an unexpected result code is reported as cancelled`() {
        assertThat(scanner.outcomeFrom(42, null)).isEqualTo(ScanOutcome.Cancelled)
    }

    @Test
    fun `an OK result with no PDF is reported as a failure`() {
        assertThat(scanner.outcomeFrom(Activity.RESULT_OK, null))
            .isInstanceOf(ScanOutcome.Failed::class.java)
    }
}
