package com.ssbmax.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing bookmarked study materials
 */
interface BookmarkRepository {
    /**
     * Get all bookmarked material IDs for a user
     */
    fun getBookmarkedMaterials(userId: String): Flow<List<String>>
    
    /**
     * Toggle bookmark status for a material
     */
    suspend fun toggleBookmark(userId: String, materialId: String)
    
    /**
     * Check if a material is bookmarked
     */
    fun isBookmarked(userId: String, materialId: String): Flow<Boolean>
}

