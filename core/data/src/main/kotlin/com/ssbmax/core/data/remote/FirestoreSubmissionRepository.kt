package com.ssbmax.core.data.remote

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.SubmissionRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore Submission Repository
 * Manages test submissions (TAT, WAT, SRT, etc.) in Firestore
 */
@Singleton
class FirestoreSubmissionRepository @Inject constructor() : SubmissionRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val submissionsCollection = firestore.collection("submissions")

    companion object {
        private const val TAG = "FirestoreSubmission"
        
        // Field names
        private const val FIELD_ID = "id"
        private const val FIELD_USER_ID = "userId"
        private const val FIELD_TEST_ID = "testId"
        private const val FIELD_TEST_TYPE = "testType"
        private const val FIELD_STATUS = "status"
        private const val FIELD_SUBMITTED_AT = "submittedAt"
        private const val FIELD_GRADED_BY_INSTRUCTOR_ID = "gradedByInstructorId"
        private const val FIELD_GRADING_TIMESTAMP = "gradingTimestamp"
        private const val FIELD_BATCH_ID = "batchId"
        private const val FIELD_DATA = "data" // Contains the actual submission data
    }

    /**
     * Submit TAT test
     */
    override suspend fun submitTAT(submission: TATSubmission, batchId: String?): Result<String> {
        return try {
            val submissionMap = mapOf(
                FIELD_ID to submission.id,
                FIELD_USER_ID to submission.userId,
                FIELD_TEST_ID to submission.testId,
                FIELD_TEST_TYPE to TestType.TAT.name,
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

            Result.success(submission.id)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to submit TAT: ${e.message}", e))
        }
    }

    /**
     * Submit WAT test
     */
    override suspend fun submitWAT(submission: WATSubmission, batchId: String?): Result<String> {
        return try {
            val submissionMap = mapOf(
                FIELD_ID to submission.id,
                FIELD_USER_ID to submission.userId,
                FIELD_TEST_ID to submission.testId,
                FIELD_TEST_TYPE to TestType.WAT.name,
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

            Result.success(submission.id)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to submit WAT: ${e.message}", e))
        }
    }

    /**
     * Submit SRT test
     */
    override suspend fun submitSRT(submission: SRTSubmission, batchId: String?): Result<String> {
        return try {
            val submissionMap = mapOf(
                FIELD_ID to submission.id,
                FIELD_USER_ID to submission.userId,
                FIELD_TEST_ID to submission.testId,
                FIELD_TEST_TYPE to TestType.SRT.name,
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

            Result.success(submission.id)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to submit SRT: ${e.message}", e))
        }
    }

    /**
     * Submit SDT test
     */
    override suspend fun submitSDT(submission: SDTSubmission, batchId: String?): Result<String> {
        return try {
            Log.d(TAG, "☁️ Firestore SDT: Preparing submission for Firestore...")
            Log.d(TAG, "   Document ID: ${submission.id}")
            Log.d(TAG, "   User ID: ${submission.userId}")
            Log.d(TAG, "   Test ID: ${submission.testId}")
            Log.d(TAG, "   Responses: ${submission.responses.size}")
            Log.d(TAG, "   Status: ${submission.status.name}")
            
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
                FIELD_DATA to submission.toMap()
            )

            Log.d(TAG, "☁️ Firestore SDT: Writing to collection 'submissions' at path: submissions/${submission.id}")
            submissionsCollection.document(submission.id)
                .set(submissionMap, SetOptions.merge())
                .await()

            Log.d(TAG, "✅ Firestore SDT: Successfully written to Firestore!")
            Log.d(TAG, "   Collection: submissions")
            Log.d(TAG, "   Document: ${submission.id}")
            Result.success(submission.id)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Firestore SDT: Failed to write to Firestore - ${e.message}", e)
            Result.failure(Exception("Failed to submit SDT: ${e.message}", e))
        }
    }

    /**
     * Submit PPDT test
     */
    override suspend fun submitPPDT(submission: PPDTSubmission, batchId: String?): Result<String> {
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
                FIELD_DATA to submission.toMap()
            )

            submissionsCollection.document(submission.submissionId)
                .set(submissionMap, SetOptions.merge())
                .await()

            Result.success(submission.submissionId)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to submit PPDT: ${e.message}", e))
        }
    }

    /**
     * Submit OIR test
     */
    override suspend fun submitOIR(submission: OIRSubmission, batchId: String?): Result<String> {
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
                FIELD_DATA to submission.toMap()
            )

            submissionsCollection.document(submission.id)
                .set(submissionMap, SetOptions.merge())
                .await()

            Result.success(submission.id)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to submit OIR: ${e.message}", e))
        }
    }

    /**
     * Submit PIQ test
     */
    override suspend fun submitPIQ(submission: PIQSubmission, batchId: String?): Result<String> {
        return try {
            Log.d(TAG, "☁️ Firestore PIQ: Preparing submission for Firestore...")
            Log.d(TAG, "   Document ID: ${submission.id}")
            Log.d(TAG, "   User ID: ${submission.userId}")
            Log.d(TAG, "   Test ID: ${submission.testId}")
            Log.d(TAG, "   Status: ${submission.status.name}")
            
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

            Log.d(TAG, "☁️ Firestore PIQ: Writing to collection 'submissions' at path: submissions/${submission.id}")
            submissionsCollection.document(submission.id)
                .set(submissionMap, SetOptions.merge())
                .await()

            Log.d(TAG, "✅ Firestore PIQ: Successfully written to Firestore!")
            Log.d(TAG, "   Collection: submissions")
            Log.d(TAG, "   Document: ${submission.id}")
            Result.success(submission.id)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Firestore PIQ: Failed to write to Firestore - ${e.message}", e)
            Result.failure(Exception("Failed to submit PIQ: ${e.message}", e))
        }
    }

    /**
     * Get submission by ID
     */
    override suspend fun getSubmission(submissionId: String): Result<Map<String, Any>?> {
        return try {
            val document = submissionsCollection.document(submissionId).get().await()

            if (!document.exists()) {
                return Result.success(null)
            }

            Result.success(document.data)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get submission: ${e.message}", e))
        }
    }

    /**
     * Get user's submissions
     */
    override suspend fun getUserSubmissions(
        userId: String,
        limit: Int
    ): Result<List<Map<String, Any>>> {
        return try {
            val querySnapshot = submissionsCollection
                .whereEqualTo(FIELD_USER_ID, userId)
                .orderBy(FIELD_SUBMITTED_AT, Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val submissions = querySnapshot.documents.mapNotNull { it.data }
            Result.success(submissions)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get user submissions: ${e.message}", e))
        }
    }

    /**
     * Get user's submissions by test type
     */
    override suspend fun getUserSubmissionsByTestType(
        userId: String,
        testType: TestType,
        limit: Int
    ): Result<List<Map<String, Any>>> {
        return try {
            val querySnapshot = submissionsCollection
                .whereEqualTo(FIELD_USER_ID, userId)
                .whereEqualTo(FIELD_TEST_TYPE, testType.name)
                .orderBy(FIELD_SUBMITTED_AT, Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val submissions = querySnapshot.documents.mapNotNull { it.data }
            Result.success(submissions)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get submissions by type: ${e.message}", e))
        }
    }

    /**
     * Observe submission changes in real-time
     */
    override fun observeSubmission(submissionId: String): Flow<Map<String, Any>?> = callbackFlow {
        val registration = submissionsCollection.document(submissionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    trySend(snapshot.data)
                } else {
                    trySend(null)
                }
            }

        awaitClose {
            registration.remove()
        }
    }

    /**
     * Observe user's submissions in real-time
     */
    override fun observeUserSubmissions(userId: String, limit: Int): Flow<List<Map<String, Any>>> = callbackFlow {
        val registration = submissionsCollection
            .whereEqualTo(FIELD_USER_ID, userId)
            .orderBy(FIELD_SUBMITTED_AT, Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (querySnapshot != null) {
                    val submissions = querySnapshot.documents.mapNotNull { it.data }
                    trySend(submissions)
                } else {
                    trySend(emptyList())
                }
            }

        awaitClose {
            registration.remove()
        }
    }

    /**
     * Update submission status
     */
    override suspend fun updateSubmissionStatus(
        submissionId: String,
        status: SubmissionStatus
    ): Result<Unit> {
        return try {
            submissionsCollection.document(submissionId)
                .update(FIELD_STATUS, status.name)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to update status: ${e.message}", e))
        }
    }

    /**
     * Update submission with instructor grading
     */
    suspend fun updateWithInstructorGrading(
        submissionId: String,
        instructorId: String,
        status: SubmissionStatus = SubmissionStatus.GRADED
    ): Result<Unit> {
        return try {
            val updates = mapOf(
                FIELD_STATUS to status.name,
                FIELD_GRADED_BY_INSTRUCTOR_ID to instructorId,
                FIELD_GRADING_TIMESTAMP to System.currentTimeMillis()
            )

            submissionsCollection.document(submissionId)
                .update(updates)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to update grading: ${e.message}", e))
        }
    }

    /**
     * Get pending submissions for instructor
     */
    suspend fun getPendingSubmissionsForInstructor(
        batchId: String? = null,
        limit: Int = 100
    ): Result<List<Map<String, Any>>> {
        return try {
            var query: Query = submissionsCollection
                .whereEqualTo(FIELD_STATUS, SubmissionStatus.SUBMITTED_PENDING_REVIEW.name)
                .orderBy(FIELD_SUBMITTED_AT, Query.Direction.ASCENDING)

            if (batchId != null) {
                query = query.whereEqualTo(FIELD_BATCH_ID, batchId)
            }

            val querySnapshot = query.limit(limit.toLong()).get().await()
            val submissions = querySnapshot.documents.mapNotNull { it.data }
            Result.success(submissions)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get pending submissions: ${e.message}", e))
        }
    }

    /**
     * Delete submission
     */
    suspend fun deleteSubmission(submissionId: String): Result<Unit> {
        return try {
            submissionsCollection.document(submissionId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to delete submission: ${e.message}", e))
        }
    }

    /**
     * Get latest PIQ submission for user
     * TODO: Full implementation in Phase 3
     */
    override suspend fun getLatestPIQSubmission(userId: String): Result<PIQSubmission?> {
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
                // TODO: Implement proper PIQSubmission deserialization in Phase 3
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting latest PIQ submission", e)
            Result.failure(e)
        }
    }

    /**
     * Get latest OIR submission for user
     * TODO: Full implementation in Phase 3
     */
    override suspend fun getLatestOIRSubmission(userId: String): Result<OIRSubmission?> {
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
                // TODO: Implement proper OIRSubmission deserialization in Phase 3
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting latest OIR submission", e)
            Result.failure(e)
        }
    }

    /**
     * Get latest PPDT submission for user
     * TODO: Full implementation in Phase 3
     */
    override suspend fun getLatestPPDTSubmission(userId: String): Result<PPDTSubmission?> {
        return try {
            val query = submissionsCollection
                .whereEqualTo(FIELD_USER_ID, userId)
                .whereEqualTo(FIELD_TEST_TYPE, "PPDT")
                .orderBy(FIELD_SUBMITTED_AT, Query.Direction.DESCENDING)
                .limit(1)

            val snapshot = query.get().await()

            if (snapshot.isEmpty) {
                Result.success(null)
            } else {
                // TODO: Implement proper PPDTSubmission deserialization in Phase 3
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting latest PPDT submission", e)
            Result.failure(e)
        }
    }
}

