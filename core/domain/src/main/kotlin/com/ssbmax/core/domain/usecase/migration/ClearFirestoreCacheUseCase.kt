package com.ssbmax.core.domain.usecase.migration

import android.util.Log
import com.ssbmax.core.domain.repository.MigrationRepository
import javax.inject.Inject

/**
 * Use case for clearing Firestore cache
 *
 * This is useful when:
 * - Testing content changes from Firebase Console
 * - Debugging content loading issues
 * - Forcing fresh data from server
 *
 * Note: This clears the offline persistence cache, forcing the app
 * to fetch fresh data from Firestore on next access.
 *
 * Architecture: Domain layer use case following clean architecture principles.
 * Depends on MigrationRepository interface, not concrete implementations.
 */
class ClearFirestoreCacheUseCase @Inject constructor(
    private val migrationRepository: MigrationRepository
) {

    suspend fun execute(): Result<Unit> {
        Log.d(TAG, "Clearing Firestore cache...")

        return migrationRepository.clearFirestoreCache()
            .onSuccess {
                Log.d(TAG, "✓ Firestore cache cleared successfully")
                Log.d(TAG, "Next content access will fetch fresh data from server")
            }
            .onFailure { e ->
                Log.e(TAG, "Failed to clear Firestore cache", e)
            }
    }
    
    companion object {
        private const val TAG = "ClearCache"
    }
}

