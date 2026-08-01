package com.ssbmax.shared.presentation.settings

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * KMP-convergence Phase 3a, row #20: [SubscriptionManagementViewModel] used to
 * compute the subscription expiry date as a flat +30-day offset in epoch
 * millis. That's wrong whenever "one calendar month" isn't exactly 30 days --
 * this pins the exact case a flat-30-day offset gets wrong: starting from
 * Jan 31, +30 days lands on Mar 2 (Mar 3 in a leap year), while +1 calendar
 * month should land on Feb 28 (Feb 29 in a leap year).
 */
class SubscriptionManagementExpiryTest {

    private val zone = TimeZone.currentSystemDefault()

    @Test
    fun oneMonthOut_fromJan31_landsOnFebruarysLastDay_notFlat30DaysLater() {
        val jan31 = LocalDateTime(2025, 1, 31, 10, 0, 0).toInstant(zone)

        val result = nextBillingCycleExpiry(jan31).toLocalDateTime(zone)

        // A flat +30-day offset would land on Mar 2, 2025 -- this must not.
        assertEquals(2025, result.year)
        assertEquals(2, result.monthNumber)
        assertEquals(28, result.dayOfMonth)
    }

    @Test
    fun oneMonthOut_fromJan15_landsOnFeb15() {
        val jan15 = LocalDateTime(2025, 1, 15, 9, 30, 0).toInstant(zone)

        val result = nextBillingCycleExpiry(jan15).toLocalDateTime(zone)

        assertEquals(2025, result.year)
        assertEquals(2, result.monthNumber)
        assertEquals(15, result.dayOfMonth)
    }

    @Test
    fun oneMonthOut_fromDecember_rollsOverToNextYear() {
        val dec20 = LocalDateTime(2025, 12, 20, 12, 0, 0).toInstant(zone)

        val result = nextBillingCycleExpiry(dec20).toLocalDateTime(zone)

        assertEquals(2026, result.year)
        assertEquals(1, result.monthNumber)
        assertEquals(20, result.dayOfMonth)
    }
}
