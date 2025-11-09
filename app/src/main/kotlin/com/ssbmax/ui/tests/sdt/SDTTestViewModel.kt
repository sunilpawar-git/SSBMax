package com.ssbmax.ui.tests.sdt

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.TestContentRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.core.domain.usecase.submission.SubmitSDTTestUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for SDT Test Screen
 * Loads 4 standard questions with 15-minute shared timer
 */
@HiltViewModel
class SDTTestViewModel @Inject constructor(
    private val testContentRepository: TestContentRepository,
    private val submitSDTTest: SubmitSDTTestUseCase,
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val userProfileRepository: com.ssbmax.core.domain.repository.UserProfileRepository,
    private val difficultyManager: com.ssbmax.core.data.repository.DifficultyProgressionManager,
    private val subscriptionManager: com.ssbmax.core.data.repository.SubscriptionManager,
    private val securityLogger: com.ssbmax.core.data.security.SecurityEventLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(SDTTestUiState())
    val uiState: StateFlow<SDTTestUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    companion object {
        private const val TAG = "SDTTestViewModel"
    }

    private suspend fun checkTestEligibility(userId: String): com.ssbmax.core.data.repository.TestEligibility {
        return subscriptionManager.canTakeTest(TestType.SD, userId)
    }

    fun loadTest(testId: String) {
        viewModelScope.launch {
            Log.d(TAG, "📋 SDT Flow: Loading test with ID: $testId")
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Checking eligibility...") }

            try {
                Log.d(TAG, "🔐 SDT Flow: Checking user authentication...")
                val user = observeCurrentUser().first()
                val userId = user?.id ?: run {
                    Log.e(TAG, "❌ SDT Flow: User not authenticated")
                    securityLogger.logUnauthenticatedAccess(TestType.SD, "SDTTestViewModel.loadTest")
                    _uiState.update { it.copy(isLoading = false, loadingMessage = null,
                        error = "Authentication required. Please login to continue.") }
                    return@launch
                }
                Log.d(TAG, "✅ SDT Flow: User authenticated - userId: $userId")

                Log.d(TAG, "🎫 SDT Flow: Checking test eligibility for user...")
                when (val eligibility = checkTestEligibility(userId)) {
                    is com.ssbmax.core.data.repository.TestEligibility.LimitReached -> {
                        Log.w(TAG, "⚠️ SDT Flow: Test limit reached - Tier: ${eligibility.tier}, Used: ${eligibility.usedCount}/${eligibility.limit}")
                        _uiState.update { it.copy(isLoading = false, loadingMessage = null, error = null,
                            isLimitReached = true, subscriptionTier = eligibility.tier,
                            testsLimit = eligibility.limit, testsUsed = eligibility.usedCount,
                            resetsAt = eligibility.resetsAt) }
                        return@launch
                    }
                    is com.ssbmax.core.data.repository.TestEligibility.Eligible -> {
                        Log.d(TAG, "✅ SDT Flow: User eligible to take test - Remaining: ${eligibility.remainingTests}")
                    }
                }

                _uiState.update { it.copy(loadingMessage = "Loading questions...") }
                Log.d(TAG, "🗂️ SDT Flow: Creating test session...")
                testContentRepository.createTestSession(userId, testId, TestType.SD).getOrThrow()
                
                Log.d(TAG, "📥 SDT Flow: Fetching SDT questions from repository...")
                val questions = testContentRepository.getSDTQuestions(testId).getOrThrow()
                if (questions.isEmpty()) {
                    Log.e(TAG, "❌ SDT Flow: No questions received from repository")
                    throw Exception("No questions found")
                }
                Log.d(TAG, "✅ SDT Flow: Loaded ${questions.size} questions successfully")
                questions.forEachIndexed { index, question ->
                    Log.d(TAG, "   Q${index + 1}: ${question.question.take(50)}...")
                }

                _uiState.update { it.copy(isLoading = false, loadingMessage = null, testId = testId,
                    questions = questions, config = SDTTestConfig(), phase = SDTPhase.INSTRUCTIONS) }
                Log.d(TAG, "🎯 SDT Flow: Test loaded successfully, showing instructions")
            } catch (e: Exception) {
                Log.e(TAG, "❌ SDT Flow: Failed to load test - ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, loadingMessage = null,
                    error = "Cloud connection required. Please check your internet connection.") }
            }
        }
    }

    fun startTest() {
        val timestamp = System.currentTimeMillis()
        Log.d(TAG, "▶️ SDT Flow: Starting test at timestamp: $timestamp")
        _uiState.update { it.copy(
            phase = SDTPhase.IN_PROGRESS,
            currentQuestionIndex = 0,
            startTime = timestamp
        ) }
        startTimer()
        Log.d(TAG, "⏱️ SDT Flow: Timer started - 15 minutes (900 seconds)")
    }

    fun updateAnswer(answer: String) {
        val words = answer.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
        val wordCount = words.size
        val maxWords = _uiState.value.config?.maxWordsPerQuestion ?: 1000
        
        if (words.size <= maxWords) {
            _uiState.update { it.copy(currentAnswer = answer) }
            Log.d(TAG, "✏️ SDT Flow: Answer updated - Q${_uiState.value.currentQuestionIndex + 1}, Words: $wordCount/$maxWords, Valid: ${wordCount <= maxWords}")
        } else {
            Log.w(TAG, "⚠️ SDT Flow: Word limit exceeded - Q${_uiState.value.currentQuestionIndex + 1}, Words: $wordCount/$maxWords")
        }
    }

    fun moveToNext() {
        val state = _uiState.value
        val currentQuestion = state.currentQuestion ?: return
        val questionNum = state.currentQuestionIndex + 1

        Log.d(TAG, "➡️ SDT Flow: Moving to next question from Q$questionNum")

        val response = SDTQuestionResponse(
            questionId = currentQuestion.id,
            question = currentQuestion.question,
            answer = state.currentAnswer,
            wordCount = state.currentWordCount,
            timeTakenSeconds = ((900 - state.totalTimeRemaining)),
            submittedAt = System.currentTimeMillis(),
            isSkipped = false
        )

        Log.d(TAG, "💾 SDT Flow: Saving response for Q$questionNum - Words: ${response.wordCount}, Time: ${response.timeTakenSeconds}s")

        val updatedResponses = state.responses.toMutableList().apply {
            removeAll { it.questionId == response.questionId }
            add(response)
        }

        _uiState.update { it.copy(responses = updatedResponses) }

        if (state.currentQuestionIndex < state.questions.size - 1) {
            val nextQuestionNum = state.currentQuestionIndex + 2
            Log.d(TAG, "🔄 SDT Flow: Transitioning to Q$nextQuestionNum")
            _uiState.update { it.copy(
                currentQuestionIndex = state.currentQuestionIndex + 1,
                currentAnswer = ""
            ) }
        } else {
            stopTimer()
            Log.d(TAG, "✅ SDT Flow: All questions answered, moving to review phase")
            Log.d(TAG, "📊 SDT Flow: Total responses: ${updatedResponses.size}, Valid: ${updatedResponses.count { it.isValidResponse }}")
            _uiState.update { it.copy(phase = SDTPhase.REVIEW) }
        }
    }

    fun skipQuestion() {
        val state = _uiState.value
        val currentQuestion = state.currentQuestion ?: return

        val response = SDTQuestionResponse(
            questionId = currentQuestion.id,
            question = currentQuestion.question,
            answer = "",
            wordCount = 0,
            timeTakenSeconds = 0,
            submittedAt = System.currentTimeMillis(),
            isSkipped = true
        )

        val updatedResponses = state.responses.toMutableList().apply {
            removeAll { it.questionId == response.questionId }
            add(response)
        }

        _uiState.update { it.copy(responses = updatedResponses, currentAnswer = "") }

        if (state.currentQuestionIndex < state.questions.size - 1) {
            _uiState.update { it.copy(currentQuestionIndex = state.currentQuestionIndex + 1) }
        } else {
            stopTimer()
            _uiState.update { it.copy(phase = SDTPhase.REVIEW) }
        }
    }

    fun editQuestion(index: Int) {
        val state = _uiState.value
        if (index in state.questions.indices) {
            val responseToEdit = state.responses.getOrNull(index)
            _uiState.update { it.copy(
                currentQuestionIndex = index,
                currentAnswer = responseToEdit?.answer ?: "",
                phase = SDTPhase.IN_PROGRESS
            ) }
        }
    }

    fun submitTest() {
        viewModelScope.launch {
            Log.d(TAG, "📤 SDT Flow: Initiating test submission...")
            _uiState.update { it.copy(isLoading = true) }
            try {
                stopTimer()
                val currentUserId = observeCurrentUser().first()?.id ?: run {
                    Log.e(TAG, "❌ SDT Flow: Cannot submit - user not authenticated")
                    _uiState.update { it.copy(isLoading = false, error = "Please login to submit test") }
                    return@launch
                }

                val subscriptionType = userProfileRepository.getUserProfile(currentUserId).first()
                    .getOrNull()?.subscriptionType ?: SubscriptionType.FREE
                val state = _uiState.value
                val totalTimeMinutes = ((System.currentTimeMillis() - state.startTime) / 60000).toInt()
                
                Log.d(TAG, "📋 SDT Flow: Creating submission object...")
                Log.d(TAG, "   User ID: $currentUserId")
                Log.d(TAG, "   Test ID: ${state.testId}")
                Log.d(TAG, "   Responses: ${state.responses.size}")
                Log.d(TAG, "   Time taken: $totalTimeMinutes minutes")
                
                val submission = SDTSubmission(
                    userId = currentUserId,
                    testId = state.testId,
                    responses = state.responses,
                    totalTimeTakenMinutes = totalTimeMinutes,
                    submittedAt = System.currentTimeMillis(),
                    aiPreliminaryScore = SDTTestScoring.generateMockAIScore(state.responses)
                )

                Log.d(TAG, "🤖 SDT Flow: Generated AI preliminary score: ${submission.aiPreliminaryScore?.overallScore}")

                val scorePercentage = if (submission.totalResponses > 0)
                    (submission.validResponses.toFloat() / submission.totalResponses) * 100 else 0f
                difficultyManager.recordPerformance("SDT", "MEDIUM", scorePercentage,
                    submission.validResponses, submission.totalResponses, (totalTimeMinutes * 60).toFloat())
                subscriptionManager.recordTestUsage(TestType.SD, currentUserId)

                Log.d(TAG, "☁️ SDT Flow: Submitting to Firestore...")
                Log.d(TAG, "   Submission ID: ${submission.id}")
                submitSDTTest(submission, null).onSuccess { submissionId ->
                    Log.d(TAG, "✅ SDT Flow: Submission successful!")
                    Log.d(TAG, "   Firestore Document ID: $submissionId")
                    _uiState.update { it.copy(isLoading = false, isSubmitted = true,
                        submissionId = submissionId, subscriptionType = subscriptionType,
                        phase = SDTPhase.SUBMITTED) }
                }.onFailure { error ->
                    Log.e(TAG, "❌ SDT Flow: Submission failed - ${error.message}", error)
                    _uiState.update { it.copy(isLoading = false, error = "Failed to submit: ${error.message}") }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ SDT Flow: Exception during submission - ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun startTimer() {
        stopTimer()
        timerJob = viewModelScope.launch {
            try {
                while (isActive && _uiState.value.totalTimeRemaining > 0) {
                    delay(1000)
                    if (!isActive) break
                    _uiState.update { it.copy(totalTimeRemaining = it.totalTimeRemaining - 1) }
                }
                if (isActive && _uiState.value.totalTimeRemaining == 0) {
                    _uiState.update { it.copy(phase = SDTPhase.REVIEW) }
                }
            } catch (e: CancellationException) {
                throw e
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}

data class SDTTestUiState(
    val isLoading: Boolean = true,
    val loadingMessage: String? = null,
    val testId: String = "",
    val questions: List<SDTQuestion> = emptyList(),
    val config: SDTTestConfig? = null,
    val currentQuestionIndex: Int = 0,
    val responses: List<SDTQuestionResponse> = emptyList(),
    val currentAnswer: String = "",
    val phase: SDTPhase = SDTPhase.INSTRUCTIONS,
    val totalTimeRemaining: Int = 900, // 15 minutes
    val startTime: Long = System.currentTimeMillis(),
    val isSubmitted: Boolean = false,
    val submissionId: String? = null,
    val subscriptionType: SubscriptionType? = null,
    val error: String? = null,
    val isLimitReached: Boolean = false,
    val subscriptionTier: SubscriptionTier = SubscriptionTier.FREE,
    val testsLimit: Int = 1,
    val testsUsed: Int = 0,
    val resetsAt: String = ""
) {
    val currentQuestion: SDTQuestion?
        get() = questions.getOrNull(currentQuestionIndex)

    val completedQuestions: Int
        get() = responses.size

    val validResponseCount: Int
        get() = responses.count { it.isValidResponse }

    val progress: Float
        get() = if (questions.isEmpty()) 0f else (completedQuestions.toFloat() / questions.size)

    val currentWordCount: Int
        get() = currentAnswer.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size

    val canMoveToNext: Boolean
        get() {
            val maxWords = config?.maxWordsPerQuestion ?: 1000
            return currentWordCount <= maxWords
        }
}

