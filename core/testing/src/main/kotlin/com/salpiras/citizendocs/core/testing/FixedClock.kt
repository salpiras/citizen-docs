package com.salpiras.citizendocs.core.testing

import kotlin.time.Clock
import kotlin.time.Instant

/** Makes `createdAt` assertable instead of "some time around now". */
class FixedClock(private var instant: Instant) : Clock {
    override fun now(): Instant = instant

    fun advanceTo(newInstant: Instant) {
        instant = newInstant
    }
}