/**
 * Extension functions to convert domain models to maps for Firestore
 */
private fun OIRSubmission.toMap(): Map<String, Any?> {
    return mapOf(
        "id" to id,
        "userId" to userId,
        "testId" to testId,
        "testResult" to mapOf(
            "testId" to testResult.testId,
            "sessionId" to testResult.sessionId,
            "userId" to testResult.userId,
            "totalQuestions" to testResult.totalQuestions,
            "correctAnswers" to testResult.correctAnswers,
            "incorrectAnswers" to testResult.incorrectAnswers,
            "skippedQuestions" to testResult.skippedQuestions,
            "totalTimeSeconds" to testResult.totalTimeSeconds,
            "timeTakenSeconds" to testResult.timeTakenSeconds,
            "rawScore" to testResult.rawScore,
            "percentageScore" to testResult.percentageScore,
            "categoryScores" to testResult.categoryScores.entries.associate { (category, score) ->
                category.name to mapOf(
                    "category" to score.category.name,
                    "totalQuestions" to score.totalQuestions,
                    "correctAnswers" to score.correctAnswers,
                    "percentage" to score.percentage,
                    "averageTimeSeconds" to score.averageTimeSeconds
                )
            },
            "difficultyBreakdown" to testResult.difficultyBreakdown.entries.associate { (difficulty, score) ->
                difficulty.name to mapOf(
                    "difficulty" to score.difficulty.name,
                    "totalQuestions" to score.totalQuestions,
                    "correctAnswers" to score.correctAnswers,
                    "percentage" to score.percentage
                )
            },
            "answeredQuestions" to testResult.answeredQuestions.map { aq ->
                mapOf(
                    "questionId" to aq.question.id,
                    "questionNumber" to aq.question.questionNumber,
                    "questionType" to aq.question.type.name,
                    "questionText" to aq.question.questionText,
                    "difficulty" to aq.question.difficulty.name,
                    "correctAnswerId" to aq.question.correctAnswerId,
                    "selectedOptionId" to aq.userAnswer.selectedOptionId,
                    "isCorrect" to aq.isCorrect,
                    "timeTakenSeconds" to aq.userAnswer.timeTakenSeconds,
                    "skipped" to aq.userAnswer.skipped
                )
            },
            "completedAt" to testResult.completedAt,
            "passed" to testResult.passed,
            "grade" to testResult.grade.name
        ),
        "submittedAt" to submittedAt,
        "status" to status.name,
        "gradedByInstructorId" to gradedByInstructorId,
        "gradingTimestamp" to gradingTimestamp
    )
}

