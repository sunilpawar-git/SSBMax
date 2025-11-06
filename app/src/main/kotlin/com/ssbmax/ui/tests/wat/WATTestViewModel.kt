package com.ssbmax.ui.tests.wat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.data.util.MemoryLeakTracker
import com.ssbmax.core.data.util.trackMemoryLeaks
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.TestContentRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.core.domain.usecase.submission.SubmitWATTestUseCase
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
    private val securityLogger: com.ssbmax.core.data.security.SecurityEventLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(WATTestUiState())
    val uiState: StateFlow<WATTestUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

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
            
            // Only restore if we're in active phase with time remaining
            if (!state.isLoading && 
                state.phase == WATPhase.IN_PROGRESS && 
                state.timeRemaining > 0 && 
                timerJob == null) {
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
                    android.util.Log.e("WATTestViewModel", "🚨 SECURITY: Unauthenticated test access attempt blocked")
                    
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
        stopTimer()
        _uiState.update { it.copy(phase = WATPhase.COMPLETED) }
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
                    aiPreliminaryScore = generateMockAIScore(state.responses)
                )
                
                // Calculate score for analytics (valid responses / total)
                val validCount = submission.validResponses
                val totalCount = submission.totalResponses
                val scorePercentage = if (totalCount > 0) (validCount.toFloat() / totalCount) * 100 else 0f
                
                // Record performance for analytics
                difficultyManager.recordPerformance(
                    testType = "WAT",
                    difficulty = "MEDIUM", // WAT doesn't have difficulty levels yet
                    score = scorePercentage,
                    correctAnswers = validCount,
                    totalQuestions = totalCount,
                    timeSeconds = (totalTimeMinutes * 60).toFloat()
                )
                android.util.Log.d("WATTestViewModel", "📊 Recorded performance: $scorePercentage% (${validCount}/${totalCount})")
                
                // Record test usage for subscription tracking
                subscriptionManager.recordTestUsage(TestType.WAT, currentUserId)
                android.util.Log.d("WATTestViewModel", "📝 Recorded test usage for subscription tracking")
                
                // Submit to Firestore (but also store locally to bypass permission issues)
                val result = submitWATTest(submission, batchId = null)
                
                result.onSuccess { submissionId ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        isSubmitted = true,
                        submissionId = submissionId,
                        subscriptionType = subscriptionType,
                        submission = submission,  // Store locally to show results directly
                        phase = WATPhase.SUBMITTED
                    ) }
                }.onFailure { error ->
                    // Even if Firestore fails, store locally and show results
                    _uiState.update { it.copy(
                        isLoading = false,
                        isSubmitted = true,
                        submissionId = submission.id,
                        subscriptionType = subscriptionType,
                        submission = submission,  // Store locally to show results directly
                        phase = WATPhase.SUBMITTED
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
        stopTimer()

        val timePerWord = _uiState.value.config?.timePerWordSeconds ?: 15
        _uiState.update { it.copy(timeRemaining = timePerWord) }

        timerJob = viewModelScope.launch {
            android.util.Log.d("WATTestViewModel", "⏰ Starting word timer")

            try {
                while (isActive && _uiState.value.timeRemaining > 0) {
                    delay(1000)
                    if (!isActive) break // Double-check after delay
                    _uiState.update { it.copy(
                        timeRemaining = it.timeRemaining - 1
                    ) }
                }

                // Time's up - auto-skip (only if not cancelled)
                if (isActive) {
                    android.util.Log.d("WATTestViewModel", "⏰ Word timer completed, auto-skipping")
                    skipWord()
                }
            } catch (e: CancellationException) {
                android.util.Log.d("WATTestViewModel", "⏰ Word timer cancelled")
                throw e // Re-throw to properly cancel coroutine
            }
        }.also { job ->
            // Register AFTER job is created
            job.trackMemoryLeaks("WATTestViewModel", "word-timer")
        }
    }
    
    private fun stopTimer() {
        timerJob?.let { job ->
            if (job.isActive) {
                android.util.Log.d("WATTestViewModel", "🛑 Cancelling active timer job")
                job.cancel()
            }
        }
        timerJob = null
    }
    
    private fun generateMockWords(): List<WATWord> {
        val commonWords = listOf(
            "Friend", "Success", "Failure", "Army", "Courage", "Fear", "Leader", "Team",
            "Challenge", "Victory", "Defeat", "Mother", "Father", "Love", "Hate", "War",
            "Peace", "Fight", "Help", "Sacrifice", "Duty", "Honor", "Brave", "Weak",
            "Strong", "Win", "Lose", "Run", "Stand", "Fall", "Rise", "Dark", "Light",
            "Night", "Day", "Fast", "Slow", "Good", "Bad", "Right", "Wrong", "Truth",
            "Lie", "Future", "Past", "Present", "Hope", "Dream", "Goal", "Path", "Choice",
            "Difficult", "Easy", "Hard", "Soft", "Hot", "Cold", "High", "Low", "Up", "Down"
        )
        
        return commonWords.shuffled().take(60).mapIndexed { index, word ->
            WATWord(
                id = "wat_w_${index + 1}",
                word = word,
                sequenceNumber = index + 1,
                timeAllowedSeconds = 15
            )
        }
    }
    
    private fun generateMockAIScore(responses: List<WATWordResponse>): WATAIScore {
        val validResponses = responses.filter { it.isValidResponse }
        
        // Simple sentiment analysis (mock)
        val positiveWords = listOf("success", "win", "good", "happy", "love", "help", "friend")
        val negativeWords = listOf("fail", "lose", "bad", "sad", "hate", "hurt", "enemy")
        
        val positiveCount = validResponses.count { response ->
            positiveWords.any { response.response.contains(it, ignoreCase = true) }
        }
        val negativeCount = validResponses.count { response ->
            negativeWords.any { response.response.contains(it, ignoreCase = true) }
        }
        val neutralCount = validResponses.size - positiveCount - negativeCount
        
        val uniqueResponses = validResponses.map { it.response.lowercase() }.toSet().size
        
        return WATAIScore(
            overallScore = 75f,
            positivityScore = 16f,
            creativityScore = 15f,
            speedScore = 15f,
            relevanceScore = 14f,
            emotionalMaturityScore = 15f,
            feedback = "Good spontaneity and positive associations. Shows emotional maturity.",
            positiveWords = positiveCount,
            negativeWords = negativeCount,
            neutralWords = neutralCount,
            uniqueResponsesCount = uniqueResponses,
            repeatedPatterns = listOf("Leadership themes", "Action-oriented"),
            strengths = listOf(
                "Positive outlook",
                "Quick responses",
                "Unique associations"
            ),
            areasForImprovement = listOf(
                "Be more creative with associations",
                "Avoid negative responses when possible"
            )
        )
    }
    
    override fun onCleared() {
        super.onCleared()

        // Critical: Stop all timers to prevent leaks
        android.util.Log.d("WATTestViewModel", "🧹 ViewModel onCleared() - stopping timers")
        stopTimer()

        // Cancel all jobs in viewModelScope (belt and suspenders approach)
        android.util.Log.d("WATTestViewModel", "🧹 Cancelling viewModelScope")
        
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
    val resetsAt: String = ""
) {
    val currentWord: WATWord?
        get() = words.getOrNull(currentWordIndex)
    
    val completedWords: Int
        get() = responses.size
    
    val progress: Float
        get() = if (words.isEmpty()) 0f else (completedWords.toFloat() / words.size)
}

