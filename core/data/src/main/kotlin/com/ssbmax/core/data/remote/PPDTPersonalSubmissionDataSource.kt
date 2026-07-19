package com.ssbmax.core.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ssbmax.shared.domain.model.*
import com.ssbmax.shared.domain.model.scoring.AnalysisStatus
import com.ssbmax.shared.domain.model.scoring.OLQAnalysisResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for PPDT personal test submissions.
 * Handles Firestore CRUD for PPDTSubmission documents.
 */
@Singleton
class PPDTPersonalSubmissionDataSource @Inject constructor() {

    private val firestore = FirebaseFirestore.getInstance()
    private val submissionsCollection = firestore.collection("submissions")
    private val ppdtResultsCollection = firestore.collection("ppdt_results")

    companion object {
        private const val TAG = "PPDTSubmissionDS"
        private const val FIELD_ID = "id"
        private const val FIELD_USER_ID = "userId"
        private const val FIELD_TEST_TYPE = "testType"
        private const val FIELD_STATUS = "status"
        private const val FIELD_SUBMITTED_AT = "submittedAt"
        private const val FIELD_DATA = "data"
    }

    suspend fun submitPPDT(submission: PPDTSubmission, batchId: String?): Result<String> {
        return try {
            val submissionMap = mapOf(
                FIELD_ID to submission.submissionId,
                FIELD_USER_ID to submission.userId,
                "testId" to submission.questionId,
                FIELD_TEST_TYPE to TestType.PPDT.name,
                FIELD_STATUS to submission.status.name,
                FIELD_SUBMITTED_AT to submission.submittedAt,
                "gradedByInstructorId" to null,
                "gradingTimestamp" to null,
                "batchId" to batchId,
                FIELD_DATA to submission.toFirestoreMap()
            )

            submissionsCollection.document(submission.submissionId)
                .set(submissionMap)
                .await()

            Result.success(submission.submissionId)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to submit PPDT: ${e.message}", e))
        }
    }