private fun PPDTSubmission.toMap(): Map<String, Any?> {
    return mapOf(
        "submissionId" to submissionId,
        "questionId" to questionId,
        "userId" to userId,
        "userName" to userName,
        "userEmail" to userEmail,
        "batchId" to batchId,
        "story" to story,
        "charactersCount" to charactersCount,
        "viewingTimeTakenSeconds" to viewingTimeTakenSeconds,
        "writingTimeTakenMinutes" to writingTimeTakenMinutes,
        "submittedAt" to submittedAt,
        "status" to status.name,
        "aiPreliminaryScore" to aiPreliminaryScore?.let {
            mapOf(
                "perceptionScore" to it.perceptionScore,
                "imaginationScore" to it.imaginationScore,
                "narrationScore" to it.narrationScore,
                "characterDepictionScore" to it.characterDepictionScore,
                "positivityScore" to it.positivityScore,
                "overallScore" to it.overallScore,
                "feedback" to it.feedback,
                "strengths" to it.strengths,
                "areasForImprovement" to it.areasForImprovement
            )
        },
        "instructorReview" to instructorReview?.let {
            mapOf(
                "reviewId" to it.reviewId,
                "instructorId" to it.instructorId,
                "instructorName" to it.instructorName,
                "finalScore" to it.finalScore,
                "feedback" to it.feedback,
                "detailedScores" to mapOf(
                    "perception" to it.detailedScores.perception,
                    "imagination" to it.detailedScores.imagination,
                    "narration" to it.detailedScores.narration,
                    "characterDepiction" to it.detailedScores.characterDepiction,
                    "positivity" to it.detailedScores.positivity
                ),
                "agreedWithAI" to it.agreedWithAI,
                "reviewedAt" to it.reviewedAt,
                "timeSpentMinutes" to it.timeSpentMinutes
            )
        }
    )
}

