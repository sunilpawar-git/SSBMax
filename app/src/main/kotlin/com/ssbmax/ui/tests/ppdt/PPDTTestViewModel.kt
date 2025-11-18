package com.ssbmax.ui.tests.ppdt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.data.util.MemoryLeakTracker
import com.ssbmax.core.data.util.trackMemoryLeaks
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.TestContentRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
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
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for PPDT Test Screen
 * Loads test questions from cloud via TestContentRepository
 * 
 * MEMORY LEAK PREVENTION:
 * - Registers with MemoryLeakTracker for profiler verification
 * - Properly cancels timerJob in onCleared()
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
    private val securityLogger: com.ssbmax.core.data.security.SecurityEventLogger
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PPDTTestUiState())
    val uiState: StateFlow<PPDTTestUiState> = _uiState.asStateFlow()
    
    private var timerJob: Job? = null
    private var currentSession: PPDTTestSession? = null
    
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
                timerJob == null) {
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
                android.util.Log.e("PPDTTestViewModel", "🚨 SECURITY: Unauthenticated test access attempt blocked")
                
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
                
                currentSession = PPDTTestSession(
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
        val session = currentSession ?: return
        
        currentSession = session.copy(
            currentPhase = PPDTPhase.IMAGE_VIEWING,
            imageViewingStartTime = System.currentTimeMillis()
        )
        
        updateUiFromSession()
        startTimer(30) // 30 seconds for image viewing
        
        // Auto-proceed to writing after viewing
        viewModelScope.launch {
            delay(30000)
            if (_uiState.value.currentPhase == PPDTPhase.IMAGE_VIEWING) {
                proceedToNextPhase()
            }
        }
    }
    
    fun proceedToNextPhase() {
        val session = currentSession ?: return
        
        when (session.currentPhase) {
            PPDTPhase.IMAGE_VIEWING -> {
                timerJob?.cancel()
                currentSession = session.copy(
                    currentPhase = PPDTPhase.WRITING,
                    writingStartTime = System.currentTimeMillis()
                )
                updateUiFromSession()
                startTimer(session.question.writingTimeMinutes * 60)
            }
            PPDTPhase.WRITING -> {
                if (_uiState.value.story.length >= session.question.minCharacters) {
                    timerJob?.cancel()
                    currentSession = session.copy(currentPhase = PPDTPhase.REVIEW)
                    updateUiFromSession()
                }
            }
            else -> {}
        }
    }
    
    fun returnToWriting() {
        val session = currentSession ?: return
        currentSession = session.copy(currentPhase = PPDTPhase.WRITING)
        updateUiFromSession()
        startTimer(session.question.writingTimeMinutes * 60)
    }
    
    fun updateStory(newStory: String) {
        val session = currentSession ?: return
        currentSession = session.copy(story = newStory)
        _uiState.update { it.copy(
            story = newStory,
            charactersCount = newStory.length,
            canProceedToNextPhase = newStory.length >= session.question.minCharacters
        ) }
    }
    
    fun submitTest() {
        timerJob?.cancel()
        
        val session = currentSession ?: return
        
        viewModelScope.launch {
            try {
                // Get user profile for subscription type
                val userProfileResult = userProfileRepository.getUserProfile(session.userId).first()
                val userProfile = userProfileResult.getOrNull()
                val subscriptionType = userProfile?.subscriptionType ?: com.ssbmax.core.domain.model.SubscriptionType.FREE
                
                // Generate submission ID
                val submissionId = UUID.randomUUID().toString()
                
                // Generate AI preliminary score
                val aiScore = generateMockAIScore(session.story)
                
                // Create submission
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
                    aiPreliminaryScore = aiScore,
                    instructorReview = null
                )
                
                // Submit to Firestore
                submissionRepository.submitPPDT(submission, null).getOrThrow()
                android.util.Log.d("PPDTTestViewModel", "✅ Submitted PPDT to Firestore: $submissionId")
                
                // Calculate score for analytics (story >200 chars is "valid")
                val isValid = session.story.length >= 200
                val scorePercentage = if (isValid) 100f else 0f
                
                // Record performance for analytics
                difficultyManager.recordPerformance(
                    testType = "PPDT",
                    difficulty = "MEDIUM", // PPDT doesn't have difficulty levels
                    score = scorePercentage,
                    correctAnswers = if (isValid) 1 else 0,
                    totalQuestions = 1,
                    timeSeconds = (4 * 60).toFloat() // 4 minutes
                )
                android.util.Log.d("PPDTTestViewModel", "📊 Recorded performance: $scorePercentage%")
                
                // Record test usage for subscription tracking
                subscriptionManager.recordTestUsage(TestType.PPDT, session.userId)
                android.util.Log.d("PPDTTestViewModel", "📝 Recorded test usage for subscription tracking")
                
                // Mark as submitted
                currentSession = session.copy(
                    currentPhase = PPDTPhase.SUBMITTED,
                    isCompleted = true
                )
                
                _uiState.update { it.copy(
                    isSubmitted = true,
                    submissionId = submissionId,
                    subscriptionType = subscriptionType,
                    submission = submission
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    error = "Failed to submit: ${e.message}"
                ) }
            }
        }
    }
    
    fun pauseTest() {
        timerJob?.cancel()
        val session = currentSession ?: return
        currentSession = session.copy(isPaused = true)
        // TODO: Save session state
    }
    
    private fun startTimer(seconds: Int) {
        timerJob?.cancel()
        _uiState.update { it.copy(timeRemainingSeconds = seconds) }
        
        timerJob = viewModelScope.launch {
            android.util.Log.d("PPDTTestViewModel", "⏰ Starting timer for $seconds seconds")
            
            try {
                while (isActive && _uiState.value.timeRemainingSeconds > 0) {
                    delay(1000)
                    if (!isActive) break // Double-check after delay
                    
                    val newTime = _uiState.value.timeRemainingSeconds - 1
                    _uiState.update { it.copy(timeRemainingSeconds = newTime) }
                    
                    if (newTime == 0 && isActive) {
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
            }
        }.also { job ->
            // Register AFTER job is created
            job.trackMemoryLeaks("PPDTTestViewModel", "phase-timer")
        }
    }
    
    private fun updateUiFromSession() {
        val session = currentSession ?: return
        
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
    
    override fun onCleared() {
        super.onCleared()
        
        // Critical: Stop all timers to prevent leaks
        android.util.Log.d("PPDTTestViewModel", "🧹 ViewModel onCleared() - stopping timers")
        timerJob?.cancel()
        timerJob = null
        
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
    val resetsAt: String = ""
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

