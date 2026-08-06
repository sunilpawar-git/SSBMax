package com.ssbmax.shared.domain.usecase.subscription

import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * e.g. "Aug 1, 2026" -- first day of next calendar month. Extracted (dev-subscription-override
 * plan's Phase 10) out of [CheckTestEligibilityUseCase] so it and
 * [com.ssbmax.shared.domain.usecase.CheckInterviewPrerequisitesUseCase] compute
 * `TestEligibility.LimitReached.resetsAt` identically instead of redeclaring the same format twice.
 */
internal fun nextMonthResetLabel(): String {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val todayMonthNumber = today.month.ordinal + 1
    val nextMonthFirst = if (todayMonthNumber == 12) {
        LocalDate(today.year + 1, 1, 1)
    } else {
        LocalDate(today.year, todayMonthNumber + 1, 1)
    }
    val monthName = MONTH_NAMES[nextMonthFirst.month.ordinal]
    return "$monthName 1, ${nextMonthFirst.year}"
}

private val MONTH_NAMES = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)
