package com.ssbmax.ui.tests.gto.gd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ssbmax.core.data.repository.DifficultyProgressionManager
import com.ssbmax.core.data.repository.SubscriptionManager
import com.ssbmax.core.data.security.SecurityEventLogger
import com.ssbmax.core.data.util.trackMemoryLeaks
import com.ssbmax.core.domain.model.SubscriptionType
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.model.gto.GTOSubmission
import com.ssbmax.core.domain.model.gto.GTOTestType
import com.ssbmax.core.domain.repository.GTORepository
import com.ssbmax.core.domain.repository.UserProfileRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.ui.tests.gto.common.GTOSequentialAccessManager
import com.ssbmax.utils.ErrorLogger
import com.ssbmax.workers.GTOAnalysisWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for Group Discussion (GD) Test
 * 
 * Test Flow:
 * 1. Instructions Phase: Show test format, rules, white noise warning
 * 2. Discussion Phase: Display topic, enable white noise, text input (20 min)
 * 3. Review Phase: Show response, word count, allow editing
 * 4. Submit: Create GDSubmission, enqueue worker, navigate to result
 * 
 * Features:
 * - Real-time word count (300-1500 words)
 * - White noise audio + visual overlay
 * - 20-minute timer with auto-advance
 * - Sequential access enforcement (must be first GTO test)
 * - Subscription limit checking
 */
