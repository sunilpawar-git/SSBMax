package com.ssbmax.ui.tests.ppdt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.data.util.MemoryLeakTracker
import com.ssbmax.core.data.util.trackMemoryLeaks
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.TestContentRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.utils.ErrorLogger
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
import java.util.UUID
import javax.inject.Inject
import com.ssbmax.ui.tests.common.TestNavigationEvent

/**
 * ViewModel for PPDT Test Screen
 * Loads test questions from cloud via TestContentRepository
 * 
 * MEMORY LEAK PREVENTION:
 * - Registers with MemoryLeakTracker for profiler verification
 * - viewModelScope automatically cancels all jobs in onCleared()
 * - Uses isTimerActive flag for timer lifecycle management
 * - Uses viewModelScope with isActive checks for cooperative cancellation
 * - No static references or context leaks
 */
@HiltViewModel
class PPDTTestViewModel @Inject constructor(
    private val testContentRepository: TestContentRepository,
    private val submissionRepository: com.ssbmax.core.domain.repository.SubmissionRepository,
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val userProfileRepository: com.ssbmax.core.domain.repository.UserProfileRepository,
    private val difficultyManager: com.ssbmax.core.data.repository.DifficultyProgressionManager,
    private val subscriptionManager: com.ssbmax.core.data.repository.SubscriptionManager,
    private val securityLogger: com.ssbmax.core.data.security.SecurityEventLogger,
    private val workManager: androidx.work.WorkManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PPDTTestUiState())
    val uiState: StateFlow<PPDTTestUiState> = _uiState.asStateFlow()
    
    // Navigation events (one-time events, consumed on collection)
    private val _navigationEvents = Channel<TestNavigationEvent>(Channel.BUFFERED)
    val navigationEvents = _navigationEvents.receiveAsFlow()
    
    // PHASE 3: All state fully migrated to StateFlow (completed)
    // Timer managed via viewModelScope + isTimerActive flag (no Job reference needed)
    
    init {
        // Register for memory leak tracking
        trackMemoryLeaks("PPDTTestViewModel")
        android.util.Log.d("PPDTTestViewModel", "🚀 ViewModel initialized with leak tracking")
        
        loadTest()
        
        // Restore timer if test was in progress (configuration change recovery)
        restoreTimerIfNeeded()
    }
    
    /**
     * Check if user is eligible to take the test based on subscription tier
     * SECURITY: Server-side check via Firestore
     */
    private suspend fun checkTestEligibility(userId: String): com.ssbmax.core.data.repository.TestEligibility {
        return subscriptionManager.canTakeTest(TestType.PPDT, userId)
    }
    
    /**
     * Restore timer after configuration change (e.g., screen rotation)
     * If test was in progress, restart the timer
     */
    private fun restoreTimerIfNeeded() {
        viewModelScope.launch {
            val state = _uiState.value
            
            // Only restore if we're in active test with time remaining
            if (!state.isLoading && 
                !state.isSubmitted && 
                state.timeRemainingSeconds > 0 && 
                (state.currentPhase == PPDTPhase.IMAGE_VIEWING || state.currentPhase == PPDTPhase.WRITING) &&
                !state.isTimerActive) {
                android.util.Log.d("PPDTTestViewModel", "🔄 Restoring timer after configuration change")
                startTimer(state.timeRemainingSeconds)
            }
        }
    }
    
    fun loadTest(testId: String = "ppdt_standard") {
        viewModelScope.launch {
            _uiState.update { it.copy(
                isLoading = true,
                loadingMessage = "Checking eligibility...",
                error = null
            ) }
            
            // Get current user - SECURITY: Require authentication
            val user = observeCurrentUser().first()
            val userId = user?.id ?: run {
                ErrorLogger.logTestError(
                    throwable = IllegalStateException("Unauthenticated PPDT test access"),
                    description = "PPDT test access without authentication",
                    testType = "PPDT"
                )

                // SECURITY: Log unauthenticated access attempt to Firebase Analytics
                securityLogger.logUnauthenticatedAccess(
                    testType = TestType.PPDT,
                    context = "PPDTTestViewModel.loadTest"
                )
                
                _uiState.update { it.copy(
                    isLoading = false,
                    loadingMessage = null,
                    error = "Authentication required. Please login to continue."
                ) }
                return@launch
            }
            
            android.util.Log.d("PPDTTestViewModel", "✅ User authenticated: $userId")
            
            try {
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
                        android.util.Log.d("PPDTTestViewModel", "❌ Test limit reached: ${eligibility.usedCount}/${eligibility.limit}")
                        return@launch
                    }
                    is com.ssbmax.core.data.repository.TestEligibility.Eligible -> {
                        android.util.Log.d("PPDTTestViewModel", "✅ Test eligible: ${eligibility.remainingTests} remaining")
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
                    testType = TestType.PPDT
                )
                
                if (sessionResult.isFailure) {
                    throw sessionResult.exceptionOrNull() ?: Exception("Failed to create test session")
                }
                
                // Fetch questions from cloud
                val questionsResult = testContentRepository.getPPDTQuestions(testId)
                
                if (questionsResult.isFailure) {
                    throw questionsResult.exceptionOrNull() ?: Exception("Failed to load test questions")
                }
                
                val questions = questionsResult.getOrNull() ?: emptyList()
                
                if (questions.isEmpty()) {
                    throw Exception("No questions found for this test")
                }
                
                val question = questions.first() // PPDT typically has one question
                android.util.Log.d("PPDTTestViewModel", "📸 Loaded question: ${question.id}")
                android.util.Log.d("PPDTTestViewModel", "📸 Question imageUrl: ${question.imageUrl}")
                android.util.Log.d("PPDTTestViewModel", "📸 ImageUrl length: ${question.imageUrl.length}")
                android.util.Log.d("PPDTTestViewModel", "📸 ImageUrl isEmpty: ${question.imageUrl.isEmpty()}")
                
                val config = PPDTTestConfig()
                
                val newSession = PPDTTestSession(
                    sessionId = sessionResult.getOrNull()!!,
                    userId = userId,
                    questionId = question.id,
                    question = question,
                    startTime = System.currentTimeMillis(),
                    imageViewingStartTime = null,
                    writingStartTime = null,
                    currentPhase = PPDTPhase.INSTRUCTIONS,
                    story = "",
                    isCompleted = false,
                    isPaused = false
                )
                
                _uiState.update { it.copy(session = newSession) }
                updateUiFromSession()
                
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
        val session = _uiState.value.session ?: return
        
        _uiState.update { it.copy(
            session = session.copy(
                currentPhase = PPDTPhase.IMAGE_VIEWING,
                imageViewingStartTime = System.currentTimeMillis()
            )
        ) }
        
        updateUiFromSession()
        startTimer(30) // 30 seconds for image viewing - timer will auto-advance via startTimer()
    }
    
    fun proceedToNextPhase() {
        val session = _uiState.value.session ?: return
        
        when (session.currentPhase) {
            PPDTPhase.IMAGE_VIEWING -> {
                // PHASE 3: Signal timer to stop via isTimerActive flag
                _uiState.update { it.copy(
                    isTimerActive = false,
                    session = session.copy(
                        currentPhase = PPDTPhase.WRITING,
                        writingStartTime = System.currentTimeMillis()
                    )
                ) }
                updateUiFromSession()
                startTimer(session.question.writingTimeMinutes * 60)
            }
            PPDTPhase.WRITING -> {
                if (_uiState.value.story.length >= session.question.minCharacters) {
                    // PHASE 3: Signal timer to stop via isTimerActive flag
                    _uiState.update { it.copy(
                        isTimerActive = false,
                        session = session.copy(currentPhase = PPDTPhase.REVIEW)
                    ) }
                    updateUiFromSession()
                }
            }
            else -> {}
        }
    }
    
    fun returnToWriting() {
        val session = _uiState.value.session ?: return
        _uiState.update { it.copy(
            session = session.copy(currentPhase = PPDTPhase.WRITING)
        ) }
        updateUiFromSession()
        startTimer(session.question.writingTimeMinutes * 60)
    }
    
    fun updateStory(newStory: String) {
        val session = _uiState.value.session ?: return
        _uiState.update { it.copy(
            session = session.copy(story = newStory),
            story = newStory,
            charactersCount = newStory.length,
            canProceedToNextPhase = newStory.length >= session.question.minCharacters
        ) }
    }
    
    fun submitTest() {
        // PHASE 3: Signal timer to stop via isTimerActive flag
        _uiState.update { it.copy(isTimerActive = false) }
        
        val session = _uiState.value.session ?: return
        
        viewModelScope.launch {
            try {
                // Get user profile for subscription type
                val userProfileResult = userProfileRepository.getUserProfile(session.userId).first()
                val userProfile = userProfileResult.getOrNull()
                val subscriptionType = userProfile?.subscriptionType ?: com.ssbmax.core.domain.model.SubscriptionType.FREE
                
                // Generate submission ID
                val submissionId = UUID.randomUUID().toString()
                
                // Create submission with OLQ analysis fields
                val submission = PPDTSubmission(
                    submissionId = submissionId,
                    questionId = session.questionId,
                    userId = session.userId,
                    userName = userProfile?.fullName ?: "Test User",
                    userEmail = "", // Email not stored in UserProfile
                    batchId = null,
                    story = session.story,
                    charactersCount = session.story.length,
                    viewingTimeTakenSeconds = 30, // From test config
                    writingTimeTakenMinutes = 4,  // From test config
                    submittedAt = System.currentTimeMillis(),
                    status = com.ssbmax.core.domain.model.SubmissionStatus.SUBMITTED_PENDING_REVIEW,
                    instructorReview = null,
                    analysisStatus = com.ssbmax.core.domain.model.scoring.AnalysisStatus.PENDING_ANALYSIS,
                    olqResult = null
                )
                
                // Submit to Firestore
                val result = submissionRepository.submitPPDT(submission, null)
                
                result.onSuccess { firestoreSubmissionId ->
                    android.util.Log.d("PPDTTestViewModel", "✅ Submitted PPDT to Firestore: $submissionId")
                    
                    // Enqueue PPDTAnalysisWorker for OLQ analysis
                    android.util.Log.d("PPDTTestViewModel", "📍 Enqueueing PPDTAnalysisWorker...")
                    enqueuePPDTAnalysisWorker(submissionId)
                    android.util.Log.d("PPDTTestViewModel", "✅ PPDTAnalysisWorker enqueued successfully")
                    
                    // Calculate score for analytics (story >200 chars is "valid")
                    val isValid = session.story.length >= 200
                    val scorePercentage = if (isValid) 100f else 0f
                    
                    // Record performance for analytics (using recommended difficulty)
                    val difficulty = difficultyManager.getRecommendedDifficulty("PPDT")
                    difficultyManager.recordPerformance(
                        testType = "PPDT",
                        difficulty = difficulty,
                        score = scorePercentage,
                        correctAnswers = if (isValid) 1 else 0,
                        totalQuestions = 1,
                        timeSeconds = (4 * 60).toFloat() // 4 minutes
                    )
                    android.util.Log.d("PPDTTestViewModel", "📊 Recorded performance ($difficulty): $scorePercentage%")
                    
                    // Record test usage for subscription tracking (with submissionId for idempotency)
                    subscriptionManager.recordTestUsage(TestType.PPDT, session.userId, submissionId)
                    android.util.Log.d("PPDTTestViewModel", "📝 Recorded test usage for subscription tracking")
                    
                    // Mark as submitted using thread-safe .update {}
                    _uiState.update { it.copy(
                        session = session.copy(
                            currentPhase = PPDTPhase.SUBMITTED,
                            isCompleted = true
                        ),
                        isSubmitted = true,
                        submissionId = submissionId,
                        subscriptionType = subscriptionType,
                        submission = submission
                    ) }
                    
                    // Emit navigation event (one-time, consumed by screen)
                    _navigationEvents.trySend(
                        TestNavigationEvent.NavigateToResult(
                            submissionId = submissionId,
                            subscriptionType = subscriptionType
                        )
                    )
                }.onFailure { error ->
                    ErrorLogger.log(error, "Failed to submit PPDT test for user: ${session.userId}")
                    _uiState.update { it.copy(
                        error = "Failed to submit: ${error.message}"
                    ) }
                }
            } catch (e: Exception) {
                ErrorLogger.log(e, "PPDT submit test exception")
                _uiState.update { it.copy(
                    error = "Failed to submit: ${e.message}"
                ) }
            }
        }
    }
    
    fun pauseTest() {
        val session = _uiState.value.session ?: return
        
        // PHASE 3: Signal timer to stop via isTimerActive flag
        _uiState.update { it.copy(
            isTimerActive = false,
            session = session.copy(isPaused = true)
        ) }
        // TODO: Save session state
    }
    
    private fun startTimer(seconds: Int) {
        // PHASE 3: Stop any existing timer by setting flag, then start new one
        _uiState.update { it.copy(
            timeRemainingSeconds = seconds,
            isTimerActive = true,
            timerStartTime = System.currentTimeMillis()
        ) }
        
        viewModelScope.launch {
            android.util.Log.d("PPDTTestViewModel", "⏰ Starting timer for $seconds seconds")
            
            try {
                while (isActive && 
                       _uiState.value.isTimerActive && 
                       _uiState.value.timeRemainingSeconds > 0) {
                    delay(1000)
                    
                    // Check if timer should still run (isTimerActive can be set false by submitTest/pauseTest)
                    if (!isActive || !_uiState.value.isTimerActive) break
                    
                    val newTime = _uiState.value.timeRemainingSeconds - 1
                    _uiState.update { it.copy(timeRemainingSeconds = newTime) }
                    
                    if (newTime == 0 && isActive && _uiState.value.isTimerActive) {
                        // Auto-proceed when time runs out
                        when (_uiState.value.currentPhase) {
                            PPDTPhase.IMAGE_VIEWING -> proceedToNextPhase()
                            PPDTPhase.WRITING -> proceedToNextPhase()
                            else -> {}
                        }
                    }
                }
            } catch (e: CancellationException) {
                android.util.Log.d("PPDTTestViewModel", "⏰ Timer cancelled")
                throw e // Re-throw to properly cancel coroutine
            } finally {
                // Ensure timer flag is cleared when loop exits
                _uiState.update { it.copy(isTimerActive = false) }
            }
        }.trackMemoryLeaks("PPDTTestViewModel", "phase-timer")
    }
    
    private fun updateUiFromSession() {
        val session = _uiState.value.session ?: return
        
        android.util.Log.d("PPDTTestViewModel", "📸 Image URL from session: ${session.question.imageUrl}")
        android.util.Log.d("PPDTTestViewModel", "📸 Image ID: ${session.question.id}")
        
        _uiState.update { it.copy(
            isLoading = false,
            loadingMessage = null,
            currentPhase = session.currentPhase,
            imageUrl = session.question.imageUrl,
            story = session.story,
            charactersCount = session.story.length,
            minCharacters = session.question.minCharacters,
            maxCharacters = session.question.maxCharacters,
            canProceedToNextPhase = session.story.length >= session.question.minCharacters
        ) }
    }
    
    private fun generateMockQuestion(): PPDTQuestion {
        return PPDTQuestion(
            id = "ppdt_q1",
            imageUrl = "https://example.com/ppdt-image.jpg",
            imageDescription = "A hazy image showing people in a situation",
            viewingTimeSeconds = 30,
            writingTimeMinutes = 4,
            minCharacters = 200,
            maxCharacters = 1000
        )
    }
    
    private fun generateMockAIScore(story: String): PPDTAIScore {
        // TODO: Actual AI scoring
        return PPDTAIScore(
            perceptionScore = 16f,
            imaginationScore = 14f,
            narrationScore = 15f,
            characterDepictionScore = 14f,
            positivityScore = 17f,
            overallScore = 76f,
            feedback = "Good overall story with positive outlook. Could improve imagination.",
            strengths = listOf(
                "Good character development",
                "Positive and optimistic outlook",
                "Clear narrative structure"
            ),
            areasForImprovement = listOf(
                "Could add more imaginative elements",
                "Describe the situation's background in more detail"
            )
        )
    }
    
    /**
     * Enqueue PPDTAnalysisWorker for background OLQ analysis
     */
    private fun enqueuePPDTAnalysisWorker(submissionId: String) {
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()
        
        val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.ssbmax.workers.PPDTAnalysisWorker>()
            .setInputData(androidx.work.workDataOf(
                com.ssbmax.workers.PPDTAnalysisWorker.KEY_SUBMISSION_ID to submissionId
            ))
            .setConstraints(constraints)
            .build()
        
        workManager.enqueueUniqueWork(
            "ppdt_analysis_$submissionId",
            androidx.work.ExistingWorkPolicy.KEEP,
            workRequest
        )
    }
    
    override fun onCleared() {
        super.onCleared()
        
        // PHASE 3: viewModelScope automatically cancels all child jobs
        android.util.Log.d("PPDTTestViewModel", "🧹 ViewModel onCleared() - viewModelScope auto-canceling all jobs")
        
        // Cancel navigation events channel
        _navigationEvents.close()
        
        // Unregister from memory leak tracker
        MemoryLeakTracker.unregisterViewModel("PPDTTestViewModel")
        
        // Force GC to help profiler detect cleanup
        MemoryLeakTracker.forceGcAndLog("PPDTTestViewModel-Cleared")
        
        android.util.Log.d("PPDTTestViewModel", "✅ PPDTTestViewModel cleanup complete")
    }
}

