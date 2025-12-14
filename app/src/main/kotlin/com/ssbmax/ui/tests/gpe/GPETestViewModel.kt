package com.ssbmax.ui.tests.gpe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.data.util.MemoryLeakTracker
import com.ssbmax.core.data.util.trackMemoryLeaks
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.model.gto.GTOSubmission
import com.ssbmax.core.domain.model.gto.GTOSubmissionStatus
import com.ssbmax.core.domain.model.gto.GTOTestType
import com.ssbmax.core.domain.repository.TestContentRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.UUID
import javax.inject.Inject
import com.ssbmax.ui.tests.common.TestNavigationEvent

/**
 * ViewModel for GPE (Group Planning Exercise) Test Screen
 * Loads test questions from cloud via TestContentRepository
 *
 * Test Format:
 * - 60 seconds: View tactical scenario image
 * - 29 minutes: Write planning response
 *
 * MEMORY LEAK PREVENTION:
 * - Registers with MemoryLeakTracker for profiler verification
 * - viewModelScope automatically cancels all jobs in onCleared()
 * - Uses isTimerActive flag for timer lifecycle management
 * - Uses viewModelScope with isActive checks for cooperative cancellation
 * - No static references or context leaks
 */
@HiltViewModel
class GPETestViewModel @Inject constructor(
    private val testContentRepository: TestContentRepository,
    private val submissionRepository: com.ssbmax.core.domain.repository.SubmissionRepository,
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val userProfileRepository: com.ssbmax.core.domain.repository.UserProfileRepository,
    private val difficultyManager: com.ssbmax.core.data.repository.DifficultyProgressionManager,
    private val subscriptionManager: com.ssbmax.core.data.repository.SubscriptionManager,
    private val securityLogger: com.ssbmax.core.data.security.SecurityEventLogger,
    private val submissionHelper: com.ssbmax.ui.tests.gto.common.GTOTestSubmissionHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(GPETestUiState())
    val uiState: StateFlow<GPETestUiState> = _uiState.asStateFlow()

    // Navigation events (one-time events, consumed on collection)
    private val _navigationEvents = Channel<TestNavigationEvent>(Channel.BUFFERED)
    val navigationEvents = _navigationEvents.receiveAsFlow()

    init {
        // Register for memory leak tracking
        trackMemoryLeaks("GPETestViewModel")
        android.util.Log.d("GPETestViewModel", "🚀 ViewModel initialized with leak tracking")

        loadTest()

        // Restore timer if test was in progress (configuration change recovery)
        restoreTimerIfNeeded()
    }

    /**
     * Check if user is eligible to take the test based on subscription tier
     * SECURITY: Server-side check via Firestore
     */
    private suspend fun checkTestEligibility(userId: String): com.ssbmax.core.data.repository.TestEligibility {
        return subscriptionManager.canTakeTest(TestType.GTO_GPE, userId)
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
                (state.currentPhase == GPEPhase.IMAGE_VIEWING || state.currentPhase == GPEPhase.PLANNING) &&
                !state.isTimerActive) {
                android.util.Log.d("GPETestViewModel", "🔄 Restoring timer after configuration change")
                startTimer(state.timeRemainingSeconds)
            }
        }
    }

    fun loadTest(testId: String = "gpe_standard") {
        viewModelScope.launch {
            _uiState.update { it.copy(
                isLoading = true,
                loadingMessage = "Checking eligibility...",
                error = null
            ) }

            // Get current user - SECURITY: Require authentication
            val user = observeCurrentUser().first()
            val userId = user?.id ?: run {
                android.util.Log.e("GPETestViewModel", "🚨 SECURITY: Unauthenticated test access attempt blocked")

                // SECURITY: Log unauthenticated access attempt to Firebase Analytics
                securityLogger.logUnauthenticatedAccess(
                    testType = TestType.GTO_GPE,
                    context = "GPETestViewModel.loadTest"
                )

                _uiState.update { it.copy(
                    isLoading = false,
                    loadingMessage = null,
                    error = "Authentication required. Please login to continue."
                ) }
                return@launch
            }

            android.util.Log.d("GPETestViewModel", "✅ User authenticated: $userId")

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
                        android.util.Log.d("GPETestViewModel", "❌ Test limit reached: ${eligibility.usedCount}/${eligibility.limit}")
                        return@launch
                    }
                    is com.ssbmax.core.data.repository.TestEligibility.Eligible -> {
                        android.util.Log.d("GPETestViewModel", "✅ Test eligible: ${eligibility.remainingTests} remaining")
                        // Continue with test loading
                    }
                }

                _uiState.update { it.copy(
                    loadingMessage = "Fetching scenario from cloud..."
                ) }

                // Create test session
                val sessionResult = testContentRepository.createTestSession(
                    userId = userId,
                    testId = testId,
                    testType = TestType.GTO_GPE
                )

                if (sessionResult.isFailure) {
                    throw sessionResult.exceptionOrNull() ?: Exception("Failed to create test session")
                }

                // Fetch questions from cloud
                val questionsResult = testContentRepository.getGPEQuestions(testId)

                if (questionsResult.isFailure) {
                    throw questionsResult.exceptionOrNull() ?: Exception("Failed to load test scenario")
                }

                val questions = questionsResult.getOrNull() ?: emptyList()

                if (questions.isEmpty()) {
                    throw Exception("No scenarios found for this test")
                }

                val question = questions.first() // GPE typically has one scenario
                android.util.Log.d("GPETestViewModel", "📸 Loaded scenario: ${question.id}")
                android.util.Log.d("GPETestViewModel", "📸 Scenario imageUrl: ${question.imageUrl}")

                val config = GPETestConfig()

                val newSession = GPETestSession(
                    sessionId = sessionResult.getOrNull()!!,
                    userId = userId,
                    questionId = question.id,
                    question = question,
                    startTime = System.currentTimeMillis(),
                    imageViewingStartTime = null,
                    planningStartTime = null,
                    currentPhase = GPEPhase.INSTRUCTIONS,
                    planningResponse = "",
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
                currentPhase = GPEPhase.IMAGE_VIEWING,
                imageViewingStartTime = System.currentTimeMillis()
            )
        ) }

        updateUiFromSession()
        startTimer(60) // 60 seconds for image viewing - timer will auto-advance via startTimer()
    }

    fun proceedToNextPhase() {
        val session = _uiState.value.session ?: return

        when (session.currentPhase) {
            GPEPhase.IMAGE_VIEWING -> {
                // Signal timer to stop via isTimerActive flag
                _uiState.update { it.copy(
                    isTimerActive = false,
                    session = session.copy(
                        currentPhase = GPEPhase.PLANNING,
                        planningStartTime = System.currentTimeMillis()
                    )
                ) }
                updateUiFromSession()
                startTimer(session.question.planningTimeSeconds)
            }
            GPEPhase.PLANNING -> {
                if (_uiState.value.planningResponse.length >= session.question.minCharacters) {
                    // Signal timer to stop via isTimerActive flag
                    _uiState.update { it.copy(
                        isTimerActive = false,
                        session = session.copy(currentPhase = GPEPhase.REVIEW)
                    ) }
                    updateUiFromSession()
                }
            }
            else -> {}
        }
    }

    fun returnToPlanning() {
        val session = _uiState.value.session ?: return
        _uiState.update { it.copy(
            session = session.copy(currentPhase = GPEPhase.PLANNING)
        ) }
        updateUiFromSession()
        startTimer(session.question.planningTimeSeconds)
    }

    fun updatePlanningResponse(newResponse: String) {
        val session = _uiState.value.session ?: return
        _uiState.update { it.copy(
            session = session.copy(planningResponse = newResponse),
            planningResponse = newResponse,
            charactersCount = newResponse.length,
            canProceedToNextPhase = newResponse.length >= session.question.minCharacters
        ) }
    }

    fun submitTest() {
        // Signal timer to stop via isTimerActive flag
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

                // Create submission
                val submission = GTOSubmission.GPESubmission(
                    id = submissionId,
                    userId = session.userId,
                    testId = session.questionId,
                    imageUrl = session.question.imageUrl,
                    scenario = session.question.scenario,
                    plan = session.planningResponse,
                    characterCount = session.planningResponse.length,
                    submittedAt = System.currentTimeMillis(),
                    timeSpent = 30 * 60, // Approx 30 mins total (1 min viewing + 29 min planning)
                    status = GTOSubmissionStatus.PENDING_ANALYSIS,
                    olqScores = emptyMap()
                )

                // Use helper to submit test and trigger worker
                submissionHelper.submitTest(
                    submission = submission,
                    testType = GTOTestType.GROUP_PLANNING_EXERCISE,
                    userId = session.userId,
                    onSuccess = { id ->
                        android.util.Log.d("GPETestViewModel", "✅ GPE submission completed via helper: $id")

                        // Calculate score for analytics (response >500 chars is "valid")
                        val isValid = session.planningResponse.length >= 500
                        val scorePercentage = if (isValid) 100f else 0f

                        // Record performance for analytics
                        viewModelScope.launch {
                            difficultyManager.recordPerformance(
                                testType = "GPE",
                                difficulty = "MEDIUM", // GPE doesn't have difficulty levels yet
                                score = scorePercentage,
                                correctAnswers = if (isValid) 1 else 0,
                                totalQuestions = 1,
                                timeSeconds = (29 * 60).toFloat() // 29 minutes
                            )
                            android.util.Log.d("GPETestViewModel", "📊 Recorded performance: $scorePercentage%")
                        }

                        // NOTE: submissionHelper handles recordTestUsage via GTORepository,
                        // so we don't need to call subscriptionManager.recordTestUsage separately here
                        // to avoid duplication, unless they track different things.
                        // For safety in this refactor, we rely on the helper for the main GTO flow.

                        // Mark as submitted using thread-safe .update {}
                        _uiState.update { it.copy(
                            session = session.copy(
                                currentPhase = GPEPhase.SUBMITTED,
                                isCompleted = true
                            ),
                            isSubmitted = true,
                            submissionId = id,
                            subscriptionType = subscriptionType,
                            submission = submission
                        ) }

                        // Success! User will see success screen and can navigate home
                        android.util.Log.d("GPETestViewModel", "✅ GPE submitted - showing success screen")
                    },
                    onError = { error ->
                         _uiState.update { it.copy(
                            error = "Failed to submit: $error"
                        ) }
                    }
                )

            } catch (e: Exception) {
                _uiState.update { it.copy(
                    error = "Failed to submit: ${e.message}"
                ) }
            }
        }
    }

    fun pauseTest() {
        val session = _uiState.value.session ?: return

        // Signal timer to stop via isTimerActive flag
        _uiState.update { it.copy(
            isTimerActive = false,
            session = session.copy(isPaused = true)
        ) }
        // TODO: Save session state
    }

    private fun startTimer(seconds: Int) {
        // Stop any existing timer by setting flag, then start new one
        _uiState.update { it.copy(
            timeRemainingSeconds = seconds,
            isTimerActive = true,
            timerStartTime = System.currentTimeMillis()
        ) }

        viewModelScope.launch {
            android.util.Log.d("GPETestViewModel", "⏰ Starting timer for $seconds seconds")

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
                            GPEPhase.IMAGE_VIEWING -> proceedToNextPhase()
                            GPEPhase.PLANNING -> proceedToNextPhase()
                            else -> {}
                        }
                    }
                }
            } catch (e: CancellationException) {
                android.util.Log.d("GPETestViewModel", "⏰ Timer cancelled")
                throw e // Re-throw to properly cancel coroutine
            } finally {
                // Ensure timer flag is cleared when loop exits
                _uiState.update { it.copy(isTimerActive = false) }
            }
        }.trackMemoryLeaks("GPETestViewModel", "phase-timer")
    }

    private fun updateUiFromSession() {
        val session = _uiState.value.session ?: return

        android.util.Log.d("GPETestViewModel", "📸 Image URL from session: ${session.question.imageUrl}")
        android.util.Log.d("GPETestViewModel", "📸 Scenario ID: ${session.question.id}")

        _uiState.update { it.copy(
            isLoading = false,
            loadingMessage = null,
            currentPhase = session.currentPhase,
            imageUrl = session.question.imageUrl,
            scenario = session.question.scenario,
            resources = session.question.resources,
            planningResponse = session.planningResponse,
            charactersCount = session.planningResponse.length,
            minCharacters = session.question.minCharacters,
            maxCharacters = session.question.maxCharacters,
            canProceedToNextPhase = session.planningResponse.length >= session.question.minCharacters
        ) }
    }

    override fun onCleared() {
        super.onCleared()

        // viewModelScope automatically cancels all child jobs
        android.util.Log.d("GPETestViewModel", "🧹 ViewModel onCleared() - viewModelScope auto-canceling all jobs")

        // Cancel navigation events channel
        _navigationEvents.close()

        // Unregister from memory leak tracker
        MemoryLeakTracker.unregisterViewModel("GPETestViewModel")

        // Force GC to help profiler detect cleanup
        MemoryLeakTracker.forceGcAndLog("GPETestViewModel-Cleared")

        android.util.Log.d("GPETestViewModel", "✅ GPETestViewModel cleanup complete")
    }
}

/**
 * UI State for GPE Test Screen
 */
data class GPETestUiState(
    val isLoading: Boolean = true,
    val loadingMessage: String? = null,
    val error: String? = null,
    val currentPhase: GPEPhase = GPEPhase.INSTRUCTIONS,
    val imageUrl: String = "",
    val scenario: String = "",
    val resources: List<String> = emptyList(),
    val planningResponse: String = "",
    val charactersCount: Int = 0,
    val minCharacters: Int = 500,
    val maxCharacters: Int = 2000,
    val timeRemainingSeconds: Int = 0,
    val canProceedToNextPhase: Boolean = false,
    val isSubmitted: Boolean = false,
    val submissionId: String? = null,
    val subscriptionType: com.ssbmax.core.domain.model.SubscriptionType? = null,
    val submission: GTOSubmission.GPESubmission? = null,
    // Subscription limit fields
    val isLimitReached: Boolean = false,
    val subscriptionTier: SubscriptionTier = SubscriptionTier.FREE,
    val testsLimit: Int = 1,
    val testsUsed: Int = 0,
    val resetsAt: String = "",
    // Timer state
    val isTimerActive: Boolean = false,
    val timerStartTime: Long = 0L,
    val session: GPETestSession? = null
)
