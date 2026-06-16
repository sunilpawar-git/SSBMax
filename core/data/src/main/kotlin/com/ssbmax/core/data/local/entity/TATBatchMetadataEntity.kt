package com.ssbmax.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for tracking TAT image batch downloads.
 * lastStalenessCheckAt gates Firestore version reads behind a 24h TTL.
 */
@Entity(tableName = "tat_batch_metadata")
data class TATBatchMetadataEntity(
    @PrimaryKey val batchId: String,
    val downloadedAt: Long,
    val imageCount: Int,
    val version: String,
    val lastStalenessCheckAt: Long = 0L
)