/**
 * UI State for PPDT Test Screen
 */
data class PPDTTestUiState(
    val isLoading: Boolean = true,
    val loadingMessage: String? = null,
    val error: String? = null,
    val currentPhase: PPDTPhase = PPDTPhase.INSTRUCTIONS,
    val imageUrl: String = "",
    val story: String = "",
    val charactersCount: Int = 0,
    val minCharacters: Int = 200,
    val maxCharacters: Int = 1000,
    val timeRemainingSeconds: Int = 0,
    val canProceedToNextPhase: Boolean = false,
    val isSubmitted: Boolean = false,
    val submissionId: String? = null,
    val subscriptionType: com.ssbmax.core.domain.model.SubscriptionType? = null,
    val submission: PPDTSubmission? = null,  // Submission stored locally until Firestore integration complete
    // Subscription limit fields
    val isLimitReached: Boolean = false,
    val subscriptionTier: SubscriptionTier = SubscriptionTier.FREE,
    val testsLimit: Int = 1,
    val testsUsed: Int = 0,
    val resetsAt: String = "",
    // PHASE 1: New StateFlow fields (replacing nullable vars)
    val isTimerActive: Boolean = false,
    val timerStartTime: Long = 0L,
    val session: PPDTTestSession? = null
)

/**
 * Test Session for PPDT
 */
data class PPDTTestSession(
    val sessionId: String,
    val userId: String,
    val questionId: String,
    val question: PPDTQuestion,
    val startTime: Long,
    val imageViewingStartTime: Long?,
    val writingStartTime: Long?,
    val currentPhase: PPDTPhase,
    val story: String,
    val isCompleted: Boolean,
    val isPaused: Boolean
)

/**
 * Test Configuration for PPDT
 */
data class PPDTTestConfig(
    val viewingTimeSeconds: Int = 30,
    val writingTimeMinutes: Int = 4,
    val minCharacters: Int = 200,
    val maxCharacters: Int = 1000
)

