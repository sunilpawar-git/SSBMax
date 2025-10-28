package com.ssbmax.core.domain.repository

import com.ssbmax.core.domain.model.CloudStudyMaterial
import com.ssbmax.core.domain.model.TopicContent
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for study content
 * Abstracts away whether content comes from Firestore or local storage
 */
interface StudyContentRepository {
    
    /**
     * Get topic content with automatic cloud/local selection
     * Returns Flow for reactive updates
     */
    fun getTopicContent(topicType: String): Flow<Result<Any>> // Any type since we need flexible return
    
    /**
     * Get study materials for a topic from Firestore
     */
    suspend fun getStudyMaterials(topicType: String): Result<List<CloudStudyMaterial>>
    
    /**
     * Get single study material by ID
     */
    suspend fun getStudyMaterial(materialId: String): Result<CloudStudyMaterial>
    
    /**
     * Force refresh content from server (bypasses cache)
     */
    suspend fun refreshContent(topicType: String): Result<TopicContent>
}

