package com.ssbmax.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ssbmax.core.domain.model.GenderTag

/**
 * Room entity for caching PPDT image metadata and Firestore-derived context.
 * imageContextJson holds the JSON-serialized PPDTImageContext (Phase 6+).
 * genderTag defaults to MIXED so images cached before Phase 6 remain visible to all users.
 */
@Entity(
    tableName = "cached_ppdt_images",
    indices = [
        Index("genderTag"),
        Index("imageDownloaded"),
        Index("usageCount"),
        Index("batchId"),
        Index("difficulty"),
        Index("category")
    ]
)
data class CachedPPDTImageEntity(
    @PrimaryKey val id: String,
    val imageUrl: String,
    val localFilePath: String? = null,
    val imageDescription: String = "Picture showing an ambiguous scene",
    val imageContextJson: String = "{}", // JSON-serialized PPDTImageContext
    val viewingTimeSeconds: Int = 30,
    val writingTimeMinutes: Int = 4,
    val minCharacters: Int = 200,
    val maxCharacters: Int = 1000,
    val category: String? = null,
    val difficulty: String? = null,
    val batchId: String,
    val cachedAt: Long,
    val lastUsed: Long? = null,
    val usageCount: Int = 0,
    val imageDownloaded: Int = 0, // SQLite stores as INTEGER
    val genderTag: GenderTag = GenderTag.MIXED // MIXED = shown to all users
)
