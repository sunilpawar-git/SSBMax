package com.ssbmax.ui.settings

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.ui.study.StudyMaterialContentProvider
import com.ssbmax.ui.topic.StudyMaterialsProvider
import com.ssbmax.ui.topic.TopicContentLoader
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Use case to migrate OIR topic and study materials to Firestore
 * 
 * Migrates:
 * - 1 topic introduction document
 * - 7 study material documents
 * 
 * Total: 8 Firestore writes (~$0.0048)
 */
class MigrateOIRUseCase @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    
    data class MigrationResult(
        val success: Boolean,
        val topicMigrated: Boolean,
        val materialsMigrated: Int,
        val totalMaterials: Int,
        val errors: List<String> = emptyList(),
        val durationMs: Long = 0
    ) {
        val message: String
            get() = when {
                success -> "✓ SUCCESS: Migrated 1 topic + $materialsMigrated materials"
                topicMigrated && materialsMigrated > 0 -> 
                    "⚠ PARTIAL: Topic + $materialsMigrated/$totalMaterials materials (${errors.size} errors)"
                else -> "✗ FAILED: ${errors.firstOrNull() ?: "Unknown error"}"
            }
    }
    
    suspend fun execute(): Result<MigrationResult> {
        return try {
            val startTime = System.currentTimeMillis()
            val errors = mutableListOf<String>()
            
            Log.d(TAG, "Starting OIR migration...")
            
            // Step 1: Migrate topic introduction
            val topicMigrated = try {
                migrateTopicContent()
                Log.d(TAG, "✓ Topic content migrated")
                true
            } catch (e: Exception) {
                val error = "Topic migration failed: ${e.message}"
                Log.e(TAG, error, e)
                errors.add(error)
                false
            }
            
            // Step 2: Migrate study materials
            val materialsMigrated = try {
                migrateStudyMaterials(errors)
            } catch (e: Exception) {
                val error = "Materials migration failed: ${e.message}"
                Log.e(TAG, error, e)
                errors.add(error)
                0
            }
            
            val duration = System.currentTimeMillis() - startTime
            val result = MigrationResult(
                success = topicMigrated && materialsMigrated == 7 && errors.isEmpty(),
                topicMigrated = topicMigrated,
                materialsMigrated = materialsMigrated,
                totalMaterials = 7,
                errors = errors,
                durationMs = duration
            )
            
            Log.d(TAG, "Migration complete: ${result.message} (${duration}ms)")
            Result.success(result)
            
        } catch (e: Exception) {
            Log.e(TAG, "Migration failed", e)
            Result.failure(e)
        }
    }
    
    private suspend fun migrateTopicContent() {
        val topicInfo = TopicContentLoader.getTopicInfo("OIR")
        
        val topicDocument = mapOf(
            "id" to "OIR",
            "topicType" to "OIR",
            "title" to topicInfo.title,
            "introduction" to topicInfo.introduction,
            "version" to 1,
            "lastUpdated" to System.currentTimeMillis(),
            "isPremium" to false,
            "metadata" to mapOf(
                "migratedBy" to "MigrateOIRUseCase",
                "migratedAt" to System.currentTimeMillis()
            )
        )
        
        firestore.collection("topic_content")
            .document("OIR")
            .set(topicDocument)
            .await()
    }
    
    private suspend fun migrateStudyMaterials(errors: MutableList<String>): Int {
        val materials = StudyMaterialsProvider.getStudyMaterials("OIR")
        var successCount = 0
        
        for ((index, materialItem) in materials.withIndex()) {
            try {
                // Get full content from provider
                val fullContent = StudyMaterialContentProvider.getMaterial(materialItem.id)
                
                val materialDocument = mapOf(
                    "id" to materialItem.id,
                    "topicType" to "OIR",
                    "title" to fullContent.title,
                    "displayOrder" to (index + 1),
                    "category" to fullContent.category,
                    "contentMarkdown" to fullContent.content,
                    "author" to fullContent.author,
                    "readTime" to materialItem.duration,
                    "isPremium" to materialItem.isPremium,
                    "version" to 1,
                    "lastUpdated" to System.currentTimeMillis(),
                    "tags" to listOf("OIR", "Screening", "Intelligence Test"),
                    "relatedMaterials" to emptyList<String>(),
                    "attachments" to emptyList<Map<String, Any>>(),
                    "metadata" to mapOf(
                        "publishedDate" to fullContent.publishedDate,
                        "migratedBy" to "MigrateOIRUseCase",
                        "migratedAt" to System.currentTimeMillis()
                    )
                )
                
                // Use document ID to prevent duplicates on re-migration
                firestore.collection("study_materials")
                    .document(materialItem.id)  // Use material ID as document ID
                    .set(materialDocument)      // Replaces if exists, creates if not
                    .await()
                
                successCount++
                Log.d(TAG, "✓ Migrated material ${index + 1}/7: ${materialItem.id}")
                
            } catch (e: Exception) {
                val error = "Failed to migrate ${materialItem.id}: ${e.message}"
                Log.e(TAG, error, e)
                errors.add(error)
            }
        }
        
        return successCount
    }
    
    companion object {
        private const val TAG = "OIRMigration"
    }
}

