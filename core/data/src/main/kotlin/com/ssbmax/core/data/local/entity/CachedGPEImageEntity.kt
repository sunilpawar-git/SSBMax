package com.ssbmax.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for caching GPE (Group Planning Exercise) scenario images locally
 * Follows progressive caching strategy similar to PPDT
 *
 * Note: Images are stored in Firebase Storage, this entity caches metadata and URLs
 */
@Entity(tableName = "cached_gpe_images")
data class CachedGPEImageEntity(
    @PrimaryKey val id: String,
    val imageUrl: String, // Firebase Storage URL
    val localFilePath: String? = null, // Local cache path if image is downloaded
    val scenario: String, // Tactical scenario description
    val solution: String? = null, // Ideal solution
    val imageDescription: String, // Alt text for accessibility
    val resources: String? = null, // JSON array of available resources
    val viewingTimeSeconds: Int = 60,
    val planningTimeSeconds: Int = 1740, // 29 minutes
    val minCharacters: Int = 500,
    val maxCharacters: Int = 2000,
    val category: String? = null, // Optional: scenario type (river crossing, wall climbing, etc.)
    val difficulty: String? = null, // easy, medium, hard
    val batchId: String,
    val cachedAt: Long,
    val lastUsed: Long?,
    val usageCount: Int = 0,
    val imageDownloaded: Boolean = false // Track if actual image file is cached
)
