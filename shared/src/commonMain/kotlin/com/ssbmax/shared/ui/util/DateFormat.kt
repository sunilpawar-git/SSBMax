package com.ssbmax.shared.ui.util

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val monthAbbreviations = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

/**
 * KMP-safe replacement for the Android original's
 * `SimpleDateFormat("MMM dd, yyyy")` (`app/.../utils/DateFormatter.formatFullDate`)
 * — `java.text.SimpleDateFormat`/`java.util.Date`/`Locale` are JVM-only,
 * unavailable on the iOS target (another recurring gotcha on this plan's
 * list). Formats a millisecond epoch timestamp as e.g. "Dec 15, 2024" using
 * `kotlinx-datetime`, in the device's current time zone (matches the Android
 * original's `Locale.getDefault()` device-local behavior).
 */
fun formatFullDate(timestampMillis: Long): String {
    val localDateTime = Instant.fromEpochMilliseconds(timestampMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    val month = monthAbbreviations[localDateTime.monthNumber - 1]
    val day = localDateTime.dayOfMonth.toString().padStart(2, '0')
    return "$month $day, ${localDateTime.year}"
}
