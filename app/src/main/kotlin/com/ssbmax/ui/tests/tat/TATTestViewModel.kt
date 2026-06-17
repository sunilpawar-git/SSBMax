package com.ssbmax.ui.tests.tat

import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkContinuation
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ssbmax.core.data.repository.SubscriptionManager
import com.ssbmax.core.data.security.SecurityEventLogger
import com.ssbmax.core.data.util.MemoryLeakTracker
import com.ssbmax.core.data.util.trackMemoryLeaks
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.model.SubscriptionType
import com.ssbmax.core.domain.model.scoring.AnalysisStatus
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.core.domain.usecase.submission.SubmitTATTestUseCase
import com.ssbmax.core.domain.usecase.tat.LoadTATTestUseCase
import com.ssbmax.ui.tests.common.BaseTestViewModel
import com.ssbmax.ui.tests.common.TestNavigationEvent
import com.ssbmax.utils.ErrorLogger
import com.ssbmax.workers.TATStoryAnalysisWorker
import com.ssbmax.workers.TATSynthesisWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class TATTestViewModel @Inject constructor(
    private val loadTATTest: LoadTATTestUseCase,
    private val submitTATTest: SubmitTATTestUseCase,
    observeCurrentUser: ObserveCurrentUserUseCase,
    private val userProfileRepository: com.ssbmax.core.domain.repository.UserProfileRepository,
    subscriptionManager: SubscriptionManager,
    private val difficultyManager: com.ssbmax.core.data.repository.DifficultyProgressionManager,
    securityLogger: SecurityEventLogger,
    workManager: WorkManager
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
                    is com.ssbmax.core.data.repository.TestEligibility.LimitReached -> {
                        _uiState.update { it.copy(isLoading = false, loadingMessage = null,
                            error = null, isLimitReached = true, subscriptionTier = eligibility.tier,
                            testsLimit = eligibility.limit, testsUsed = eligibility.usedCount,
                            resetsAt = eligibility.resetsAt) }
                        return@launch
                    }
                    is com.ssbmax.core.data.repository.TestEligibility.NetworkError -> {
                        _uiState.update { it.copy(isLoading = false, loadingMessage = null,
                            error = "No connection. Please check your network and try again.") }
                        return@launch
                    }
                    is com.ssbmax.core.data.repository.TestEligibility.Eligible -> Unit
                }

                _uiState.update { it.copy(loadingMessage = "Fetching questions from cloud...") }

                val loadResult = loadTATTest(userId, testId)
                if (loadResult.isFailure) {
                    val e = loadResult.exceptionOrNull()!!
                    if (e is com.ssbmax.core.domain.usecase.tat.LoadTATTestUseCase.ProfileIncompleteException) {
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
        _uiState.update { it.copy(
            phase = TATPhase.IMAGE_VIEWING,
            currentQuestionIndex = 0
        ) }
        startViewingTimer()
    }
    
    fun updateStory(story: String) {
        _uiState.update { it.copy(currentStory = story) }
    }
    
    private fun saveCurrentStoryToResponses() {
        val state = _uiState.value
        val currentQuestion = state.currentQuestion
        if (state.currentStory.isNotBlank() && currentQuestion != null) {
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
    }

    fun moveToNextQuestion() {
        saveCurrentStoryToResponses()
        val state = _uiState.value
        if (state.currentQuestionIndex < state.questions.size - 1) {
            _uiState.update { it.copy(
                currentQuestionIndex = state.currentQuestionIndex + 1,
                currentStory = "",
                phase = TATPhase.IMAGE_VIEWING
            ) }
            startViewingTimer()
        } else {
            _uiState.update { it.copy(isTimerActive = false) }
        }
    }
    
    fun editCurrentStory() {
        _uiState.update { it.copy(phase = TATPhase.WRITING) }
        startWritingTimer()
    }
    
    fun confirmCurrentStory() {
        moveToNextQuestion()
    }
    
    fun submitTest() {
        viewModelScope.launch {
            android.util.Log.d("TATTestViewModel", "════════════════════════════════════════")
            android.util.Log.d("TATTestViewModel", "📤 submitTest() called")
            android.util.Log.d("TATTestViewModel", "════════════════════════════════════════")
            
            _uiState.update { it.copy(isLoading = true) }

            var currentUserId: String? = null
            try {
                _uiState.update { it.copy(isTimerActive = false) }

                // Get current user
                android.util.Log.d("TATTestViewModel", "📍 Step 1: Getting current user...")
                currentUserId = withTimeout(3000L) { // 3 second timeout for auth state
                    observeCurrentUser().first()?.id
                } ?: run {
                    ErrorLogger.log(Exception("User not authenticated during TAT submission"), "TAT submission failed: user not authenticated")
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = "Please login to submit test"
                    ) }
                    return@launch
                }
                android.util.Log.d("TATTestViewModel", "✅ User ID: $currentUserId")
                
                // Get user profile for subscription type
                android.util.Log.d("TATTestViewModel", "📍 Step 2: Getting user profile...")
                val userProfileResult = withTimeout(5000L) { // 5 second timeout for user profile fetch
                    userProfileRepository.getUserProfile(currentUserId).first()
                }
                val userProfile = userProfileResult.getOrNull()
                val subscriptionType = userProfile?.subscriptionType ?: SubscriptionType.FREE
                android.util.Log.d("TATTestViewModel", "✅ Subscription type: $subscriptionType")
                
                // Create submission
                android.util.Log.d("TATTestViewModel", "📍 Step 3: Creating submission...")
                val state = _uiState.value
                val submission = TATSubmission(
                    userId = currentUserId,
                    testId = state.testId,
                    stories = state.responses,
                    totalTimeTakenMinutes = ((System.currentTimeMillis() - state.startTime) / 60000).toInt(),
                    submittedAt = System.currentTimeMillis(),
                    analysisStatus = AnalysisStatus.PENDING_ANALYSIS,  // OLQ analysis will be done by worker
                    olqResult = null  // Will be populated by worker
                )
                android.util.Log.d("TATTestViewModel", "✅ Submission created with ${submission.stories.size} stories")
                
                // Submit to Firestore (but also store locally to bypass permission issues)
                android.util.Log.d("TATTestViewModel", "📍 Step 4: Submitting to Firestore...")
                val result = submitTATTest(submission, batchId = null)
                
                result.onSuccess { submissionId ->
                    android.util.Log.d("TATTestViewModel", "✅ Submission successful! ID: $submissionId")

                    // Enqueue per-story analysis workers chained with synthesis worker
                    android.util.Log.d("TATTestViewModel", "📍 Step 4a: Enqueueing progressive synthesis chain...")
                    try {
                        enqueueSynthesisChain(submissionId, submission.stories)
                        android.util.Log.d("TATTestViewModel", "✅ Per-story + synthesis chain enqueued")
                    } catch (e: Exception) {
                        ErrorLogger.log(e, "Failed to enqueue TAT synthesis chain — submission saved, analysis deferred")
                    }

                    // Calculate score for analytics (stories with >150 chars are "valid")
                    val validCount = submission.stories.count { it.charactersCount >= 150 }
                    val totalCount = submission.stories.size
                    val scorePercentage = if (totalCount > 0) (validCount.toFloat() / totalCount) * 100 else 0f
                    
                    // Record performance for analytics (using recommended difficulty)
                    android.util.Log.d("TATTestViewModel", "📍 Step 5: Recording performance analytics...")
                    val difficulty = difficultyManager.getRecommendedDifficulty("TAT")
                    difficultyManager.recordPerformance(
                        testType = "TAT",
                        difficulty = difficulty,
                        score = scorePercentage,
                        correctAnswers = validCount,
                        totalQuestions = totalCount,
                        timeSeconds = (submission.totalTimeTakenMinutes * 60).toFloat()
                    )
                    android.util.Log.d("TATTestViewModel", "✅ Performance recorded ($difficulty): $scorePercentage% (${validCount}/${totalCount})")
                    
                    // Record test usage for subscription tracking (with submissionId for idempotency)
                    android.util.Log.d("TATTestViewModel", "📍 Step 6: Recording test usage for subscription...")
                    android.util.Log.d("TATTestViewModel", "   userId: $currentUserId")
                    android.util.Log.d("TATTestViewModel", "   testType: TAT")
                    android.util.Log.d("TATTestViewModel", "   submissionId: $submissionId")
                    subscriptionManager.recordTestUsage(TestType.TAT, currentUserId, submissionId)
                    android.util.Log.d("TATTestViewModel", "✅ Test usage recorded successfully!")

                    android.util.Log.d("TATTestViewModel", "📍 Step 7: Updating UI state...")
                    _uiState.update { it.copy(
                        isLoading = false,
                        isSubmitted = true,
                        submissionId = submissionId,
                        subscriptionType = subscriptionType,
                        submission = submission,  // Store locally to show results directly
                        phase = TATPhase.SUBMITTED
                    ) }
                    
                    // Emit navigation event (one-time, consumed by screen)
                    android.util.Log.d("TATTestViewModel", "📍 Step 8: Emitting navigation event...")
                    sendNavigationEvent(
                        TestNavigationEvent.NavigateToResult(
                            submissionId = submissionId,
                            subscriptionType = subscriptionType
                        )
                    )
                    
                    android.util.Log.d("TATTestViewModel", "✅ TAT test submission complete!")
                    android.util.Log.d("TATTestViewModel", "════════════════════════════════════════")
                }.onFailure { error ->
                    ErrorLogger.log(error, "Failed to submit TAT test for user: $currentUserId")
                    android.util.Log.d("TATTestViewModel", "════════════════════════════════════════")
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = "Failed to submit: ${error.message}"
                    ) }
                }
            } catch (e: Exception) {
                ErrorLogger.log(e, "Exception during TAT submission for user: ${currentUserId ?: "unknown"}")
                android.util.Log.d("TATTestViewModel", "════════════════════════════════════════")
                _uiState.update { it.copy(
                    isLoading = false,
                    error = e.message
                ) }
            }
        }
    }
    
    private fun startViewingTimer() {
        timerJob?.cancel()
        val myGeneration = ++timerGeneration
        val viewingTime = _uiState.value.config?.viewingTimePerPictureSeconds ?: 30
        _uiState.update { it.copy(
            viewingTimeRemaining = viewingTime,
            isTimerActive = true,
            timerStartTime = myGeneration
        ) }
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
                // Only clear isTimerActive if no newer timer has taken over
                _uiState.update { current ->
                    if (current.timerStartTime == myGeneration) current.copy(isTimerActive = false)
                    else current
                }
            }
        }.also { it.trackMemoryLeaks("TATTestViewModel", "viewing-timer") }
    }

    private fun startWritingTimer() {
        timerJob?.cancel()
        val myGeneration = ++timerGeneration
        val writingTimeSeconds = (_uiState.value.config?.writingTimePerPictureMinutes ?: 4) * 60
        _uiState.update { it.copy(
            writingTimeRemaining = writingTimeSeconds,
            isTimerActive = true,
            timerStartTime = myGeneration
        ) }
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
                _uiState.update { current ->
                    if (current.timerStartTime == myGeneration) current.copy(isTimerActive = false)
                    else current
                }
            }
        }.also { it.trackMemoryLeaks("TATTestViewModel", "writing-timer") }
    }
    
    private fun enqueueSynthesisChain(
        submissionId: String,
        stories: List<com.ssbmax.core.domain.model.TATStoryResponse>
    ) {
        android.util.Log.d("TATTestViewModel", "📋 enqueueSynthesisChain called with ${stories.size} stories")

        if (stories.isEmpty()) {
            android.util.Log.e("TATTestViewModel", "❌ No stories to analyze — skipping synthesis chain")
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val storyRequests = stories.mapIndexed { index, story ->
            android.util.Log.d("TATTestViewModel", "   Building work request for story $index: ${story.questionId}")
            OneTimeWorkRequestBuilder<TATStoryAnalysisWorker>()
                .setInputData(
                    workDataOf(
                        TATStoryAnalysisWorker.KEY_SUBMISSION_ID to submissionId,
                        TATStoryAnalysisWorker.KEY_QUESTION_ID to story.questionId,
                        TATStoryAnalysisWorker.KEY_STORY_INDEX to index
                    )
                )
                .setConstraints(constraints)
                .build()
        }

        android.util.Log.d("TATTestViewModel", "📦 Built ${storyRequests.size} story work requests")

        val synthesisRequest = OneTimeWorkRequestBuilder<TATSynthesisWorker>()
            .setInputData(workDataOf(TATSynthesisWorker.KEY_SUBMISSION_ID to submissionId))
            .setConstraints(constraints)
            .build()

        android.util.Log.d("TATTestViewModel", "🔗 Combining ${storyRequests.size} story workers + synthesis worker")

        val continuations = storyRequests.map { workManager.beginWith(it) }
        android.util.Log.d("TATTestViewModel", "🔗 Created ${continuations.size} continuations")

        WorkContinuation.combine(continuations)
            .then(synthesisRequest)
            .enqueue()

        android.util.Log.d("TATTestViewModel", "✅ Synthesis chain enqueued successfully")
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        MemoryLeakTracker.unregisterViewModel("TATTestViewModel")

        MemoryLeakTracker.forceGcAndLog("TATTestViewModel-Cleared")
    }
}