    suspend fun getPPDTSubmission(submissionId: String): Result<PPDTSubmission?> {
        return try {
            val snapshot = try {
                submissionsCollection.document(submissionId)
                    .get(com.google.firebase.firestore.Source.SERVER).await()
            } catch (e: Exception) {
                Log.w(TAG, "Server fetch failed for PPDT, using cache: ${e.message}")
                submissionsCollection.document(submissionId)
                    .get(com.google.firebase.firestore.Source.CACHE).await()
            }

            if (snapshot.exists()) {
                val data = snapshot.get(FIELD_DATA) as? Map<*, *>
                Result.success(data?.let { parsePPDTSubmission(it) })
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get PPDT submission: ${e.message}", e))
        }
    }

    suspend fun getLatestPPDTSubmission(userId: String): Result<PPDTSubmission?> {
        return try {
            val query = submissionsCollection
                .whereEqualTo(FIELD_USER_ID, userId)
                .whereEqualTo(FIELD_TEST_TYPE, "PPDT")
                .orderBy(FIELD_SUBMITTED_AT, Query.Direction.DESCENDING)
                .limit(1)

            val snapshot = try {
                query.get(com.google.firebase.firestore.Source.SERVER).await()
            } catch (e: Exception) {
                Log.w(TAG, "Server fetch failed, falling back to cache: ${e.message}")
                query.get(com.google.firebase.firestore.Source.CACHE).await()
            }

            if (snapshot.isEmpty) {
                Result.success(null)
            } else {
                val data = snapshot.documents[0].get("data") as? Map<*, *>
                if (data == null) {
                    Log.w(TAG, "PPDT submission found but data field is null")
                    Result.success(null)
                } else {
                    Result.success(parsePPDTSubmission(data))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting latest PPDT submission", e)
            Result.failure(e)
        }
    }

    suspend fun updatePPDTAnalysisStatus(submissionId: String, status: AnalysisStatus): Result<Unit> {
        return try {
            submissionsCollection.document(submissionId)
                .update("$FIELD_DATA.analysisStatus", status.name)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to update PPDT status: ${e.message}", e))
        }
    }

    suspend fun updatePPDTOLQResult(submissionId: String, olqResult: OLQAnalysisResult): Result<Unit> {
        return try {
            Log.d(TAG, "📝 Writing PPDT OLQ result to ppdt_results: $submissionId")

            val submissionDoc = submissionsCollection.document(submissionId).get().await()
            val userId = submissionDoc.getString(FIELD_USER_ID)
                ?: throw Exception("userId not found in submission")

            val olqScoresMap = olqResult.olqScores.mapKeys { it.key.name }.mapValues { entry ->
                mapOf(
                    "score" to entry.value.score,
                    "confidence" to entry.value.confidence,
                    "reasoning" to entry.value.reasoning
                )
            }

            val resultData = mapOf(
                "submissionId" to submissionId,
                "userId" to userId,
                "testType" to "PPDT",
                "olqScores" to olqScoresMap,
                "overallScore" to olqResult.overallScore,
                "overallRating" to olqResult.overallRating,
                "strengths" to olqResult.strengths,
                "weaknesses" to olqResult.weaknesses,
                "recommendations" to olqResult.recommendations,
                "aiConfidence" to olqResult.aiConfidence,
                "analyzedAt" to olqResult.analyzedAt
            )

            firestore.runBatch { batch ->
                batch.set(ppdtResultsCollection.document(submissionId), resultData)
                batch.update(
                    submissionsCollection.document(submissionId),
                    mapOf(
                        "$FIELD_DATA.analysisStatus" to SubmissionConstants.ANALYSIS_STATUS_COMPLETED,
                        FIELD_STATUS to "COMPLETED"
                    )
                )
            }.await()

            Log.d(TAG, "✅ PPDT result written and status updated")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update PPDT OLQ result", e)
            Result.failure(Exception("Failed to update PPDT OLQ result: ${e.message}", e))
        }
    }

    suspend fun getPPDTResult(submissionId: String): Result<OLQAnalysisResult?> {
        return try {
            Log.d(TAG, "📥 Fetching PPDT result from ppdt_results: $submissionId")

            val doc = try {
                ppdtResultsCollection.document(submissionId)
                    .get(com.google.firebase.firestore.Source.SERVER).await()
            } catch (e: Exception) {
                Log.w(TAG, "Server fetch failed for ppdt_results, using cache: ${e.message}")
                ppdtResultsCollection.document(submissionId)
                    .get(com.google.firebase.firestore.Source.CACHE).await()
            }

            if (!doc.exists()) {
                Log.d(TAG, "   - No ppdt_results document found")
                return Result.success(null)
            }

            val data = doc.data ?: return Result.success(null)
            val parsed = parsePPDTOLQAnalysisResult(data, submissionId)
            Log.d(TAG, "✅ PPDT result loaded: ${parsed?.olqScores?.size} OLQ scores")
            Result.success(parsed)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching PPDT result", e)
            Result.failure(e)
        }
    }

    fun observePPDTSubmission(submissionId: String): Flow<PPDTSubmission?> = callbackFlow {
        val regressionFilter = OLQRegressionFilter()

        val listener = submissionsCollection.document(submissionId).addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) {
                trySend(null)
                return@addSnapshotListener
            }

            try {
                val data = snapshot.get(FIELD_DATA) as? Map<*, *>
                if (data == null) {
                    trySend(null)
                    return@addSnapshotListener
                }

                if (regressionFilter.shouldFilterSnapshot(data, snapshot.metadata, "PPDT", submissionId)) {
                    return@addSnapshotListener
                }

                trySend(parsePPDTSubmission(data))
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing PPDT submission", e)
            }
        }
        awaitClose { listener.remove() }
    }
}
