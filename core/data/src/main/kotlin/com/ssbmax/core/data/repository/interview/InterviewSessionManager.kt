package com.ssbmax.core.data.repository.interview

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ssbmax.core.domain.constants.InterviewConstants
import com.ssbmax.core.domain.model.interview.InterviewMode
import com.ssbmax.core.domain.model.interview.InterviewSession
import com.ssbmax.core.domain.model.interview.InterviewStatus
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Delegate class to handle all session management operations for the Interview system.
 * Keeps file sizes under the strict 300-line requirement.
 */
@Singleton
class InterviewSessionManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val questionGenerator: InterviewQuestionGenerator
) {
    companion object {
        private const val TAG = "InterviewSessionMgr"
        private const val COLLECTION_SESSIONS = "interview_sessions"
        private const val COLLECTION_QUESTIONS = "interview_questions"
        private const val FIELD_USER_ID = "userId"
        private const val FIELD_STATUS = "status"
        private const val FIELD_STARTED_AT = "startedAt"
    }

    suspend fun createSession(
        userId: String,
        mode: InterviewMode,
        piqSnapshotId: String,
        consentGiven: Boolean
    ): Result<InterviewSession> {
        return try {
            val questionsResult = questionGenerator.generateQuestions(
                piqSnapshotId,
                InterviewConstants.TARGET_TOTAL_QUESTIONS
            )

            if (questionsResult.isFailure) {
                return Result.failure(
                    questionsResult.exceptionOrNull() ?: Exception("Failed to generate questions")
                )
            }

            val questions = questionsResult.getOrNull() ?: emptyList()

            val session = InterviewSession(
                id = UUID.randomUUID().toString(),
                userId = userId,
                mode = mode,
                status = InterviewStatus.IN_PROGRESS,
                startedAt = Instant.now(),
                completedAt = null,
                piqSnapshotId = piqSnapshotId,
                consentGiven = consentGiven,
                questionIds = questions.map { it.id },
                currentQuestionIndex = 0,
                estimatedDuration = InterviewConstants.DEFAULT_DURATION_MINUTES
            )

            val batch = firestore.batch()
            questions.forEach { question ->
                val questionDoc = firestore.collection(COLLECTION_QUESTIONS).document(question.id)
                batch.set(questionDoc, InterviewFirestoreMappers.questionToMap(question))
            }

            val sessionDoc = firestore.collection(COLLECTION_SESSIONS).document(session.id)
            batch.set(sessionDoc, InterviewFirestoreMappers.sessionToMap(session))

            batch.commit().await()

            Log.d(TAG, "Created interview session: ${session.id} with ${questions.size} questions")
            Result.success(session)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create interview session", e)
            Result.failure(e)
        }
    }

    suspend fun getActiveSession(userId: String): Result<InterviewSession?> {
        return try {
            val snapshot = firestore.collection(COLLECTION_SESSIONS)
                .whereEqualTo(FIELD_USER_ID, userId)
                .whereEqualTo(FIELD_STATUS, InterviewStatus.IN_PROGRESS.name)
                .orderBy(FIELD_STARTED_AT, Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            val session = snapshot.documents.firstOrNull()?.let { doc ->
                InterviewFirestoreMappers.mapToSession(doc.data ?: return@let null)
            }

            Result.success(session)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get active session for user: $userId", e)
            Result.failure(e)
        }
    }

    suspend fun getSession(sessionId: String): Result<InterviewSession> {
        return try {
            val doc = firestore.collection(COLLECTION_SESSIONS)
                .document(sessionId)
                .get()
                .await()

            val session = InterviewFirestoreMappers.mapToSession(
                doc.data ?: return Result.failure(IllegalStateException("Session not found: $sessionId"))
            )

            Result.success(session)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get session: $sessionId", e)
            Result.failure(e)
        }
    }

    suspend fun updateSession(session: InterviewSession): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_SESSIONS)
                .document(session.id)
                .set(InterviewFirestoreMappers.sessionToMap(session))
                .await()

            Log.d(TAG, "Updated session: ${session.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update session: ${session.id}", e)
            Result.failure(e)
        }
    }

    suspend fun abandonSession(sessionId: String): Result<Unit> {
        return try {
            val sessionResult = getSession(sessionId)
            if (sessionResult.isFailure) {
                return Result.failure(sessionResult.exceptionOrNull() ?: Exception("Session not found"))
            }

            val session = sessionResult.getOrNull() ?: return Result.failure(
                IllegalStateException("Session is null despite successful result")
            )

            updateSession(session.copy(status = InterviewStatus.ABANDONED, completedAt = Instant.now()))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to abandon session: $sessionId", e)
            Result.failure(e)
        }
    }
}
