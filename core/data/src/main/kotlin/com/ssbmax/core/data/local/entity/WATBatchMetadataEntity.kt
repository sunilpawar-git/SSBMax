package com.ssbmax.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for tracking WAT word batch downloads
 */
@Entity(tableName = "wat_batch_metadata")
data class WATBatchMetadataEntity(
    @PrimaryKey val batchId: String,
    val downloadedAt: Long,
    val wordCount: Int,
    val version: String
)

