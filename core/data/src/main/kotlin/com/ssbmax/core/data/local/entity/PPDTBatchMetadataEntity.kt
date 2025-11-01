package com.ssbmax.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for tracking PPDT image batch downloads
 */
@Entity(tableName = "ppdt_batch_metadata")
data class PPDTBatchMetadataEntity(
    @PrimaryKey val batchId: String,
    val downloadedAt: Long,
    val imageCount: Int,
    val version: String
)

