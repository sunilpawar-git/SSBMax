package com.ssbmax.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for caching TAT images locally
 * Follows progressive caching strategy similar to OIR, WAT, and SRT
 * 
 * Note: Images are stored in Firebase Storage, this entity caches metadata and URLs
 */
@Entity(tableName = "cached_tat_images")
data class CachedTATImageEntity(
    @PrimaryKey val id: String,
    val imageUrl: String, // Firebase Storage URL
    val localFilePath: String? = null, // Local cache path if image is downloaded
    val sequenceNumber: Int,
    val prompt: String = "Write a story about what you see in the picture",
    val viewingTimeSeconds: Int = 30,
    val writingTimeMinutes: Int = 4,
    val minCharacters: Int = 50,
    val maxCharacters: Int = 1500,
    val category: String? = null, // Optional: theme category for analytics
    val difficulty: String? = null, // easy, medium, hard
    val batchId: String,
    val cachedAt: Long,
    val lastUsed: Long?,
    val usageCount: Int = 0,
    val imageDownloaded: Boolean = false // Track if actual image file is cached
)

