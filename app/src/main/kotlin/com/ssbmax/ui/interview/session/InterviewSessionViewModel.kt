package com.ssbmax.ui.interview.session

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ssbmax.core.data.util.trackMemoryLeaks
import com.ssbmax.core.domain.model.interview.InterviewQuestion
import com.ssbmax.core.domain.model.interview.InterviewResponse
import com.ssbmax.core.domain.model.interview.InterviewStatus
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQScore
import com.ssbmax.core.domain.repository.InterviewRepository
import com.ssbmax.core.domain.service.AIService
import com.ssbmax.utils.ErrorLogger
import com.ssbmax.workers.InterviewAnalysisWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for Interview Session screen
 *
 * Responsibilities:
 * - Load interview session and questions
 * - Manage question-by-question flow
 * - Submit responses with AI analysis
 * - Complete interview and navigate to results
 *
 * MEMORY LEAK PREVENTION:
 * - Registers with MemoryLeakTracker
 * - Uses viewModelScope for all coroutines (auto-cancelled)
 * - No static references or context leaks
 */
@HiltViewModel
class InterviewSessionViewModel @Inject constructor(
    private val interviewRepository: InterviewRepository,
    private val aiService: AIService,
    private val workManager: WorkManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val TAG = "InterviewSessionVM"
    }

    private val sessionId: String = checkNotNull(savedStateHandle.get<String>("sessionId")) {
        "sessionId is required"
    }

    private val _uiState = MutableStateFlow(InterviewSessionUiState())
    val uiState: StateFlow<InterviewSessionUiState> = _uiState.asStateFlow()

    init {
        // Register for memory leak tracking
        trackMemoryLeaks("InterviewSessionViewModel")

        // Load session
        loadSession()
    }

    /**
     * Load interview session and first question
     */
    private fun loadSession() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    loadingMessage = "Loading interview session..."
                )
            }

            try {
                // Get session
                val sessionResult = interviewRepository.getSession(sessionId)
                if (sessionResult.isFailure) {
                    ErrorLogger.log(
                        throwable = sessionResult.exceptionOrNull() ?: Exception("Unknown error"),
                        description = "Failed to load interview session"
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadingMessage = null,
                            error = "Failed to load session"
                        )
                    }
                    return@launch
                }

                val session = sessionResult.getOrNull()
                if (session == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadingMessage = null,
                            error = "Session not found"
                        )
                    }
                    return@launch
                }

                // Load current question
                val questionId = session.questionIds.getOrNull(session.currentQuestionIndex)
                if (questionId == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadingMessage = null,
                            error = "No questions available"
                        )
                    }
                    return@launch
                }

                val questionResult = interviewRepository.getQuestion(questionId)
                if (questionResult.isFailure) {
                    ErrorLogger.log(
                        throwable = questionResult.exceptionOrNull() ?: Exception("Unknown error"),
                        description = "Failed to load interview question"
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadingMessage = null,
                            error = "Failed to load question"
                        )
                    }
                    return@launch
                }

                val question = questionResult.getOrNull()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingMessage = null,
                        session = session,
                        currentQuestion = question,
                        currentQuestionIndex = session.currentQuestionIndex,
                        totalQuestions = session.questionIds.size,
                        thinkingStartTime = System.currentTimeMillis()
                    )
                }
            } catch (e: Exception) {
                ErrorLogger.log(e, "Exception loading interview session")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingMessage = null,
                        error = "An error occurred"
                    )
                }
            }
        }
    }

    /**
     * Update response text
     */
    fun updateResponse(text: String) {
        _uiState.update { it.copy(responseText = text) }
    }

    /**
     * Submit current response and move to next question
     *
     * OPTIMIZATION: Stores response locally and moves to next question INSTANTLY.
     * AI analysis is deferred to completeInterview() for batch processing.
     * This reduces per-question time from 6-8s to <100ms.
     */
    fun submitResponse() {
        if (!_uiState.value.canSubmitResponse()) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSubmittingResponse = true,
                    error = null
                )
            }

            try {
                val state = _uiState.value
                val currentQuestion = state.currentQuestion ?: return@launch

                // Store response locally (NO AI analysis here - instant!)
                Log.d(TAG, "⚡ Storing response locally (batch analysis at end) - Question: ${currentQuestion.id}")
                val pendingResponse = PendingResponse(
                    questionId = currentQuestion.id,
                    questionText = currentQuestion.questionText,
                    responseText = state.responseText,
                    thinkingTimeSec = state.getThinkingTimeSeconds()
                )

                // Add to pending list
                val updatedPending = state.pendingResponses + pendingResponse
                Log.d(TAG, "📝 Added to pending (${updatedPending.size}/${state.totalQuestions} responses stored)")

                // Check if there are more questions
                if (state.hasMoreQuestions()) {
                    // Update state with new pending response and load next question
                    _uiState.update { it.copy(pendingResponses = updatedPending) }
                    loadNextQuestion()
                } else {
                    // Last question - update state and start batch analysis
                    _uiState.update { it.copy(pendingResponses = updatedPending) }
                    completeInterview()
                }
            } catch (e: Exception) {
                ErrorLogger.log(e, "Exception submitting interview response")
                _uiState.update {
                    it.copy(
                        isSubmittingResponse = false,
                        error = "An error occurred"
                    )
                }
            }
        }
    }

    /**
     * Load next question (instant - no AI analysis)
     */
    private suspend fun loadNextQuestion() {
        val state = _uiState.value
        val session = state.session ?: return

        val nextIndex = state.currentQuestionIndex + 1
        val nextQuestionId = session.questionIds.getOrNull(nextIndex) ?: return

        Log.d(TAG, "➡️ Loading next question (${nextIndex + 1}/${state.totalQuestions})")

        // Update session progress in Firestore (async, don't wait)
        val updatedSession = session.copy(currentQuestionIndex = nextIndex)
        viewModelScope.launch {
            interviewRepository.updateSession(updatedSession)
        }

        // Load next question
        val questionResult = interviewRepository.getQuestion(nextQuestionId)
        if (questionResult.isFailure) {
            ErrorLogger.log(
                throwable = questionResult.exceptionOrNull() ?: Exception("Unknown error"),
                description = "Failed to load next question"
            )
            _uiState.update {
                it.copy(
                    isSubmittingResponse = false,
                    error = "Failed to load next question"
                )
            }
            return
        }

        val question = questionResult.getOrNull()

        _uiState.update {
            it.copy(
                isSubmittingResponse = false,
                session = updatedSession,
                currentQuestion = question,
                currentQuestionIndex = nextIndex,
                responseText = "",
                thinkingStartTime = System.currentTimeMillis()
            )
        }
    }

    /**
     * Complete interview with BACKGROUND AI analysis
     *
     * FLOW:
     * 1. Save responses immediately WITHOUT OLQ scores (instant)
     * 2. Update session status to PENDING_ANALYSIS
     * 3. Enqueue InterviewAnalysisWorker for background processing
     * 4. Navigate user to home immediately with "results pending" message
     * 5. Background: AI analyzes all responses, generates result, sends notification
     *
     * This provides real-time UX - user doesn't wait for AI analysis.
     * Results appear later via notification (like real SSB).
     */
    private suspend fun completeInterview() {
        val state = _uiState.value
        val session = state.session ?: return
        val pendingResponses = state.pendingResponses

        Log.d(TAG, "🏁 Completing interview with ${pendingResponses.size} responses (background analysis mode)")

        _uiState.update {
            it.copy(
                isSubmittingResponse = true,
                loadingMessage = "Submitting your answers..."
            )
        }

        try {
            // STEP 1: Save all responses to Firestore WITHOUT OLQ scores (INSTANT)
            // OLQ scores will be added by background worker
            Log.d(TAG, "💾 Saving ${pendingResponses.size} responses to Firestore (no AI analysis)...")
            
            for ((index, pending) in pendingResponses.withIndex()) {
                // Create response object WITHOUT OLQ scores - worker will add them
                val response = InterviewResponse(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    questionId = pending.questionId,
                    responseText = pending.responseText,
                    responseMode = state.mode,
                    respondedAt = Instant.ofEpochMilli(pending.respondedAt),
                    thinkingTimeSec = pending.thinkingTimeSec,
                    audioUrl = null,
                    olqScores = emptyMap(),  // Empty - worker will fill this
                    confidenceScore = 0       // Worker will update
                )

                val submitResult = interviewRepository.submitResponse(response)
                if (submitResult.isFailure) {
                    Log.w(TAG, "Failed to save response ${index + 1}: ${submitResult.exceptionOrNull()?.message}")
                }
            }

            Log.d(TAG, "✅ Saved ${pendingResponses.size} responses to Firestore")

            // STEP 2: Update session status to PENDING_ANALYSIS
            Log.d(TAG, "📊 Updating session status to PENDING_ANALYSIS...")
            val updatedSession = session.copy(
                status = InterviewStatus.PENDING_ANALYSIS,
                currentQuestionIndex = session.questionIds.size - 1  // Mark all questions answered
            )
            val sessionUpdateResult = interviewRepository.updateSession(updatedSession)
            if (sessionUpdateResult.isFailure) {
                Log.w(TAG, "Failed to update session status: ${sessionUpdateResult.exceptionOrNull()?.message}")
            }

            // STEP 3: Enqueue background analysis worker
            Log.d(TAG, "🔄 Enqueuing InterviewAnalysisWorker for background processing...")
            enqueueAnalysisWorker(sessionId)

            Log.d(TAG, "🎉 Responses saved! Navigating to home. Results will be available shortly via notification.")

            // STEP 4: Navigate user away immediately
            _uiState.update {
                it.copy(
                    isSubmittingResponse = false,
                    loadingMessage = null,
                    isCompleted = true,
                    isResultPending = true,  // Signal UI to show pending message
                    resultId = null          // No result yet - will come via notification
                )
            }

        } catch (e: Exception) {
            ErrorLogger.log(e, "Exception completing interview")
            _uiState.update {
                it.copy(
                    isSubmittingResponse = false,
                    loadingMessage = null,
                    error = "An error occurred"
                )
            }
        }
    }

    /**
     * Enqueue the InterviewAnalysisWorker for background AI processing
     */
    private fun enqueueAnalysisWorker(sessionId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)  // Need network for Gemini API
            .build()

        val workRequest = OneTimeWorkRequestBuilder<InterviewAnalysisWorker>()
            .setInputData(
                workDataOf(InterviewAnalysisWorker.KEY_SESSION_ID to sessionId)
            )
            .setConstraints(constraints)
            .build()

        // Use unique work to prevent duplicate workers for same session
        workManager.enqueueUniqueWork(
            "interview_analysis_$sessionId",
            ExistingWorkPolicy.KEEP,  // Don't restart if already running
            workRequest
        )

        Log.d(TAG, "📥 InterviewAnalysisWorker enqueued for session: $sessionId")
    }

    /**
     * Generate mock OLQ scores for development (when AI fails)
     *
     * Uses SSB 1-10 scale (LOWER is BETTER):
     * - Expected OLQs: 5-6 (Good to Very Good)
     * - Other OLQs: 6-7 (Good to Average)
     * Bell curve distribution based on SSB standards
     */
    private fun generateMockOLQScores(question: InterviewQuestion): Map<OLQ, OLQScore> {
        val scores = mutableMapOf<OLQ, OLQScore>()

        // Score the question's expected OLQs better (5-6 range, weighted toward 5)
        question.expectedOLQs.forEach { olq ->
            // Weighted random: 70% chance of 5, 30% chance of 6
            val score = if (Math.random() < 0.7) 5 else 6  // Very Good to Good
            scores[olq] = OLQScore(
                score = score,
                confidence = 75,
                reasoning = "Mock score for development (AI not available)"
            )
        }

        // Score a few random other OLQs slightly lower (6-7 range)
        val otherOLQs = OLQ.entries
            .filter { it !in question.expectedOLQs }
            .shuffled()
            .take(2) // Score 2 additional random OLQs

        otherOLQs.forEach { olq ->
            // Weighted random: 60% chance of 6, 40% chance of 7
            val score = if (Math.random() < 0.6) 6 else 7  // Good to Average
            scores[olq] = OLQScore(
                score = score,
                confidence = 65,
                reasoning = "Mock score for development (AI not available)"
            )
        }

        return scores
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
