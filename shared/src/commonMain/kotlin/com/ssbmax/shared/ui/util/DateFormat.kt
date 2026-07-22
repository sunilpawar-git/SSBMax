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

/**
 * KMP-safe replacement for the Android original's
 * `SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a")` (`app/.../utils/DateFormatter.formatDateTime`,
 * used by the grading-detail screen). Same `SimpleDateFormat`/`Locale` unavailability
 * rationale as [formatFullDate] above; adds a 12-hour clock + AM/PM suffix computed
 * manually from `kotlinx-datetime`'s `LocalDateTime.hour` (0-23).
 */
fun formatDateTime(timestampMillis: Long): String {
    val localDateTime = Instant.fromEpochMilliseconds(timestampMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    val month = monthAbbreviations[localDateTime.monthNumber - 1]
    val day = localDateTime.dayOfMonth.toString().padStart(2, '0')
    val hour24 = localDateTime.hour
    val amPm = if (hour24 < 12) "AM" else "PM"
    val hour12 = when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
    }
    val minute = localDateTime.minute.toString().padStart(2, '0')
    return "$month $day, ${localDateTime.year} at ${hour12.toString().padStart(2, '0')}:$minute $amPm"
}
