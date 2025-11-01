package com.ssbmax.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for tracking GTO task batch downloads
 */
@Entity(tableName = "gto_batch_metadata")
data class GTOBatchMetadataEntity(
    @PrimaryKey val batchId: String,
    val downloadedAt: Long,
    val taskCount: Int,
    val version: String
)

