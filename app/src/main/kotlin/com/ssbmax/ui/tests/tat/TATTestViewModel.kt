package com.ssbmax.ui.tests.tat

import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.ssbmax.core.data.repository.DifficultyProgressionManager
import com.ssbmax.core.data.repository.SubscriptionManager
import com.ssbmax.core.data.repository.TestEligibility
import com.ssbmax.core.data.security.SecurityEventLogger
import com.ssbmax.core.data.util.MemoryLeakTracker
import com.ssbmax.core.data.util.trackMemoryLeaks
import com.ssbmax.shared.domain.model.*
import com.ssbmax.shared.domain.model.SubscriptionType
import com.ssbmax.shared.domain.model.scoring.AnalysisStatus
import com.ssbmax.shared.domain.repository.UserProfileRepository
import com.ssbmax.shared.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.shared.domain.usecase.submission.SubmitTATTestUseCase
import com.ssbmax.shared.domain.usecase.tat.LoadTATTestUseCase
import com.ssbmax.shared.domain.usecase.tat.LoadTATTestUseCase.ProfileIncompleteException
import com.ssbmax.ui.tests.common.BaseTestViewModel
import com.ssbmax.ui.tests.common.TestNavigationEvent
import com.ssbmax.utils.ErrorLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class TATTestViewModel(
    private val loadTATTest: LoadTATTestUseCase,
    private val submitTATTest: SubmitTATTestUseCase,
    observeCurrentUser: ObserveCurrentUserUseCase,
    private val userProfileRepository: UserProfileRepository,
    subscriptionManager: SubscriptionManager,
    private val difficultyManager: DifficultyProgressionManager,
    securityLogger: SecurityEventLogger,
    workManager: WorkManager,
    private val pipelineOrchestrator: TATAnalysisPipelineOrchestrator
) : BaseTestViewModel(observeCurrentUser, subscriptionManager, securityLogger, workManager) {

    private val _uiState = MutableStateFlow(TATTestUiState())
    val uiState: StateFlow<TATTestUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        trackMemoryLeaks("TATTestViewModel")
    }

    fun loadTest(testId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Checking eligibility...") }
            try {
                val user = withTimeout(3000L) { observeCurrentUser().first() }
                val userId = user?.id ?: run {
                    ErrorLogger.log(
                        Exception("Unauthenticated TAT test access attempt"),
                        "SECURITY: Unauthenticated test access attempt blocked"
                    )
                    securityLogger.logUnauthenticatedAccess(
                        testType = TestType.TAT,
                        context = "TATTestViewModel.loadTest"
                    )
                    _uiState.update { it.copy(isLoading = false, loadingMessage = null,
                        error = "Authentication required. Please login to continue.") }
                    return@launch
                }
                when (val eligibility = subscriptionManager.canTakeTest(TestType.TAT, userId)) {
                    is TestEligibility.LimitReached -> {
                        _uiState.update { it.copy(isLoading = false, loadingMessage = null,
                            isLimitReached = true, subscriptionTier = eligibility.tier,
                            testsLimit = eligibility.limit, testsUsed = eligibility.usedCount,
                            resetsAt = eligibility.resetsAt) }
                        return@launch
                    }
                    is TestEligibility.NetworkError -> {
                        _uiState.update { it.copy(isLoading = false, loadingMessage = null,
                            error = "No connection. Please check your network and try again.") }
                        return@launch
                    }
                    is TestEligibility.Eligible -> Unit
                }
                _uiState.update { it.copy(loadingMessage = "Fetching questions from cloud...") }
                val loadResult = loadTATTest(userId, testId)
                if (loadResult.isFailure) {
                    val e = loadResult.exceptionOrNull()!!
                    if (e is ProfileIncompleteException) {
                        _uiState.update { it.copy(isLoading = false, loadingMessage = null,
                            isProfileIncomplete = true) }
                    } else {
                        ErrorLogger.log(e, "Failed to load TAT test: $testId")
                        _uiState.update { it.copy(isLoading = false, loadingMessage = null,
                            error = "Cloud connection required. Please check your internet connection.") }
                    }
                    return@launch
                }
                val questions = loadResult.getOrThrow()
                if (questions.isEmpty()) {
                    val e = Exception("No TAT questions found for test: $testId")
                    ErrorLogger.log(e, "No TAT questions found")
                    throw e
                }
                _uiState.update { it.copy(isLoading = false, loadingMessage = null,
                    testId = testId, questions = questions, config = TATTestConfig(),
                    phase = TATPhase.INSTRUCTIONS) }
            } catch (e: Exception) {
                ErrorLogger.log(e, "Exception loading TAT test: $testId")
                _uiState.update { it.copy(isLoading = false, loadingMessage = null,
                    error = "Cloud connection required. Please check your internet connection.") }
            }
        }
    }

    fun startTest() {
        _uiState.update { it.copy(phase = TATPhase.IMAGE_VIEWING, currentQuestionIndex = 0) }
        startViewingTimer()
    }

    fun updateStory(story: String) {
        _uiState.update { it.copy(currentStory = story) }
    }

    private fun saveCurrentStoryToResponses() {
        val state = _uiState.value
        val currentQuestion = state.currentQuestion ?: return
        if (state.currentStory.isBlank()) return
        val response = TATStoryResponse(
            questionId = currentQuestion.id,
            story = state.currentStory,
            charactersCount = state.currentStory.length,
            viewingTimeTakenSeconds = 30 - state.viewingTimeRemaining,
            writingTimeTakenSeconds = (4 * 60) - state.writingTimeRemaining,
            submittedAt = System.currentTimeMillis()
        )
        val updatedResponses = state.responses.toMutableList().apply {
            removeAll { it.questionId == response.questionId }
            add(response)
        }
        _uiState.update { it.copy(responses = updatedResponses) }
    }

    fun moveToNextQuestion() {
        saveCurrentStoryToResponses()
        val state = _uiState.value
        if (state.currentQuestionIndex < state.questions.size - 1) {
            _uiState.update { it.copy(currentQuestionIndex = state.currentQuestionIndex + 1,
                currentStory = "", phase = TATPhase.IMAGE_VIEWING) }
            startViewingTimer()
        } else {
            _uiState.update { it.copy(isTimerActive = false, phase = TATPhase.REVIEW) }
        }
    }

    fun editCurrentStory() { _uiState.update { it.copy(phase = TATPhase.WRITING) }; startWritingTimer() }

    fun confirmCurrentStory() { moveToNextQuestion() }

    fun submitTest() {
        viewModelScope.launch {
            saveCurrentStoryToResponses()
            _uiState.update { it.copy(isLoading = true, isTimerActive = false) }
            var capturedUserId: String? = null
            try {
                val userId = withTimeout(3000L) { observeCurrentUser().first()?.id }
                if (userId == null) {
                    ErrorLogger.log(Exception("Unauthenticated TAT submission"), "TAT: submission blocked")
                    _uiState.update { it.copy(isLoading = false, error = "Please login to submit test") }
                    return@launch
                }
                capturedUserId = userId

                val subscriptionType = withTimeout(5000L) {
                    userProfileRepository.getUserProfile(userId).first().getOrNull()
                }?.subscriptionType ?: SubscriptionType.FREE

                val state = _uiState.value
                val submission = TATSubmission(
                    userId = userId,
                    testId = state.testId,
                    stories = state.responses,
                    totalTimeTakenMinutes = ((System.currentTimeMillis() - state.startTime) / 60000).toInt(),
                    submittedAt = System.currentTimeMillis(),
                    analysisStatus = AnalysisStatus.PENDING_ANALYSIS,
                    olqResult = null
                )

                submitTATTest(submission, batchId = null)
                    .onSuccess { submissionId ->
                        pipelineOrchestrator.startPipeline(submissionId, submission.stories, state.questions)
                            .onFailure { e ->
                                ErrorLogger.log(e, "Pipeline start failed for $submissionId — submission saved")
                            }
                        recordPerformanceAndUsage(submission, submissionId, userId)
                        _uiState.update { it.copy(
                            isLoading = false, isSubmitted = true, submissionId = submissionId,
                            subscriptionType = subscriptionType, submission = submission,
                            phase = TATPhase.SUBMITTED
                        ) }
                        sendNavigationEvent(TestNavigationEvent.NavigateToResult(submissionId, subscriptionType))
                    }
                    .onFailure { error ->
                        ErrorLogger.log(error, "Failed to submit TAT test for user: $userId")
                        _uiState.update { it.copy(isLoading = false, error = "Failed to submit: ${error.message}") }
                    }
            } catch (e: Exception) {
                ErrorLogger.log(e, "Exception during TAT submission for user: ${capturedUserId ?: "unknown"}")
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private suspend fun recordPerformanceAndUsage(
        submission: TATSubmission,
        submissionId: String,
        userId: String
    ) {
        val validCount = submission.stories.count { it.charactersCount >= 150 }
        val totalCount = submission.stories.size
        val scorePercentage = if (totalCount > 0) (validCount.toFloat() / totalCount) * 100 else 0f
        val difficulty = difficultyManager.getRecommendedDifficulty("TAT")
        difficultyManager.recordPerformance(
            testType = "TAT",
            difficulty = difficulty,
            score = scorePercentage,
            correctAnswers = validCount,
            totalQuestions = totalCount,
            timeSeconds = (submission.totalTimeTakenMinutes * 60).toFloat()
        )
        subscriptionManager.recordTestUsage(TestType.TAT, userId, submissionId)
    }

    private fun startViewingTimer() {
        timerJob?.cancel()
        val myGeneration = ++timerGeneration
        val viewingTime = _uiState.value.config?.viewingTimePerPictureSeconds ?: 30
        _uiState.update { it.copy(viewingTimeRemaining = viewingTime, isTimerActive = true,
            timerStartTime = myGeneration) }
        val endTime = System.currentTimeMillis() + (viewingTime * 1000L)
        timerJob = viewModelScope.launch {
            try {
                while (isActive) {
                    val remaining = ((endTime - System.currentTimeMillis()) / 1000).toInt()
                    if (remaining <= 0) break
                    _uiState.update { it.copy(viewingTimeRemaining = remaining) }
                    delay(200)
                }
                if (isActive) {
                    _uiState.update { it.copy(phase = TATPhase.WRITING) }
                    startWritingTimer()
                }
            } finally {
                _uiState.update { c ->
                    if (c.timerStartTime == myGeneration) c.copy(isTimerActive = false) else c
                }
            }
        }.also { it.trackMemoryLeaks("TATTestViewModel", "viewing-timer") }
    }

    private fun startWritingTimer() {
        timerJob?.cancel()
        val myGeneration = ++timerGeneration
        val writingTimeSeconds = (_uiState.value.config?.writingTimePerPictureMinutes ?: 4) * 60
        _uiState.update { it.copy(writingTimeRemaining = writingTimeSeconds, isTimerActive = true,
            timerStartTime = myGeneration) }
        val endTime = System.currentTimeMillis() + (writingTimeSeconds * 1000L)
        timerJob = viewModelScope.launch {
            try {
                while (isActive) {
                    val remaining = ((endTime - System.currentTimeMillis()) / 1000).toInt()
                    if (remaining <= 0) break
                    _uiState.update { it.copy(writingTimeRemaining = remaining) }
                    delay(200)
                }
                if (isActive) {
                    saveCurrentStoryToResponses()
                    _uiState.update { it.copy(phase = TATPhase.REVIEW) }
                }
            } finally {
                _uiState.update { c ->
                    if (c.timerStartTime == myGeneration) c.copy(isTimerActive = false) else c
                }
            }
        }.also { it.trackMemoryLeaks("TATTestViewModel", "writing-timer") }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        MemoryLeakTracker.unregisterViewModel("TATTestViewModel")
        MemoryLeakTracker.forceGcAndLog("TATTestViewModel-Cleared")
    }
}
