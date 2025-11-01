package com.ssbmax.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for caching SRT situations locally
 * Follows progressive caching strategy similar to OIR and WAT
 */
@Entity(tableName = "cached_srt_situations")
data class CachedSRTSituationEntity(
    @PrimaryKey val id: String,
    val situation: String,
    val sequenceNumber: Int,
    val category: String, // Leadership, Decision_Making, Crisis_Management, etc.
    val timeAllowedSeconds: Int = 30,
    val difficulty: String? = null, // easy, medium, hard
    val batchId: String,
    val cachedAt: Long,
    val lastUsed: Long?,
    val usageCount: Int = 0
)

