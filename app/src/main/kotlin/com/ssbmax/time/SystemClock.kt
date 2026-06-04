package com.ssbmax.time

import javax.inject.Inject

/** Production [Clock] that delegates to [System.currentTimeMillis]. */
class SystemClock @Inject constructor() : Clock {
    override fun nowMs(): Long = System.currentTimeMillis()
}
