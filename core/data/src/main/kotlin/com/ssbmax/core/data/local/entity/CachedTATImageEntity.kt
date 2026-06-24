package com.ssbmax.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for caching TAT image metadata.
 * cardPosition (1–11) is the OLQ-pool slot used for test assembly.
 * genderTag routes images to the right users (MALE/FEMALE/MIXED).
 * imageContextJson holds JSON-serialized TATImageContext for AI scoring.
 */
@Entity(
    tableName = "cached_tat_images",
    indices = [
        Index("cardPosition"),
        Index("genderTag"),
        Index("usageCount"),
        Index("batchId")
    ]
)
data class CachedTATImageEntity(
    @PrimaryKey val id: String,
    val imageUrl: String,
    val cardPosition: Int,
    val genderTag: String = "MIXED",
    val imageContextJson: String = "{}",
    val viewingTimeSeconds: Int = 30,
    val writingTimeMinutes: Int = 4,
    val minCharacters: Int = 150,
    val maxCharacters: Int = 1500,
    val category: String? = null,
    val difficulty: String? = null,
    val batchId: String,
    val cachedAt: Long,
    val lastUsed: Long? = null,
    val usageCount: Int = 0
)
