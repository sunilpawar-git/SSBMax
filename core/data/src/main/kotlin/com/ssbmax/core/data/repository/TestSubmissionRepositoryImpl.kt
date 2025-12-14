package com.ssbmax.core.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.core.domain.model.SubmissionStatus
import com.ssbmax.core.domain.model.TestSubmission
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.repository.TestSubmissionRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestSubmissionRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : TestSubmissionRepository {

    private val submissionsCollection = firestore.collection("submissions")

    override suspend fun getSubmissionById(submissionId: String): Result<TestSubmission> {
        return try {
            val document = submissionsCollection.document(submissionId).get().await()
            val submission = document.toTestSubmission()
            if (submission != null) {
                Result.success(submission)
            } else {
                Result.failure(Exception("Submission not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getSubmissionsForStudent(studentId: String): Flow<List<TestSubmission>> {
        return callbackFlow {
            val listener = submissionsCollection
                .whereEqualTo("studentId", studentId)
                .orderBy("submittedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val submissions = snapshot?.documents?.mapNotNull { it.toTestSubmission() } ?: emptyList()
                    trySend(submissions)
                }
            awaitClose { listener.remove() }
        }
    }

    override fun getPendingSubmissions(assessorId: String): Flow<List<TestSubmission>> {
        return callbackFlow {
            val listener = submissionsCollection
                .whereEqualTo("gradingStatus", com.ssbmax.core.domain.model.GradingStatus.PENDING.name)
                .orderBy("submittedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val submissions = snapshot?.documents?.mapNotNull { it.toTestSubmission() } ?: emptyList()
                    trySend(submissions)
                }
            awaitClose { listener.remove() }
        }
    }

    override suspend fun submitTest(submission: TestSubmission): Result<Unit> {
        return try {
            submissionsCollection.document(submission.id)
                .set(submission.toMap())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateSubmission(submission: TestSubmission): Result<Unit> {
        return try {
            submissionsCollection.document(submission.id)
                .set(submission.toMap())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteSubmission(submissionId: String): Result<Unit> {
        return try {
            submissionsCollection.document(submissionId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Mappers
    private fun TestSubmission.toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "testId" to testId,
            "userId" to userId,
            "testType" to testType.name,
            "phase" to phase.name,
            "submittedAt" to submittedAt,
            "responses" to responses.map { it.toMap() },
            "aiPreliminaryScore" to aiPreliminaryScore,
            "instructorScore" to instructorScore,
            "finalScore" to finalScore,
            "gradingStatus" to gradingStatus.name,
            "instructorId" to instructorId,
            "instructorFeedback" to instructorFeedback,
            "gradedAt" to gradedAt,
            "timeSpent" to timeSpent,
            "batchId" to batchId
        )
    }

    private fun com.ssbmax.core.domain.model.TestResponse.toMap(): Map<String, Any?> {
        // Simplified mapping - just store questionId and timestamp
        // Actual response data will vary by test type
        return mapOf(
            "questionId" to questionId,
            "timestamp" to timestamp,
            "type" to this::class.simpleName
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun com.google.firebase.firestore.DocumentSnapshot.toTestSubmission(): TestSubmission? {
        return try {
            TestSubmission(
                id = getString("id") ?: return null,
                testId = getString("testId") ?: return null,
                userId = getString("userId") ?: return null,
                testType = TestType.valueOf(getString("testType") ?: return null),
                phase = com.ssbmax.core.domain.model.TestPhase.valueOf(getString("phase") ?: return null),
                submittedAt = getLong("submittedAt") ?: return null,
                responses = (get("responses") as? List<Map<String, Any?>>)?.mapNotNull { it.toTestResponse() } ?: emptyList(),
                aiPreliminaryScore = getDouble("aiPreliminaryScore")?.toFloat(),
                instructorScore = getDouble("instructorScore")?.toFloat(),
                finalScore = getDouble("finalScore")?.toFloat(),
                gradingStatus = com.ssbmax.core.domain.model.GradingStatus.valueOf(getString("gradingStatus") ?: "PENDING"),
                instructorId = getString("instructorId"),
                instructorFeedback = getString("instructorFeedback"),
                gradedAt = getLong("gradedAt"),
                timeSpent = getLong("timeSpent") ?: 0L,
                batchId = getString("batchId")
            )
        } catch (e: Exception) {
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.toTestResponse(): com.ssbmax.core.domain.model.TestResponse? {
        return try {
            val questionId = this["questionId"] as? String ?: return null
            val timestamp = (this["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
            
            // For now, return a simple MultipleChoice response
            // TODO: Implement proper deserialization based on "type" field
            com.ssbmax.core.domain.model.TestResponse.MultipleChoice(
                questionId = questionId,
                timestamp = timestamp,
                selectedOption = 0,
                isCorrect = null
            )
        } catch (e: Exception) {
            null
        }
    }
}

