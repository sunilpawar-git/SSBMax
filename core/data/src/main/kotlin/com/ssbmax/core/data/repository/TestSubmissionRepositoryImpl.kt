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
    internal fun TestSubmission.toMap(): Map<String, Any?> {
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

    internal fun com.ssbmax.core.domain.model.TestResponse.toMap(): Map<String, Any?> {
        val baseMap = mutableMapOf<String, Any?>(
            "questionId" to questionId,
            "timestamp" to timestamp,
            "type" to this::class.simpleName
        )
        when (this) {
            is com.ssbmax.core.domain.model.TestResponse.MultipleChoice -> {
                baseMap["selectedOption"] = selectedOption
                baseMap["isCorrect"] = isCorrect
            }
            is com.ssbmax.core.domain.model.TestResponse.TextResponse -> {
                baseMap["answer"] = answer
                baseMap["wordCount"] = wordCount
            }
            is com.ssbmax.core.domain.model.TestResponse.ImageBasedResponse -> {
                baseMap["imageUrl"] = imageUrl
                baseMap["description"] = description
            }
            is com.ssbmax.core.domain.model.TestResponse.RatingResponse -> {
                baseMap["rating"] = rating
                baseMap["comment"] = comment
            }
        }
        return baseMap
    }

    @Suppress("UNCHECKED_CAST")
    internal fun com.google.firebase.firestore.DocumentSnapshot.toTestSubmission(): TestSubmission? {
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
    internal fun Map<String, Any?>.toTestResponse(): com.ssbmax.core.domain.model.TestResponse? {
        return try {
            val questionId = this["questionId"] as? String ?: return null
            val timestamp = (this["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
            val type = this["type"] as? String ?: return null

            when (type) {
                "MultipleChoice" -> {
                    val selectedOption = (this["selectedOption"] as? Number)?.toInt() ?: 0
                    val isCorrect = this["isCorrect"] as? Boolean
                    com.ssbmax.core.domain.model.TestResponse.MultipleChoice(
                        questionId = questionId,
                        timestamp = timestamp,
                        selectedOption = selectedOption,
                        isCorrect = isCorrect
                    )
                }
                "TextResponse" -> {
                    val answer = this["answer"] as? String ?: ""
                    com.ssbmax.core.domain.model.TestResponse.TextResponse(
                        questionId = questionId,
                        timestamp = timestamp,
                        answer = answer
                    )
                }
                "ImageBasedResponse" -> {
                    val imageUrl = this["imageUrl"] as? String ?: ""
                    val description = this["description"] as? String ?: ""
                    com.ssbmax.core.domain.model.TestResponse.ImageBasedResponse(
                        questionId = questionId,
                        timestamp = timestamp,
                        imageUrl = imageUrl,
                        description = description
                    )
                }
                "RatingResponse" -> {
                    val rating = (this["rating"] as? Number)?.toInt() ?: 0
                    val comment = this["comment"] as? String
                    com.ssbmax.core.domain.model.TestResponse.RatingResponse(
                        questionId = questionId,
                        timestamp = timestamp,
                        rating = rating,
                        comment = comment
                    )
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}

