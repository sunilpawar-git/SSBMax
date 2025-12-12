package com.ssbmax.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for tracking GPE image batch downloads
 */
@Entity(tableName = "gpe_batch_metadata")
data class GPEBatchMetadataEntity(
    @PrimaryKey val batchId: String,
    val downloadedAt: Long,
    val imageCount: Int,
    val version: String
)
