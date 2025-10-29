package com.ssbmax.ui.settings

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.ui.study.StudyMaterialContentProvider
import com.ssbmax.ui.topic.StudyMaterialsProvider
import com.ssbmax.ui.topic.TopicContentLoader
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Use case for migrating PIQ_FORM topic and study materials to Firestore
 * Based on successful OIR, PPDT, and Psychology migration pattern
 */
class MigratePIQFormUseCase @Inject constructor(
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
                "✓ PIQ Form migration successful! Migrated $materialsMigrated/$totalMaterials materials"
            } else {
                "⚠ PIQ Form migration completed with issues: $materialsMigrated/$totalMaterials materials migrated"
            }
    }
    
    suspend fun execute(): Result<MigrationResult> {
        return try {
            val startTime = System.currentTimeMillis()
            val errors = mutableListOf<String>()
            
            Log.d(TAG, "Starting PIQ Form migration...")
            
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
            
            // Step 2: Migrate study materials (3 materials: piq_1 to piq_3)
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
                success = topicMigrated && materialsMigrated == 3 && errors.isEmpty(),
                topicMigrated = topicMigrated,
                materialsMigrated = materialsMigrated,
                totalMaterials = 3,
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
        val topicInfo = TopicContentLoader.getTopicInfo("PIQ_FORM")
        
        val topicDocument = mapOf(
            "id" to "PIQ_FORM",
            "topicType" to "PIQ_FORM",
            "title" to topicInfo.title,
            "introduction" to topicInfo.introduction,
            "version" to 1,
            "lastUpdated" to System.currentTimeMillis(),
            "isPremium" to false,
            "metadata" to mapOf(
                "migratedBy" to "MigratePIQFormUseCase",
                "migratedAt" to System.currentTimeMillis()
            )
        )
        
        firestore.collection("topic_content")
            .document("PIQ_FORM")
            .set(topicDocument)
            .await()
    }
    
    private suspend fun migrateStudyMaterials(errors: MutableList<String>): Int {
        val materials = StudyMaterialsProvider.getStudyMaterials("PIQ_FORM")
        var successCount = 0
        
        for ((index, materialItem) in materials.withIndex()) {
            try {
                // Get full content from provider (with fallback for missing content)
                val fullContent = try {
                    StudyMaterialContentProvider.getMaterial(materialItem.id)
                } catch (e: Exception) {
                    // Fallback content if not in provider yet
                    Log.w(TAG, "Using fallback content for ${materialItem.id}")
                    createFallbackContent(materialItem)
                }
                
                val materialDocument = mapOf(
                    "id" to materialItem.id,
                    "topicType" to "PIQ_FORM",
                    "title" to fullContent.title,
                    "displayOrder" to (index + 1),
                    "category" to fullContent.category,
                    "contentMarkdown" to fullContent.content,
                    "author" to fullContent.author,
                    "readTime" to materialItem.duration,
                    "isPremium" to materialItem.isPremium,
                    "version" to 1,
                    "lastUpdated" to System.currentTimeMillis(),
                    "tags" to listOf("PIQ Form", "Personal Information", "SSB Preparation"),
                    "relatedMaterials" to emptyList<String>(),
                    "attachments" to emptyList<Map<String, Any>>(),
                    "metadata" to mapOf(
                        "publishedDate" to fullContent.publishedDate,
                        "migratedBy" to "MigratePIQFormUseCase",
                        "migratedAt" to System.currentTimeMillis()
                    )
                )
                
                // Use document ID to prevent duplicates on re-migration
                firestore.collection("study_materials")
                    .document(materialItem.id)  // Use material ID as document ID
                    .set(materialDocument)      // Replaces if exists, creates if not
                    .await()
                
                successCount++
                Log.d(TAG, "✓ Migrated material ${index + 1}/3: ${materialItem.id}")
                
            } catch (e: Exception) {
                val error = "Failed to migrate ${materialItem.id}: ${e.message}"
                Log.e(TAG, error, e)
                errors.add(error)
            }
        }
        
        return successCount
    }
    
    private fun createFallbackContent(materialItem: com.ssbmax.ui.topic.StudyMaterialItem): com.ssbmax.ui.study.StudyMaterialContent {
        return com.ssbmax.ui.study.StudyMaterialContent(
            id = materialItem.id,
            title = materialItem.title,
            category = "PIQ Form",
            author = "SSB Expert",
            publishedDate = "Oct 29, 2025",
            readTime = materialItem.duration,
            content = """
# ${materialItem.title}

This comprehensive guide will help you understand and complete the Personal Information Questionnaire (PIQ) form correctly for your SSB interview.

## Coming Soon

Detailed content for this material is being prepared and will be available soon.

**Key Points to Remember**:
- Fill the PIQ form honestly and accurately
- Maintain consistency across all sections
- Be prepared to explain every detail
- Know your PIQ thoroughly for the interview
- Update if any information changes

Stay tuned for the complete guide!
            """.trimIndent(),
            isPremium = false,
            tags = listOf("PIQ Form", "SSB Preparation"),
            relatedMaterials = emptyList()
        )
    }
    
    companion object {
        private const val TAG = "PIQFormMigration"
    }
}

