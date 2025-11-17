package com.ssbmax.core.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.core.domain.repository.MigrationRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Implementation of MigrationRepository using Firebase Firestore
 *
 * This implementation handles migration of study content from local providers to Firestore.
 *
 * Architecture Note:
 * This class depends on UI layer providers (StudyMaterialContentProvider, etc.) which is
 * a temporary architectural compromise during the migration process. These providers should
 * eventually be moved to the data layer or refactored into repository implementations.
 *
 * @param firestore Firebase Firestore instance for cloud operations
 * @param contentProviders Provider interface to access local content data
 */
class MigrationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val contentProviders: MigrationContentProviders
) : MigrationRepository {

    override suspend fun migrateTopicContent(topicType: String): Result<Unit> {
        return try {
            Log.d(TAG, "Migrating topic content for: $topicType")

            val topicInfo = contentProviders.getTopicInfo(topicType)

            val topicDocument = mapOf(
                "id" to topicType,
                "topicType" to topicType,
                "title" to topicInfo.title,
                "introduction" to topicInfo.introduction,
                "version" to 1,
                "lastUpdated" to System.currentTimeMillis(),
                "isPremium" to false,
                "metadata" to mapOf(
                    "migratedBy" to "MigrationRepositoryImpl",
                    "migratedAt" to System.currentTimeMillis()
                )
            )

            firestore.collection("topic_content")
                .document(topicType)
                .set(topicDocument)
                .await()

            Log.d(TAG, "✓ Topic content migrated successfully: $topicType")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to migrate topic content for $topicType", e)
            Result.failure(e)
        }
    }

    override suspend fun migrateStudyMaterials(
        topicType: String,
        onProgress: (current: Int, total: Int) -> Unit
    ): Result<Int> {
        return try {
            Log.d(TAG, "Migrating study materials for: $topicType")

            val materials = contentProviders.getStudyMaterials(topicType)
            val total = materials.size
            var successCount = 0

            for ((index, materialItem) in materials.withIndex()) {
                try {
                    // Get full content from provider
                    val fullContent = contentProviders.getMaterialContent(materialItem.id)

                    val materialDocument = mapOf(
                        "id" to materialItem.id,
                        "topicType" to topicType,
                        "title" to fullContent.title,
                        "displayOrder" to (index + 1),
                        "category" to fullContent.category,
                        "contentMarkdown" to fullContent.content,
                        "author" to fullContent.author,
                        "readTime" to materialItem.duration,
                        "isPremium" to materialItem.isPremium,
                        "version" to 1,
                        "lastUpdated" to System.currentTimeMillis(),
                        "tags" to fullContent.tags,
                        "relatedMaterials" to emptyList<String>(),
                        "attachments" to emptyList<Map<String, Any>>(),
                        "metadata" to mapOf(
                            "publishedDate" to fullContent.publishedDate,
                            "migratedBy" to "MigrationRepositoryImpl",
                            "migratedAt" to System.currentTimeMillis()
                        )
                    )

                    // Use document ID to prevent duplicates on re-migration
                    firestore.collection("study_materials")
                        .document(materialItem.id)
                        .set(materialDocument)
                        .await()

                    successCount++
                    onProgress(index + 1, total)
                    Log.d(TAG, "✓ Migrated material ${index + 1}/$total: ${materialItem.id}")

                } catch (e: Exception) {
                    Log.e(TAG, "Failed to migrate material: ${materialItem.id}", e)
                    // Continue with other materials even if one fails
                }
            }

            Log.d(TAG, "✓ Migrated $successCount/$total materials for $topicType")
            Result.success(successCount)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to migrate materials for $topicType", e)
            Result.failure(e)
        }
    }

    override suspend fun clearFirestoreCache(): Result<Unit> {
        return try {
            Log.d(TAG, "Clearing Firestore cache...")

            firestore.clearPersistence().await()

            Log.d(TAG, "✓ Firestore cache cleared successfully")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear Firestore cache", e)

            // If clearPersistence fails (app has active listeners),
            // inform the caller they may need to restart the app
            if (e.message?.contains("active", ignoreCase = true) == true) {
                Result.failure(
                    Exception("Please restart the app to clear cache completely", e)
                )
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun forceRefreshContent(topicType: String): Result<Unit> {
        return try {
            Log.d(TAG, "Force refreshing content for: $topicType")

            // Force get from server by using Source.SERVER
            firestore.collection("topic_content")
                .document(topicType)
                .get(com.google.firebase.firestore.Source.SERVER)
                .await()

            // Also refresh study materials
            firestore.collection("study_materials")
                .whereEqualTo("topicType", topicType)
                .get(com.google.firebase.firestore.Source.SERVER)
                .await()

            Log.d(TAG, "✓ Content refreshed from server: $topicType")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh content for $topicType", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "MigrationRepository"
    }
}

/**
 * Provider interface for migration content data
 *
 * This interface abstracts the source of content data for migration.
 * It will be implemented by a bridge class that delegates to the UI layer providers
 * until those providers are refactored into proper data layer implementations.
 */
interface MigrationContentProviders {

    /**
     * Data class representing topic information
     */
    data class TopicInfo(
        val title: String,
        val introduction: String
    )

    /**
     * Data class representing a study material item
     */
    data class MaterialItem(
        val id: String,
        val duration: String,
        val isPremium: Boolean
    )

    /**
     * Data class representing full material content
     */
    data class MaterialContent(
        val title: String,
        val category: String,
        val content: String,
        val author: String,
        val publishedDate: String,
        val tags: List<String>
    )

    /**
     * Get topic information for migration
     */
    fun getTopicInfo(topicType: String): TopicInfo

    /**
     * Get list of study materials for a topic
     */
    fun getStudyMaterials(topicType: String): List<MaterialItem>

    /**
     * Get full content for a specific material
     */
    fun getMaterialContent(materialId: String): MaterialContent
}
