package com.ssbmax.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for tracking Interview question batch downloads
 */
@Entity(tableName = "interview_batch_metadata")
data class InterviewBatchMetadataEntity(
    @PrimaryKey val batchId: String,
    val downloadedAt: Long,
    val questionCount: Int,
    val version: String
)