private fun TATSubmission.toMap(): Map<String, Any?> {
    return mapOf(
        "id" to id,
        "userId" to userId,
        "testId" to testId,
        "stories" to stories.map { it.toMap() },
        "totalTimeTakenMinutes" to totalTimeTakenMinutes,
        "submittedAt" to submittedAt,
        "status" to status.name,
        "aiPreliminaryScore" to aiPreliminaryScore?.toMap(),
        "instructorScore" to instructorScore?.toMap(),
        "gradedByInstructorId" to gradedByInstructorId,
        "gradingTimestamp" to gradingTimestamp
    )
}

private fun TATStoryResponse.toMap(): Map<String, Any?> {
    return mapOf(
        "questionId" to questionId,
        "story" to story,
        "charactersCount" to charactersCount,
        "viewingTimeTakenSeconds" to viewingTimeTakenSeconds,
        "writingTimeTakenSeconds" to writingTimeTakenSeconds,
        "submittedAt" to submittedAt
    )
}

private fun TATAIScore.toMap(): Map<String, Any?> {
    return mapOf(
        "overallScore" to overallScore,
        "thematicPerceptionScore" to thematicPerceptionScore,
        "imaginationScore" to imaginationScore,
        "characterDepictionScore" to characterDepictionScore,
        "emotionalToneScore" to emotionalToneScore,
        "narrativeStructureScore" to narrativeStructureScore,
        "feedback" to feedback,
        "strengths" to strengths,
        "areasForImprovement" to areasForImprovement
    )
}

