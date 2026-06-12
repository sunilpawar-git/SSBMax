package com.ssbmax.core.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.ssbmax.core.domain.model.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for PIQ personal test submissions.
 * Handles Firestore CRUD for PIQSubmission documents.
 */
@Singleton
class PIQPersonalSubmissionDataSource @Inject constructor() {

    private val firestore = FirebaseFirestore.getInstance()
    private val submissionsCollection = firestore.collection("submissions")

    companion object {
        private const val TAG = "PIQSubmissionDS"
        private const val FIELD_USER_ID = "userId"
        private const val FIELD_TEST_TYPE = "testType"
        private const val FIELD_STATUS = "status"
        private const val FIELD_SUBMITTED_AT = "submittedAt"
    }

    suspend fun submitPIQ(submission: PIQSubmission, batchId: String?): Result<String> {
        return try {
            Log.d(TAG, "☁️ Firestore PIQ: Preparing submission...")

            val submissionMap = mapOf(
                "id" to submission.id,
                FIELD_USER_ID to submission.userId,
                "testId" to submission.testId,
                FIELD_TEST_TYPE to TestType.PIQ.name,
                FIELD_STATUS to submission.status.name,
                FIELD_SUBMITTED_AT to submission.submittedAt,
                "gradedByInstructorId" to submission.gradedByInstructorId,
                "gradingTimestamp" to submission.gradingTimestamp,
                "batchId" to batchId,
                "data" to submission.toMap()
            )

            submissionsCollection.document(submission.id)
                .set(submissionMap, SetOptions.merge())
                .await()

            Log.d(TAG, "✅ Firestore PIQ: Successfully written!")
            Result.success(submission.id)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Firestore PIQ: Failed - ${e.message}", e)
            Result.failure(Exception("Failed to submit PIQ: ${e.message}", e))
        }
    }

    suspend fun getLatestPIQSubmission(userId: String): Result<PIQSubmission?> {
        return try {
            val query = submissionsCollection
                .whereEqualTo(FIELD_USER_ID, userId)
                .whereEqualTo(FIELD_TEST_TYPE, "PIQ")
                .orderBy(FIELD_SUBMITTED_AT, Query.Direction.DESCENDING)
                .limit(1)

            val snapshot = query.get().await()

            if (snapshot.isEmpty) {
                Result.success(null)
            } else {
                val data = snapshot.documents[0].get("data") as? Map<*, *>
                if (data == null) {
                    Log.w(TAG, "PIQ submission found but data field is null")
                    Result.success(null)
                } else {
                    Result.success(parsePIQSubmission(data))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting latest PIQ submission", e)
            Result.failure(e)
        }
    }
}
