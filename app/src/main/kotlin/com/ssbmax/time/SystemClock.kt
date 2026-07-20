package com.ssbmax.time


/** Production [Clock] that delegates to [System.currentTimeMillis]. */
class SystemClock : Clock {
    override fun nowMs(): Long = System.currentTimeMillis()
}
