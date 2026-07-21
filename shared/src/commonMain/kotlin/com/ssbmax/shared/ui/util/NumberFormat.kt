package com.ssbmax.shared.ui.util

import kotlin.math.abs
import kotlin.math.round

/**
 * KMP-safe replacement for the Android originals' `"%.1f".format(score)`
 * calls (dashboard test-score chips, OLQ strength badges, overall-average
 * badge). `String.format`/`Float.format` have no common `actual` — a
 * recurring Kotlin/Native compile gotcha on this plan's own list (hit 3x
 * already in Phase 2's Ktor/Gemini work). Rounds to one decimal place and
 * always renders exactly one digit after the point, matching the Android
 * original's output for this app's 0.0-10.0 score range (never negative).
 */
fun formatOneDecimal(value: Float): String {
    val roundedTenths = round(value * 10).toInt()
    val whole = roundedTenths / 10
    val decimal = abs(roundedTenths % 10)
    return "$whole.$decimal"
}
