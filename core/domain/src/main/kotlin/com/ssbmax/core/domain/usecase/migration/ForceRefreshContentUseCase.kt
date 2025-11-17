package com.ssbmax.core.domain.usecase.migration

import android.util.Log
import com.ssbmax.core.domain.repository.MigrationRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

/**
 * Force refresh content from Firestore server (bypasses cache)
 *
 * This is more reliable than clearPersistence() because:
 * - Works while app is running
 * - Doesn't require app restart
 * - Doesn't clear authentication
 * - Directly fetches from server
 *
 * Use this when:
 * - Testing content changes from Firebase Console
 * - Need to see latest content immediately
 *
 * Architecture: Domain layer use case following clean architecture principles.
 * Depends on MigrationRepository interface, not concrete implementations.
 */
class ForceRefreshContentUseCase @Inject constructor(
    private val migrationRepository: MigrationRepository
) {

    data class RefreshResult(
        val topicsRefreshed: Int,
        val materialsRefreshed: Int,
        val errors: List<String>
    )

    suspend fun execute(): Result<RefreshResult> = coroutineScope {
        try {
            Log.d(TAG, "Force refreshing content from server...")

            val errors = mutableListOf<String>()
            var successCount = 0

            // Force refresh all topics
            val topics = listOf(
                "OIR", "PPDT", "PSYCHOLOGY", "PIQ_FORM",
                "GTO", "INTERVIEW", "SSB_OVERVIEW", "MEDICALS", "CONFERENCE"
            )

            topics.forEach { topicId ->
                val result = migrationRepository.forceRefreshContent(topicId)
                if (result.isSuccess) {
                    successCount++
                    Log.d(TAG, "✓ Refreshed content for: $topicId")
                } else {
                    val error = "Failed to refresh $topicId: ${result.exceptionOrNull()?.message}"
                    errors.add(error)
                    Log.e(TAG, error)
                }
            }

            val refreshResult = RefreshResult(
                topicsRefreshed = successCount,
                materialsRefreshed = 0, // Repository method refreshes both topic and materials
                errors = errors
            )

            Log.d(TAG, "✓ Force refresh complete: $successCount/${topics.size} topics refreshed")
            Result.success(refreshResult)

        } catch (e: Exception) {
            Log.e(TAG, "Force refresh failed", e)
            Result.failure(e)
        }
    }
    
    companion object {
        private const val TAG = "ForceRefresh"
    }
}

