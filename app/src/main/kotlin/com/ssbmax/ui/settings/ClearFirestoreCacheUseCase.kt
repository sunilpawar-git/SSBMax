package com.ssbmax.ui.settings

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
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
 */
class ClearFirestoreCacheUseCase @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    
    suspend fun execute(): Result<Unit> {
        return try {
            Log.d(TAG, "Clearing Firestore cache...")
            
            // Clear Firestore cache
            // This forces all subsequent queries to fetch from server
            firestore.clearPersistence().await()
            
            Log.d(TAG, "✓ Firestore cache cleared successfully")
            Log.d(TAG, "Next content access will fetch fresh data from server")
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear Firestore cache", e)
            
            // If clearPersistence fails (app has active listeners), 
            // inform the user they may need to restart the app
            if (e.message?.contains("active") == true) {
                Result.failure(
                    Exception("Please restart the app to clear cache completely")
                )
            } else {
                Result.failure(e)
            }
        }
    }
    
    companion object {
        private const val TAG = "ClearCache"
    }
}