private fun TATInstructorScore.toMap(): Map<String, Any?> {
    return mapOf(
        "overallScore" to overallScore,
        "thematicPerceptionScore" to thematicPerceptionScore,
        "imaginationScore" to imaginationScore,
        "characterDepictionScore" to characterDepictionScore,
        "emotionalToneScore" to emotionalToneScore,
        "narrativeStructureScore" to narrativeStructureScore,
        "feedback" to feedback,
        "storyWiseComments" to storyWiseComments,
        "gradedByInstructorId" to gradedByInstructorId,
        "gradedByInstructorName" to gradedByInstructorName,
        "gradedAt" to gradedAt,
        "agreedWithAI" to agreedWithAI
    )
}

private fun WATSubmission.toMap(): Map<String, Any?> {
    return mapOf(
        "id" to id,
        "userId" to userId,
        "testId" to testId,
        "responses" to responses.map { it.toMap() },
        "totalTimeTakenMinutes" to totalTimeTakenMinutes,
        "submittedAt" to submittedAt,
        "status" to status.name,
        "aiPreliminaryScore" to aiPreliminaryScore?.toMap(),
        "instructorScore" to instructorScore?.toMap(),
        "gradedByInstructorId" to gradedByInstructorId,
        "gradingTimestamp" to gradingTimestamp
    )
}

private fun WATWordResponse.toMap(): Map<String, Any?> {
    return mapOf(
        "wordId" to wordId,
        "word" to word,
        "response" to response,
        "timeTakenSeconds" to timeTakenSeconds,
        "submittedAt" to submittedAt,
        "isSkipped" to isSkipped
    )
}

private fun WATAIScore.toMap(): Map<String, Any?> {
    return mapOf(
        "overallScore" to overallScore,
        "positivityScore" to positivityScore,
        "creativityScore" to creativityScore,
        "speedScore" to speedScore,
        "relevanceScore" to relevanceScore,
        "emotionalMaturityScore" to emotionalMaturityScore,
        "feedback" to feedback,
        "positiveWords" to positiveWords,
        "negativeWords" to negativeWords,
        "neutralWords" to neutralWords,
        "uniqueResponsesCount" to uniqueResponsesCount,
        "repeatedPatterns" to repeatedPatterns,
        "strengths" to strengths,
        "areasForImprovement" to areasForImprovement
    )
}

private fun WATInstructorScore.toMap(): Map<String, Any?> {
    return mapOf(
        "overallScore" to overallScore,
        "positivityScore" to positivityScore,
        "creativityScore" to creativityScore,
        "speedScore" to speedScore,
        "relevanceScore" to relevanceScore,
        "emotionalMaturityScore" to emotionalMaturityScore,
        "feedback" to feedback,
        "gradedByInstructorId" to gradedByInstructorId,
        "gradedByInstructorName" to gradedByInstructorName,
        "gradedAt" to gradedAt
    )
}

private fun SRTSubmission.toMap(): Map<String, Any?> {
    return mapOf(
        "id" to id,
        "userId" to userId,
        "testId" to testId,
        "responses" to responses.map { it.toMap() },
        "totalTimeTakenMinutes" to totalTimeTakenMinutes,
        "submittedAt" to submittedAt,
        "status" to status.name,
        "aiPreliminaryScore" to aiPreliminaryScore?.toMap(),
        "instructorScore" to instructorScore?.toMap(),
        "gradedByInstructorId" to gradedByInstructorId,
        "gradingTimestamp" to gradingTimestamp
    )
}

private fun SRTSituationResponse.toMap(): Map<String, Any?> {
    return mapOf(
        "situationId" to situationId,
        "situation" to situation,
        "response" to response,
        "charactersCount" to charactersCount,
        "timeTakenSeconds" to timeTakenSeconds,
        "submittedAt" to submittedAt,
        "isSkipped" to isSkipped
    )
}

