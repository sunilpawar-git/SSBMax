package com.ssbmax.core.data.remote

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
}

/**
 * Extension functions to convert domain models to maps for Firestore
 */
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

