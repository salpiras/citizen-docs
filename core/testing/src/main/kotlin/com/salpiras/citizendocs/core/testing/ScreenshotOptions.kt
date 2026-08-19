package com.salpiras.citizendocs.core.testing

import com.github.takahirom.roborazzi.RoborazziOptions

/**
 * Roborazzi's default validator is `ThresholdValidator(0F)` — a single differing pixel fails
 * the comparison. That is too strict to survive a change of host OS: goldens recorded on
 * macOS and verified on Linux CI differ by a handful of pixels on anti-aliased rounded
 * corners and on the FAB's elevation shadow, while all text, layout and colour are identical.
 *
 * A 1% allowance absorbs that rasterisation noise. It is far below the footprint of any real
 * UI change — a moved control, a restyled component or a text change all move well past 1%
 * of the screen.
 */
val CitizenDocsRoborazziOptions = RoborazziOptions(
    compareOptions = RoborazziOptions.CompareOptions(changeThreshold = CHANGE_THRESHOLD),
)

private const val CHANGE_THRESHOLD = 0.01F
