package com.ssbmax.time

/**
 * Abstraction over wall-clock time.
 * Production code uses [SystemClock]; tests use a controllable fake.
 */
interface Clock {
    fun nowMs(): Long
}
