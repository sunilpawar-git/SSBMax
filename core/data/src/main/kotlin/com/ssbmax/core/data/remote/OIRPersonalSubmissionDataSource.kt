package com.ssbmax.core.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.ssbmax.shared.domain.model.CategoryScore
import com.ssbmax.shared.domain.model.DifficultyScore
import com.ssbmax.shared.domain.model.OIRQuestionType
import com.ssbmax.shared.domain.model.OIRSubmission
import com.ssbmax.shared.domain.model.OIRTestResult
import com.ssbmax.shared.domain.model.QuestionDifficulty
import com.ssbmax.shared.domain.model.SubmissionStatus
import com.ssbmax.shared.domain.model.TestType
import kotlinx.coroutines.tasks.await

/**
 * Data source for OIR personal test submissions.
 * Handles Firestore CRUD for OIRSubmission documents.
 */
class OIRPersonalSubmissionDataSource(
    private val firestore: FirebaseFirestore
) {

    private val submissionsCollection = firestore.collection("submissions")

    companion object {
        private const val TAG = "OIRSubmissionDS"
        private const val FIELD_ID = "id"
        private const val FIELD_USER_ID = "userId"
        private const val FIELD_TEST_TYPE = "testType"
        private const val FIELD_STATUS = "status"
        private const val FIELD_SUBMITTED_AT = "submittedAt"
    }

    suspend fun submitOIR(submission: OIRSubmission, batchId: String?): Result<String> {
        return try {
            val submissionMap = mapOf(
                FIELD_ID to submission.id,
                FIELD_USER_ID to submission.userId,
                "testId" to submission.testId,
                FIELD_TEST_TYPE to TestType.OIR.name,
                FIELD_STATUS to submission.status.name,
                FIELD_SUBMITTED_AT to submission.submittedAt,
                "gradedByInstructorId" to submission.gradedByInstructorId,
                "gradingTimestamp" to submission.gradingTimestamp,
                "batchId" to batchId,
                "data" to submission.toFirestoreMap()
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

    @Suppress("UNCHECKED_CAST")
    internal fun parseOIRSubmission(data: Map<*, *>): OIRSubmission {
        val testResultMap = data["testResult"] as? Map<*, *> ?: emptyMap<String, Any>()
        val testResult = parseOIRTestResult(testResultMap)

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

    private data class OIRQuestionCounts(
        val totalQuestions: Int,
        val correctAnswers: Int,
        val incorrectAnswers: Int,
        val skippedQuestions: Int
    )

    private fun parseOIRQuestionCounts(testResultMap: Map<*, *>) = OIRQuestionCounts(
        totalQuestions = (testResultMap["totalQuestions"] as? Number)?.toInt() ?: 0,
        correctAnswers = (testResultMap["correctAnswers"] as? Number)?.toInt() ?: 0,
        incorrectAnswers = (testResultMap["incorrectAnswers"] as? Number)?.toInt() ?: 0,
        skippedQuestions = (testResultMap["skippedQuestions"] as? Number)?.toInt() ?: 0
    )

    private fun parseOIRTestResult(testResultMap: Map<*, *>): OIRTestResult {
        val categoryScoresMap = testResultMap["categoryScores"] as? Map<*, *> ?: emptyMap<String, Any>()
        val difficultyBreakdownMap = testResultMap["difficultyBreakdown"] as? Map<*, *> ?: emptyMap<String, Any>()
        val counts = parseOIRQuestionCounts(testResultMap)

        return OIRTestResult(
            testId = testResultMap["testId"] as? String ?: "",
            sessionId = testResultMap["sessionId"] as? String ?: "",
            userId = testResultMap["userId"] as? String ?: "",
            totalQuestions = counts.totalQuestions,
            correctAnswers = counts.correctAnswers,
            incorrectAnswers = counts.incorrectAnswers,
            skippedQuestions = counts.skippedQuestions,
            totalTimeSeconds = (testResultMap["totalTimeSeconds"] as? Number)?.toInt() ?: 0,
            timeTakenSeconds = (testResultMap["timeTakenSeconds"] as? Number)?.toInt() ?: 0,
            rawScore = (testResultMap["rawScore"] as? Number)?.toInt() ?: 0,
            percentageScore = (testResultMap["percentageScore"] as? Number)?.toFloat() ?: 0f,
            categoryScores = parseCategoryScores(categoryScoresMap),
            difficultyBreakdown = parseDifficultyBreakdown(difficultyBreakdownMap),
            answeredQuestions = emptyList(),
            completedAt = (testResultMap["completedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }

    private fun parseCategoryScores(categoryScoresMap: Map<*, *>): Map<OIRQuestionType, CategoryScore> =
        categoryScoresMap.mapNotNull { (key, value) ->
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

    private fun parseDifficultyBreakdown(difficultyBreakdownMap: Map<*, *>): Map<QuestionDifficulty, DifficultyScore> =
        difficultyBreakdownMap.mapNotNull { (key, value) ->
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
}
