package com.salpiras.citizendocs.benchmarks

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until

internal const val PACKAGE_NAME = "com.salpiras.citizendocs"

/**
 * The journey worth optimising: open the app, scroll the list, open search.
 *
 * Shared between the profile generator and the startup benchmark on purpose. A baseline
 * profile is only as good as the code paths it was recorded over, so the thing being
 * measured and the thing being profiled have to be the same thing.
 *
 * Driven through content descriptions rather than test tags, because the production UI has
 * none and adding them purely for benchmarking would put test scaffolding in the shipped
 * app. They are English literals: this is a benchmark, and it runs against the default locale.
 */
internal fun MacrobenchmarkScope.exerciseDocumentList() {
    device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), UI_TIMEOUT_MS)

    device.findObject(By.scrollable(true))?.let { list ->
        // Keeps the fling clear of the system gesture insets, which would otherwise trigger
        // back or the app switcher instead of scrolling.
        list.setGestureMargin(device.displayWidth / GESTURE_MARGIN_FRACTION)
        repeat(SCROLL_PASSES) {
            list.fling(Direction.DOWN)
            device.waitForIdle()
        }
        list.fling(Direction.UP)
        device.waitForIdle()
    }

    device.findObject(By.desc("Search documents"))?.let { search ->
        search.click()
        device.waitForIdle()
        device.pressBack()
        device.waitForIdle()
    }
}

private const val UI_TIMEOUT_MS = 10_000L
private const val SCROLL_PASSES = 3
private const val GESTURE_MARGIN_FRACTION = 5
