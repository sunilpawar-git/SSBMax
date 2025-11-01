package com.ssbmax.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for caching PPDT (Picture Perception & Description Test) images locally
 * Follows progressive caching strategy similar to TAT
 * 
 * Note: Images are stored in Firebase Storage, this entity caches metadata and URLs
 */
@Entity(tableName = "cached_ppdt_images")
data class CachedPPDTImageEntity(
    @PrimaryKey val id: String,
    val imageUrl: String, // Firebase Storage URL
    val localFilePath: String? = null, // Local cache path if image is downloaded
    val imageDescription: String, // Alt text for accessibility
    val viewingTimeSeconds: Int = 30,
    val writingTimeMinutes: Int = 4,
    val minCharacters: Int = 200,
    val maxCharacters: Int = 1000,
    val category: String? = null, // Optional: theme category (leadership, conflict, teamwork)
    val difficulty: String? = null, // easy, medium, hard
    val batchId: String,
    val cachedAt: Long,
    val lastUsed: Long?,
    val usageCount: Int = 0,
    val imageDownloaded: Boolean = false // Track if actual image file is cached
)

