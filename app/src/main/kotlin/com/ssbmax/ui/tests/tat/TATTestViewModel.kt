package com.ssbmax.ui.tests.tat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.data.util.MemoryLeakTracker
import com.ssbmax.core.data.util.trackMemoryLeaks
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.model.SubscriptionType
import com.ssbmax.core.domain.repository.TestContentRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.core.domain.usecase.submission.SubmitTATTestUseCase
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
    private val difficultyManager: com.ssbmax.core.data.repository.DifficultyProgressionManager,
    private val subscriptionManager: com.ssbmax.core.data.repository.SubscriptionManager,
    private val securityLogger: com.ssbmax.core.data.security.SecurityEventLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(TATTestUiState())
    val uiState: StateFlow<TATTestUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        // Register for memory leak tracking
        trackMemoryLeaks("TATTestViewModel")
        android.util.Log.d("TATTestViewModel", "🚀 ViewModel initialized with leak tracking")
        
        // Restore timer if test was in progress (configuration change recovery)
        restoreTimerIfNeeded()
    }
    
    /**
     * Check if user is eligible to take the test based on subscription tier
     * SECURITY: Server-side check via Firestore
     */
    private suspend fun checkTestEligibility(userId: String): com.ssbmax.core.data.repository.TestEligibility {
        return subscriptionManager.canTakeTest(TestType.TAT, userId)
    }
    
    /**
     * Restore timer after configuration change (e.g., screen rotation)
     * If test was in IMAGE_VIEWING or WRITING phase, restart the appropriate timer
     */
    private fun restoreTimerIfNeeded() {
        viewModelScope.launch {
            // Wait for initial state to be set
            val state = _uiState.value
            
            // Only restore if we're in an active phase (not loading or instructions)
            if (!state.isLoading && state.phase != TATPhase.INSTRUCTIONS && state.phase != TATPhase.SUBMITTED) {
                android.util.Log.d("TATTestViewModel", "🔄 Restoring timer for phase: ${state.phase}")
                
                when (state.phase) {
                    TATPhase.IMAGE_VIEWING -> {
                        if (state.viewingTimeRemaining > 0 && timerJob == null) {
                            startViewingTimer()
                        }
                    }
                    TATPhase.WRITING -> {
                        if (state.writingTimeRemaining > 0 && timerJob == null) {
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
                val user = observeCurrentUser().first()
                val userId = user?.id ?: run {
                    android.util.Log.e("TATTestViewModel", "🚨 SECURITY: Unauthenticated test access attempt blocked")
                    
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
                val eligibility = checkTestEligibility(userId)
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
                    android.util.Log.e("TATTestViewModel", "❌ Failed to create test session: ${sessionResult.exceptionOrNull()?.message}")
                    throw sessionResult.exceptionOrNull() ?: Exception("Failed to create test session")
                }
                android.util.Log.d("TATTestViewModel", "✅ Test session created")
                
                // Fetch questions from cloud
                android.util.Log.d("TATTestViewModel", "📍 Step 4: Fetching TAT questions from cloud...")
                val questionsResult = testContentRepository.getTATQuestions(testId)
                
                if (questionsResult.isFailure) {
                    android.util.Log.e("TATTestViewModel", "❌ Failed to load questions: ${questionsResult.exceptionOrNull()?.message}")
                    throw questionsResult.exceptionOrNull() ?: Exception("Failed to load test questions")
                }
                
                val questions = questionsResult.getOrNull() ?: emptyList()
                android.util.Log.d("TATTestViewModel", "✅ Loaded ${questions.size} TAT questions")
                
                if (questions.isEmpty()) {
                    android.util.Log.e("TATTestViewModel", "❌ No questions found!")
                    throw Exception("No questions found for this test")
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
                android.util.Log.e("TATTestViewModel", "❌ Error loading test: ${e.message}", e)
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
            stopTimer()
        }
    }
    
    fun moveToPreviousQuestion() {
        val state = _uiState.value
        
        if (state.currentQuestionIndex > 0) {
            stopTimer()
            
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
            
            try {
                stopTimer()
                
                // Get current user
                android.util.Log.d("TATTestViewModel", "📍 Step 1: Getting current user...")
                val currentUserId: String = observeCurrentUser().first()?.id ?: run {
                    android.util.Log.e("TATTestViewModel", "❌ User not authenticated")
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = "Please login to submit test"
                    ) }
                    return@launch
                }
                android.util.Log.d("TATTestViewModel", "✅ User ID: $currentUserId")
                
                // Get user profile for subscription type
                android.util.Log.d("TATTestViewModel", "📍 Step 2: Getting user profile...")
                val userProfileResult = userProfileRepository.getUserProfile(currentUserId).first()
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
                    aiPreliminaryScore = generateMockAIScore(state.responses)
                )
                android.util.Log.d("TATTestViewModel", "✅ Submission created with ${submission.stories.size} stories")
                
                // Submit to Firestore (but also store locally to bypass permission issues)
                android.util.Log.d("TATTestViewModel", "📍 Step 4: Submitting to Firestore...")
                val result = submitTATTest(submission, batchId = null)
                
                result.onSuccess { submissionId ->
                    android.util.Log.d("TATTestViewModel", "✅ Submission successful! ID: $submissionId")
                    
                    // Calculate score for analytics (stories with >150 chars are "valid")
                    val validCount = submission.stories.count { it.charactersCount >= 150 }
                    val totalCount = submission.stories.size
                    val scorePercentage = if (totalCount > 0) (validCount.toFloat() / totalCount) * 100 else 0f
                    
                    // Record performance for analytics
                    android.util.Log.d("TATTestViewModel", "📍 Step 5: Recording performance analytics...")
                    difficultyManager.recordPerformance(
                        testType = "TAT",
                        difficulty = "MEDIUM", // TAT doesn't have difficulty levels
                        score = scorePercentage,
                        correctAnswers = validCount,
                        totalQuestions = totalCount,
                        timeSeconds = (submission.totalTimeTakenMinutes * 60).toFloat()
                    )
                    android.util.Log.d("TATTestViewModel", "✅ Performance recorded: $scorePercentage% (${validCount}/${totalCount})")
                    
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
                    
                    android.util.Log.d("TATTestViewModel", "✅ TAT test submission complete!")
                    android.util.Log.d("TATTestViewModel", "════════════════════════════════════════")
                }.onFailure { error ->
                    android.util.Log.e("TATTestViewModel", "❌ Submission failed: ${error.message}", error)
                    android.util.Log.d("TATTestViewModel", "════════════════════════════════════════")
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = "Failed to submit: ${error.message}"
                    ) }
                }
            } catch (e: Exception) {
                android.util.Log.e("TATTestViewModel", "❌ Exception during submission: ${e.message}", e)
                android.util.Log.d("TATTestViewModel", "════════════════════════════════════════")
                _uiState.update { it.copy(
                    isLoading = false,
                    error = e.message
                ) }
            }
        }
    }
    
    private fun startViewingTimer() {
        stopTimer()

        _uiState.update { it.copy(
            viewingTimeRemaining = it.config?.viewingTimePerPictureSeconds ?: 30
        ) }

        timerJob = viewModelScope.launch {
            android.util.Log.d("TATTestViewModel", "⏰ Starting viewing timer")

            try {
                while (isActive && _uiState.value.viewingTimeRemaining > 0) {
                    delay(1000)
                    if (!isActive) break // Double-check after delay
                    _uiState.update { it.copy(
                        viewingTimeRemaining = it.viewingTimeRemaining - 1
                    ) }
                }

                // Auto-transition to writing (only if not cancelled)
                if (isActive) {
                    android.util.Log.d("TATTestViewModel", "⏰ Viewing timer completed, transitioning to writing")
                    _uiState.update { it.copy(phase = TATPhase.WRITING) }
                    startWritingTimer()
                }
            } catch (e: CancellationException) {
                android.util.Log.d("TATTestViewModel", "⏰ Viewing timer cancelled")
                throw e // Re-throw to properly cancel coroutine
            }
        }.also { job ->
            // Register AFTER job is created
            job.trackMemoryLeaks("TATTestViewModel", "viewing-timer")
        }
    }
    
    private fun startWritingTimer() {
        stopTimer()

        _uiState.update { it.copy(
            writingTimeRemaining = (it.config?.writingTimePerPictureMinutes ?: 4) * 60
        ) }

        timerJob = viewModelScope.launch {
            android.util.Log.d("TATTestViewModel", "⏰ Starting writing timer")

            try {
                while (isActive && _uiState.value.writingTimeRemaining > 0) {
                    delay(1000)
                    if (!isActive) break // Double-check after delay
                    _uiState.update { it.copy(
                        writingTimeRemaining = it.writingTimeRemaining - 1
                    ) }
                }

                // Time's up - move to review (only if not cancelled)
                if (isActive) {
                    android.util.Log.d("TATTestViewModel", "⏰ Writing timer completed, moving to review")
                    _uiState.update { it.copy(phase = TATPhase.REVIEW_CURRENT) }
                }
            } catch (e: CancellationException) {
                android.util.Log.d("TATTestViewModel", "⏰ Writing timer cancelled")
                throw e // Re-throw to properly cancel coroutine
            }
        }.also { job ->
            // Register AFTER job is created
            job.trackMemoryLeaks("TATTestViewModel", "writing-timer")
        }
    }
    
    private fun stopTimer() {
        timerJob?.let { job ->
            if (job.isActive) {
                android.util.Log.d("TATTestViewModel", "🛑 Cancelling active timer job")
                job.cancel()
            }
        }
        timerJob = null
    }
    
    private fun generateMockQuestions(): List<TATQuestion> {
        return (1..12).map { index ->
            TATQuestion(
                id = "tat_q_$index",
                imageUrl = "https://via.placeholder.com/800x600/3498db/ffffff?text=TAT+Picture+$index",
                sequenceNumber = index,
                prompt = "Write a story about what you see in the picture",
                viewingTimeSeconds = 30,
                writingTimeMinutes = 4,
                minCharacters = 150,
                maxCharacters = 800
            )
        }
    }
    
    private fun generateMockAIScore(stories: List<TATStoryResponse>): TATAIScore {
        // TODO: Actual AI scoring
        return TATAIScore(
            overallScore = 78f,
            thematicPerceptionScore = 16f,
            imaginationScore = 15f,
            characterDepictionScore = 16f,
            emotionalToneScore = 16f,
            narrativeStructureScore = 15f,
            feedback = "Good storytelling with positive themes. Shows leadership qualities and imagination.",
            storyWiseAnalysis = stories.mapIndexed { index, story ->
                StoryAnalysis(
                    questionId = story.questionId,
                    sequenceNumber = index + 1,
                    score = (70..85).random().toFloat(),
                    themes = listOf("Leadership", "Courage", "Teamwork").shuffled().take(2),
                    sentimentScore = kotlin.random.Random.nextFloat() * 0.4f + 0.5f, // 0.5 to 0.9
                    keyInsights = listOf("Shows initiative", "Positive resolution")
                )
            },
            strengths = listOf(
                "Creative storytelling",
                "Positive outlook",
                "Good character development"
            ),
            areasForImprovement = listOf(
                "Add more detail to situation descriptions",
                "Include emotional depth"
            )
        )
    }
    
    override fun onCleared() {
        super.onCleared()

        // Critical: Stop all timers to prevent leaks
        android.util.Log.d("TATTestViewModel", "🧹 ViewModel onCleared() - stopping timers")
        stopTimer()

        // Cancel all jobs in viewModelScope (belt and suspenders approach)
        android.util.Log.d("TATTestViewModel", "🧹 Cancelling viewModelScope")
        
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
    val resetsAt: String = ""
) {
    val currentQuestion: TATQuestion?
        get() = questions.getOrNull(currentQuestionIndex)
    
    val completedStories: Int
        get() = responses.size
    
    val progress: Float
        get() = if (questions.isEmpty()) 0f else (completedStories.toFloat() / questions.size)
    
    val canMoveToNextQuestion: Boolean
        get() = when (phase) {
            TATPhase.WRITING -> currentStory.length >= (currentQuestion?.minCharacters ?: 150) &&
                                currentStory.length <= (currentQuestion?.maxCharacters ?: 800)
            TATPhase.REVIEW_CURRENT -> true
            else -> false
        }
    
    val canMoveToPreviousQuestion: Boolean
        get() = currentQuestionIndex > 0 && phase == TATPhase.WRITING
    
    val canSubmitTest: Boolean
        get() = completedStories >= 11 // Allow submission after 11 stories minimum
}

