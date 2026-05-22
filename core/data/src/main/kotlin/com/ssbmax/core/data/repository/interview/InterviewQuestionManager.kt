package com.ssbmax.core.data.repository.interview

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.core.domain.constants.InterviewConstants
import com.ssbmax.core.domain.model.interview.InterviewQuestion
import com.ssbmax.core.domain.model.interview.QuestionCacheRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Delegate class to handle all question operations for the Interview system.
 * Keeps file sizes under the strict 300-line requirement.
 */
@Singleton
class InterviewQuestionManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val questionGenerator: InterviewQuestionGenerator,
    private val questionCacheRepository: QuestionCacheRepository
) {
    companion object {
        private const val TAG = "InterviewQuestionMgr"
        private const val COLLECTION_QUESTIONS = "interview_questions"
    }

    suspend fun generateQuestions(
        sessionId: String,
        piqSnapshotId: String,
        count: Int
    ): Result<List<InterviewQuestion>> {
        return questionGenerator.generateQuestions(piqSnapshotId, count)
    }

    suspend fun getQuestion(questionId: String): Result<InterviewQuestion> {
        return try {
            val doc = firestore.collection(COLLECTION_QUESTIONS)
                .document(questionId)
                .get()
                .await()

            val question = InterviewFirestoreMappers.mapToQuestion(
                doc.data ?: return Result.failure(IllegalStateException("Question not found: $questionId"))
            )

            Result.success(question)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get question: $questionId", e)
            Result.failure(e)
        }
    }

    suspend fun cacheQuestions(
        piqSnapshotId: String,
        questions: List<InterviewQuestion>
    ): Result<Unit> {
        return questionCacheRepository.cachePIQQuestions(
            piqSnapshotId = piqSnapshotId,
            questions = questions,
            expirationDays = InterviewConstants.DEFAULT_CACHE_EXPIRATION_DAYS
        )
    }

    suspend fun getCachedQuestions(
        piqSnapshotId: String,
        limit: Int
    ): Result<List<InterviewQuestion>> {
        return questionCacheRepository.getPIQQuestions(
            piqSnapshotId = piqSnapshotId,
            limit = limit,
            excludeUsed = true
        )
    }
}
