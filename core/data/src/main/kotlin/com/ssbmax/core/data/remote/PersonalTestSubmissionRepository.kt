package com.ssbmax.core.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
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
 * Repository for personal test submissions (PIQ, OIR, PPDT).
 * Handles submission, retrieval, and observation of personal interview tests.
 * 
 * Extracted from FirestoreSubmissionRepository during Phase 8 refactoring.
 */
@Singleton
class PersonalTestSubmissionRepository @Inject constructor() {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val submissionsCollection = firestore.collection("submissions")
    
    companion object {
        private const val TAG = "PersonalTestRepo"
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
    
    // ===========================
    // PPDT Operations
    // ===========================
    
    /**
     * Submit PPDT test.
     * IMPORTANT: toFirestoreMap() does NOT include analysisStatus or olqResult.
     * These fields are written ONLY by PPDTAnalysisWorker via updatePPDTOLQResult().
     */
    suspend fun submitPPDT(submission: PPDTSubmission, batchId: String?): Result<String> {
        return try {
            val submissionMap = mapOf(
                FIELD_ID to submission.submissionId,
                FIELD_USER_ID to submission.userId,
                FIELD_TEST_ID to submission.questionId,
                FIELD_TEST_TYPE to TestType.PPDT.name,
                FIELD_STATUS to submission.status.name,
                FIELD_SUBMITTED_AT to submission.submittedAt,
                FIELD_GRADED_BY_INSTRUCTOR_ID to null,
                FIELD_GRADING_TIMESTAMP to null,
                FIELD_BATCH_ID to batchId,
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
            // Force server fetch to ensure we have latest OLQ results
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

            // Force server fetch to avoid stale cache data (OLQ results may be updated by worker)
            // Fall back to cache if offline
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
            Log.d(TAG, "📝 Writing PPDT OLQ result to ppdt_results collection: $submissionId")
            
            // Fetch userId from submission (OLQAnalysisResult doesn't have userId)
            val submissionDoc = submissionsCollection.document(submissionId).get().await()
            val userId = submissionDoc.getString(FIELD_USER_ID) 
                ?: throw Exception("userId not found in submission")
            
            // Build the result document for ppdt_results collection
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
            
            // Batch write: ppdt_results + submission status update
            val ppdtResultsCollection = firestore.collection("ppdt_results")
            
            firestore.runBatch { batch ->
                // 1. Write to ppdt_results collection (primary storage)
                batch.set(ppdtResultsCollection.document(submissionId), resultData)
                
                // 2. Update submission status to COMPLETED (for observers to detect)
                batch.update(
                    submissionsCollection.document(submissionId),
                    mapOf(
                        "$FIELD_DATA.analysisStatus" to SubmissionConstants.ANALYSIS_STATUS_COMPLETED,
                        "status" to "COMPLETED"
                    )
                )
            }.await()
            
            Log.d(TAG, "✅ PPDT result written to ppdt_results and status updated")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update PPDT OLQ result", e)
            Result.failure(Exception("Failed to update PPDT OLQ result: ${e.message}", e))
        }
    }
    
    /**
     * Fetch PPDT OLQ result from ppdt_results collection.
     * Uses Source.SERVER to avoid stale cache issues.
     */
    suspend fun getPPDTResult(submissionId: String): Result<OLQAnalysisResult?> {
        return try {
            Log.d(TAG, "📥 Fetching PPDT result from ppdt_results: $submissionId")
            
            val ppdtResultsCollection = firestore.collection("ppdt_results")
            
            // Try server first, fallback to cache
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
            val parsed = parseOLQAnalysisResult(data, submissionId)
            Log.d(TAG, "✅ PPDT result loaded: ${parsed?.olqScores?.size} OLQ scores")
            Result.success(parsed)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching PPDT result", e)
            Result.failure(e)
        }
    }
    
    /**
     * Parse OLQAnalysisResult from Firestore data (ppdt_results format)
     */
    private fun parseOLQAnalysisResult(data: Map<String, Any?>, submissionId: String): OLQAnalysisResult? {
        return try {
            @Suppress("UNCHECKED_CAST")
            val scoresMap = data["olqScores"] as? Map<String, Map<String, Any>> ?: return null
            
            val olqScores = scoresMap.mapNotNull { (key, value) ->
                try {
                    val olq = com.ssbmax.core.domain.model.interview.OLQ.valueOf(key)
                    val score = (value["score"] as? Number)?.toInt() ?: 0
                    val confidence = (value["confidence"] as? Number)?.toInt() ?: 0
                    val reasoning = value["reasoning"] as? String ?: ""
                    olq to com.ssbmax.core.domain.model.interview.OLQScore(score, confidence, reasoning)
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing OLQ score: $key", e)
                    null
                }
            }.toMap()
            
            @Suppress("UNCHECKED_CAST")
            OLQAnalysisResult(
                submissionId = submissionId,
                testType = TestType.PPDT,
                olqScores = olqScores,
                overallScore = (data["overallScore"] as? Number)?.toFloat() ?: 0f,
                overallRating = data["overallRating"] as? String ?: "",
                strengths = (data["strengths"] as? List<String>) ?: emptyList(),
                weaknesses = (data["weaknesses"] as? List<String>) ?: emptyList(),
                recommendations = (data["recommendations"] as? List<String>) ?: emptyList(),
                aiConfidence = (data["aiConfidence"] as? Number)?.toInt() ?: 0,
                analyzedAt = (data["analyzedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing OLQAnalysisResult", e)
            null
        }
    }
    
    /**
     * Observe PPDT submission in real-time with stale cache protection.
     */
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
    
    // ===========================
    // OIR Operations
    // ===========================
    
    suspend fun submitOIR(submission: OIRSubmission, batchId: String?): Result<String> {
        return try {
            val submissionMap = mapOf(
                FIELD_ID to submission.id,
                FIELD_USER_ID to submission.userId,
                FIELD_TEST_ID to submission.testId,
                FIELD_TEST_TYPE to TestType.OIR.name,
                FIELD_STATUS to submission.status.name,
                FIELD_SUBMITTED_AT to submission.submittedAt,
                FIELD_GRADED_BY_INSTRUCTOR_ID to submission.gradedByInstructorId,
                FIELD_GRADING_TIMESTAMP to submission.gradingTimestamp,
                FIELD_BATCH_ID to batchId,
                FIELD_DATA to submission.toFirestoreMap()
            )

            submissionsCollection.document(submission.id)
                .set(submissionMap, SetOptions.merge())
                .await()

            Result.success(submission.id)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to submit OIR: ${e.message}", e))
        }
    }
    
    suspend fun getLatestOIRSubmission(userId: String): Result<OIRSubmission?> {
        return try {
            val query = submissionsCollection
                .whereEqualTo(FIELD_USER_ID, userId)
                .whereEqualTo(FIELD_TEST_TYPE, "OIR")
                .orderBy(FIELD_SUBMITTED_AT, Query.Direction.DESCENDING)
                .limit(1)

            val snapshot = query.get().await()

            if (snapshot.isEmpty) {
                Result.success(null)
            } else {
                val data = snapshot.documents[0].get("data") as? Map<*, *>
                if (data == null) {
                    Log.w(TAG, "OIR submission found but data field is null")
                    Result.success(null)
                } else {
                    Result.success(parseOIRSubmission(data))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting latest OIR submission", e)
            Result.failure(e)
        }
    }
    
    // ===========================
    // PIQ Operations
    // ===========================
    
    suspend fun submitPIQ(submission: PIQSubmission, batchId: String?): Result<String> {
        return try {
            Log.d(TAG, "☁️ Firestore PIQ: Preparing submission...")
            
            val submissionMap = mapOf(
                FIELD_ID to submission.id,
                FIELD_USER_ID to submission.userId,
                FIELD_TEST_ID to submission.testId,
                FIELD_TEST_TYPE to TestType.PIQ.name,
                FIELD_STATUS to submission.status.name,
                FIELD_SUBMITTED_AT to submission.submittedAt,
                FIELD_GRADED_BY_INSTRUCTOR_ID to submission.gradedByInstructorId,
                FIELD_GRADING_TIMESTAMP to submission.gradingTimestamp,
                FIELD_BATCH_ID to batchId,
                FIELD_DATA to submission.toMap()
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
    
    // ===========================
    // Parsing Methods
    // ===========================
    
    @Suppress("UNCHECKED_CAST")
    private fun parseOIRSubmission(data: Map<*, *>): OIRSubmission {
        val testResultMap = data["testResult"] as? Map<*, *> ?: emptyMap<String, Any>()

        val categoryScoresMap = testResultMap["categoryScores"] as? Map<*, *> ?: emptyMap<String, Any>()
        val categoryScores = categoryScoresMap.mapNotNull { (key, value) ->
            val categoryName = key as? String ?: return@mapNotNull null
            val scoreMap = value as? Map<*, *> ?: return@mapNotNull null

            val category = try { OIRQuestionType.valueOf(categoryName) } catch (e: Exception) { return@mapNotNull null }

            category to CategoryScore(
                category = category,
                totalQuestions = (scoreMap["totalQuestions"] as? Number)?.toInt() ?: 0,
                correctAnswers = (scoreMap["correctAnswers"] as? Number)?.toInt() ?: 0,
                percentage = (scoreMap["percentage"] as? Number)?.toFloat() ?: 0f,
                averageTimeSeconds = (scoreMap["averageTimeSeconds"] as? Number)?.toInt() ?: 0
            )
        }.toMap()

        val difficultyBreakdownMap = testResultMap["difficultyBreakdown"] as? Map<*, *> ?: emptyMap<String, Any>()
        val difficultyBreakdown = difficultyBreakdownMap.mapNotNull { (key, value) ->
            val difficultyName = key as? String ?: return@mapNotNull null
            val scoreMap = value as? Map<*, *> ?: return@mapNotNull null

            val difficulty = try { QuestionDifficulty.valueOf(difficultyName) } catch (e: Exception) { return@mapNotNull null }

            difficulty to DifficultyScore(
                difficulty = difficulty,
                totalQuestions = (scoreMap["totalQuestions"] as? Number)?.toInt() ?: 0,
                correctAnswers = (scoreMap["correctAnswers"] as? Number)?.toInt() ?: 0,
                percentage = (scoreMap["percentage"] as? Number)?.toFloat() ?: 0f
            )
        }.toMap()

        val testResult = OIRTestResult(
            testId = testResultMap["testId"] as? String ?: "",
            sessionId = testResultMap["sessionId"] as? String ?: "",
            userId = testResultMap["userId"] as? String ?: "",
            totalQuestions = (testResultMap["totalQuestions"] as? Number)?.toInt() ?: 0,
            correctAnswers = (testResultMap["correctAnswers"] as? Number)?.toInt() ?: 0,
            incorrectAnswers = (testResultMap["incorrectAnswers"] as? Number)?.toInt() ?: 0,
            skippedQuestions = (testResultMap["skippedQuestions"] as? Number)?.toInt() ?: 0,
            totalTimeSeconds = (testResultMap["totalTimeSeconds"] as? Number)?.toInt() ?: 0,
            timeTakenSeconds = (testResultMap["timeTakenSeconds"] as? Number)?.toInt() ?: 0,
            rawScore = (testResultMap["rawScore"] as? Number)?.toInt() ?: 0,
            percentageScore = (testResultMap["percentageScore"] as? Number)?.toFloat() ?: 0f,
            categoryScores = categoryScores,
            difficultyBreakdown = difficultyBreakdown,
            answeredQuestions = emptyList(),
            completedAt = (testResultMap["completedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )

        return OIRSubmission(
            id = data["id"] as? String ?: "",
            userId = data["userId"] as? String ?: "",
            testId = data["testId"] as? String ?: "",
            testResult = testResult,
            submittedAt = (data["submittedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            status = SubmissionStatus.valueOf(data["status"] as? String ?: "SUBMITTED"),
            gradedByInstructorId = data["gradedByInstructorId"] as? String,
            gradingTimestamp = (data["gradingTimestamp"] as? Number)?.toLong()
        )
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun parsePPDTSubmission(data: Map<*, *>): PPDTSubmission {
        val instructorReviewMap = data["instructorReview"] as? Map<*, *>
        val instructorReview = instructorReviewMap?.let {
            val detailedScoresMap = it["detailedScores"] as? Map<*, *> ?: emptyMap<String, Any>()
            val detailedScores = PPDTDetailedScores(
                perception = (detailedScoresMap["perception"] as? Number)?.toFloat() ?: 0f,
                imagination = (detailedScoresMap["imagination"] as? Number)?.toFloat() ?: 0f,
                narration = (detailedScoresMap["narration"] as? Number)?.toFloat() ?: 0f,
                characterDepiction = (detailedScoresMap["characterDepiction"] as? Number)?.toFloat() ?: 0f,
                positivity = (detailedScoresMap["positivity"] as? Number)?.toFloat() ?: 0f
            )

            PPDTInstructorReview(
                reviewId = it["reviewId"] as? String ?: "",
                instructorId = it["instructorId"] as? String ?: "",
                instructorName = it["instructorName"] as? String ?: "",
                finalScore = (it["finalScore"] as? Number)?.toFloat() ?: 0f,
                feedback = it["feedback"] as? String ?: "",
                detailedScores = detailedScores,
                agreedWithAI = it["agreedWithAI"] as? Boolean ?: false,
                reviewedAt = (it["reviewedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                timeSpentMinutes = (it["timeSpentMinutes"] as? Number)?.toInt() ?: 0
            )
        }

        val olqResultMap = data["olqResult"] as? Map<*, *>
        val olqResult = parseOLQResult(olqResultMap)

        val analysisStatusStr = data["analysisStatus"] as? String
        val analysisStatus = try {
            if (analysisStatusStr != null) AnalysisStatus.valueOf(analysisStatusStr)
            else AnalysisStatus.PENDING_ANALYSIS
        } catch (e: Exception) { AnalysisStatus.PENDING_ANALYSIS }

        return PPDTSubmission(
            submissionId = data["submissionId"] as? String ?: "",
            questionId = data["questionId"] as? String ?: "",
            userId = data["userId"] as? String ?: "",
            userName = data["userName"] as? String ?: "",
            userEmail = data["userEmail"] as? String ?: "",
            batchId = data["batchId"] as? String,
            story = data["story"] as? String ?: "",
            charactersCount = (data["charactersCount"] as? Number)?.toInt() ?: 0,
            viewingTimeTakenSeconds = (data["viewingTimeTakenSeconds"] as? Number)?.toInt() ?: 0,
            writingTimeTakenMinutes = (data["writingTimeTakenMinutes"] as? Number)?.toInt() ?: 0,
            submittedAt = (data["submittedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            status = SubmissionStatus.valueOf(data["status"] as? String ?: "SUBMITTED"),
            instructorReview = instructorReview,
            analysisStatus = analysisStatus,
            olqResult = olqResult
        )
    }
}
