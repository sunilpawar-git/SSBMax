package com.ssbmax.core.domain.usecase.migration

import android.util.Log
import com.ssbmax.core.domain.repository.MigrationRepository
import javax.inject.Inject

/**
 * Use case to migrate OIR topic and study materials to Firestore
 *
 * Migrates:
 * - 1 topic introduction document
 * - 7 study material documents
 *
 * Total: 8 Firestore writes (~$0.0048)
 *
 * Architecture: Domain layer use case following clean architecture principles.
 * Depends on MigrationRepository interface, not concrete implementations.
 */
class MigrateOIRUseCase @Inject constructor(
    private val migrationRepository: MigrationRepository
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

            // Step 1: Migrate topic introduction via repository
            val topicMigrated = migrationRepository.migrateTopicContent("OIR")
                .onSuccess {
                    Log.d(TAG, "✓ Topic content migrated")
                }
                .onFailure { e ->
                    val error = "Topic migration failed: ${e.message}"
                    Log.e(TAG, error, e)
                    errors.add(error)
                }
                .isSuccess

            // Step 2: Migrate study materials via repository
            val materialsResult = migrationRepository.migrateStudyMaterials(
                topicType = "OIR",
                onProgress = { current, total ->
                    Log.d(TAG, "✓ Migrated material $current/$total")
                }
            )

            val materialsMigrated = materialsResult.getOrElse { e ->
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
    
    companion object {
        private const val TAG = "OIRMigration"
    }
}

