package com.ssbmax.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for tracking SRT situation batch downloads
 */
@Entity(tableName = "srt_batch_metadata")
data class SRTBatchMetadataEntity(
    @PrimaryKey val batchId: String,
    val downloadedAt: Long,
    val situationCount: Int,
    val version: String
)

