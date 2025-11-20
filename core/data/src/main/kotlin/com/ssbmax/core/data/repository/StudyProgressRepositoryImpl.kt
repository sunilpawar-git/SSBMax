package com.ssbmax.core.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.core.domain.model.StudyProgress
import com.ssbmax.core.domain.model.StudySession
import com.ssbmax.core.domain.repository.StudyProgressRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase implementation of StudyProgressRepository
 * Stores user study progress and session tracking in Firestore
 */
@Singleton
class StudyProgressRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : StudyProgressRepository {

    override fun observeProgress(userId: String, materialId: String): Flow<StudyProgress?> =
        callbackFlow {
            val listenerRegistration = firestore
                .collection(PROGRESS_COLLECTION)
                .document(getProgressDocId(userId, materialId))
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error observing progress", error)
                        close(error)
                        return@addSnapshotListener
                    }

                    val progress = snapshot?.toStudyProgress()
                    trySend(progress)
                }

            awaitClose { listenerRegistration.remove() }
        }

    override suspend fun saveProgress(progress: StudyProgress): Result<Unit> {
        return try {
            val docId = getProgressDocId(progress.userId, progress.materialId)
            firestore.collection(PROGRESS_COLLECTION)
                .document(docId)
                .set(progress.toMap())
                .await()

            Log.d(TAG, "Progress saved for material ${progress.materialId}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving progress", e)
            Result.failure(e)
        }
    }

    override suspend fun getProgress(userId: String, materialId: String): Result<StudyProgress?> {
        return try {
            val docId = getProgressDocId(userId, materialId)
            val snapshot = firestore.collection(PROGRESS_COLLECTION)
                .document(docId)
                .get()
                .await()

            val progress = snapshot.toStudyProgress()
            Result.success(progress)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting progress", e)
            Result.failure(e)
        }
    }

    override suspend fun getAllProgress(userId: String): Result<List<StudyProgress>> {
        return try {
            val querySnapshot = firestore.collection(PROGRESS_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val progressList = querySnapshot.documents.mapNotNull { it.toStudyProgress() }
            Result.success(progressList)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all progress", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteProgress(userId: String, materialId: String): Result<Unit> {
        return try {
            val docId = getProgressDocId(userId, materialId)
            firestore.collection(PROGRESS_COLLECTION)
                .document(docId)
                .delete()
                .await()

            Log.d(TAG, "Progress deleted for material $materialId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting progress", e)
            Result.failure(e)
        }
    }

    override suspend fun startSession(userId: String, materialId: String): Result<StudySession> {
        return try {
            val sessionId = UUID.randomUUID().toString()
            val session = StudySession(
                id = sessionId,
                userId = userId,
                materialId = materialId,
                startedAt = System.currentTimeMillis(),
                endedAt = null,
                duration = 0,
                progressIncrement = 0f
            )

            firestore.collection(SESSIONS_COLLECTION)
                .document(sessionId)
                .set(session.toMap())
                .await()

            Log.d(TAG, "Study session started: $sessionId")
            Result.success(session)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting session", e)
            Result.failure(e)
        }
    }

    override suspend fun endSession(sessionId: String, progressIncrement: Float): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()

            // Get session to calculate duration
            val sessionDoc = firestore.collection(SESSIONS_COLLECTION)
                .document(sessionId)
                .get()
                .await()

            val startedAt = sessionDoc.getLong("startedAt") ?: now
            val duration = now - startedAt

            // Update session
            firestore.collection(SESSIONS_COLLECTION)
                .document(sessionId)
                .update(
                    mapOf(
                        "endedAt" to now,
                        "duration" to duration,
                        "progressIncrement" to progressIncrement
                    )
                )
                .await()

            Log.d(TAG, "Study session ended: $sessionId (duration: ${duration / 1000}s)")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error ending session", e)
            Result.failure(e)
        }
    }

    override suspend fun getActiveSession(userId: String): Result<StudySession?> {
        return try {
            val querySnapshot = firestore.collection(SESSIONS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("endedAt", null)
                .limit(1)
                .get()
                .await()

            val session = querySnapshot.documents.firstOrNull()?.toStudySession()
            Result.success(session)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting active session", e)
            Result.failure(e)
        }
    }

    override suspend fun getSessionsForMaterial(
        userId: String,
        materialId: String
    ): Result<List<StudySession>> {
        return try {
            val querySnapshot = firestore.collection(SESSIONS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("materialId", materialId)
                .get()
                .await()

            val sessions = querySnapshot.documents.mapNotNull { it.toStudySession() }
            Result.success(sessions)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting sessions for material", e)
            Result.failure(e)
        }
    }

    private fun getProgressDocId(userId: String, materialId: String): String {
        return "${userId}_$materialId"
    }

    companion object {
        private const val TAG = "StudyProgressRepo"
        private const val PROGRESS_COLLECTION = "study_progress"
        private const val SESSIONS_COLLECTION = "study_sessions"
    }
}

// Extension functions for Firestore conversions
private fun com.google.firebase.firestore.DocumentSnapshot.toStudyProgress(): StudyProgress? {
    return try {
        StudyProgress(
            materialId = getString("materialId") ?: return null,
            userId = getString("userId") ?: return null,
            progress = getDouble("progress")?.toFloat() ?: 0f,
            lastReadAt = getLong("lastReadAt"),
            timeSpent = getLong("timeSpent") ?: 0L,
            isBookmarked = getBoolean("isBookmarked") ?: false,
            isCompleted = getBoolean("isCompleted") ?: false,
            notes = getString("notes"),
            highlights = get("highlights") as? List<String> ?: emptyList()
        )
    } catch (e: Exception) {
        Log.e("StudyProgressRepo", "Error converting progress", e)
        null
    }
}

private fun StudyProgress.toMap(): Map<String, Any?> {
    return mapOf(
        "materialId" to materialId,
        "userId" to userId,
        "progress" to progress,
        "lastReadAt" to lastReadAt,
        "timeSpent" to timeSpent,
        "isBookmarked" to isBookmarked,
        "isCompleted" to isCompleted,
        "notes" to notes,
        "highlights" to highlights
    )
}

private fun com.google.firebase.firestore.DocumentSnapshot.toStudySession(): StudySession? {
    return try {
        StudySession(
            id = id,
            userId = getString("userId") ?: return null,
            materialId = getString("materialId") ?: return null,
            startedAt = getLong("startedAt") ?: return null,
            endedAt = getLong("endedAt"),
            duration = getLong("duration") ?: 0L,
            progressIncrement = getDouble("progressIncrement")?.toFloat() ?: 0f
        )
    } catch (e: Exception) {
        Log.e("StudyProgressRepo", "Error converting session", e)
        null
    }
}

private fun StudySession.toMap(): Map<String, Any?> {
    return mapOf(
        "userId" to userId,
        "materialId" to materialId,
        "startedAt" to startedAt,
        "endedAt" to endedAt,
        "duration" to duration,
        "progressIncrement" to progressIncrement
    )
}
