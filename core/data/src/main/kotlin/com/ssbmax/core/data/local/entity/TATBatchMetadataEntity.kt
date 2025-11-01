package com.ssbmax.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for tracking TAT image batch downloads
 */
@Entity(tableName = "tat_batch_metadata")
data class TATBatchMetadataEntity(
    @PrimaryKey val batchId: String,
    val downloadedAt: Long,
    val imageCount: Int,
    val version: String
)

