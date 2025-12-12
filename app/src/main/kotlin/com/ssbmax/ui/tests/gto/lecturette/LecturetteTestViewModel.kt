package com.ssbmax.ui.tests.gto.lecturette

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
 * ViewModel for Lecturette Test
 * 
 * Test Flow:
 * 1. Instructions: Test format, 3-min speech, white noise warning
 * 2. Topic Selection: Show 4 random topics, user picks 1
 * 3. Speech: 3-minute timer + text input + white noise
 * 4. Review: Show speech transcript, word count
 * 5. Submit: Create LecturetteSubmission, enqueue worker
 * 
 * Features:
 * - 4-topic random selection from Firestore
 * - 3-minute countdown timer
 * - White noise during speech
 * - Real-time word count
 * - No preparation time (immediate start after selection)
 * - Sequential access enforcement (must complete GD, GPE first)
 */
@HiltViewModel
class LecturetteTestViewModel @Inject constructor(
    private val gtoRepository: GTORepository,
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val userProfileRepository: UserProfileRepository,
    private val subscriptionManager: SubscriptionManager,
    private val difficultyManager: DifficultyProgressionManager,
    private val sequentialAccessManager: GTOSequentialAccessManager,
    private val securityLogger: SecurityEventLogger,
    private val workManager: WorkManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LecturetteTestUiState())
    val uiState: StateFlow<LecturetteTestUiState> = _uiState.asStateFlow()
    
    companion object {
        private const val TAG = "LecturetteViewModel"
        private const val SPEECH_TIME_SECONDS = 180 // 3 minutes
        private const val MIN_WORDS = 100 // Shorter than GD since it's a speech
        private const val TOPIC_COUNT = 4
    }
    
    init {
        trackMemoryLeaks("LecturetteTestViewModel")
        android.util.Log.d(TAG, "🚀 LecturetteTestViewModel initialized")
    }
    
    /**
     * Load Lecturette test (check eligibility, fetch 4 topics)
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
                        testType = TestType.GTO_LECTURETTE,
                        context = "LecturetteTestViewModel.loadTest"
                    )
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = "Authentication required. Please login to continue."
                    ) }
                    return@launch
                }
                
                android.util.Log.d(TAG, "✅ User authenticated: $userId")
                
                // Check sequential access (Lecturette is 3rd GTO test)
                android.util.Log.d(TAG, "📍 Step 2: Checking sequential access...")
                val (canAccess, accessError) = sequentialAccessManager.checkAccess(
                    userId,
                    GTOTestType.LECTURETTE
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
                val eligibility = subscriptionManager.canTakeTest(TestType.GTO_LECTURETTE, userId)
                
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
                
                // Fetch 4 random Lecturette topics
                android.util.Log.d(TAG, "📍 Step 4: Fetching 4 random topics...")
                _uiState.update { it.copy(loadingMessage = "Loading topics...") }
                
                val topicsResult = gtoRepository.getRandomLecturetteTopics(TOPIC_COUNT)
                if (topicsResult.isFailure) {
                    android.util.Log.e(TAG, "❌ Failed to fetch topics", topicsResult.exceptionOrNull())
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = "Failed to load test topics. Please try again."
                    ) }
                    return@launch
                }
                
                val topics = topicsResult.getOrNull()!!
                android.util.Log.d(TAG, "✅ Topics loaded: ${topics.size} topics")
                topics.forEachIndexed { index, topic ->
                    android.util.Log.d(TAG, "   ${index + 1}. ${topic.take(50)}...")
                }
                
                // Get subscription tier
                val profile = userProfileRepository.getUserProfile(userId).getOrNull()
                val subscriptionType = profile?.subscriptionType ?: SubscriptionType.FREE
                
                // Test loaded successfully
                _uiState.update { it.copy(
                    isLoading = false,
                    loadingMessage = null,
                    testId = testId,
                    userId = userId,
                    topicChoices = topics,
                    subscriptionType = subscriptionType,
                    phase = LecturettePhase.INSTRUCTIONS
                ) }
                
                android.util.Log.d(TAG, "════════════════════════════════════════")
                android.util.Log.d(TAG, "✅ Test loaded successfully!")
                android.util.Log.d(TAG, "════════════════════════════════════════")
                
            } catch (e: Exception) {
                ErrorLogger.log(e, "Failed to load Lecturette test")
                _uiState.update { it.copy(
                    isLoading = false,
                    error = "Failed to load test. Please try again."
                ) }
            }
        }
    }
    
    /**
     * Proceed to topic selection
     */
    fun proceedToTopicSelection() {
        android.util.Log.d(TAG, "➡️ Proceeding to topic selection")
        _uiState.update { it.copy(phase = LecturettePhase.TOPIC_SELECTION) }
    }
    
    /**
     * Select a topic and start speech
     */
    fun selectTopic(topic: String) {
        android.util.Log.d(TAG, "🎯 Topic selected: $topic")
        _uiState.update { it.copy(
            selectedTopic = topic,
            phase = LecturettePhase.SPEECH,
            speechStartTime = System.currentTimeMillis(),
            timeRemaining = SPEECH_TIME_SECONDS
        ) }
        startTimer()
    }
    
    /**
     * Start 3-minute timer
     */
    private fun startTimer() {
        viewModelScope.launch {
            android.util.Log.d(TAG, "⏱️ Timer started: ${SPEECH_TIME_SECONDS}s")
            
            while (_uiState.value.timeRemaining > 0 && _uiState.value.phase == LecturettePhase.SPEECH) {
                delay(1000)
                _uiState.update { it.copy(
                    timeRemaining = (it.timeRemaining - 1).coerceAtLeast(0)
                ) }
            }
            
            // Time expired - auto-advance to review
            if (_uiState.value.phase == LecturettePhase.SPEECH && _uiState.value.timeRemaining == 0) {
                android.util.Log.d(TAG, "⏰ Time expired - auto-advancing to review")
                proceedToReview()
            }
        }
    }
    
    /**
     * Update speech transcript
     */
    fun onTranscriptChanged(newTranscript: String) {
        val wordCount = countWords(newTranscript)
        _uiState.update { it.copy(
            speechTranscript = newTranscript,
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
                validationError = "Speech must be at least $MIN_WORDS words (currently $wordCount)"
            ) }
            return
        }
        
        _uiState.update { it.copy(
            phase = LecturettePhase.REVIEW,
            validationError = null
        ) }
    }
    
    /**
     * Go back to speech (from review)
     */
    fun backToSpeech() {
        android.util.Log.d(TAG, "⬅️ Back to speech phase")
        _uiState.update { it.copy(phase = LecturettePhase.SPEECH) }
        startTimer() // Restart timer
    }
    
    /**
     * Submit Lecturette test
     */
    fun submitTest() {
        viewModelScope.launch {
            android.util.Log.d(TAG, "📤 Submitting Lecturette test...")
            
            _uiState.update { it.copy(
                isSubmitting = true,
                submitError = null
            ) }
            
            try {
                val state = _uiState.value
                val submissionId = UUID.randomUUID().toString()
                val timeSpent = ((System.currentTimeMillis() - state.speechStartTime) / 1000).toInt()
                
                // Create submission
                val submission = GTOSubmission.LecturetteSubmission(
                    id = submissionId,
                    userId = state.userId,
                    testId = state.testId,
                    topicChoices = state.topicChoices,
                    selectedTopic = state.selectedTopic,
                    speechTranscript = state.speechTranscript,
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
                
                // Record test usage
                android.util.Log.d(TAG, "📊 Recording test usage...")
                gtoRepository.recordTestUsage(
                    userId = state.userId,
                    testType = GTOTestType.LECTURETTE,
                    submissionId = submissionId
                )
                
                // Update progress
                android.util.Log.d(TAG, "📈 Updating user progress...")
                gtoRepository.updateProgress(
                    userId = state.userId,
                    completedTestType = GTOTestType.LECTURETTE
                )
                
                // Enqueue analysis worker
                android.util.Log.d(TAG, "🤖 Enqueuing analysis worker...")
                val workRequest = OneTimeWorkRequestBuilder<GTOAnalysisWorker>()
                    .setInputData(workDataOf(
                        GTOAnalysisWorker.KEY_SUBMISSION_ID to submissionId
                    ))
                    .build()
                
                workManager.enqueue(workRequest)
                
                // Update UI state
                _uiState.update { it.copy(
                    isSubmitting = false,
                    phase = LecturettePhase.SUBMITTED,
                    submissionId = submissionId,
                    isCompleted = true
                ) }
                
                android.util.Log.d(TAG, "✅ Lecturette test submitted successfully!")
                
            } catch (e: Exception) {
                ErrorLogger.log(e, "Failed to submit Lecturette test")
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
     * Count words in text
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
 * Lecturette Test UI State
 */
data class LecturetteTestUiState(
    val isLoading: Boolean = false,
    val loadingMessage: String? = null,
    val error: String? = null,
    
    // Test info
    val testId: String = "",
    val userId: String = "",
    val topicChoices: List<String> = emptyList(),
    val selectedTopic: String = "",
    val subscriptionType: SubscriptionType = SubscriptionType.FREE,
    
    // Phase management
    val phase: LecturettePhase = LecturettePhase.INSTRUCTIONS,
    
    // Speech phase
    val speechStartTime: Long = 0L,
    val timeRemaining: Int = 180, // 3 minutes
    val speechTranscript: String = "",
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
     * Check if transcript meets minimum word count
     */
    val meetsMinWordCount: Boolean
        get() = wordCount >= 100
    
    /**
     * Format time remaining as MM:SS
     */
    val formattedTime: String
        get() {
            val minutes = timeRemaining / 60
            val seconds = timeRemaining % 60
            return "%d:%02d".format(minutes, seconds)
        }
    
    /**
     * Warning when time is running low (< 30 seconds)
     */
    val isTimeLow: Boolean
        get() = timeRemaining < 30 && timeRemaining > 0
}

/**
 * Lecturette Test Phases
 */
enum class LecturettePhase {
    INSTRUCTIONS,       // Show test format and rules
    TOPIC_SELECTION,    // Show 4 topics, user picks 1
    SPEECH,             // 3-minute speech phase with white noise
    REVIEW,             // Review speech transcript
    SUBMITTED           // Test submitted
}
