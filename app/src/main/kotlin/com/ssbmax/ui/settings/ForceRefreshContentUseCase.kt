package com.ssbmax.ui.settings

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
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
 */
class ForceRefreshContentUseCase @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    
    data class RefreshResult(
        val topicsRefreshed: Int,
        val materialsRefreshed: Int,
        val errors: List<String>
    )
    
    suspend fun execute(): Result<RefreshResult> {
        return try {
            Log.d(TAG, "Force refreshing content from server...")
            
            val errors = mutableListOf<String>()
            var topicsCount = 0
            var materialsCount = 0
            
            // Force refresh all topics
            val topics = listOf(
                "OIR", "PPDT", "PSYCHOLOGY", "PIQ_FORM",
                "GTO", "INTERVIEW", "SSB_OVERVIEW", "MEDICALS", "CONFERENCE"
            )
            
            topics.forEach { topicId ->
                try {
                    // Force server fetch (bypasses cache)
                    firestore.collection("topic_content")
                        .document(topicId)
                        .get(Source.SERVER)
                        .await()
                    
                    topicsCount++
                    Log.d(TAG, "✓ Refreshed topic: $topicId")
                } catch (e: Exception) {
                    errors.add("Failed to refresh topic $topicId: ${e.message}")
                    Log.e(TAG, "Failed to refresh topic $topicId", e)
                }
            }
            
            // Force refresh all study materials
            topics.forEach { topicId ->
                try {
                    val snapshot = firestore.collection("study_materials")
                        .whereEqualTo("topicType", topicId)
                        .get(Source.SERVER)  // Force server fetch
                        .await()
                    
                    materialsCount += snapshot.size()
                    Log.d(TAG, "✓ Refreshed ${snapshot.size()} materials for $topicId")
                } catch (e: Exception) {
                    errors.add("Failed to refresh materials for $topicId: ${e.message}")
                    Log.e(TAG, "Failed to refresh materials for $topicId", e)
                }
            }
            
            val result = RefreshResult(
                topicsRefreshed = topicsCount,
                materialsRefreshed = materialsCount,
                errors = errors
            )
            
            Log.d(TAG, "✓ Force refresh complete: $topicsCount topics, $materialsCount materials")
            Result.success(result)
            
        } catch (e: Exception) {
            Log.e(TAG, "Force refresh failed", e)
            Result.failure(e)
        }
    }
    
    companion object {
        private const val TAG = "ForceRefresh"
    }
}