@HiltViewModel
class GDTestViewModel @Inject constructor(
    private val gtoRepository: GTORepository,
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val userProfileRepository: UserProfileRepository,
    private val subscriptionManager: SubscriptionManager,
    private val difficultyManager: DifficultyProgressionManager,
    private val sequentialAccessManager: GTOSequentialAccessManager,
    private val securityLogger: SecurityEventLogger,
    private val workManager: WorkManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(GDTestUiState())
    val uiState: StateFlow<GDTestUiState> = _uiState.asStateFlow()
    
    init {
        trackMemoryLeaks("GDTestViewModel")
        android.util.Log.d(TAG, "🚀 GDTestViewModel initialized")
    }
    
    companion object {
        private const val TAG = "GDTestViewModel"
        private const val DISCUSSION_TIME_SECONDS = 1200 // 20 minutes
        private const val MIN_WORDS = 300
        private const val MAX_WORDS = 1500
    }
    
    /**
     * Load GD test (check eligibility, fetch topic)
     */
    fun loadTest(testId: String) {
        viewModelScope.launch {
            android.util.Log.d(TAG, "════════════════════════════════════════")
            android.util.Log.d(TAG, "🎬 loadTest() called for testId: $testId")
            android.util.Log.d(TAG, "════════════════════════════════════════")
            
            _uiState.update { it.copy(
                isLoading = true,
                loadingMessage = "Checking eligibility..."
            ) }
            
            try {
                // SECURITY: Get authenticated user
                android.util.Log.d(TAG, "📍 Step 1: Authenticating user...")
                val user = observeCurrentUser().first()
                val userId = user?.id ?: run {
                    android.util.Log.e(TAG, "🚨 SECURITY: Unauthenticated access blocked")
                    securityLogger.logUnauthenticatedAccess(
                        testType = TestType.GTO_GD,
                        context = "GDTestViewModel.loadTest"
                    )
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = "Authentication required. Please login to continue."
                    ) }
                    return@launch
                }
                
                android.util.Log.d(TAG, "✅ User authenticated: $userId")
                
                // Check sequential access (GD must be first GTO test)
                android.util.Log.d(TAG, "📍 Step 2: Checking sequential access...")
                val (canAccess, accessError) = sequentialAccessManager.checkAccess(
                    userId,
                    GTOTestType.GROUP_DISCUSSION
                )
                
                if (!canAccess) {
                    android.util.Log.w(TAG, "❌ Sequential access denied: $accessError")
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = accessError ?: "Complete previous GTO tests first"
                    ) }
                    return@launch
                }
                
                // Check subscription limits
                android.util.Log.d(TAG, "📍 Step 3: Checking subscription limits...")
                val eligibility = subscriptionManager.canTakeTest(TestType.GTO_GD, userId)
                
                when (eligibility) {
                    is com.ssbmax.core.data.repository.TestEligibility.LimitReached -> {
                        android.util.Log.w(TAG, "❌ Test limit reached")
                        _uiState.update { it.copy(
                            isLoading = false,
                            showLimitDialog = true,
                            limitMessage = eligibility.message
                        ) }
                        return@launch
                    }
                    is com.ssbmax.core.data.repository.TestEligibility.UpgradeRequired -> {
                        android.util.Log.w(TAG, "⚠️ Upgrade required")
                        _uiState.update { it.copy(
                            isLoading = false,
                            showUpgradeDialog = true,
                            upgradeMessage = eligibility.message
                        ) }
                        return@launch
                    }
                    is com.ssbmax.core.data.repository.TestEligibility.Eligible -> {
                        android.util.Log.d(TAG, "✅ Eligible: ${eligibility.testsRemaining} attempts remaining")
                    }
                }
                
                // Fetch random GD topic
                android.util.Log.d(TAG, "📍 Step 4: Fetching random topic...")
                _uiState.update { it.copy(loadingMessage = "Loading topic...") }
                
                val topicResult = gtoRepository.getRandomGDTopic()
                if (topicResult.isFailure) {
                    android.util.Log.e(TAG, "❌ Failed to fetch topic", topicResult.exceptionOrNull())
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = "Failed to load test topic. Please try again."
                    ) }
                    return@launch
                }
                
                val topic = topicResult.getOrNull()!!
                android.util.Log.d(TAG, "✅ Topic loaded: ${topic.take(50)}...")
                
                // Get subscription tier for result navigation
                val profile = userProfileRepository.getUserProfile(userId).getOrNull()
                val subscriptionType = profile?.subscriptionType ?: SubscriptionType.FREE
                
                // Test loaded successfully
                _uiState.update { it.copy(
                    isLoading = false,
                    loadingMessage = null,
                    testId = testId,
                    userId = userId,
                    topic = topic,
                    subscriptionType = subscriptionType,
                    phase = GDPhase.INSTRUCTIONS
                ) }
                
                android.util.Log.d(TAG, "════════════════════════════════════════")
                android.util.Log.d(TAG, "✅ Test loaded successfully!")
                android.util.Log.d(TAG, "════════════════════════════════════════")
                
            } catch (e: Exception) {
                ErrorLogger.log(e, "Failed to load GD test")
                _uiState.update { it.copy(
                    isLoading = false,
                    error = "Failed to load test. Please try again."
                ) }
            }
        }
    }
    
    /**
     * Start the discussion phase (from instructions)
     */
    fun startDiscussion() {
        android.util.Log.d(TAG, "▶️ Starting discussion phase")
        _uiState.update { it.copy(
            phase = GDPhase.DISCUSSION,
            discussionStartTime = System.currentTimeMillis(),
            timeRemaining = DISCUSSION_TIME_SECONDS
        ) }
        startTimer()
    }
    
    /**
     * Start 20-minute timer
     */
    private fun startTimer() {
        viewModelScope.launch {
            android.util.Log.d(TAG, "⏱️ Timer started: ${DISCUSSION_TIME_SECONDS}s")
            
            while (_uiState.value.timeRemaining > 0 && _uiState.value.phase == GDPhase.DISCUSSION) {
                delay(1000)
                _uiState.update { it.copy(
                    timeRemaining = (it.timeRemaining - 1).coerceAtLeast(0)
                ) }
            }
            
            // Time expired - auto-advance to review
            if (_uiState.value.phase == GDPhase.DISCUSSION && _uiState.value.timeRemaining == 0) {
                android.util.Log.d(TAG, "⏰ Time expired - auto-advancing to review")
                proceedToReview()
            }
        }
    }
    
    /**
     * Update response text
     */
    fun onResponseChanged(newResponse: String) {
        val wordCount = countWords(newResponse)
        _uiState.update { it.copy(
            response = newResponse,
            wordCount = wordCount
        ) }
    }
    
    /**
     * Proceed to review phase
     */
    fun proceedToReview() {
        android.util.Log.d(TAG, "📋 Proceeding to review phase")
        
        // Validate word count
        val wordCount = _uiState.value.wordCount
        if (wordCount < MIN_WORDS) {
            _uiState.update { it.copy(
                validationError = "Response must be at least $MIN_WORDS words (currently $wordCount)"
            ) }
            return
        }
        
        if (wordCount > MAX_WORDS) {
            _uiState.update { it.copy(
                validationError = "Response must not exceed $MAX_WORDS words (currently $wordCount)"
            ) }
            return
        }
        
        _uiState.update { it.copy(
            phase = GDPhase.REVIEW,
            validationError = null
        ) }
    }
    
    /**
     * Go back to discussion phase (from review)
     */
    fun backToDiscussion() {
        android.util.Log.d(TAG, "⬅️ Back to discussion phase")
        _uiState.update { it.copy(phase = GDPhase.DISCUSSION) }
        startTimer() // Restart timer
    }
    
    /**
     * Submit GD test
     */
    fun submitTest() {
        viewModelScope.launch {
            android.util.Log.d(TAG, "📤 Submitting GD test...")
            
            _uiState.update { it.copy(
                isSubmitting = true,
                submitError = null
            ) }
            
            try {
                val state = _uiState.value
                val submissionId = UUID.randomUUID().toString()
                val timeSpent = ((System.currentTimeMillis() - state.discussionStartTime) / 1000).toInt()
                
                // Create submission
                val submission = GTOSubmission.GDSubmission(
                    id = submissionId,
                    userId = state.userId,
                    testId = state.testId,
                    topic = state.topic,
                    response = state.response,
                    wordCount = state.wordCount,
                    submittedAt = System.currentTimeMillis(),
                    timeSpent = timeSpent
                )
                
                // Save to Firestore
                android.util.Log.d(TAG, "💾 Saving submission to Firestore...")
                val submitResult = gtoRepository.submitTest(submission)
                
                if (submitResult.isFailure) {
                    throw submitResult.exceptionOrNull() ?: Exception("Unknown submission error")
                }
                
                android.util.Log.d(TAG, "✅ Submission saved: $submissionId")
                
                // Record test usage for subscription limits
                android.util.Log.d(TAG, "📊 Recording test usage...")
                gtoRepository.recordTestUsage(
                    userId = state.userId,
                    testType = GTOTestType.GROUP_DISCUSSION,
                    submissionId = submissionId
                )
                
                // Update progress (mark GD as completed)
                android.util.Log.d(TAG, "📈 Updating user progress...")
                gtoRepository.updateProgress(
                    userId = state.userId,
                    completedTestType = GTOTestType.GROUP_DISCUSSION
                )
                
                // Enqueue background analysis worker
                android.util.Log.d(TAG, "🤖 Enqueuing analysis worker...")
                val workRequest = OneTimeWorkRequestBuilder<GTOAnalysisWorker>()
                    .setInputData(workDataOf(
                        GTOAnalysisWorker.KEY_SUBMISSION_ID to submissionId
                    ))
                    .build()
                
                workManager.enqueue(workRequest)
                
                // Update UI state with completion info
                _uiState.update { it.copy(
                    isSubmitting = false,
                    phase = GDPhase.SUBMITTED,
                    submissionId = submissionId,
                    isCompleted = true
                ) }
                
                android.util.Log.d(TAG, "✅ GD test submitted successfully!")
                
            } catch (e: Exception) {
                ErrorLogger.log(e, "Failed to submit GD test")
                _uiState.update { it.copy(
                    isSubmitting = false,
                    submitError = "Failed to submit test. Please try again."
                ) }
            }
        }
    }
    
    /**
     * Dismiss error dialog
     */
    fun dismissError() {
        _uiState.update { it.copy(error = null, validationError = null, submitError = null) }
    }
    
    /**
     * Dismiss limit dialog
     */
    fun dismissLimitDialog() {
        _uiState.update { it.copy(showLimitDialog = false) }
    }
    
    /**
     * Dismiss upgrade dialog
     */
    fun dismissUpgradeDialog() {
        _uiState.update { it.copy(showUpgradeDialog = false) }
    }
    
    /**
     * Count words in text (split by whitespace)
     */
    private fun countWords(text: String): Int {
        if (text.isBlank()) return 0
        return text.trim().split(Regex("\\s+")).size
    }
    
    override fun onCleared() {
        super.onCleared()
        android.util.Log.d(TAG, "🧹 ViewModel cleared")
    }
}

