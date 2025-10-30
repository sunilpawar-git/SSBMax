package com.ssbmax.core.data.repository

import android.util.Log
import com.ssbmax.core.data.source.FirestoreContentSource
import com.ssbmax.core.domain.config.ContentFeatureFlags
import com.ssbmax.core.domain.model.CloudStudyMaterial
import com.ssbmax.core.domain.model.TopicContent
import com.ssbmax.core.domain.repository.StudyContentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for study content with cloud + local fallback
 * 
 * Strategy:
 * 1. Check if cloud content is enabled for topic
 * 2. If yes, fetch from Firestore (with cache)
 * 3. If no or on error, fallback to local hardcoded content
 * 
 * Benefits:
 * - Zero downtime: Always have content available
 * - Gradual rollout: Enable cloud per topic
 * - Cost optimization: Cache-first strategy
 * - Instant rollback: Disable cloud instantly
 */
@Singleton
class StudyContentRepositoryImpl @Inject constructor(
    private val firestoreSource: FirestoreContentSource
) : StudyContentRepository {
    
    override fun getTopicContent(topicType: String): Flow<Result<TopicContentData>> = flow {
        try {
            // Normalize case for consistency
            val normalizedType = topicType.uppercase()
            Log.d(TAG, "━━━ REPOSITORY: Loading $topicType (normalized: $normalizedType) ━━━")
            
            // Check if cloud content is enabled for this topic
            if (ContentFeatureFlags.isTopicCloudEnabled(topicType)) {
                Log.d(TAG, "REPO: Cloud enabled for $topicType, attempting Firestore load")
                
                // Try cloud first
                val cloudResult = loadFromCloud(normalizedType)
                
                if (cloudResult.isSuccess) {
                    Log.d(TAG, "REPO: ✓ Cloud load SUCCESS for $topicType")
                    emit(cloudResult)
                    return@flow
                } else {
                    Log.e(TAG, "REPO: ✗ Cloud load FAILED for $topicType: ${cloudResult.exceptionOrNull()?.message}")
                    if (ContentFeatureFlags.fallbackToLocalOnError) {
                        Log.w(TAG, "REPO: Fallback enabled, returning LOCAL indicator")
                        emit(loadFromLocal(topicType))
                    } else {
                        emit(cloudResult)
                    }
                }
            } else {
                // Cloud not enabled, use local
                Log.d(TAG, "REPO: Cloud NOT enabled for $topicType, using local")
                emit(loadFromLocal(topicType))
            }
        } catch (e: Exception) {
            Log.e(TAG, "REPO: Exception loading $topicType", e)
            if (ContentFeatureFlags.fallbackToLocalOnError) {
                emit(loadFromLocal(topicType))
            } else {
                emit(Result.failure(e))
            }
        }
    }
    
    override suspend fun getStudyMaterials(topicType: String): Result<List<CloudStudyMaterial>> {
        return if (ContentFeatureFlags.isTopicCloudEnabled(topicType)) {
            Log.d(TAG, "Fetching materials for $topicType from Firestore")
            firestoreSource.getStudyMaterials(topicType)
        } else {
            // Return empty list - local materials are loaded differently
            Result.success(emptyList())
        }
    }
    
    override suspend fun getStudyMaterial(materialId: String): Result<CloudStudyMaterial> {
        return firestoreSource.getStudyMaterial(materialId)
    }
    
    override suspend fun refreshContent(topicType: String): Result<TopicContent> {
        return firestoreSource.forceRefresh(topicType)
    }
    
    /**
     * Load content from Firestore
     */
    private suspend fun loadFromCloud(topicType: String): Result<TopicContentData> {
        return try {
            Log.d(TAG, "REPO: Fetching topic document '$topicType' from Firestore...")
            val topicResult = firestoreSource.getTopicContent(topicType)
            
            if (topicResult.isFailure) {
                val error = topicResult.exceptionOrNull()!!
                Log.e(TAG, "REPO: Topic fetch FAILED: ${error.message}", error)
                return Result.failure(error)
            }
            
            val topic = topicResult.getOrNull()!!
            Log.d(TAG, "REPO: ✓ Topic fetched: ${topic.title}")
            
            Log.d(TAG, "REPO: Fetching materials for '$topicType' from Firestore...")
            val materialsResult = firestoreSource.getStudyMaterials(topicType)
            
            if (materialsResult.isFailure) {
                val error = materialsResult.exceptionOrNull()!!
                Log.e(TAG, "REPO: Materials fetch FAILED: ${error.message}", error)
                return Result.failure(error)
            }
            
            val materials = materialsResult.getOrNull()!!
            Log.d(TAG, "REPO: ✓ Materials fetched: ${materials.size} items")
            
            Log.d(TAG, "REPO: ✓✓✓ SUCCESS! Loaded $topicType from cloud: ${materials.size} materials")
            
            Result.success(
                TopicContentData(
                    title = topic.title,
                    introduction = topic.introduction,
                    materials = materials,
                    source = ContentSource.CLOUD
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "REPO: Exception in loadFromCloud: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Return empty local placeholder with LOCAL flag
     * The ViewModel will detect this and use TopicContentLoader directly
     * This avoids circular dependencies (data layer can't access UI layer)
     */
    private fun loadFromLocal(topicType: String): Result<TopicContentData> {
        Log.d(TAG, "REPO: Returning LOCAL flag for $topicType (ViewModel will load from TopicContentLoader)")
        return Result.success(
            TopicContentData(
                title = "",
                introduction = "",
                materials = emptyList(),
                source = ContentSource.LOCAL
            )
        )
    }
    
    companion object {
        private const val TAG = "StudyContentRepo"
    }
}

/**
 * Combined topic content data
 */
data class TopicContentData(
    val title: String,
    val introduction: String,
    val materials: List<CloudStudyMaterial>,
    val source: ContentSource
)

/**
 * Content source indicator
 */
enum class ContentSource {
    CLOUD,
    LOCAL
}

