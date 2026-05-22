package com.ssbmax.core.data.repository.interview

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ssbmax.core.domain.model.interview.InterviewResponse
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Delegate class to handle all response operations for the Interview system.
 * Keeps file sizes under the strict 300-line requirement.
 */
@Singleton
class InterviewResponseManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val sessionManager: InterviewSessionManager
) {
    companion object {
        private const val TAG = "InterviewResponseMgr"
        private const val COLLECTION_RESPONSES = "interview_responses"
        private const val FIELD_SESSION_ID = "sessionId"
        private const val FIELD_USER_ID = "userId"
        private const val FIELD_RESPONDED_AT = "respondedAt"
    }

    suspend fun submitResponse(response: InterviewResponse): Result<InterviewResponse> {
        return try {
            val session = sessionManager.getSession(response.sessionId).getOrNull()
                ?: return Result.failure(Exception("Session not found"))

            val responseMap = InterviewFirestoreMappers.responseToMap(response).toMutableMap()
            responseMap[FIELD_USER_ID] = session.userId

            firestore.collection(COLLECTION_RESPONSES)
                .document(response.id)
                .set(responseMap)
                .await()

            Log.d(TAG, "Submitted response: ${response.id}")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to submit response: ${response.id}", e)
            Result.failure(e)
        }
    }

    suspend fun updateResponse(response: InterviewResponse): Result<InterviewResponse> {
        return try {
            val session = sessionManager.getSession(response.sessionId).getOrNull()
                ?: return Result.failure(Exception("Session not found"))

            val responseMap = InterviewFirestoreMappers.responseToMap(response).toMutableMap()
            responseMap[FIELD_USER_ID] = session.userId

            firestore.collection(COLLECTION_RESPONSES)
                .document(response.id)
                .set(responseMap)
                .await()

            Log.d(TAG, "Updated response with OLQ scores: ${response.id}")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update response: ${response.id}", e)
            Result.failure(e)
        }
    }

    suspend fun getResponses(sessionId: String): Result<List<InterviewResponse>> {
        return try {
            val session = sessionManager.getSession(sessionId).getOrNull()
                ?: return Result.failure(Exception("Session not found"))

            val snapshot = firestore.collection(COLLECTION_RESPONSES)
                .whereEqualTo(FIELD_SESSION_ID, sessionId)
                .whereEqualTo(FIELD_USER_ID, session.userId)
                .orderBy(FIELD_RESPONDED_AT, Query.Direction.ASCENDING)
                .get()
                .await()

            val responses = snapshot.documents.mapNotNull { doc ->
                InterviewFirestoreMappers.mapToResponse(doc.data ?: return@mapNotNull null)
            }

            Log.d(TAG, "Retrieved ${responses.size} responses for session: $sessionId")
            Result.success(responses)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get responses for session: $sessionId", e)
            Result.failure(e)
        }
    }

    suspend fun getResponse(responseId: String): Result<InterviewResponse> {
        return try {
            val doc = firestore.collection(COLLECTION_RESPONSES)
                .document(responseId)
                .get()
                .await()

            val response = InterviewFirestoreMappers.mapToResponse(
                doc.data ?: return Result.failure(IllegalStateException("Response not found: $responseId"))
            )

            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get response: $responseId", e)
            Result.failure(e)
        }
    }
}
