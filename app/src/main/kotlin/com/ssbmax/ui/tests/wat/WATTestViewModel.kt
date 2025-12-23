package com.ssbmax.ui.tests.wat

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
import com.ssbmax.core.domain.model.scoring.AnalysisStatus
import com.ssbmax.core.domain.repository.TestContentRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.core.domain.usecase.submission.SubmitWATTestUseCase
import com.ssbmax.utils.ErrorLogger
import com.ssbmax.workers.WATAnalysisWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.ssbmax.ui.tests.common.TestNavigationEvent

/**
 * ViewModel for WAT Test Screen
 * Loads test questions from cloud via TestContentRepository
 *
 * MEMORY LEAK PREVENTION:
 * - Registers with MemoryLeakTracker for profiler verification
 * - Properly cancels timerJob in onCleared()
 * - Uses viewModelScope for all coroutines (auto-cancelled)
 * - No static references or context leaks
 */
@HiltViewModel
class WATTestViewModel @Inject constructor(
    private val testContentRepository: TestContentRepository,
    private val submitWATTest: SubmitWATTestUseCase,
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val userProfileRepository: com.ssbmax.core.domain.repository.UserProfileRepository,
    private val difficultyManager: com.ssbmax.core.data.repository.DifficultyProgressionManager,
    private val subscriptionManager: com.ssbmax.core.data.repository.SubscriptionManager,
    private val getOLQDashboard: com.ssbmax.core.domain.usecase.dashboard.GetOLQDashboardUseCase,
    private val securityLogger: com.ssbmax.core.data.security.SecurityEventLogger,
    private val workManager: WorkManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(WATTestUiState())
    val uiState: StateFlow<WATTestUiState> = _uiState.asStateFlow()
    
    // Navigation events (one-time events, consumed on collection)
    private val _navigationEvents = Channel<TestNavigationEvent>(Channel.BUFFERED)
    val navigationEvents = _navigationEvents.receiveAsFlow()
    
    // Timer Job reference for explicit cancellation (prevents "rushing" bug)
    private var timerJob: Job? = null

    // PHASE 3: Job removed - timer managed by viewModelScope lifecycle

    init {
        // Register for memory leak tracking
        trackMemoryLeaks("WATTestViewModel")
        android.util.Log.d("WATTestViewModel", "🚀 ViewModel initialized with leak tracking")
        
        // Restore timer if test was in progress (configuration change recovery)
        restoreTimerIfNeeded()
    }
    
    /**
     * Restore timer after configuration change (e.g., screen rotation)
     * If test was in IN_PROGRESS phase, restart the timer
     */
    private fun restoreTimerIfNeeded() {
        viewModelScope.launch {
            val state = _uiState.value
            
            // PHASE 2: Check isTimerActive instead of timerJob
            // Only restore if we're in active phase with time remaining
            if (!state.isLoading && 
                state.phase == WATPhase.IN_PROGRESS && 
                state.timeRemaining > 0 && 
                !state.isTimerActive) {
                android.util.Log.d("WATTestViewModel", "🔄 Restoring word timer after configuration change")
                startWordTimer()
            }
        }
    }
    
    /**
     * Check if user is eligible to take the test based on subscription tier
     */
    private suspend fun checkTestEligibility(userId: String): com.ssbmax.core.data.repository.TestEligibility {
        return subscriptionManager.canTakeTest(TestType.WAT, userId)
    }
    
    fun loadTest(testId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(
                isLoading = true,
                loadingMessage = "Checking eligibility..."
            ) }
            
            try {
                // Get current user - SECURITY: Require authentication
                val user = observeCurrentUser().first()
                val userId = user?.id ?: run {
                    ErrorLogger.logTestError(
                        throwable = IllegalStateException("Unauthenticated WAT test access"),
                        description = "WAT test access without authentication",
                        testType = "WAT"
                    )

                    // SECURITY: Log unauthenticated access attempt to Firebase Analytics
                    securityLogger.logUnauthenticatedAccess(
                        testType = TestType.WAT,
                        context = "WATTestViewModel.loadTest"
                    )
                    
                    _uiState.update { it.copy(
                        isLoading = false,
                        loadingMessage = null,
                        error = "Authentication required. Please login to continue."
                    ) }
                    return@launch
                }
                
                android.util.Log.d("WATTestViewModel", "✅ User authenticated: $userId")
                
                // Check subscription eligibility BEFORE loading test
                val eligibility = checkTestEligibility(userId)
                
                when (eligibility) {
                    is com.ssbmax.core.data.repository.TestEligibility.LimitReached -> {
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
                        android.util.Log.d("WATTestViewModel", "❌ Test limit reached: ${eligibility.usedCount}/${eligibility.limit}")
                        return@launch
                    }
                    is com.ssbmax.core.data.repository.TestEligibility.Eligible -> {
                        android.util.Log.d("WATTestViewModel", "✅ Test eligible: ${eligibility.remainingTests} remaining")
                        // Continue with test loading
                    }
                }
                
                _uiState.update { it.copy(
                    loadingMessage = "Fetching questions from cloud..."
                ) }
                
                // Create test session
                val sessionResult = testContentRepository.createTestSession(
                    userId = userId,
                    testId = testId,
                    testType = TestType.WAT
                )
                
                if (sessionResult.isFailure) {
                    throw sessionResult.exceptionOrNull() ?: Exception("Failed to create test session")
                }
                
                // Fetch words from cloud
                val wordsResult = testContentRepository.getWATQuestions(testId)
                
                if (wordsResult.isFailure) {
                    throw wordsResult.exceptionOrNull() ?: Exception("Failed to load test words")
                }
                
                val words = wordsResult.getOrNull() ?: emptyList()
                
                if (words.isEmpty()) {
                    throw Exception("No words found for this test")
                }
                
                val config = WATTestConfig()
                
                _uiState.update { it.copy(
                    isLoading = false,
                    loadingMessage = null,
                    testId = testId,
                    words = words,
                    config = config,
                    phase = WATPhase.INSTRUCTIONS
                ) }
            } catch (e: Exception) {
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
            phase = WATPhase.IN_PROGRESS,
            currentWordIndex = 0,
            startTime = System.currentTimeMillis()
        ) }
        startWordTimer()
    }
    
    fun updateResponse(response: String) {
        // Limit to max length
        val maxLength = _uiState.value.config?.maxResponseLength ?: 50
        if (response.length <= maxLength) {
            _uiState.update { it.copy(currentResponse = response) }
        }
    }
    
    fun submitResponse() {
        val state = _uiState.value
        val currentWord = state.currentWord ?: return
        
        // Save response
        val response = WATWordResponse(
            wordId = currentWord.id,
            word = currentWord.word,
            response = state.currentResponse,
            timeTakenSeconds = (state.config?.timePerWordSeconds ?: 15) - state.timeRemaining,
            submittedAt = System.currentTimeMillis(),
            isSkipped = false
        )
        
        val updatedResponses = state.responses + response
        _uiState.update { it.copy(responses = updatedResponses) }
        
        // Move to next word or finish
        moveToNextWord()
    }
    
    fun skipWord() {
        val state = _uiState.value
        val currentWord = state.currentWord ?: return
        
        // Save skipped response
        val response = WATWordResponse(
            wordId = currentWord.id,
            word = currentWord.word,
            response = "",
            timeTakenSeconds = 0,
            submittedAt = System.currentTimeMillis(),
            isSkipped = true
        )
        
        val updatedResponses = state.responses + response
        _uiState.update { it.copy(responses = updatedResponses) }
        
        // Move to next word or finish
        moveToNextWord()
    }
    
    private fun moveToNextWord() {
        val state = _uiState.value
        
        // Clear current response
        _uiState.update { it.copy(currentResponse = "") }
        
        // Check if more words
        if (state.currentWordIndex < state.words.size - 1) {
            _uiState.update { it.copy(
                currentWordIndex = state.currentWordIndex + 1
            ) }
            startWordTimer()
        } else {
            // Test complete
            completeTest()
        }
    }
    
    private fun completeTest() {
        // PHASE 2: stopTimer() removed - viewModelScope auto-cancels
        _uiState.update { it.copy(
            isTimerActive = false,
            phase = WATPhase.COMPLETED
        ) }
        submitTest()
    }
    
    private fun submitTest() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // Get current user
                val currentUserId: String = observeCurrentUser().first()?.id ?: run {
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = "Please login to submit test"
                    ) }
                    return@launch
                }
                
                // Get user profile for subscription type
                val userProfileResult = userProfileRepository.getUserProfile(currentUserId).first()
                val userProfile = userProfileResult.getOrNull()
                val subscriptionType = userProfile?.subscriptionType ?: com.ssbmax.core.domain.model.SubscriptionType.FREE
                
                val state = _uiState.value
                
                // Create submission
                val totalTimeMinutes = ((System.currentTimeMillis() - state.startTime) / 60000).toInt()
                val submission = WATSubmission(
                    userId = currentUserId,
                    testId = state.testId,
                    responses = state.responses,
                    totalTimeTakenMinutes = totalTimeMinutes,
                    submittedAt = System.currentTimeMillis(),

                    analysisStatus = AnalysisStatus.PENDING_ANALYSIS,
                    olqResult = null
                )
                
                // Calculate score for analytics (valid responses / total)
                val validCount = submission.validResponses
                val totalCount = submission.totalResponses
                val scorePercentage = if (totalCount > 0) (validCount.toFloat() / totalCount) * 100 else 0f
                
                // Record performance for analytics (using recommended difficulty)
                val difficulty = difficultyManager.getRecommendedDifficulty("WAT")
                difficultyManager.recordPerformance(
                    testType = "WAT",
                    difficulty = difficulty,
                    score = scorePercentage,
                    correctAnswers = validCount,
                    totalQuestions = totalCount,
                    timeSeconds = (totalTimeMinutes * 60).toFloat()
                )
                android.util.Log.d("WATTestViewModel", "📊 Recorded performance ($difficulty): $scorePercentage% (${validCount}/${totalCount})")
                
                // Submit to Firestore (but also store locally to bypass permission issues)
                val result = submitWATTest(submission, batchId = null)

                result.onSuccess { submissionId ->
                    android.util.Log.d("WATTestViewModel", "✅ Submission successful! ID: $submissionId")

                    // Record test usage for subscription tracking (with submissionId for idempotency)
                    android.util.Log.d("WATTestViewModel", "📍 Recording test usage for subscription...")
                    subscriptionManager.recordTestUsage(TestType.WAT, currentUserId, submissionId)
                    android.util.Log.d("WATTestViewModel", "✅ Test usage recorded successfully!")

                    // Invalidate OLQ dashboard cache (user just completed a test)
                    android.util.Log.d("WATTestViewModel", "📍 Invalidating OLQ dashboard cache...")
                    getOLQDashboard.invalidateCache(currentUserId)
                    android.util.Log.d("WATTestViewModel", "✅ Dashboard cache invalidated!")

                    // Enqueue WATAnalysisWorker for OLQ analysis
                    android.util.Log.d("WATTestViewModel", "📍 Enqueueing WATAnalysisWorker...")
                    enqueueWATAnalysisWorker(submissionId)
                    android.util.Log.d("WATTestViewModel", "✅ WATAnalysisWorker enqueued successfully")

                    _uiState.update { it.copy(
                        isLoading = false,
                        isSubmitted = true,
                        submissionId = submissionId,
                        subscriptionType = subscriptionType,
                        submission = submission,  // Store locally to show results directly
                        phase = WATPhase.SUBMITTED
                    ) }

                    // Emit navigation event (one-time, consumed by screen)
                    _navigationEvents.trySend(
                        TestNavigationEvent.NavigateToResult(
                            submissionId = submissionId,
                            subscriptionType = subscriptionType
                        )
                    )
                }.onFailure { error ->
                    ErrorLogger.log(error, "Failed to submit WAT test for user: $currentUserId")
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = "Failed to submit: ${error.message}"
                    ) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = e.message
                ) }
            }
        }
    }
    
    private fun startWordTimer() {
        // PHASE 2 FIX: Cancel previous timer to prevent concurrency bug
        timerJob?.cancel()
        
        val timePerWord = _uiState.value.config?.timePerWordSeconds ?: 15
        
        _uiState.update { it.copy(
            timeRemaining = timePerWord,
            isTimerActive = true,
            timerStartTime = System.currentTimeMillis()
        ) }

        // PHASE 2 FIX: Store Job reference and use delta-based calculation
        val startTime = System.currentTimeMillis()
        val endTime = startTime + (timePerWord * 1000)
        
        timerJob = viewModelScope.launch {
            android.util.Log.d("WATTestViewModel", "⏰ Starting word timer (${timePerWord}s)")

            try {
                while (isActive) {
                    // Delta-based calculation: self-correcting for drift
                    val remainingMillis = endTime - System.currentTimeMillis()
                    val remainingSeconds = (remainingMillis / 1000).toInt()
                    
                    if (remainingSeconds <= 0) break
                    
                    _uiState.update { it.copy(
                        timeRemaining = remainingSeconds
                    ) }
                    
                    // Update every 200ms for smooth UI (more frequent than 1s)
                    delay(200)
                }

                // Time's up - auto-skip (only if not cancelled)
                if (isActive) {
                    android.util.Log.d("WATTestViewModel", "⏰ Word timer completed, auto-skipping")
                    skipWord()
                }
            } catch (e: CancellationException) {
                android.util.Log.d("WATTestViewModel", "⏰ Word timer cancelled")
                throw e // Re-throw to properly cancel coroutine
            } finally {
                _uiState.update { it.copy(isTimerActive = false) }
            }
        }.also {
            it.trackMemoryLeaks("WATTestViewModel", "word-timer")
        }
    }
    
    // PHASE 3: stopTimer() removed - viewModelScope automatically cancels all jobs
    
    

    
    /**
     * Enqueue WATAnalysisWorker for background OLQ analysis
     */
    private fun enqueueWATAnalysisWorker(submissionId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<WATAnalysisWorker>()
            .setInputData(workDataOf(WATAnalysisWorker.KEY_SUBMISSION_ID to submissionId))
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            "wat_analysis_$submissionId",
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }

    override fun onCleared() {
        super.onCleared()

        // PHASE 2 FIX: Explicitly cancel timer
        timerJob?.cancel()
        
        android.util.Log.d("WATTestViewModel", "🧹 ViewModel onCleared() - timer cancelled")

        // Cancel navigation events channel
        _navigationEvents.close()

        // Unregister from memory leak tracker
        MemoryLeakTracker.unregisterViewModel("WATTestViewModel")

        // Force GC to help profiler detect cleanup
        MemoryLeakTracker.forceGcAndLog("WATTestViewModel-Cleared")

        android.util.Log.d("WATTestViewModel", "✅ WATTestViewModel cleanup complete")
    }
}

