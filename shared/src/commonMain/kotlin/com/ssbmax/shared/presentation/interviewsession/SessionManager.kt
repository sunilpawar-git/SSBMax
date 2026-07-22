package com.ssbmax.shared.presentation.interviewsession

import com.ssbmax.shared.domain.model.interview.InterviewQuestion
import com.ssbmax.shared.domain.model.interview.InterviewSession
import com.ssbmax.shared.domain.repository.InterviewRepository
import com.ssbmax.shared.domain.util.DomainLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * KMP port of `app/.../ui/interview/session/SessionManager.kt`. Verbatim
 * logic port -- only `android.util.Log` -> [DomainLogger] changes.
 *
 * Manages interview session state and question navigation. Single Source of
 * Truth for session state during an interview.
 */
class SessionManager(
    private val interviewRepository: InterviewRepository,
    private val logger: DomainLogger
) {
    private val tag = "SessionManager"

    private val _session = MutableStateFlow<InterviewSession?>(null)
    val session: StateFlow<InterviewSession?> = _session.asStateFlow()

    private val _currentQuestion = MutableStateFlow<InterviewQuestion?>(null)
    val currentQuestion: StateFlow<InterviewQuestion?> = _currentQuestion.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    val totalQuestions: Int
        get() = _session.value?.questionIds?.size ?: 0

    /** Load session and first question from repository. */
    suspend fun loadSession(sessionId: String): Result<Unit> {
        return try {
            val sessionResult = interviewRepository.getSession(sessionId)
            if (sessionResult.isFailure) {
                logger.e(tag, "Failed to get session: ${sessionResult.exceptionOrNull()?.message}")
                return Result.failure(sessionResult.exceptionOrNull() ?: Exception("Unknown error"))
            }

            val sessionData = sessionResult.getOrNull()
                ?: return Result.failure(Exception("Session not found"))

            _session.update { sessionData }
            _currentIndex.update { sessionData.currentQuestionIndex }

            val questionId = sessionData.questionIds.getOrNull(sessionData.currentQuestionIndex)
                ?: return Result.failure(Exception("No questions available"))

            val questionResult = interviewRepository.getQuestion(questionId)
            if (questionResult.isFailure) {
                logger.e(tag, "Failed to load question: ${questionResult.exceptionOrNull()?.message}")
                return Result.failure(questionResult.exceptionOrNull() ?: Exception("Failed to load question"))
            }

            _currentQuestion.update { questionResult.getOrNull() }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.e(tag, "Exception loading session", e)
            Result.failure(e)
        }
    }

    /** Load next question and update session state. */
    suspend fun loadNextQuestion(): Result<InterviewQuestion?> {
        val sessionData = _session.value ?: return Result.failure(Exception("No session loaded"))
        val nextIndex = _currentIndex.value + 1

        if (nextIndex >= sessionData.questionIds.size) {
            return Result.success(null)
        }

        val nextQuestionId = sessionData.questionIds.getOrNull(nextIndex)
            ?: return Result.success(null)

        return try {
            val updatedSession = sessionData.copy(currentQuestionIndex = nextIndex)
            interviewRepository.updateSession(updatedSession)

            val questionResult = interviewRepository.getQuestion(nextQuestionId)
            if (questionResult.isFailure) {
                logger.e(tag, "Failed to load question: ${questionResult.exceptionOrNull()?.message}")
                return Result.failure(questionResult.exceptionOrNull() ?: Exception("Failed to load question"))
            }

            val question = questionResult.getOrNull()
            _session.update { updatedSession }
            _currentIndex.update { nextIndex }
            _currentQuestion.update { question }

            Result.success(question)
        } catch (e: Exception) {
            logger.e(tag, "Exception loading next question", e)
            Result.failure(e)
        }
    }

    /** Check if there are more questions after the current one. */
    fun hasMoreQuestions(): Boolean {
        val sessionData = _session.value ?: return false
        return _currentIndex.value < sessionData.questionIds.size - 1
    }
}
