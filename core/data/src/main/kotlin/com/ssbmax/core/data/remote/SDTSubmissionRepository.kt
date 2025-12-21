package com.ssbmax.core.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.ssbmax.core.data.remote.mapper.PsychTestMapper
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.model.scoring.AnalysisStatus
import com.ssbmax.core.domain.model.scoring.OLQAnalysisResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for SDT (Self Description Test) submissions.
 * Extracted from PsychTestSubmissionRepository during Phase 2 refactoring.
 * Adheres to 300-line limit and single responsibility principle.
 */
@Singleton
class SDTSubmissionRepository @Inject constructor() {

    private val firestore = FirebaseFirestore.getInstance()
    private val submissionsCollection = firestore.collection("submissions")
    private val psychResultsCollection = firestore.collection("psych_results")

    companion object {
        private const val TAG = "SDTRepo"
        private const val FIELD_ID = "id"
        private const val FIELD_USER_ID = "userId"
        private const val FIELD_TEST_ID = "testId"
        private const val FIELD_TEST_TYPE = "testType"
        private const val FIELD_STATUS = "status"
        private const val FIELD_SUBMITTED_AT = "submittedAt"
        private const val FIELD_GRADED_BY_INSTRUCTOR_ID = "gradedByInstructorId"
        private const val FIELD_GRADING_TIMESTAMP = "gradingTimestamp"
        private const val FIELD_BATCH_ID = "batchId"
        private const val FIELD_DATA = "data"
    }

    suspend fun submitSDT(submission: SDTSubmission, batchId: String?): Result<String> {
        return try {
            Log.d(TAG, "☁️ Firestore SDT: Preparing submission...")
            val submissionMap = mapOf(
                FIELD_ID to submission.id,
                FIELD_USER_ID to submission.userId,
                FIELD_TEST_ID to submission.testId,
                FIELD_TEST_TYPE to TestType.SD.name,
                FIELD_STATUS to submission.status.name,
                FIELD_SUBMITTED_AT to submission.submittedAt,
                FIELD_GRADED_BY_INSTRUCTOR_ID to submission.gradedByInstructorId,
                FIELD_GRADING_TIMESTAMP to submission.gradingTimestamp,
                FIELD_BATCH_ID to batchId,
                FIELD_DATA to submission.toFirestoreMap()
            )
            submissionsCollection.document(submission.id).set(submissionMap, SetOptions.merge()).await()
            Log.d(TAG, "✅ Firestore SDT: Successfully written!")
            Result.success(submission.id)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Firestore SDT: Failed - ${e.message}", e)
            Result.failure(Exception("Failed to submit SDT: ${e.message}", e))
        }
    }

    suspend fun getSDTSubmission(submissionId: String): Result<SDTSubmission?> {
        return try {
            val document = submissionsCollection.document(submissionId).get().await()
            if (!document.exists()) return Result.success(null)
            val data = document.get(FIELD_DATA) as? Map<*, *> ?: return Result.success(null)
            Result.success(PsychTestMapper.parseSDTSubmission(data))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to fetch SDT submission: ${e.message}", e))
        }
    }

    suspend fun getLatestSDTSubmission(userId: String): Result<SDTSubmission?> {
        return try {
            val snapshot = submissionsCollection
                .whereEqualTo(FIELD_USER_ID, userId)
                .whereEqualTo(FIELD_TEST_TYPE, TestType.SD.name)
                .orderBy(FIELD_SUBMITTED_AT, Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            if (snapshot.isEmpty) return Result.success(null)
            val doc = snapshot.documents.first()
            val data = doc.get(FIELD_DATA) as? Map<*, *> ?: return Result.success(null)
            Result.success(PsychTestMapper.parseSDTSubmission(data))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to fetch latest SDT submission: ${e.message}", e))
        }
    }

    suspend fun updateSDTAnalysisStatus(submissionId: String, status: AnalysisStatus): Result<Unit> {
        return try {
            submissionsCollection.document(submissionId).update("data.analysisStatus", status.name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to update SDT status: ${e.message}", e))
        }
    }

    suspend fun updateSDTOLQResult(submissionId: String, olqResult: OLQAnalysisResult): Result<Unit> {
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

    suspend fun getSDTResult(submissionId: String): Result<OLQAnalysisResult?> {
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

    fun observeSDTSubmission(submissionId: String): Flow<SDTSubmission?> = callbackFlow {
        val regressionFilter = OLQRegressionFilter()
        val listener = submissionsCollection.document(submissionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }
                try {
                    val data = snapshot.get(FIELD_DATA) as? Map<*, *>
                    if (data == null || regressionFilter.shouldFilterSnapshot(data, snapshot.metadata, "SDT", submissionId)) {
                        if (data == null) trySend(null)
                        return@addSnapshotListener
                    }
                    trySend(PsychTestMapper.parseSDTSubmission(data))
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing SDT submission", e)
                }
            }
        awaitClose { listener.remove() }
    }
}