/**
 * GD Test UI State
 */
data class GDTestUiState(
    val isLoading: Boolean = false,
    val loadingMessage: String? = null,
    val error: String? = null,
    
    // Test info
    val testId: String = "",
    val userId: String = "",
    val topic: String = "",
    val subscriptionType: SubscriptionType = SubscriptionType.FREE,
    
    // Phase management
    val phase: GDPhase = GDPhase.INSTRUCTIONS,
    
    // Discussion phase
    val discussionStartTime: Long = 0L,
    val timeRemaining: Int = 1200, // 20 minutes
    val response: String = "",
    val wordCount: Int = 0,
    val validationError: String? = null,
    
    // Submission
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val submissionId: String? = null,
    val isCompleted: Boolean = false,
    
    // Dialogs
    val showLimitDialog: Boolean = false,
    val limitMessage: String? = null,
    val showUpgradeDialog: Boolean = false,
    val upgradeMessage: String? = null
) {
    /**
     * Check if response meets word count requirements
     */
    val meetsMinWordCount: Boolean
        get() = wordCount >= 300
    
    val meetsMaxWordCount: Boolean
        get() = wordCount <= 1500
    
    val meetsWordCountRequirements: Boolean
        get() = meetsMinWordCount && meetsMaxWordCount
    
    /**
     * Format time remaining as MM:SS
     */
    val formattedTime: String
        get() {
            val minutes = timeRemaining / 60
            val seconds = timeRemaining % 60
            return "%02d:%02d".format(minutes, seconds)
        }
    
    /**
     * Warning when time is running low (< 2 minutes)
     */
    val isTimeLow: Boolean
        get() = timeRemaining < 120 && timeRemaining > 0
}

/**
 * GD Test Phases
 */
enum class GDPhase {
    INSTRUCTIONS,   // Show test format and rules
    DISCUSSION,     // Active discussion phase (20 min)
    REVIEW,         // Review response before submission
    SUBMITTED       // Test submitted
}
