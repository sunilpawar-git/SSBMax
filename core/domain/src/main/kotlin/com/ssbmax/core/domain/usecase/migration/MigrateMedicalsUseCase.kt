package com.ssbmax.ui.settings

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.ui.study.StudyMaterialContentProvider
import com.ssbmax.ui.topic.StudyMaterialsProvider
import com.ssbmax.ui.topic.TopicContentLoader
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Use case for migrating Medicals topic and study materials to Firestore
 */
class MigrateMedicalsUseCase @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    
    data class MigrationResult(
        val success: Boolean,
        val topicMigrated: Boolean,
        val materialsMigrated: Int,
        val totalMaterials: Int,
        val errors: List<String>,
        val durationMs: Long
    ) {
        val message: String
            get() = if (success) {
                "✓ Medicals migration successful! Migrated $materialsMigrated/$totalMaterials materials"
            } else {
                "⚠ Medicals migration completed with issues: $materialsMigrated/$totalMaterials materials migrated"
            }
    }
    
    suspend fun execute(): Result<MigrationResult> {
        return try {
            val startTime = System.currentTimeMillis()
            val errors = mutableListOf<String>()
            
            Log.d(TAG, "Starting Medicals migration...")
            
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
                success = topicMigrated && materialsMigrated == 5 && errors.isEmpty(),
                topicMigrated = topicMigrated,
                materialsMigrated = materialsMigrated,
                totalMaterials = 5,
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
        val topicInfo = TopicContentLoader.getTopicInfo("MEDICALS")
        
        val topicDocument = mapOf(
            "id" to "MEDICALS",
            "topicType" to "MEDICALS",
            "title" to topicInfo.title,
            "introduction" to topicInfo.introduction,
            "version" to 1,
            "lastUpdated" to System.currentTimeMillis(),
            "isPremium" to false,
            "metadata" to mapOf(
                "migratedBy" to "MigrateMedicalsUseCase",
                "migratedAt" to System.currentTimeMillis()
            )
        )
        
        firestore.collection("topic_content")
            .document("MEDICALS")
            .set(topicDocument)
            .await()
    }
    
    private suspend fun migrateStudyMaterials(errors: MutableList<String>): Int {
        val materials = StudyMaterialsProvider.getStudyMaterials("MEDICALS")
        var successCount = 0
        
        for ((index, materialItem) in materials.withIndex()) {
            try {
                val fullContent = StudyMaterialContentProvider.getMaterial(materialItem.id)
                
                val materialDocument = mapOf(
                    "id" to materialItem.id,
                    "topicType" to "MEDICALS",
                    "title" to fullContent.title,
                    "displayOrder" to (index + 1),
                    "category" to fullContent.category,
                    "contentMarkdown" to fullContent.content,
                    "author" to fullContent.author,
                    "readTime" to materialItem.duration,
                    "isPremium" to materialItem.isPremium,
                    "version" to 1,
                    "lastUpdated" to System.currentTimeMillis(),
                    "tags" to listOf("Medicals", "Medical Standards", "Physical Fitness"),
                    "relatedMaterials" to emptyList<String>(),
                    "metadata" to mapOf(
                        "publishedDate" to fullContent.publishedDate,
                        "migratedBy" to "MigrateMedicalsUseCase",
                        "migratedAt" to System.currentTimeMillis()
                    )
                )
                
                firestore.collection("study_materials")
                    .document(materialItem.id)
                    .set(materialDocument)
                    .await()
                
                successCount++
                Log.d(TAG, "✓ Migrated material ${index + 1}/5: ${materialItem.id}")
                
            } catch (e: Exception) {
                val error = "Failed to migrate ${materialItem.id}: ${e.message}"
                Log.e(TAG, error, e)
                errors.add(error)
            }
        }
        
        return successCount
    }
    
    companion object {
        private const val TAG = "MedicalsMigration"
    }
}

