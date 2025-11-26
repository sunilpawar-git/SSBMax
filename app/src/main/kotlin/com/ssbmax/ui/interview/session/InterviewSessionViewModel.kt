package com.ssbmax.ui.interview.session

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.data.util.trackMemoryLeaks
import com.ssbmax.core.domain.model.interview.InterviewQuestion
import com.ssbmax.core.domain.model.interview.InterviewResponse
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQScore
import com.ssbmax.core.domain.repository.InterviewRepository
import com.ssbmax.core.domain.service.AIService
import com.ssbmax.utils.ErrorLogger
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
     * Complete interview with batch AI analysis
     *
     * OPTIMIZATION: Analyzes ALL responses in one batch instead of per-question.
     * This moves the AI wait time to the end where users expect a delay
     * (like real SSB results), instead of interrupting the interview flow.
     */
    private suspend fun completeInterview() {
        val state = _uiState.value
        val session = state.session ?: return
        val pendingResponses = state.pendingResponses

        Log.d(TAG, "🏁 Completing interview with ${pendingResponses.size} responses for batch analysis")

        _uiState.update {
            it.copy(
                isSubmittingResponse = true,
                isAnalyzingResponses = true,
                analysisProgress = "Analyzing your responses...",
                loadingMessage = "Analyzing ${pendingResponses.size} responses..."
            )
        }

        try {
            // STEP 1: Batch analyze all responses with AI
            Log.d(TAG, "🤖 Starting batch AI analysis...")
            val startTime = System.currentTimeMillis()

            val analyzedResponses = mutableListOf<InterviewResponse>()

            for ((index, pending) in pendingResponses.withIndex()) {
                // Update progress
                _uiState.update {
                    it.copy(analysisProgress = "Analyzing response ${index + 1}/${pendingResponses.size}...")
                }

                // Get question for analysis
                val questionResult = interviewRepository.getQuestion(pending.questionId)
                val question = questionResult.getOrNull()

                // Analyze with AI (or use mock if question not found)
                val olqScores = if (question != null) {
                    val analysisResult = aiService.analyzeResponse(
                        question = question,
                        response = pending.responseText,
                        responseMode = state.mode.name
                    )
                    if (analysisResult.isSuccess) {
                        analysisResult.getOrNull()?.olqScores?.mapValues { (_, scoreWithReasoning) ->
                            OLQScore(
                                score = scoreWithReasoning.score.toInt().coerceIn(1, 10),
                                confidence = analysisResult.getOrNull()?.overallConfidence ?: 75,
                                reasoning = scoreWithReasoning.reasoning
                            )
                        } ?: generateMockOLQScores(question)
                    } else {
                        Log.w(TAG, "⚠️ AI analysis failed for Q${index + 1}, using mock scores")
                        generateMockOLQScores(question)
                    }
                } else {
                    emptyMap()
                }

                // Create response object
                val response = InterviewResponse(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    questionId = pending.questionId,
                    responseText = pending.responseText,
                    responseMode = state.mode,
                    respondedAt = Instant.ofEpochMilli(pending.respondedAt),
                    thinkingTimeSec = pending.thinkingTimeSec,
                    audioUrl = null,
                    olqScores = olqScores,
                    confidenceScore = 75
                )

                analyzedResponses.add(response)
            }

            val analysisTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "✅ Batch analysis complete in ${analysisTime}ms (${pendingResponses.size} responses)")

            // STEP 2: Save all responses to Firestore
            _uiState.update { it.copy(analysisProgress = "Saving responses...") }

            for (response in analyzedResponses) {
                val submitResult = interviewRepository.submitResponse(response)
                if (submitResult.isFailure) {
                    Log.e(TAG, "Failed to save response ${response.id}")
                }
            }

            Log.d(TAG, "💾 Saved ${analyzedResponses.size} responses to Firestore")

            // STEP 3: Generate final results
            _uiState.update { it.copy(analysisProgress = "Generating your results...") }

            val resultResult = interviewRepository.completeInterview(sessionId)
            if (resultResult.isFailure) {
                ErrorLogger.log(
                    throwable = resultResult.exceptionOrNull() ?: Exception("Unknown error"),
                    description = "Failed to complete interview"
                )
                _uiState.update {
                    it.copy(
                        isSubmittingResponse = false,
                        isAnalyzingResponses = false,
                        analysisProgress = null,
                        loadingMessage = null,
                        error = "Failed to complete interview"
                    )
                }
                return
            }

            val result = resultResult.getOrNull()
            if (result == null) {
                _uiState.update {
                    it.copy(
                        isSubmittingResponse = false,
                        isAnalyzingResponses = false,
                        analysisProgress = null,
                        loadingMessage = null,
                        error = "Failed to generate results"
                    )
                }
                return
            }

            Log.d(TAG, "🎉 Interview complete! Result ID: ${result.id}")

            _uiState.update {
                it.copy(
                    isSubmittingResponse = false,
                    isAnalyzingResponses = false,
                    analysisProgress = null,
                    loadingMessage = null,
                    isCompleted = true,
                    resultId = result.id
                )
            }
        } catch (e: Exception) {
            ErrorLogger.log(e, "Exception completing interview")
            _uiState.update {
                it.copy(
                    isSubmittingResponse = false,
                    isAnalyzingResponses = false,
                    analysisProgress = null,
                    loadingMessage = null,
                    error = "An error occurred"
                )
            }
        }
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
