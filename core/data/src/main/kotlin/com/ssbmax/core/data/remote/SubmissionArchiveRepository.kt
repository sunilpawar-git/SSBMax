package com.ssbmax.core.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for submission archival operations.
 * Handles moving old submissions to archived collection and cleanup.
 * 
 * Extracted from FirestoreSubmissionRepository during Phase 6 refactoring.
 */
@Singleton
class SubmissionArchiveRepository @Inject constructor() {
    
    private val firestore = FirebaseFirestore.getInstance()
    
    companion object {
        private const val TAG = "SubmissionArchiveRepo"
    }
    
    /**
     * Archive submissions older than the specified timestamp.
     * Moves data to archived_submissions collection and deletes from main collection.
     * 
     * @param beforeTimestamp Unix timestamp - submissions before this will be archived
     * @return Number of submissions successfully archived
     */
    suspend fun archiveOldSubmissions(beforeTimestamp: Long): Result<Int> {
        return try {
            Log.d(TAG, "Starting archival for submissions before $beforeTimestamp")
            
            // Query all submissions older than timestamp using collection group
            val submissionsSnapshot = firestore.collectionGroup("submissions")
                .whereLessThan("submittedAt", beforeTimestamp)
                .get()
                .await()
            
            if (submissionsSnapshot.isEmpty) {
                Log.d(TAG, "No submissions to archive")
                return Result.success(0)
            }
            
            var archivedCount = 0
            val failedArchives = mutableListOf<String>()
            
            // Archive each submission
            submissionsSnapshot.documents.forEach { doc ->
                try {
                    // Get submission data
                    val submissionData = doc.data ?: emptyMap()
                    val submissionId = submissionData["id"] as? String ?: doc.id
                    
                    // Move to archived_submissions collection
                    firestore.collection("archived_submissions")
                        .document(submissionId)
                        .set(submissionData)
                        .await()
                    
                    // Delete from original collection
                    doc.reference.delete().await()
                    
                    archivedCount++
                    
                    if (archivedCount % 10 == 0) {
                        Log.d(TAG, "Archived $archivedCount submissions so far...")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to archive submission ${doc.id}", e)
                    failedArchives.add(doc.id)
                }
            }
            
            Log.d(TAG, "✅ Archival complete: $archivedCount archived, ${failedArchives.size} failed")
            
            if (failedArchives.isNotEmpty()) {
                Log.w(TAG, "Failed to archive: ${failedArchives.joinToString()}")
            }
            
            Result.success(archivedCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Archival failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Delete old archived submissions.
     * This is a cleanup operation for data older than retention period.
     * 
     * @param beforeTimestamp Unix timestamp - archived submissions before this will be deleted
     * @return Number of submissions successfully deleted
     */
    suspend fun deleteOldArchivedSubmissions(beforeTimestamp: Long): Result<Int> {
        return try {
            Log.d(TAG, "Deleting archived submissions before $beforeTimestamp")
            
            val archivedSnapshot = firestore.collection("archived_submissions")
                .whereLessThan("submittedAt", beforeTimestamp)
                .get()
                .await()
            
            if (archivedSnapshot.isEmpty) {
                Log.d(TAG, "No archived submissions to delete")
                return Result.success(0)
            }
            
            var deletedCount = 0
            
            archivedSnapshot.documents.forEach { doc ->
                try {
                    doc.reference.delete().await()
                    deletedCount++
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete archived submission ${doc.id}", e)
                }
            }
            
            Log.d(TAG, "✅ Cleanup complete: $deletedCount archived submissions deleted")
            Result.success(deletedCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Cleanup failed", e)
            Result.failure(e)
        }
    }
}
