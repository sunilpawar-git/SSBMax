package com.ssbmax.core.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.model.gto.GTOSubmission
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for GTO test submissions.
 * Handles GD, GPE, and Lecturette submission operations.
 * 
 * Extracted from FirestoreSubmissionRepository during Phase 7 refactoring.
 */
@Singleton
class GTOSubmissionRepository @Inject constructor() {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val submissionsCollection = firestore.collection("submissions")
    
    companion object {
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
    
    /**
     * Submit GPE test
     */
    suspend fun submitGPE(
        submission: GTOSubmission.GPESubmission,
        batchId: String?
    ): Result<String> {
        return try {
            val submissionMap = mapOf(
                FIELD_ID to submission.id,
                FIELD_USER_ID to submission.userId,
                FIELD_TEST_ID to submission.testId,
                FIELD_TEST_TYPE to TestType.GTO_GPE.name,
                FIELD_STATUS to submission.status.name,
                FIELD_SUBMITTED_AT to submission.submittedAt,
                FIELD_GRADED_BY_INSTRUCTOR_ID to null,
                FIELD_GRADING_TIMESTAMP to null,
                FIELD_BATCH_ID to batchId,
                FIELD_DATA to mapOf(
                    "imageUrl" to submission.imageUrl,
                    "scenario" to submission.scenario,
                    "solution" to submission.solution,
                    "plan" to submission.plan,
                    "characterCount" to submission.characterCount,
                    "timeSpent" to submission.timeSpent,
                    "olqScores" to submission.olqScores.mapKeys { it.key.name }.mapValues { entry ->
                        mapOf(
                            "score" to entry.value.score,
                            "confidence" to entry.value.confidence,
                            "reasoning" to entry.value.reasoning
                        )
                    }
                )
            )

            submissionsCollection.document(submission.id)
                .set(submissionMap, SetOptions.merge())
                .await()

            Result.success(submission.id)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to submit GPE: ${e.message}", e))
        }
    }
    
    /**
     * Submit GD test
     */
    suspend fun submitGD(
        submission: GTOSubmission.GDSubmission,
        batchId: String?
    ): Result<String> {
        return try {
            val submissionMap = mapOf(
                FIELD_ID to submission.id,
                FIELD_USER_ID to submission.userId,
                FIELD_TEST_ID to submission.testId,
                FIELD_TEST_TYPE to TestType.GTO_GD.name,
                FIELD_STATUS to submission.status.name,
                FIELD_SUBMITTED_AT to submission.submittedAt,
                FIELD_GRADED_BY_INSTRUCTOR_ID to null,
                FIELD_GRADING_TIMESTAMP to null,
                FIELD_BATCH_ID to batchId,
                FIELD_DATA to mapOf(
                    "topic" to submission.topic,
                    "response" to submission.response,
                    "wordCount" to submission.wordCount,
                    "timeSpent" to submission.timeSpent,
                    "olqScores" to submission.olqScores.mapKeys { it.key.name }.mapValues { entry ->
                        mapOf(
                            "score" to entry.value.score,
                            "confidence" to entry.value.confidence,
                            "reasoning" to entry.value.reasoning
                        )
                    }
                )
            )

            submissionsCollection.document(submission.id)
                .set(submissionMap, SetOptions.merge())
                .await()

            Result.success(submission.id)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to submit GD: ${e.message}", e))
        }
    }
    
    /**
     * Submit Lecturette test
     */
    suspend fun submitLecturette(
        submission: GTOSubmission.LecturetteSubmission,
        batchId: String?
    ): Result<String> {
        return try {
            val submissionMap = mapOf(
                FIELD_ID to submission.id,
                FIELD_USER_ID to submission.userId,
                FIELD_TEST_ID to submission.testId,
                FIELD_TEST_TYPE to TestType.GTO_LECTURETTE.name,
                FIELD_STATUS to submission.status.name,
                FIELD_SUBMITTED_AT to submission.submittedAt,
                FIELD_GRADED_BY_INSTRUCTOR_ID to null,
                FIELD_GRADING_TIMESTAMP to null,
                FIELD_BATCH_ID to batchId,
                FIELD_DATA to mapOf(
                    "topicChoices" to submission.topicChoices,
                    "selectedTopic" to submission.selectedTopic,
                    "speechTranscript" to submission.speechTranscript,
                    "wordCount" to submission.wordCount,
                    "timeSpent" to submission.timeSpent,
                    "olqScores" to submission.olqScores.mapKeys { it.key.name }.mapValues { entry ->
                        mapOf(
                            "score" to entry.value.score,
                            "confidence" to entry.value.confidence,
                            "reasoning" to entry.value.reasoning
                        )
                    }
                )
            )

            submissionsCollection.document(submission.id)
                .set(submissionMap, SetOptions.merge())
                .await()

            Result.success(submission.id)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to submit Lecturette: ${e.message}", e))
        }
    }
}
