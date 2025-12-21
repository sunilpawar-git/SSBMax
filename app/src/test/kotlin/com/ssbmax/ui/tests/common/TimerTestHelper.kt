package com.ssbmax.ui.tests.common

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

/**
 * Helper utilities for testing timer accuracy in Psychology Test ViewModels
 * 
 * Provides:
 * - Timer accuracy assertions
 * - Virtual time control
 * - Delta-based calculation verification
 */
@OptIn(ExperimentalCoroutinesApi::class)
object TimerTestHelper {
    
    /**
     * Assert that timer remaining time matches expected value within tolerance
     * 
     * @param actual Current timeRemaining from ViewModel state
     * @param expected Expected time in seconds
     * @param toleranceSeconds Allowed variance (default 1s for timer updates)
     */
    fun assertTimerAccuracy(
        actual: Int,
        expected: Int,
        toleranceSeconds: Int = 1
    ) {
        val diff = kotlin.math.abs(actual - expected)
        assertTrue(
            "Timer accuracy failed: expected $expected ± $toleranceSeconds, got $actual",
            diff <= toleranceSeconds
        )
    }
    
    /**
     * Advance virtual time and process pending coroutines
     * 
     * @param dispatcher Test dispatcher to control time
     * @param delayMillis Amount of virtual time to advance
     */
    fun advanceTime(dispatcher: TestDispatcher, delayMillis: Long) {
        dispatcher.scheduler.advanceTimeBy(delayMillis)
        dispatcher.scheduler.runCurrent()
    }
    
    /**
     * Verify timer decreases by EXACTLY expectedDelta after time advancement
     * Proves that only ONE timer coroutine is running (not multiple)
     * 
     * @param beforeTime Time before advancement
     * @param afterTime Time after advancement
     * @param expectedDelta Expected decrease (e.g., if advanced 2s, delta should be ~2)
     * @param toleranceSeconds Allowed variance
     */
    fun assertSingleTimerRunning(
        beforeTime: Int,
        afterTime: Int,
        expectedDelta: Int,
        toleranceSeconds: Int = 1
    ) {
        val actualDelta = beforeTime - afterTime
        val diff = kotlin.math.abs(actualDelta - expectedDelta)
        
        assertTrue(
            "Multiple timers detected: expected delta $expectedDelta ± $toleranceSeconds, got $actualDelta. " +
            "This indicates ${actualDelta / expectedDelta} timers are running concurrently.",
            diff <= toleranceSeconds
        )
    }
    
    /**
     * Calculate expected remaining time using delta-based approach
     * This is the reference implementation that ViewModels should match
     * 
     * @param startTimeMillis When timer started (System.currentTimeMillis())
     * @param durationSeconds Total timer duration
     * @param currentTimeMillis Current time (System.currentTimeMillis())
     * @return Remaining seconds (0 if expired)
     */
    fun calculateExpectedRemaining(
        startTimeMillis: Long,
        durationSeconds: Int,
        currentTimeMillis: Long
    ): Int {
        val endTime = startTimeMillis + (durationSeconds * 1000)
        val remainingMillis = endTime - currentTimeMillis
        return maxOf(0, (remainingMillis / 1000).toInt())
    }
}
