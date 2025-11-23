package com.ssbmax.ui.interview.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.data.util.trackMemoryLeaks
import com.ssbmax.core.domain.model.interview.InterviewResponse
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
                val session = state.session ?: return@launch

                // Analyze response with AI
                val analysisResult = aiService.analyzeResponse(
                    question = currentQuestion,
                    response = state.responseText,
                    responseMode = state.mode.name
                )

                val analysis = if (analysisResult.isSuccess) {
                    analysisResult.getOrNull()
                } else {
                    ErrorLogger.log(
                        throwable = analysisResult.exceptionOrNull() ?: Exception("Unknown error"),
                        description = "AI analysis failed for interview response"
                    )
                    null
                }

                // Convert OLQScoreWithReasoning to OLQScore, or use mock scores as fallback
                val olqScores = if (analysis != null) {
                    analysis.olqScores.mapValues { (_, scoreWithReasoning) ->
                        OLQScore(
                            score = scoreWithReasoning.score.toInt().coerceIn(1, 5),
                            confidence = analysis.overallConfidence,
                            reasoning = scoreWithReasoning.reasoning
                        )
                    }
                } else {
                    // AI failed - use mock OLQ scores for development
                    generateMockOLQScores(currentQuestion)
                }

                // Create response object
                val response = InterviewResponse(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    questionId = currentQuestion.id,
                    responseText = state.responseText,
                    responseMode = state.mode,
                    respondedAt = Instant.now(),
                    thinkingTimeSec = state.getThinkingTimeSeconds(),
                    audioUrl = null,
                    olqScores = olqScores,
                    confidenceScore = analysis?.overallConfidence ?: 75 // Mock confidence when AI fails
                )

                // Submit response
                val submitResult = interviewRepository.submitResponse(response)
                if (submitResult.isFailure) {
                    ErrorLogger.log(
                        throwable = submitResult.exceptionOrNull() ?: Exception("Unknown error"),
                        description = "Failed to submit interview response"
                    )
                    _uiState.update {
                        it.copy(
                            isSubmittingResponse = false,
                            error = "Failed to submit response"
                        )
                    }
                    return@launch
                }

                // Check if there are more questions
                if (state.hasMoreQuestions()) {
                    // Move to next question
                    loadNextQuestion()
                } else {
                    // Complete interview
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
     * Load next question
     */
    private suspend fun loadNextQuestion() {
        val state = _uiState.value
        val session = state.session ?: return

        val nextIndex = state.currentQuestionIndex + 1
        val nextQuestionId = session.questionIds.getOrNull(nextIndex) ?: return

        // Update session progress
        val updatedSession = session.copy(currentQuestionIndex = nextIndex)
        interviewRepository.updateSession(updatedSession)

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
     * Complete interview and generate result
     */
    private suspend fun completeInterview() {
        _uiState.update {
            it.copy(
                isSubmittingResponse = true,
                loadingMessage = "Generating results..."
            )
        }

        try {
            val resultResult = interviewRepository.completeInterview(sessionId)
            if (resultResult.isFailure) {
                ErrorLogger.log(
                    throwable = resultResult.exceptionOrNull() ?: Exception("Unknown error"),
                    description = "Failed to complete interview"
                )
                _uiState.update {
                    it.copy(
                        isSubmittingResponse = false,
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
                        loadingMessage = null,
                        error = "Failed to generate results"
                    )
                }
                return
            }

            _uiState.update {
                it.copy(
                    isSubmittingResponse = false,
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
    private fun generateMockOLQScores(question: com.ssbmax.core.domain.model.interview.InterviewQuestion): Map<com.ssbmax.core.domain.model.interview.OLQ, OLQScore> {
        val scores = mutableMapOf<com.ssbmax.core.domain.model.interview.OLQ, OLQScore>()

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
        val otherOLQs = com.ssbmax.core.domain.model.interview.OLQ.entries
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
