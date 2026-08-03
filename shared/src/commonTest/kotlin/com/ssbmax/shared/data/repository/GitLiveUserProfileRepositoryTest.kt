package com.ssbmax.shared.data.repository

import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Pins `todayStartMillis` to device-local midnight. The Android
 * UserProfileRepositoryImpl used `Calendar.getInstance()` (device-default
 * timezone) for the login-streak day boundary; an earlier port of this class
 * used `now - (now % DAY_MILLIS)`, which is UTC epoch-day midnight instead.
 * In a positive-UTC-offset zone (e.g. IST, UTC+5:30) those two disagree for
 * hours after local midnight, which would mis-fire `updateLoginStreak`'s
 * same-day/consecutive-day/reset branches right when a user's local day
 * actually turns over.
 */
class GitLiveUserProfileRepositoryTest {

    private val repository = GitLiveUserProfileRepository()

    @Test
    fun `todayStartMillis matches device-local midnight, not UTC epoch-day midnight`() {
        val now = Clock.System.now()
        val zone = TimeZone.currentSystemDefault()
        val expectedLocalMidnight = now.toLocalDateTime(zone).date.atStartOfDayIn(zone).toEpochMilliseconds()

        assertEquals(expectedLocalMidnight, repository.todayStartMillis(now.toEpochMilliseconds()))
    }

    @Test
    fun `todayStartMillis groups two instants on the same local calendar day identically`() {
        val zone = TimeZone.currentSystemDefault()
        val noon = LocalDate(2026, 3, 15).atStartOfDayIn(zone) + 12.hours
        val lateEvening = LocalDate(2026, 3, 15).atStartOfDayIn(zone) + 23.hours + 59.minutes

        assertEquals(
            repository.todayStartMillis(noon.toEpochMilliseconds()),
            repository.todayStartMillis(lateEvening.toEpochMilliseconds())
        )
    }

    @Test
    fun `todayStartMillis differs across a local calendar day boundary`() {
        val zone = TimeZone.currentSystemDefault()
        val day1 = LocalDate(2026, 3, 15).atStartOfDayIn(zone) + 23.hours
        val day2 = LocalDate(2026, 3, 16).atStartOfDayIn(zone) + 1.hours

        assertNotEquals(
            repository.todayStartMillis(day1.toEpochMilliseconds()),
            repository.todayStartMillis(day2.toEpochMilliseconds())
        )
    }
}