/**
 * UI State for WAT Test
 */
data class WATTestUiState(
    val isLoading: Boolean = true,
    val loadingMessage: String? = null,
    val testId: String = "",
    val words: List<WATWord> = emptyList(),
    val config: WATTestConfig? = null,
    val currentWordIndex: Int = 0,
    val responses: List<WATWordResponse> = emptyList(),
    val currentResponse: String = "",
    val phase: WATPhase = WATPhase.INSTRUCTIONS,
    val timeRemaining: Int = 15,
    val startTime: Long = System.currentTimeMillis(),
    val isSubmitted: Boolean = false,
    val submissionId: String? = null,
    val subscriptionType: com.ssbmax.core.domain.model.SubscriptionType? = null,
    val submission: WATSubmission? = null,  // Submission stored locally to bypass Firestore permission issues
    val error: String? = null,
    // Subscription limit fields
    val isLimitReached: Boolean = false,
    val subscriptionTier: com.ssbmax.core.domain.model.SubscriptionTier = com.ssbmax.core.domain.model.SubscriptionTier.FREE,
    val testsLimit: Int = 1,
    val testsUsed: Int = 0,
    val resetsAt: String = "",
    // PHASE 1: New StateFlow fields (replacing nullable vars)
    val isTimerActive: Boolean = false,
    val timerStartTime: Long = 0L
) {
    val currentWord: WATWord?
        get() = words.getOrNull(currentWordIndex)
    
    val completedWords: Int
        get() = responses.size
    
    val progress: Float
        get() = if (words.isEmpty()) 0f else (completedWords.toFloat() / words.size)
}

