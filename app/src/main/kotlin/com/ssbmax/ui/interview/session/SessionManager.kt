package com.ssbmax.ui.interview.session

import android.util.Log
import com.ssbmax.core.domain.model.interview.InterviewQuestion
import com.ssbmax.core.domain.model.interview.InterviewSession
import com.ssbmax.core.domain.repository.InterviewRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Manages interview session state and question navigation.
 *
 * Responsibilities:
 * - Load session from repository
 * - Load current and next questions
 * - Track session progress (current index, total questions)
 * - Update session state in repository
 *
 * Single Source of Truth for session state during interview.
 */
class SessionManager(
    private val interviewRepository: InterviewRepository
) {
    companion object {
        private const val TAG = "SessionManager"
    }

    private val _session = MutableStateFlow<InterviewSession?>(null)
    val session: StateFlow<InterviewSession?> = _session.asStateFlow()

    private val _currentQuestion = MutableStateFlow<InterviewQuestion?>(null)
    val currentQuestion: StateFlow<InterviewQuestion?> = _currentQuestion.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    val totalQuestions: Int
        get() = _session.value?.questionIds?.size ?: 0

    /**
     * Load session and first question from repository.
     *
     * @param sessionId ID of the session to load
     * @return Success with Unit, or Failure with exception
     */
    suspend fun loadSession(sessionId: String): Result<Unit> {
        Log.d(TAG, "📥 Loading session: $sessionId")

        return try {
            val sessionResult = interviewRepository.getSession(sessionId)
            if (sessionResult.isFailure) {
                Log.e(TAG, "Failed to get session: ${sessionResult.exceptionOrNull()?.message}")
                return Result.failure(sessionResult.exceptionOrNull() ?: Exception("Unknown error"))
            }

            val sessionData = sessionResult.getOrNull()
            if (sessionData == null) {
                Log.e(TAG, "Session not found: $sessionId")
                return Result.failure(Exception("Session not found"))
            }

            _session.update { sessionData }
            _currentIndex.update { sessionData.currentQuestionIndex }

            // Load first question
            val questionId = sessionData.questionIds.getOrNull(sessionData.currentQuestionIndex)
            if (questionId == null) {
                Log.e(TAG, "No question at index ${sessionData.currentQuestionIndex}")
                return Result.failure(Exception("No questions available"))
            }

            val questionResult = interviewRepository.getQuestion(questionId)
            if (questionResult.isFailure) {
                Log.e(TAG, "Failed to load question: ${questionResult.exceptionOrNull()?.message}")
                return Result.failure(questionResult.exceptionOrNull() ?: Exception("Failed to load question"))
            }

            _currentQuestion.update { questionResult.getOrNull() }
            Log.d(TAG, "✅ Session loaded: ${sessionData.questionIds.size} questions")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Exception loading session", e)
            Result.failure(e)
        }
    }

    /**
     * Load next question and update session state.
     *
     * @return Success with next question (null if no more), or Failure with exception
     */
    suspend fun loadNextQuestion(): Result<InterviewQuestion?> {
        val sessionData = _session.value ?: return Result.failure(Exception("No session loaded"))
        val nextIndex = _currentIndex.value + 1

        if (nextIndex >= sessionData.questionIds.size) {
            Log.d(TAG, "📥 No more questions (index $nextIndex >= ${sessionData.questionIds.size})")
            return Result.success(null)
        }

        val nextQuestionId = sessionData.questionIds.getOrNull(nextIndex)
        if (nextQuestionId == null) {
            Log.e(TAG, "No question ID at index $nextIndex")
            return Result.success(null)
        }

        Log.d(TAG, "📥 Loading question $nextIndex: $nextQuestionId")

        return try {
            // Update session in repository
            val updatedSession = sessionData.copy(currentQuestionIndex = nextIndex)
            interviewRepository.updateSession(updatedSession)

            // Load the question
            val questionResult = interviewRepository.getQuestion(nextQuestionId)
            if (questionResult.isFailure) {
                Log.e(TAG, "Failed to load question: ${questionResult.exceptionOrNull()?.message}")
                return Result.failure(questionResult.exceptionOrNull() ?: Exception("Failed to load question"))
            }

            val question = questionResult.getOrNull()
            _session.update { updatedSession }
            _currentIndex.update { nextIndex }
            _currentQuestion.update { question }

            Log.d(TAG, "✅ Loaded question $nextIndex")
            Result.success(question)
        } catch (e: Exception) {
            Log.e(TAG, "Exception loading next question", e)
            Result.failure(e)
        }
    }

    /**
     * Check if there are more questions after the current one.
     */
    fun hasMoreQuestions(): Boolean {
        val sessionData = _session.value ?: return false
        return _currentIndex.value < sessionData.questionIds.size - 1
    }
}
