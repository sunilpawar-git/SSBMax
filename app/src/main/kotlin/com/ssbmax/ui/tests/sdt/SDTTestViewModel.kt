package com.ssbmax.ui.tests.sdt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.model.scoring.AnalysisStatus
import com.ssbmax.core.domain.repository.TestContentRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.core.domain.usecase.submission.SubmitSDTTestUseCase
import com.ssbmax.utils.ErrorLogger
import com.ssbmax.workers.SDTAnalysisWorker
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
import kotlinx.coroutines.flow.receiveAsFlow
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
    private val securityLogger: com.ssbmax.core.data.security.SecurityEventLogger,
    private val workManager: WorkManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SDTTestUiState())
    val uiState: StateFlow<SDTTestUiState> = _uiState.asStateFlow()


    // Navigation events (one-time events, consumed on collection)
    private val _navigationEvents = kotlinx.coroutines.channels.Channel<com.ssbmax.ui.tests.common.TestNavigationEvent>(kotlinx.coroutines.channels.Channel.BUFFERED)
    val navigationEvents = _navigationEvents.receiveAsFlow()

    // Timer Job reference for explicit cancellation (prevents "rushing" bug)
    private var timerJob: kotlinx.coroutines.Job? = null

    companion object {
        private const val TAG = "SDTTestViewModel"
    }

    private suspend fun checkTestEligibility(userId: String): com.ssbmax.core.data.repository.TestEligibility {
        return subscriptionManager.canTakeTest(TestType.SD, userId)
    }

    fun loadTest(testId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Checking eligibility...") }

            try {
                val user = observeCurrentUser().first()
                val userId = user?.id ?: run {
                    ErrorLogger.logTestError(
                        throwable = IllegalStateException("User not authenticated"),
                        description = "SDT test access without authentication",
                        testType = "SDT"
                    )
                    securityLogger.logUnauthenticatedAccess(TestType.SD, "SDTTestViewModel.loadTest")
                    _uiState.update { it.copy(isLoading = false, loadingMessage = null,
                        error = "Authentication required. Please login to continue.") }
                    return@launch
                }

                when (val eligibility = checkTestEligibility(userId)) {
                    is com.ssbmax.core.data.repository.TestEligibility.LimitReached -> {
                        _uiState.update { it.copy(isLoading = false, loadingMessage = null, error = null,
                            isLimitReached = true, subscriptionTier = eligibility.tier,
                            testsLimit = eligibility.limit, testsUsed = eligibility.usedCount,
                            resetsAt = eligibility.resetsAt) }
                        return@launch
                    }
                    is com.ssbmax.core.data.repository.TestEligibility.Eligible -> {
                        // User is eligible to take test
                    }
                }

                _uiState.update { it.copy(loadingMessage = "Loading questions...") }
                testContentRepository.createTestSession(userId, testId, TestType.SD).getOrThrow()

                val questions = testContentRepository.getSDTQuestions(testId).getOrThrow()
                if (questions.isEmpty()) {
                    throw Exception("No questions found")
                }

                _uiState.update { it.copy(isLoading = false, loadingMessage = null, testId = testId,
                    questions = questions, config = SDTTestConfig(), phase = SDTPhase.INSTRUCTIONS) }
            } catch (e: Exception) {
                ErrorLogger.logTestError(e, "Failed to load SDT test", "SDT")
                _uiState.update { it.copy(isLoading = false, loadingMessage = null,
                    error = "Cloud connection required. Please check your internet connection.") }
            }
        }
    }

    fun startTest() {
        val timestamp = System.currentTimeMillis()
        _uiState.update { it.copy(
            phase = SDTPhase.IN_PROGRESS,
            currentQuestionIndex = 0,
            startTime = timestamp
        ) }
        startTimer()
    }

    fun updateAnswer(answer: String) {
        val maxChars = _uiState.value.config?.maxCharsPerQuestion ?: 1500

        if (answer.trim().length <= maxChars) {
            _uiState.update { it.copy(currentAnswer = answer) }
        }
    }

    fun moveToNext() {
        val state = _uiState.value
        val currentQuestion = state.currentQuestion ?: return

        val response = SDTQuestionResponse(
            questionId = currentQuestion.id,
            question = currentQuestion.question,
            answer = state.currentAnswer,
            charCount = state.currentCharCount,
            timeTakenSeconds = ((900 - state.totalTimeRemaining)),
            submittedAt = System.currentTimeMillis(),
            isSkipped = false
        )

        val updatedResponses = state.responses.toMutableList().apply {
            removeAll { it.questionId == response.questionId }
            add(response)
        }

        _uiState.update { it.copy(responses = updatedResponses) }

        if (state.currentQuestionIndex < state.questions.size - 1) {
            _uiState.update { it.copy(
                currentQuestionIndex = state.currentQuestionIndex + 1,
                currentAnswer = ""
            ) }
        } else {
            // PHASE 2: stopTimer() removed - viewModelScope auto-cancels
            _uiState.update { it.copy(isTimerActive = false) }
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
            charCount = 0,
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
            // PHASE 2: stopTimer() removed - viewModelScope auto-cancels
            _uiState.update { it.copy(
                isTimerActive = false,
                phase = SDTPhase.REVIEW
            ) }
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
            _uiState.update { it.copy(isLoading = true) }
            val currentUserId = observeCurrentUser().first()?.id ?: run {
                ErrorLogger.logTestError(
                    throwable = IllegalStateException("User not authenticated"),
                    description = "SDT test submission without authentication",
                    testType = "SDT"
                )
                _uiState.update { it.copy(isLoading = false, error = "Please login to submit test") }
                return@launch
            }

            try {
                // PHASE 2: stopTimer() removed - viewModelScope auto-cancels
                _uiState.update { it.copy(isTimerActive = false) }

                val subscriptionType = userProfileRepository.getUserProfile(currentUserId).first()
                    .getOrNull()?.subscriptionType ?: SubscriptionType.FREE
                val state = _uiState.value
                val totalTimeMinutes = ((System.currentTimeMillis() - state.startTime) / 60000).toInt()

                val submission = SDTSubmission(
                    userId = currentUserId,
                    testId = state.testId,
                    responses = state.responses,
                    totalTimeTakenMinutes = totalTimeMinutes,
                    submittedAt = System.currentTimeMillis(),

                    analysisStatus = AnalysisStatus.PENDING_ANALYSIS,
                    olqResult = null
                )

                val scorePercentage = if (submission.totalResponses > 0)
                    (submission.validResponses.toFloat() / submission.totalResponses) * 100 else 0f
                
                // Record performance analytics (using recommended difficulty)
                val difficulty = difficultyManager.getRecommendedDifficulty("SDT")
                difficultyManager.recordPerformance(
                    testType = "SDT",
                    difficulty = difficulty,
                    score = scorePercentage,
                    correctAnswers = submission.validResponses,
                    totalQuestions = submission.totalResponses,
                    timeSeconds = (totalTimeMinutes * 60).toFloat()
                )

                submitSDTTest(submission, null).onSuccess { submissionId ->
                    android.util.Log.d(TAG, "✅ Submission successful! ID: $submissionId")

                    // Enqueue SDTAnalysisWorker for OLQ analysis
                    android.util.Log.d(TAG, "📍 Enqueueing SDTAnalysisWorker...")
                    enqueueSDTAnalysisWorker(submissionId)
                    android.util.Log.d(TAG, "✅ SDTAnalysisWorker enqueued successfully")

                    // Record test usage for subscription tracking (with submissionId for idempotency)
                    android.util.Log.d(TAG, "📍 Recording test usage for subscription...")
                    subscriptionManager.recordTestUsage(TestType.SD, currentUserId, submissionId)
                    android.util.Log.d(TAG, "✅ Test usage recorded successfully!")

                    _uiState.update { it.copy(isLoading = false, isSubmitted = true,
                        submissionId = submissionId, subscriptionType = subscriptionType,
                        submission = submission,  // Store locally to show results directly
                        phase = SDTPhase.SUBMITTED) }

                    // Emit navigation event (one-time, consumed by screen)
                    _navigationEvents.trySend(
                        com.ssbmax.ui.tests.common.TestNavigationEvent.NavigateToResult(
                            submissionId = submissionId,
                            subscriptionType = subscriptionType
                        )
                    )
                }.onFailure { error ->
                    ErrorLogger.logTestError(error, "SDT test submission failed", "SDT", currentUserId)
                    _uiState.update { it.copy(isLoading = false, error = "Failed to submit: ${error.message}") }
                }
            } catch (e: Exception) {
                ErrorLogger.logTestError(e, "SDT test submission exception", "SDT", currentUserId)
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun startTimer() {
        // PHASE 4 FIX: Cancel previous timer to prevent concurrency bug
        timerJob?.cancel()
        
        val totalTimeMinutes = 15
        val totalTimeSeconds = totalTimeMinutes * 60
        
        _uiState.update { it.copy(
            totalTimeRemaining = totalTimeSeconds,
            isTimerActive = true,
            timerStartTime = System.currentTimeMillis()
        ) }
        
        // PHASE 4 FIX: Delta-based calculation for 15-minute timer
        val startTime = System.currentTimeMillis()
        val endTime = startTime + (totalTimeSeconds * 1000)
        
        timerJob = viewModelScope.launch {
            android.util.Log.d(TAG, "⏰ Starting SDT timer (${totalTimeMinutes}min)")
            
            try {
                while (isActive) {
                    val remainingMillis = endTime - System.currentTimeMillis()
                    val remainingSeconds = (remainingMillis / 1000).toInt()
                    
                    if (remainingSeconds <= 0) break
                    
                    _uiState.update { it.copy(totalTimeRemaining = remainingSeconds) }
                    
                    delay(200) // Update every 200ms for smooth UI
                }
                
                if (isActive) {
                    android.util.Log.d(TAG, "⏰ SDT timer completed, moving to review")
                    _uiState.update { it.copy(phase = SDTPhase.REVIEW) }
                }
            } catch (e: CancellationException) {
                android.util.Log.d(TAG, "⏰ SDT timer cancelled")
                throw e
            } finally {
                _uiState.update { it.copy(isTimerActive = false) }
            }
        }
    }

    // PHASE 3: stopTimer() removed - viewModelScope automatically cancels all jobs

    /**
     * Enqueue SDTAnalysisWorker for background OLQ analysis
     */
    private fun enqueueSDTAnalysisWorker(submissionId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<SDTAnalysisWorker>()
            .setInputData(workDataOf(SDTAnalysisWorker.KEY_SUBMISSION_ID to submissionId))
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            "sdt_analysis_$submissionId",
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }

    override fun onCleared() {
        super.onCleared()
        
        // PHASE 4 FIX: Explicitly cancel timer
        timerJob?.cancel()
        
        android.util.Log.d(TAG, "🧹 ViewModel onCleared() - timer cancelled")
        _navigationEvents.close()
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
    val resetsAt: String = "",
    // PHASE 1: New StateFlow fields (replacing nullable vars)
    val isTimerActive: Boolean = false,
    val timerStartTime: Long = 0L,
    val submission: SDTSubmission? = null
) {
    val currentQuestion: SDTQuestion?
        get() = questions.getOrNull(currentQuestionIndex)

    val completedQuestions: Int
        get() = responses.size

    val validResponseCount: Int
        get() = responses.count { it.isValidResponse }

    val progress: Float
        get() = if (questions.isEmpty()) 0f else (completedQuestions.toFloat() / questions.size)

    val currentCharCount: Int
        get() = currentAnswer.trim().length

    val canMoveToNext: Boolean
        get() {
            val minChars = config?.minCharsPerQuestion ?: 50
            val maxChars = config?.maxCharsPerQuestion ?: 1500
            return currentCharCount >= minChars && currentCharCount <= maxChars
        }
}

