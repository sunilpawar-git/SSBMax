package com.ssbmax.ui.tests.srt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.model.scoring.AnalysisStatus
import com.ssbmax.core.domain.repository.TestContentRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.core.domain.usecase.submission.SubmitSRTTestUseCase
import com.ssbmax.ui.tests.common.TestNavigationEvent
import com.ssbmax.utils.ErrorLogger
import com.ssbmax.workers.SRTAnalysisWorker
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
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

/**
 * ViewModel for SRT Test Screen
 * Loads test questions from cloud via TestContentRepository
 */
@HiltViewModel
class SRTTestViewModel @Inject constructor(
    private val testContentRepository: TestContentRepository,
    private val submitSRTTest: SubmitSRTTestUseCase,
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val userProfileRepository: com.ssbmax.core.domain.repository.UserProfileRepository,
    private val difficultyManager: com.ssbmax.core.data.repository.DifficultyProgressionManager,
    private val subscriptionManager: com.ssbmax.core.data.repository.SubscriptionManager,
    private val getOLQDashboard: com.ssbmax.core.domain.usecase.dashboard.GetOLQDashboardUseCase,
    private val securityLogger: com.ssbmax.core.data.security.SecurityEventLogger,
    private val workManager: WorkManager
) : ViewModel() {
    
    // Timer Job reference
    private var timerJob: Job? = null
    
    private val _uiState = MutableStateFlow(SRTTestUiState())
    val uiState: StateFlow<SRTTestUiState> = _uiState.asStateFlow()
    
    // Navigation events (one-time events, consumed on collection)
    private val _navigationEvents = Channel<TestNavigationEvent>(Channel.BUFFERED)
    val navigationEvents = _navigationEvents.receiveAsFlow()
    
    /**
     * Check if user is eligible to take the test based on subscription tier
     */
    private suspend fun checkTestEligibility(userId: String): com.ssbmax.core.data.repository.TestEligibility {
        return subscriptionManager.canTakeTest(TestType.SRT, userId)
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
                        throwable = IllegalStateException("Unauthenticated SRT test access"),
                        description = "SRT test access without authentication",
                        testType = "SRT"
                    )

                    // SECURITY: Log unauthenticated access attempt to Firebase Analytics
                    securityLogger.logUnauthenticatedAccess(
                        testType = TestType.SRT,
                        context = "SRTTestViewModel.loadTest"
                    )
                    
                    _uiState.update { it.copy(
                        isLoading = false,
                        loadingMessage = null,
                        error = "Authentication required. Please login to continue."
                    ) }
                    return@launch
                }
                
                android.util.Log.d("SRTTestViewModel", "✅ User authenticated: $userId")
                
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
                        android.util.Log.d("SRTTestViewModel", "❌ Test limit reached: ${eligibility.usedCount}/${eligibility.limit}")
                        return@launch
                    }
                    is com.ssbmax.core.data.repository.TestEligibility.Eligible -> {
                        android.util.Log.d("SRTTestViewModel", "✅ Test eligible: ${eligibility.remainingTests} remaining")
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
                    testType = TestType.SRT
                )
                
                if (sessionResult.isFailure) {
                    throw sessionResult.exceptionOrNull() ?: Exception("Failed to create test session")
                }
                
                // Fetch situations from cloud
                val situationsResult = testContentRepository.getSRTQuestions(testId)
                
                if (situationsResult.isFailure) {
                    throw situationsResult.exceptionOrNull() ?: Exception("Failed to load test situations")
                }
                
                val situations = situationsResult.getOrNull() ?: emptyList()
                
                if (situations.isEmpty()) {
                    throw Exception("No situations found for this test")
                }
                
                val config = SRTTestConfig()
                
                _uiState.update { it.copy(
                    isLoading = false,
                    loadingMessage = null,
                    testId = testId,
                    situations = situations,
                    config = config,
                    phase = SRTPhase.INSTRUCTIONS
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
            phase = SRTPhase.IN_PROGRESS,
            currentSituationIndex = 0,
            startTime = System.currentTimeMillis()
        ) }
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        
        val totalTimeMinutes = _uiState.value.config?.totalTimeMinutes ?: 30
        val totalTimeSeconds = totalTimeMinutes * 60
        
        _uiState.update { it.copy(
            timeRemaining = totalTimeSeconds,
            isTimerActive = true,
            timerStartTime = System.currentTimeMillis()
        ) }
        
        val startTime = System.currentTimeMillis()
        val endTime = startTime + (totalTimeSeconds * 1000)
        
        timerJob = viewModelScope.launch {
            try {
                while (isActive) {
                    val remainingMillis = endTime - System.currentTimeMillis()
                    val remainingSeconds = (remainingMillis / 1000).toInt()
                    
                    if (remainingSeconds <= 0) break
                    
                    _uiState.update { it.copy(timeRemaining = remainingSeconds) }
                    delay(500)
                }
                
                // Timer expired
                if (isActive) {
                    // Set Time's Up state (blocks UI)
                    _uiState.update { it.copy(isTimeUp = true) }
                    
                    // Grace period - let user see "Time's Up" dialog
                    delay(2000)
                    
                    if (isActive) {
                        // Auto-submit with partial flush
                        submitTest()
                    }
                }
            } catch (e: CancellationException) {
                // Timer cancelled
            } finally {
                _uiState.update { it.copy(isTimerActive = false) }
            }
        }
    }
    
    fun updateResponse(response: String) {
        val maxLength = _uiState.value.config?.maxResponseLength ?: 200
        if (response.length <= maxLength) {
            _uiState.update { it.copy(currentResponse = response) }
        }
    }
    
    fun moveToNext() {
        val state = _uiState.value
        val currentSituation = state.currentSituation ?: return
        
        // Save current response
        val response = SRTSituationResponse(
            situationId = currentSituation.id,
            situation = currentSituation.situation,
            response = state.currentResponse,
            charactersCount = state.currentResponse.length,
            timeTakenSeconds = 30, // TODO: Track actual time
            submittedAt = System.currentTimeMillis(),
            isSkipped = false
        )
        
        val updatedResponses = state.responses.toMutableList().apply {
            removeAll { it.situationId == response.situationId }
            add(response)
        }
        
        _uiState.update { it.copy(responses = updatedResponses) }
        
        // Move to next or review
        if (state.currentSituationIndex < state.situations.size - 1) {
            _uiState.update { it.copy(
                currentSituationIndex = state.currentSituationIndex + 1,
                currentResponse = ""
            ) }
        } else {
            // All situations shown, go to review
            _uiState.update { it.copy(phase = SRTPhase.REVIEW) }
        }
    }
    
    fun skipSituation() {
        val state = _uiState.value
        val currentSituation = state.currentSituation ?: return
        
        // Save skipped response
        val response = SRTSituationResponse(
            situationId = currentSituation.id,
            situation = currentSituation.situation,
            response = "",
            charactersCount = 0,
            timeTakenSeconds = 0,
            submittedAt = System.currentTimeMillis(),
            isSkipped = true
        )
        
        val updatedResponses = state.responses.toMutableList().apply {
            removeAll { it.situationId == response.situationId }
            add(response)
        }
        
        _uiState.update { it.copy(
            responses = updatedResponses,
            currentResponse = ""
        ) }
        
        // Move to next
        if (state.currentSituationIndex < state.situations.size - 1) {
            _uiState.update { it.copy(
                currentSituationIndex = state.currentSituationIndex + 1
            ) }
        } else {
            // All situations shown, go to review
            _uiState.update { it.copy(phase = SRTPhase.REVIEW) }
        }
    }
    
    fun editResponse(index: Int) {
        val state = _uiState.value
        if (index in state.situations.indices) {
            val responseToEdit = state.responses.getOrNull(index)
            _uiState.update { it.copy(
                currentSituationIndex = index,
                currentResponse = responseToEdit?.response ?: "",
                phase = SRTPhase.IN_PROGRESS
            ) }
        }
    }
    
    fun submitTest() {
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
                
                // Flush partial response if exists and not already submitted
                // This saves the "mid-typing" answer
                val finalResponses = if (state.currentResponse.isNotEmpty() && state.currentSituation != null) {
                    val partialResponse = SRTSituationResponse(
                        situationId = state.currentSituation!!.id,
                        situation = state.currentSituation!!.situation,
                        response = state.currentResponse,
                        charactersCount = state.currentResponse.length,
                        timeTakenSeconds = 30, // Default for now
                        submittedAt = System.currentTimeMillis(),
                        isSkipped = false
                    )
                    
                    state.responses.toMutableList().apply {
                        removeAll { it.situationId == partialResponse.situationId }
                        add(partialResponse)
                    }
                } else {
                    state.responses
                }
                
                // Create submission
                val totalTimeMinutes = ((System.currentTimeMillis() - state.startTime) / 60000).toInt()
                val submission = SRTSubmission(
                    userId = currentUserId,
                    testId = state.testId,
                    responses = finalResponses,
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
                val difficulty = difficultyManager.getRecommendedDifficulty("SRT")
                difficultyManager.recordPerformance(
                    testType = "SRT",
                    difficulty = difficulty,
                    score = scorePercentage,
                    correctAnswers = validCount,
                    totalQuestions = totalCount,
                    timeSeconds = (totalTimeMinutes * 60).toFloat()
                )
                android.util.Log.d("SRTTestViewModel", "📊 Recorded performance ($difficulty): $scorePercentage% (${validCount}/${totalCount})")
                
                // Submit to Firestore
                val result = submitSRTTest(submission, batchId = null)

                result.onSuccess { submissionId ->
                    android.util.Log.d("SRTTestViewModel", "✅ Submission successful! ID: $submissionId")

                    // Enqueue SRTAnalysisWorker for OLQ analysis
                    android.util.Log.d("SRTTestViewModel", "📍 Enqueueing SRTAnalysisWorker...")
                    enqueueSRTAnalysisWorker(submissionId)
                    android.util.Log.d("SRTTestViewModel", "✅ SRTAnalysisWorker enqueued successfully")

                    // Record test usage for subscription tracking (with submissionId for idempotency)
                    android.util.Log.d("SRTTestViewModel", "📍 Recording test usage for subscription...")
                    subscriptionManager.recordTestUsage(TestType.SRT, currentUserId, submissionId)
                    android.util.Log.d("SRTTestViewModel", "✅ Test usage recorded successfully!")

                    // NOTE: Cache invalidation moved to SRTAnalysisWorker.
                    // Invalidating here is premature because analysis takes ~15-30s.
                    // The next dashboard fetch would cache empty SRT result.
                    // See: SRTAnalysisWorker.doWork() for correct cache invalidation timing.

                    _uiState.update { it.copy(
                        isLoading = false,
                        isSubmitted = true,
                        submissionId = submissionId,
                        subscriptionType = subscriptionType,
                        submission = submission,  // Store locally to show results directly
                        phase = SRTPhase.SUBMITTED
                    ) }

                    // Emit navigation event (one-time, consumed by screen)
                    _navigationEvents.trySend(
                        TestNavigationEvent.NavigateToResult(
                            submissionId = submissionId,
                            subscriptionType = subscriptionType
                        )
                    )
                }.onFailure { error ->
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
    
    

    
    /**
     * Enqueue SRTAnalysisWorker for background OLQ analysis
     */
    private fun enqueueSRTAnalysisWorker(submissionId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<SRTAnalysisWorker>()
            .setInputData(workDataOf(SRTAnalysisWorker.KEY_SUBMISSION_ID to submissionId))
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            "srt_analysis_$submissionId",
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        _navigationEvents.close()
    }
}

/**
 * UI State for SRT Test
 */
data class SRTTestUiState(
    val isLoading: Boolean = true,
    val loadingMessage: String? = null,
    val testId: String = "",
    val situations: List<SRTSituation> = emptyList(),
    val config: SRTTestConfig? = null,
    val currentSituationIndex: Int = 0,
    val responses: List<SRTSituationResponse> = emptyList(),
    val currentResponse: String = "",
    val phase: SRTPhase = SRTPhase.INSTRUCTIONS,
    val startTime: Long = System.currentTimeMillis(),
    val isSubmitted: Boolean = false,
    val submissionId: String? = null,
    val subscriptionType: com.ssbmax.core.domain.model.SubscriptionType? = null,
    val error: String? = null,
    // Subscription limit fields
    val isLimitReached: Boolean = false,
    val subscriptionTier: com.ssbmax.core.domain.model.SubscriptionTier = com.ssbmax.core.domain.model.SubscriptionTier.FREE,
    val testsLimit: Int = 1,
    val testsUsed: Int = 0,
    val resetsAt: String = "",
    val submission: SRTSubmission? = null,
    // Timer fields
    val timeRemaining: Int = 1800, // 30 minutes
    val isTimerActive: Boolean = false,
    val timerStartTime: Long = 0L,
    val isTimeUp: Boolean = false
) {
    val currentSituation: SRTSituation?
        get() = situations.getOrNull(currentSituationIndex)
    
    val completedSituations: Int
        get() = responses.size
    
    val validResponseCount: Int
        get() = responses.count { it.isValidResponse }
    
    val progress: Float
        get() = if (situations.isEmpty()) 0f else (completedSituations.toFloat() / situations.size)
    
    val canMoveToNext: Boolean
        get() {
            val minLength = config?.minResponseLength ?: 0
            val maxLength = config?.maxResponseLength ?: 200
            return currentResponse.length in minLength..maxLength
        }
}

