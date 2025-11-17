package com.ssbmax.core.domain.usecase.migration

import android.util.Log
import com.ssbmax.core.domain.repository.MigrationRepository
import javax.inject.Inject

/**
 * Use case for migrating Piq Form topic and study materials to Firestore
 *
 * Migrates:
 * - 1 topic introduction document
 * - 3 study material documents
 *
 * Total: 4 Firestore writes
 *
 * Architecture: Domain layer use case following clean architecture principles.
 * Depends on MigrationRepository interface, not concrete implementations.
 */
class MigratePIQFormUseCase @Inject constructor(
    private val migrationRepository: MigrationRepository
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
                "✓ Piq Form migration successful! Migrated $materialsMigrated/$totalMaterials materials"
            } else {
                "⚠ Piq Form migration completed with issues: $materialsMigrated/$totalMaterials materials migrated"
            }
    }

    suspend fun execute(): Result<MigrationResult> {
        return try {
            val startTime = System.currentTimeMillis()
            val errors = mutableListOf<String>()

            Log.d(TAG, "Starting Piq Form migration...")

            // Step 1: Migrate topic introduction via repository
            val topicMigrated = migrationRepository.migrateTopicContent("PIQ_FORM")
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
                topicType = "PIQ_FORM",
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

    companion object {
        private const val TAG = "PIQFormMigration"
    }
}
