package com.ssbmax.core.domain.scoring

import com.ssbmax.core.domain.model.EntryType as ProfileEntryType

object ScoringUtils {

    // Maps UserProfile.EntryType (model) to scoring.EntryType (validation rules).
    // ENTRY_10_PLUS_2 → NDA (most stringent), SERVICE → OTA, GRADUATE → GRADUATE.
    fun toScoringEntryType(profileEntry: ProfileEntryType?): EntryType = when (profileEntry) {
        ProfileEntryType.GRADUATE -> EntryType.GRADUATE
        ProfileEntryType.SERVICE -> EntryType.OTA
        else -> EntryType.NDA
    }
}
