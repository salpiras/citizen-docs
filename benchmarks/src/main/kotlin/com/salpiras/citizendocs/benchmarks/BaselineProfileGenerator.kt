package com.salpiras.citizendocs.benchmarks

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records which classes and methods the app actually uses on the way in, so ART can compile
 * them ahead of time instead of interpreting them on first launch.
 *
 * This matters more here than it would have before: an animation-rich UI runs far more
 * Compose animation code on the very first frames, and that is exactly the code a cold,
 * unprofiled process is slowest at.
 *
 * Run it against a connected device or emulator:
 *
 * ```
 * ./gradlew :app:generateBaselineProfile
 * ```
 *
 * That writes `app/src/main/generated/baselineProfiles/baseline-prof.txt`, which is
 * **committed** — CI has no emulator and only consumes the file.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() = baselineProfileRule.collect(packageName = PACKAGE_NAME) {
        pressHome()
        startActivityAndWait()
        exerciseDocumentList()
    }
}
