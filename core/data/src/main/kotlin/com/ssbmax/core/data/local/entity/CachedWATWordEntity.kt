package com.ssbmax.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for caching WAT words locally
 * Follows progressive caching strategy similar to OIR questions
 */
@Entity(tableName = "cached_wat_words")
data class CachedWATWordEntity(
    @PrimaryKey val id: String,
    val word: String,
    val sequenceNumber: Int,
    val timeAllowedSeconds: Int = 15,
    val category: String? = null, // e.g., "leadership", "moral_values", "military_virtues"
    val difficulty: String? = null, // e.g., "easy", "medium", "hard"
    val batchId: String,
    val cachedAt: Long,
    val lastUsed: Long?,
    val usageCount: Int = 0
)

