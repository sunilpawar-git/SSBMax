package com.ssbmax.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for TimerProgressBar helper functions.
 * 
 * Tests cover:
 * - Progress calculation logic
 * - Low time threshold detection
 * - Edge cases (zero, negative, overflow)
 * - TimerThresholds constants
 */
class TimerProgressBarTest {

    // =========================================================================
    // calculateProgress Tests
    // =========================================================================

    @Test
    fun `calculateProgress returns 1 when time equals total`() {
        val result = calculateProgress(
            timeRemainingSeconds = 240,
            totalTimeSeconds = 240
        )
        assertEquals(1.0f, result, 0.001f)
    }

    @Test
    fun `calculateProgress returns 0 when time is zero`() {
        val result = calculateProgress(
            timeRemainingSeconds = 0,
            totalTimeSeconds = 240
        )
        assertEquals(0.0f, result, 0.001f)
    }

    @Test
    fun `calculateProgress returns 0_5 when half time remaining`() {
        val result = calculateProgress(
            timeRemainingSeconds = 120,
            totalTimeSeconds = 240
        )
        assertEquals(0.5f, result, 0.001f)
    }

    @Test
    fun `calculateProgress returns 0 when totalTime is zero`() {
        // Avoid division by zero
        val result = calculateProgress(
            timeRemainingSeconds = 100,
            totalTimeSeconds = 0
        )
        assertEquals(0.0f, result, 0.001f)
    }

    @Test
    fun `calculateProgress returns 0 when totalTime is negative`() {
        val result = calculateProgress(
            timeRemainingSeconds = 100,
            totalTimeSeconds = -10
        )
        assertEquals(0.0f, result, 0.001f)
    }

    @Test
    fun `calculateProgress clamps to 1 when time exceeds total`() {
        // Edge case: timeRemaining > totalTime (should not happen but handle gracefully)
        val result = calculateProgress(
            timeRemainingSeconds = 300,
            totalTimeSeconds = 240
        )
        assertEquals(1.0f, result, 0.001f)
    }

    @Test
    fun `calculateProgress clamps to 0 when time is negative`() {
        val result = calculateProgress(
            timeRemainingSeconds = -10,
            totalTimeSeconds = 240
        )
        assertEquals(0.0f, result, 0.001f)
    }

    @Test
    fun `calculateProgress handles small values correctly`() {
        // WAT: 15 second timer
        val result = calculateProgress(
            timeRemainingSeconds = 5,
            totalTimeSeconds = 15
        )
        assertEquals(0.333f, result, 0.01f)
    }

    @Test
    fun `calculateProgress handles large values correctly`() {
        // GPE: 25 minutes = 1500 seconds
        val result = calculateProgress(
            timeRemainingSeconds = 750,
            totalTimeSeconds = 1500
        )
        assertEquals(0.5f, result, 0.001f)
    }

    // =========================================================================
    // isLowTime Tests
    // =========================================================================

    @Test
    fun `isLowTime returns true when time equals threshold`() {
        val result = isLowTime(
            timeRemainingSeconds = 60,
            lowTimeThresholdSeconds = 60
        )
        assertTrue(result)
    }

    @Test
    fun `isLowTime returns true when time below threshold`() {
        val result = isLowTime(
            timeRemainingSeconds = 30,
            lowTimeThresholdSeconds = 60
        )
        assertTrue(result)
    }

    @Test
    fun `isLowTime returns false when time above threshold`() {
        val result = isLowTime(
            timeRemainingSeconds = 120,
            lowTimeThresholdSeconds = 60
        )
        assertFalse(result)
    }

    @Test
    fun `isLowTime returns false when time is zero`() {
        // Zero time means test is over, not "low time warning"
        val result = isLowTime(
            timeRemainingSeconds = 0,
            lowTimeThresholdSeconds = 60
        )
        assertFalse(result)
    }

    @Test
    fun `isLowTime returns false when time is negative`() {
        val result = isLowTime(
            timeRemainingSeconds = -10,
            lowTimeThresholdSeconds = 60
        )
        assertFalse(result)
    }

    @Test
    fun `isLowTime works with short threshold for WAT`() {
        // WAT uses 5 second threshold
        assertTrue(isLowTime(timeRemainingSeconds = 3, lowTimeThresholdSeconds = 5))
        assertTrue(isLowTime(timeRemainingSeconds = 5, lowTimeThresholdSeconds = 5))
        assertFalse(isLowTime(timeRemainingSeconds = 6, lowTimeThresholdSeconds = 5))
    }

    @Test
    fun `isLowTime works with standard threshold for PPDT`() {
        // PPDT uses 30 second threshold
        assertTrue(isLowTime(timeRemainingSeconds = 15, lowTimeThresholdSeconds = 30))
        assertTrue(isLowTime(timeRemainingSeconds = 30, lowTimeThresholdSeconds = 30))
        assertFalse(isLowTime(timeRemainingSeconds = 31, lowTimeThresholdSeconds = 30))
    }

    // =========================================================================
    // TimerThresholds Constants Tests
    // =========================================================================

    @Test
    fun `TimerThresholds SHORT_TEST is 5 seconds`() {
        assertEquals(5, TimerThresholds.SHORT_TEST)
    }

    @Test
    fun `TimerThresholds MEDIUM_TEST is 10 seconds`() {
        assertEquals(10, TimerThresholds.MEDIUM_TEST)
    }

    @Test
    fun `TimerThresholds STANDARD_TEST is 30 seconds`() {
        assertEquals(30, TimerThresholds.STANDARD_TEST)
    }

    @Test
    fun `TimerThresholds LONG_TEST is 60 seconds`() {
        assertEquals(60, TimerThresholds.LONG_TEST)
    }

    // =========================================================================
    // Integration-style Tests (combining both functions)
    // =========================================================================

    @Test
    fun `PPDT writing phase scenario - 4 min timer at 29s remaining`() {
        val totalTime = 240 // 4 minutes
        val timeRemaining = 29
        
        val progress = calculateProgress(timeRemaining, totalTime)
        val isLow = isLowTime(timeRemaining, TimerThresholds.STANDARD_TEST)
        
        assertEquals(0.121f, progress, 0.01f)
        assertTrue(isLow) // 29 < 30
    }

    @Test
    fun `WAT scenario - 15s timer at 3s remaining`() {
        val totalTime = 15
        val timeRemaining = 3
        
        val progress = calculateProgress(timeRemaining, totalTime)
        val isLow = isLowTime(timeRemaining, TimerThresholds.SHORT_TEST)
        
        assertEquals(0.2f, progress, 0.01f)
        assertTrue(isLow) // 3 < 5
    }

    @Test
    fun `GD scenario - 20 min timer at 90s remaining`() {
        val totalTime = 1200 // 20 minutes
        val timeRemaining = 90
        
        val progress = calculateProgress(timeRemaining, totalTime)
        val isLow = isLowTime(timeRemaining, TimerThresholds.LONG_TEST)
        
        assertEquals(0.075f, progress, 0.01f)
        assertFalse(isLow) // 90 > 60
    }

    @Test
    fun `GD scenario - 20 min timer at 45s remaining`() {
        val totalTime = 1200 // 20 minutes
        val timeRemaining = 45
        
        val progress = calculateProgress(timeRemaining, totalTime)
        val isLow = isLowTime(timeRemaining, TimerThresholds.LONG_TEST)
        
        assertEquals(0.0375f, progress, 0.01f)
        assertTrue(isLow) // 45 < 60
    }
}