private fun SRTAIScore.toMap(): Map<String, Any?> {
    return mapOf(
        "overallScore" to overallScore,
        "leadershipScore" to leadershipScore,
        "decisionMakingScore" to decisionMakingScore,
        "practicalityScore" to practicalityScore,
        "initiativeScore" to initiativeScore,
        "socialResponsibilityScore" to socialResponsibilityScore,
        "feedback" to feedback,
        "categoryWiseScores" to categoryWiseScores.mapKeys { it.key.name },
        "positiveTraits" to positiveTraits,
        "concerningPatterns" to concerningPatterns,
        "responseQuality" to responseQuality.name,
        "strengths" to strengths,
        "areasForImprovement" to areasForImprovement
    )
}

private fun SRTInstructorScore.toMap(): Map<String, Any?> {
    return mapOf(
        "overallScore" to overallScore,
        "leadershipScore" to leadershipScore,
        "decisionMakingScore" to decisionMakingScore,
        "practicalityScore" to practicalityScore,
        "initiativeScore" to initiativeScore,
        "socialResponsibilityScore" to socialResponsibilityScore,
        "feedback" to feedback,
        "categoryWiseComments" to categoryWiseComments.mapKeys { it.key.name },
        "flaggedResponses" to flaggedResponses,
        "exemplaryResponses" to exemplaryResponses,
        "gradedByInstructorId" to gradedByInstructorId,
        "gradedByInstructorName" to gradedByInstructorName,
        "gradedAt" to gradedAt,
        "agreedWithAI" to agreedWithAI
    )
}

// ==================== SDT Mapping Functions ====================

private fun SDTSubmission.toMap(): Map<String, Any?> {
    return mapOf(
        "id" to id,
        "userId" to userId,
        "testId" to testId,
        "responses" to responses.map { it.toMap() },
        "totalTimeTakenMinutes" to totalTimeTakenMinutes,
        "submittedAt" to submittedAt,
        "status" to status.name,
        "aiPreliminaryScore" to aiPreliminaryScore?.toMap(),
        "instructorScore" to instructorScore?.toMap(),
        "gradedByInstructorId" to gradedByInstructorId,
        "gradingTimestamp" to gradingTimestamp
    )
}

private fun SDTQuestionResponse.toMap(): Map<String, Any?> {
    return mapOf(
        "questionId" to questionId,
        "question" to question,
        "answer" to answer,
        "wordCount" to wordCount,
        "timeTakenSeconds" to timeTakenSeconds,
        "submittedAt" to submittedAt,
        "isSkipped" to isSkipped
    )
}

private fun SDTAIScore.toMap(): Map<String, Any?> {
    return mapOf(
        "overallScore" to overallScore,
        "selfAwarenessScore" to selfAwarenessScore,
        "emotionalMaturityScore" to emotionalMaturityScore,
        "socialPerceptionScore" to socialPerceptionScore,
        "introspectionScore" to introspectionScore,
        "feedback" to feedback,
        "positiveTraits" to positiveTraits,
        "concerningPatterns" to concerningPatterns,
        "responseQuality" to responseQuality.name,
        "strengths" to strengths,
        "areasForImprovement" to areasForImprovement,
        "questionWiseAnalysis" to questionWiseAnalysis.map { it.toMap() }
    )
}

private fun QuestionAnalysis.toMap(): Map<String, Any?> {
    return mapOf(
        "questionId" to questionId,
        "sequenceNumber" to sequenceNumber,
        "score" to score,
        "themes" to themes,
        "sentimentScore" to sentimentScore,
        "keyInsights" to keyInsights
    )
}

private fun SDTInstructorScore.toMap(): Map<String, Any?> {
    return mapOf(
        "overallScore" to overallScore,
        "selfAwarenessScore" to selfAwarenessScore,
        "emotionalMaturityScore" to emotionalMaturityScore,
        "socialPerceptionScore" to socialPerceptionScore,
        "introspectionScore" to introspectionScore,
        "feedback" to feedback,
        "flaggedResponses" to flaggedResponses,
        "exemplaryResponses" to exemplaryResponses,
        "gradedByInstructorId" to gradedByInstructorId,
        "gradedByInstructorName" to gradedByInstructorName,
        "gradedAt" to gradedAt,
        "agreedWithAI" to agreedWithAI
    )
}

