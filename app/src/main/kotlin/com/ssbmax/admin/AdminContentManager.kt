package com.ssbmax.admin

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Admin tool for managing Firestore content
 * Use this for bulk updates, content edits, and content management
 */
@Singleton
class AdminContentManager @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    
    /**
     * Update a single material's content
     * 
     * @param materialId The material ID (e.g., "oir_1", "ppdt_2")
     * @param newContent The new markdown content
     * @return Success or failure
     */
    suspend fun updateMaterialContent(
        materialId: String,
        newContent: String
    ): Result<Unit> {
        return try {
            Log.d(TAG, "Updating content for $materialId")
            
            firestore.collection("study_materials")
                .document(materialId)
                .update(
                    mapOf(
                        "contentMarkdown" to newContent,
                        "lastUpdated" to System.currentTimeMillis()
                    )
                )
                .await()
            
            Log.d(TAG, "✓ Content updated for $materialId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update $materialId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update material metadata (title, readTime, isPremium, etc.)
     * 
     * @param materialId The material ID
     * @param title New title (null to keep existing)
     * @param readTime New read time (null to keep existing)
     * @param isPremium Premium status (null to keep existing)
     * @param author New author (null to keep existing)
     * @param tags New tags list (null to keep existing)
     * @return Success or failure
     */
    suspend fun updateMaterialMetadata(
        materialId: String,
        title: String? = null,
        readTime: String? = null,
        isPremium: Boolean? = null,
        author: String? = null,
        tags: List<String>? = null
    ): Result<Unit> {
        return try {
            Log.d(TAG, "Updating metadata for $materialId")
            
            val updates = mutableMapOf<String, Any>(
                "lastUpdated" to System.currentTimeMillis()
            )
            
            title?.let { updates["title"] = it }
            readTime?.let { updates["readTime"] = it }
            isPremium?.let { updates["isPremium"] = it }
            author?.let { updates["author"] = it }
            tags?.let { updates["tags"] = it }
            
            firestore.collection("study_materials")
                .document(materialId)
                .update(updates)
                .await()
            
            Log.d(TAG, "✓ Metadata updated for $materialId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update metadata for $materialId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Bulk update content for multiple materials
     * 
     * @param updates Map of materialId to new content
     * @return Number of successful updates
     */
    suspend fun bulkUpdateContent(
        updates: Map<String, String>
    ): Result<BulkUpdateResult> {
        return try {
            Log.d(TAG, "Starting bulk update for ${updates.size} materials")
            var successCount = 0
            val errors = mutableListOf<String>()
            
            updates.forEach { (materialId, newContent) ->
                updateMaterialContent(materialId, newContent)
                    .onSuccess { 
                        successCount++
                        Log.d(TAG, "✓ Updated $materialId ($successCount/${updates.size})")
                    }
                    .onFailure { error ->
                        errors.add("$materialId: ${error.message}")
                        Log.e(TAG, "✗ Failed to update $materialId", error)
                    }
            }
            
            val result = BulkUpdateResult(
                totalAttempted = updates.size,
                successCount = successCount,
                failureCount = updates.size - successCount,
                errors = errors
            )
            
            Log.d(TAG, "Bulk update complete: $successCount/${updates.size} successful")
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Bulk update failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get current content for a material
     * 
     * @param materialId The material ID
     * @return The material content
     */
    suspend fun getMaterialContent(materialId: String): Result<MaterialData> {
        return try {
            Log.d(TAG, "Fetching content for $materialId")
            
            val doc = firestore.collection("study_materials")
                .document(materialId)
                .get()
                .await()
            
            if (!doc.exists()) {
                throw Exception("Material not found: $materialId")
            }
            
            val materialData = MaterialData(
                id = doc.getString("id") ?: materialId,
                title = doc.getString("title") ?: "",
                content = doc.getString("contentMarkdown") ?: "",
                readTime = doc.getString("readTime") ?: "",
                author = doc.getString("author") ?: "",
                isPremium = doc.getBoolean("isPremium") ?: false,
                tags = doc.get("tags") as? List<String> ?: emptyList(),
                lastUpdated = doc.getLong("lastUpdated") ?: 0L
            )
            
            Log.d(TAG, "✓ Fetched content for $materialId")
            Result.success(materialData)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch $materialId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Add a new material to Firestore
     * 
     * @param material The new material data
     * @return Success or failure
     */
    suspend fun addNewMaterial(material: NewMaterial): Result<Unit> {
        return try {
            Log.d(TAG, "Adding new material: ${material.id}")
            
            val materialDocument = mapOf(
                "id" to material.id,
                "topicType" to material.topicType,
                "title" to material.title,
                "contentMarkdown" to material.content,
                "displayOrder" to material.displayOrder,
                "category" to material.category,
                "author" to material.author,
                "readTime" to material.readTime,
                "isPremium" to material.isPremium,
                "version" to 1,
                "lastUpdated" to System.currentTimeMillis(),
                "tags" to material.tags,
                "relatedMaterials" to emptyList<String>(),
                "metadata" to mapOf(
                    "createdBy" to "AdminContentManager",
                    "createdAt" to System.currentTimeMillis()
                )
            )
            
            firestore.collection("study_materials")
                .document(material.id)
                .set(materialDocument)
                .await()
            
            Log.d(TAG, "✓ Added new material: ${material.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add material: ${material.id}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Delete a material from Firestore
     * ⚠️ Use with caution! No undo!
     * 
     * @param materialId The material ID to delete
     * @return Success or failure
     */
    suspend fun deleteMaterial(materialId: String): Result<Unit> {
        return try {
            Log.w(TAG, "⚠️ DELETING material: $materialId")
            
            firestore.collection("study_materials")
                .document(materialId)
                .delete()
                .await()
            
            Log.w(TAG, "✓ Deleted material: $materialId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete $materialId", e)
            Result.failure(e)
        }
    }
    
    /**
     * List all materials for a topic
     * 
     * @param topicType The topic type (e.g., "OIR", "PPDT")
     * @return List of material IDs
     */
    suspend fun listMaterialsForTopic(topicType: String): Result<List<String>> {
        return try {
            Log.d(TAG, "Listing materials for $topicType")
            
            val snapshot = firestore.collection("study_materials")
                .whereEqualTo("topicType", topicType.uppercase())
                .get()
                .await()
            
            val materialIds = snapshot.documents.map { it.id }
            
            Log.d(TAG, "✓ Found ${materialIds.size} materials for $topicType")
            Result.success(materialIds)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list materials for $topicType", e)
            Result.failure(e)
        }
    }
    
    companion object {
        private const val TAG = "AdminContentManager"
    }
}

/**
 * Result of bulk update operation
 */
data class BulkUpdateResult(
    val totalAttempted: Int,
    val successCount: Int,
    val failureCount: Int,
    val errors: List<String>
) {
    val successRate: Float
        get() = if (totalAttempted > 0) 
            (successCount.toFloat() / totalAttempted.toFloat()) * 100 
        else 0f
}

/**
 * Material data retrieved from Firestore
 */
data class MaterialData(
    val id: String,
    val title: String,
    val content: String,
    val readTime: String,
    val author: String,
    val isPremium: Boolean,
    val tags: List<String>,
    val lastUpdated: Long
)

/**
 * Data for creating a new material
 */
data class NewMaterial(
    val id: String,
    val topicType: String,
    val title: String,
    val content: String,
    val displayOrder: Int,
    val category: String = topicType,
    val author: String = "SSB Expert",
    val readTime: String = "10 min read",
    val isPremium: Boolean = false,
    val tags: List<String> = listOf(topicType)
)

