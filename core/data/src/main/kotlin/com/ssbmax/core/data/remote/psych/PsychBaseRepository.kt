package com.ssbmax.core.data.remote.psych

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.ssbmax.core.data.remote.OLQMapper
import com.ssbmax.core.data.remote.SubmissionConstants
import com.ssbmax.core.data.remote.mapper.PsychTestMapper
import com.ssbmax.core.domain.model.scoring.OLQAnalysisResult
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Base Repository for Psychology Tests.
 * Contains shared Firestore references and OLQ processing logic.
 * 
 * Part of the Refactoring Phase 2 (Split PsychTestSubmissionRepository).
 */
@Singleton
open class PsychBaseRepository @Inject constructor() {
    
    protected val firestore = FirebaseFirestore.getInstance()
    protected val submissionsCollection = firestore.collection("submissions")
    protected val psychResultsCollection = firestore.collection("psych_results")
    
    companion object {
        const val TAG = "PsychBaseRepo"
        const val FIELD_ID = "id"
        const val FIELD_USER_ID = "userId"
        const val FIELD_TEST_ID = "testId"
        const val FIELD_TEST_TYPE = "testType"
        const val FIELD_STATUS = "status"
        const val FIELD_SUBMITTED_AT = "submittedAt"
        const val FIELD_GRADED_BY_INSTRUCTOR_ID = "gradedByInstructorId"
        const val FIELD_GRADING_TIMESTAMP = "gradingTimestamp"
        const val FIELD_BATCH_ID = "batchId"
        const val FIELD_DATA = "data"
    }

    suspend fun updateOLQResult(submissionId: String, olqResult: OLQAnalysisResult): Result<Unit> {
        return try {
            // First, fetch the userId from the submission document
            // This is required by Firestore security rules for psych_results
            val submissionDoc = submissionsCollection.document(submissionId).get().await()
            val userId = submissionDoc.getString(FIELD_USER_ID)
                ?: throw Exception("Cannot find userId for submission: $submissionId")
            
            val olqResultMap = OLQMapper.toFirestoreMap(olqResult).toMutableMap()
            // CRITICAL: Add userId for Firestore security rules compliance
            olqResultMap["userId"] = userId
            
            Log.d(TAG, "📝 Writing OLQ result to psych_results for submission: $submissionId, userId: $userId")
            
            // 1. Write to psych_results collection (with userId for security rules)
            psychResultsCollection.document(submissionId)
                .set(olqResultMap, SetOptions.merge())
                .await()
            
            Log.d(TAG, "✅ Successfully wrote OLQ result to psych_results")
            
            // 2. Update analysis status in submissions collection
            submissionsCollection.document(submissionId)
                .update(
                    mapOf(
                        "$FIELD_DATA.analysisStatus" to SubmissionConstants.ANALYSIS_STATUS_COMPLETED
                    )
                )
                .await()
            
            Log.d(TAG, "✅ Successfully updated analysis status to COMPLETED")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update OLQ result: ${e.message}", e)
            Result.failure(Exception("Failed to update OLQ result: ${e.message}", e))
        }
    }

    suspend fun getOLQResult(submissionId: String): Result<OLQAnalysisResult?> {
        return try {
            // Priority 1: Fetch from psych_results (New Architecture)
            val resultDoc = psychResultsCollection.document(submissionId).get().await()
            if (resultDoc.exists()) {
                val data = resultDoc.data
                return Result.success(PsychTestMapper.parseOLQResult(data))
            }

            // Priority 2: Fallback to submissions collection (Legacy / Transitional)
            Log.w(TAG, "OLQ result not found in psych_results for $submissionId, checking submissions collection.")
            val submissionDoc = submissionsCollection.document(submissionId).get().await()
            if (submissionDoc.exists()) {
                 val data = submissionDoc.get(FIELD_DATA) as? Map<*, *>
                 val olqMap = data?.get("olqResult") as? Map<*, *>
                 if (olqMap != null) {
                      return Result.success(PsychTestMapper.parseOLQResult(olqMap))
                 }
            }
            
            Result.success(null)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get OLQ result: ${e.message}", e))
        }
    }
}
