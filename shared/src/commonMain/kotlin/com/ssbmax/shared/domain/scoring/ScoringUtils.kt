package com.ssbmax.shared.domain.scoring

import com.ssbmax.shared.domain.model.EntryType as ProfileEntryType

object ScoringUtils {

    // Maps UserProfile.EntryType (model) to scoring.EntryType (validation rules).
    // ENTRY_10_PLUS_2 → NDA (most stringent), SERVICE → OTA, GRADUATE → GRADUATE.
    fun toScoringEntryType(profileEntry: ProfileEntryType?): EntryType = when (profileEntry) {
        ProfileEntryType.GRADUATE -> EntryType.GRADUATE
        ProfileEntryType.SERVICE -> EntryType.OTA
        else -> EntryType.NDA
    }

    /**
     * Fixed-decimal formatting (e.g. "%.1f"/"%.2f" equivalent) without `String.format`
     * or `java.util.Formatter`, neither of which is available in commonMain for the
     * iOS target. Replaces two JVM-only call sites surfaced by the Phase 1 KMP domain
     * move (SSBScoreValidator, ValidationIntegration).
     */
    fun formatDecimal(value: Double, decimals: Int): String {
        val sign = if (value < 0) "-" else ""
        val absValue = kotlin.math.abs(value)
        var factor = 1L
        repeat(decimals) { factor *= 10 }
        val scaled = kotlin.math.round(absValue * factor).toLong()
        val whole = scaled / factor
        val fraction = (scaled % factor).toString().padStart(decimals, '0')
        return "$sign$whole.$fraction"
    }
}
