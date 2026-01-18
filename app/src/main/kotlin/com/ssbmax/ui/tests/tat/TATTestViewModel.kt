package com.ssbmax.ui.tests.tat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ssbmax.core.data.util.MemoryLeakTracker
import com.ssbmax.core.data.util.trackMemoryLeaks
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.model.SubscriptionType
import com.ssbmax.core.domain.model.scoring.AnalysisStatus
import com.ssbmax.core.domain.repository.TestContentRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.core.domain.usecase.submission.SubmitTATTestUseCase

import com.ssbmax.ui.tests.common.TestNavigationEvent
import com.ssbmax.utils.ErrorLogger
import com.ssbmax.workers.TATAnalysisWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for TAT Test Screen
 * Loads test questions from cloud via TestContentRepository
 *
 * MEMORY LEAK PREVENTION:
 * - Registers with MemoryLeakTracker for profiler verification
 * - Properly cancels timerJob in onCleared()
 * - Uses viewModelScope for all coroutines (auto-cancelled)
 * - No static references or context leaks
 */
@HiltViewModel
class TATTestViewModel @Inject constructor(
    private val testContentRepository: TestContentRepository,
    private val submitTATTest: SubmitTATTestUseCase,
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val userProfileRepository: com.ssbmax.core.domain.repository.UserProfileRepository,
    private val subscriptionManager: com.ssbmax.core.data.repository.SubscriptionManager,
    private val difficultyManager: com.ssbmax.core.data.repository.DifficultyProgressionManager,
    private val getOLQDashboard: com.ssbmax.core.domain.usecase.dashboard.GetOLQDashboardUseCase,
    private val securityLogger: com.ssbmax.core.data.security.SecurityEventLogger,
    private val workManager: WorkManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(TATTestUiState())
    val uiState: StateFlow<TATTestUiState> = _uiState.asStateFlow()
    
    
    // Navigation events (one-time events, consumed on collection)
    private val _navigationEvents = Channel<TestNavigationEvent>(Channel.BUFFERED)
    val navigationEvents = _navigationEvents.receiveAsFlow()

    // Timer Job references for explicit cancellation (prevents "rushing" bug)
    private var viewingTimerJob: Job? = null
    private var writingTimerJob: Job? = null

    init {
        // Register for memory leak tracking
        trackMemoryLeaks("TATTestViewModel")
        android.util.Log.d("TATTestViewModel", "🚀 ViewModel initialized with leak tracking")
        
        // Restore timer if test was in progress (configuration change recovery)
        restoreTimerIfNeeded()
    }
    
    // Removed: Business logic moved to CheckTestEligibilityUseCase
    
    /**
     * Restore timer after configuration change (e.g., screen rotation)
     * If test was in IMAGE_VIEWING or WRITING phase, restart the appropriate timer
     */
    private fun restoreTimerIfNeeded() {
        viewModelScope.launch {
            // Wait for initial state to be set
            val state = _uiState.value
            
            // PHASE 2: Check isTimerActive instead of timerJob
            // Only restore if we're in an active phase (not loading or instructions)
            if (!state.isLoading && state.phase != TATPhase.INSTRUCTIONS && state.phase != TATPhase.SUBMITTED) {
                android.util.Log.d("TATTestViewModel", "🔄 Restoring timer for phase: ${state.phase}")
                
                when (state.phase) {
                    TATPhase.IMAGE_VIEWING -> {
                        if (state.viewingTimeRemaining > 0 && !state.isTimerActive) {
                            startViewingTimer()
                        }
                    }
                    TATPhase.WRITING -> {
                        if (state.writingTimeRemaining > 0 && !state.isTimerActive) {
                            startWritingTimer()
                        }
                    }
                    TATPhase.REVIEW_CURRENT -> {
                        // No timer needed in review phase
                    }
                    else -> {
                        // Other phases don't need timers
                    }
                }
            }
        }
    }
    
    fun loadTest(testId: String) {
        viewModelScope.launch {
            android.util.Log.d("TATTestViewModel", "════════════════════════════════════════")
            android.util.Log.d("TATTestViewModel", "🎬 loadTest() called for testId: $testId")
            android.util.Log.d("TATTestViewModel", "════════════════════════════════════════")
            
            _uiState.update { it.copy(
                isLoading = true,
                loadingMessage = "Checking eligibility..."
            ) }
            
            try {
                // Get current user - SECURITY: Require authentication
                android.util.Log.d("TATTestViewModel", "📍 Step 1: Fetching current user...")
                val user = withTimeout(3000L) { // 3 second timeout for auth state
                    observeCurrentUser().first()
                }
                val userId = user?.id ?: run {
                    ErrorLogger.log(Exception("Unauthenticated TAT test access attempt"), "SECURITY: Unauthenticated test access attempt blocked")

                    // SECURITY: Log unauthenticated access attempt to Firebase Analytics
                    securityLogger.logUnauthenticatedAccess(
                        testType = TestType.TAT,
                        context = "TATTestViewModel.loadTest"
                    )
                    
                    _uiState.update { it.copy(
                        isLoading = false,
                        loadingMessage = null,
                        error = "Authentication required. Please login to continue."
                    ) }
                    return@launch
                }
                
                android.util.Log.d("TATTestViewModel", "✅ User authenticated: $userId")
                android.util.Log.d("TATTestViewModel", "   User email: ${user.email}")
                android.util.Log.d("TATTestViewModel", "   User role: ${user.role}")
                
                // Check subscription eligibility BEFORE loading test
                android.util.Log.d("TATTestViewModel", "📍 Step 2: Checking subscription eligibility...")
                val eligibility = subscriptionManager.canTakeTest(TestType.TAT, userId)
                android.util.Log.d("TATTestViewModel", "   Eligibility result: ${eligibility.javaClass.simpleName}")
                
                when (eligibility) {
                    is com.ssbmax.core.data.repository.TestEligibility.LimitReached -> {
                        android.util.Log.w("TATTestViewModel", "❌ TEST LIMIT REACHED!")
                        android.util.Log.w("TATTestViewModel", "   Tier: ${eligibility.tier}")
                        android.util.Log.w("TATTestViewModel", "   Limit: ${eligibility.limit}")
                        android.util.Log.w("TATTestViewModel", "   Used: ${eligibility.usedCount}")
                        android.util.Log.w("TATTestViewModel", "   Resets at: ${eligibility.resetsAt}")
                        
                        // Show limit reached state
                        _uiState.update { it.copy(
                            isLoading = false,
                            loadingMessage = null,
                            error = null,
                            isLimitReached = true,
                            subscriptionTier = eligibility.tier,
                            testsLimit = eligibility.limit,
                            testsUsed = eligibility.usedCount,
                            resetsAt = eligibility.resetsAt
                        ) }
                        
                        android.util.Log.d("TATTestViewModel", "🛑 Stopping test load - showing subscription screen")
                        android.util.Log.d("TATTestViewModel", "════════════════════════════════════════")
                        return@launch
                    }
                    is com.ssbmax.core.data.repository.TestEligibility.Eligible -> {
                        android.util.Log.d("TATTestViewModel", "✅ Test eligible!")
                        android.util.Log.d("TATTestViewModel", "   Remaining tests: ${eligibility.remainingTests}")
                        // Continue with test loading
                    }
                }
                
                _uiState.update { it.copy(
                    loadingMessage = "Fetching questions from cloud..."
                ) }
                
                // Create test session
                android.util.Log.d("TATTestViewModel", "📍 Step 3: Creating test session...")
                val sessionResult = testContentRepository.createTestSession(
                    userId = userId,
                    testId = testId,
                    testType = TestType.TAT
                )
                
                if (sessionResult.isFailure) {
                    val exception = sessionResult.exceptionOrNull() ?: Exception("Failed to create test session")
                    ErrorLogger.log(exception, "Failed to create TAT test session for user: $userId")
                    throw exception
                }
                android.util.Log.d("TATTestViewModel", "✅ Test session created")
                
                // Fetch questions from cloud
                android.util.Log.d("TATTestViewModel", "📍 Step 4: Fetching TAT questions from cloud...")
                val questionsResult = testContentRepository.getTATQuestions(testId)
                
                if (questionsResult.isFailure) {
                    val exception = questionsResult.exceptionOrNull() ?: Exception("Failed to load test questions")
                    ErrorLogger.log(exception, "Failed to load TAT questions for test: $testId")
                    throw exception
                }

                val questions = questionsResult.getOrNull() ?: emptyList()
                android.util.Log.d("TATTestViewModel", "✅ Loaded ${questions.size} TAT questions")

                if (questions.isEmpty()) {
                    val exception = Exception("No TAT questions found for test: $testId")
                    ErrorLogger.log(exception, "No TAT questions found for test")
                    throw exception
                }
                
                val config = TATTestConfig()
                
                _uiState.update { it.copy(
                    isLoading = false,
                    loadingMessage = null,
                    testId = testId,
                    questions = questions,
                    config = config,
                    phase = TATPhase.INSTRUCTIONS
                ) }
                
                android.util.Log.d("TATTestViewModel", "✅ Test loaded successfully - showing instructions")
                android.util.Log.d("TATTestViewModel", "════════════════════════════════════════")
            } catch (e: Exception) {
                ErrorLogger.log(e, "Exception loading TAT test: $testId")
                android.util.Log.d("TATTestViewModel", "════════════════════════════════════════")
                _uiState.update { it.copy(
                    isLoading = false,
                    loadingMessage = null,
                    error = "Cloud connection required. Please check your internet connection."
                ) }
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
    
    fun moveToNextQuestion() {
        val state = _uiState.value
        
        // Save current story
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
        
        // Move to next or finish
        if (state.currentQuestionIndex < state.questions.size - 1) {
            _uiState.update { it.copy(
                currentQuestionIndex = state.currentQuestionIndex + 1,
                currentStory = "",
                phase = TATPhase.IMAGE_VIEWING
            ) }
            startViewingTimer()
        } else {
            // All pictures shown, allow review or submit
            // PHASE 2: No manual stopTimer() needed - viewModelScope handles it
            _uiState.update { it.copy(isTimerActive = false) }
        }
    }
    
    fun moveToPreviousQuestion() {
        val state = _uiState.value
        
        if (state.currentQuestionIndex > 0) {
            // PHASE 2: No manual stopTimer() - timer replaced by new startWritingTimer()
            
            // Load previous story if exists
            val previousQuestionId = state.questions[state.currentQuestionIndex - 1].id
            val previousStory = state.responses.find { it.questionId == previousQuestionId }?.story ?: ""
            
            _uiState.update { it.copy(
                currentQuestionIndex = state.currentQuestionIndex - 1,
                currentStory = previousStory,
                phase = TATPhase.WRITING
            ) }
            startWritingTimer()
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
                // PHASE 2: No manual stopTimer() - viewModelScope cancels on completion
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

                    // Enqueue TATAnalysisWorker for OLQ analysis
                    android.util.Log.d("TATTestViewModel", "📍 Step 4a: Enqueueing TATAnalysisWorker...")
                    enqueueTATAnalysisWorker(submissionId)
                    android.util.Log.d("TATTestViewModel", "✅ TATAnalysisWorker enqueued successfully")

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

                    // NOTE: Cache invalidation moved to TATAnalysisWorker.
                    // Invalidating here is premature because analysis takes ~15-30s.
                    // The next dashboard fetch would cache empty TAT result.
                    // See: TATAnalysisWorker.doWork() for correct cache invalidation timing.

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
                    _navigationEvents.trySend(
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
        // PHASE 3 FIX: Cancel previous viewing timer
        viewingTimerJob?.cancel()
        
        val viewingTime = _uiState.value.config?.viewingTimePerPictureSeconds ?: 30
        
        _uiState.update { it.copy(
            viewingTimeRemaining = viewingTime,
            isTimerActive = true,
            timerStartTime = System.currentTimeMillis()
        ) }

        // PHASE 3 FIX: Delta-based calculation
        val startTime = System.currentTimeMillis()
        val endTime = startTime + (viewingTime * 1000)
        
        viewingTimerJob = viewModelScope.launch {
            android.util.Log.d("TATTestViewModel", "⏰ Starting viewing timer (${viewingTime}s)")

            try {
                while (isActive) {
                    val remainingMillis = endTime - System.currentTimeMillis()
                    val remainingSeconds = (remainingMillis / 1000).toInt()
                    
                    if (remainingSeconds <= 0) break
                    
                    _uiState.update { it.copy(
                        viewingTimeRemaining = remainingSeconds
                    ) }
                    
                    delay(200) // Update every 200ms for smooth UI
                }

                // Auto-transition to writing (only if not cancelled)
                if (isActive) {
                    android.util.Log.d("TATTestViewModel", "⏰ Viewing timer completed, transitioning to writing")
                    _uiState.update { it.copy(phase = TATPhase.WRITING, isTimerActive = false) }
                    startWritingTimer()
                }
            } catch (e: CancellationException) {
                android.util.Log.d("TATTestViewModel", "⏰ Viewing timer cancelled")
                throw e // Re-throw to properly cancel coroutine
            } finally {
                _uiState.update { it.copy(isTimerActive = false) }
            }
        }.also {
            it.trackMemoryLeaks("TATTestViewModel", "viewing-timer")
        }
    }
    
    private fun startWritingTimer() {
        // PHASE 3 FIX: Cancel previous writing timer
        writingTimerJob?.cancel()
        
        val writingTimeMinutes = _uiState.value.config?.writingTimePerPictureMinutes ?: 4
        val writingTimeSeconds = writingTimeMinutes * 60
        
        _uiState.update { it.copy(
            writingTimeRemaining = writingTimeSeconds,
            isTimerActive = true,
            timerStartTime = System.currentTimeMillis()
        ) }

        // PHASE 3 FIX: Delta-based calculation
        val startTime = System.currentTimeMillis()
        val endTime = startTime + (writingTimeSeconds * 1000)
        
        writingTimerJob = viewModelScope.launch {
            android.util.Log.d("TATTestViewModel", "⏰ Starting writing timer (${writingTimeMinutes}min)")

            try {
                while (isActive) {
                    val remainingMillis = endTime - System.currentTimeMillis()
                    val remainingSeconds = (remainingMillis / 1000).toInt()
                    
                    if (remainingSeconds <= 0) break
                    
                    _uiState.update { it.copy(
                        writingTimeRemaining = remainingSeconds
                    ) }
                    
                    delay(200) // Update every 200ms for smooth UI
                }

                // Time's up - move to review (only if not cancelled)
                if (isActive) {
                    android.util.Log.d("TATTestViewModel", "⏰ Writing timer completed, moving to review")
                    _uiState.update { it.copy(phase = TATPhase.REVIEW_CURRENT) }
                }
            } catch (e: CancellationException) {
                android.util.Log.d("TATTestViewModel", "⏰ Writing timer cancelled")
                throw e // Re-throw to properly cancel coroutine
            } finally {
                _uiState.update { it.copy(isTimerActive = false) }
            }
        }.also {
            it.trackMemoryLeaks("TATTestViewModel", "writing-timer")
        }
    }
    
    // PHASE 3: stopTimer() removed - viewModelScope automatically cancels all jobs
    
    // Removed: Mock question generation (tests now loaded from cloud)
    // Removed: AI score generation moved to GenerateTATAIScoreUseCase
    
    /**
     * Enqueue TATAnalysisWorker for background OLQ analysis
     */
    private fun enqueueTATAnalysisWorker(submissionId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<TATAnalysisWorker>()
            .setInputData(workDataOf(TATAnalysisWorker.KEY_SUBMISSION_ID to submissionId))
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            "tat_analysis_$submissionId",
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }

    override fun onCleared() {
        super.onCleared()

        // PHASE 3 FIX: Explicitly cancel both timers
        viewingTimerJob?.cancel()
        writingTimerJob?.cancel()
        
        android.util.Log.d("TATTestViewModel", "🧹 ViewModel onCleared() - timers cancelled")

        // Cancel navigation events channel
        _navigationEvents.close()

        // Unregister from memory leak tracker
        MemoryLeakTracker.unregisterViewModel("TATTestViewModel")

        // Force GC to help profiler detect cleanup
        MemoryLeakTracker.forceGcAndLog("TATTestViewModel-Cleared")

        android.util.Log.d("TATTestViewModel", "✅ TATTestViewModel cleanup complete")
    }
}

/**
 * UI State for TAT Test
 */
data class TATTestUiState(
    val isLoading: Boolean = true,
    val loadingMessage: String? = null,
    val testId: String = "",
    val questions: List<TATQuestion> = emptyList(),
    val config: TATTestConfig? = null,
    val currentQuestionIndex: Int = 0,
    val responses: List<TATStoryResponse> = emptyList(),
    val currentStory: String = "",
    val phase: TATPhase = TATPhase.INSTRUCTIONS,
    val viewingTimeRemaining: Int = 30,
    val writingTimeRemaining: Int = 240, // 4 minutes
    val startTime: Long = System.currentTimeMillis(),
    val isSubmitted: Boolean = false,
    val submissionId: String? = null,
    val subscriptionType: SubscriptionType? = null,
    val submission: TATSubmission? = null,  // Submission stored locally to bypass Firestore permission issues
    val error: String? = null,
    // Subscription limit fields
    val isLimitReached: Boolean = false,
    val subscriptionTier: SubscriptionTier = SubscriptionTier.FREE,
    val testsLimit: Int = 1,
    val testsUsed: Int = 0,
    val resetsAt: String = "",
    // PHASE 1: New StateFlow fields (replacing nullable vars)
    val isTimerActive: Boolean = false,  // Track if timer is running
    val timerStartTime: Long = 0L        // When timer was started
) {
    val currentQuestion: TATQuestion?
        get() = questions.getOrNull(currentQuestionIndex)
    
    val completedStories: Int
        get() = responses.size
    
    val progress: Float
        get() = if (questions.isEmpty()) 0f else (completedStories.toFloat() / questions.size)
    
    val canMoveToNextQuestion: Boolean
        get() = when (phase) {
            TATPhase.WRITING -> currentStory.length >= (currentQuestion?.minCharacters ?: 50) &&
                                currentStory.length <= (currentQuestion?.maxCharacters ?: 1500)
            TATPhase.REVIEW_CURRENT -> true
            else -> false
        }
    
    val canMoveToPreviousQuestion: Boolean
        get() = currentQuestionIndex > 0 && phase == TATPhase.WRITING

    val canSubmitTest: Boolean
        get() = completedStories >= questions.size // Only allow submission after All stories completed
}

