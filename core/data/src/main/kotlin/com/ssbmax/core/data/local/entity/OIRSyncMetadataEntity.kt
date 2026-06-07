package com.ssbmax.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row table tracking the content-version of the locally-cached OIR bank.
 *
 * Firestore (`test_content/oir/meta/config`) is the source of truth for OIR content;
 * Room reconciles to it via [contentVersion]. When the remote version differs from the
 * locally-stored one, [com.ssbmax.core.data.repository.OIRQuestionCacheManager] clears
 * and re-downloads the bank so every device — existing installs included — self-heals
 * to the current content. Always stored at [SINGLE_ROW_ID].
 */
@Entity(tableName = "oir_sync_metadata")
data class OIRSyncMetadataEntity(
    @PrimaryKey
    val id: Int = SINGLE_ROW_ID,
    val contentVersion: Int,
    val lastSyncAt: Long
) {
    companion object {
        const val SINGLE_ROW_ID = 0
    }
}
