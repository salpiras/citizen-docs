package com.salpiras.citizendocs.benchmarks

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * What the baseline profile is worth, in milliseconds.
 *
 * Two variants of the same journey, so the numbers can be compared directly:
 * [startupNoCompilation] is the worst case, and [startupWithBaselineProfile] is what a user
 * installing from Play actually gets. Run both and read the difference — an absolute number
 * from a single run on one device says very little on its own.
 *
 * [FrameTimingMetric] is included because startup time is not the point of this redesign;
 * the point is that a list which now animates still hits its frame deadlines.
 *
 * Emulator results are far too noisy to trust. Run these on a physical device:
 *
 * ```
 * ./gradlew :benchmarks:connectedBenchmarkAndroidTest
 * ```
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startupNoCompilation() = measure(CompilationMode.None())

    @Test
    fun startupWithBaselineProfile() =
        measure(CompilationMode.Partial(baselineProfileMode = BaselineProfileMode.Require))

    private fun measure(compilationMode: CompilationMode) = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(StartupTimingMetric(), FrameTimingMetric()),
        compilationMode = compilationMode,
        startupMode = StartupMode.COLD,
        iterations = ITERATIONS,
        setupBlock = { pressHome() },
    ) {
        startActivityAndWait()
        exerciseDocumentList()
    }
}

private const val ITERATIONS = 10
