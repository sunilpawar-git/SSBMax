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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    private val securityLogger: com.ssbmax.core.data.security.SecurityEventLogger,
    private val workManager: WorkManager
) : ViewModel() {
    
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
                
                // Create submission
                val totalTimeMinutes = ((System.currentTimeMillis() - state.startTime) / 60000).toInt()
                val submission = SRTSubmission(
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
                
                // Record performance for analytics
                difficultyManager.recordPerformance(
                    testType = "SRT",
                    difficulty = "MEDIUM", // SRT doesn't have difficulty levels yet
                    score = scorePercentage,
                    correctAnswers = validCount,
                    totalQuestions = totalCount,
                    timeSeconds = (totalTimeMinutes * 60).toFloat()
                )
                android.util.Log.d("SRTTestViewModel", "📊 Recorded performance: $scorePercentage% (${validCount}/${totalCount})")
                
                // Record test usage for subscription tracking
                subscriptionManager.recordTestUsage(TestType.SRT, currentUserId)
                android.util.Log.d("SRTTestViewModel", "📝 Recorded test usage for subscription tracking")
                
                // Submit to Firestore
                val result = submitSRTTest(submission, batchId = null)

                result.onSuccess { submissionId ->
                    android.util.Log.d("SRTTestViewModel", "✅ Submission successful! ID: $submissionId")

                    // Enqueue SRTAnalysisWorker for OLQ analysis
                    android.util.Log.d("SRTTestViewModel", "📍 Enqueueing SRTAnalysisWorker...")
                    enqueueSRTAnalysisWorker(submissionId)
                    android.util.Log.d("SRTTestViewModel", "✅ SRTAnalysisWorker enqueued successfully")

                    _uiState.update { it.copy(
                        isLoading = false,
                        isSubmitted = true,
                        submissionId = submissionId,
                        subscriptionType = subscriptionType,
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
    
    private fun generateMockSituations(): List<SRTSituation> {
        val situations = listOf(
            "You are the captain of your college team. During an important match, you notice that your best player is not feeling well but insists on playing." to SRTCategory.LEADERSHIP,
            "You witness a senior colleague taking credit for your junior's work in a meeting." to SRTCategory.ETHICAL_DILEMMA,
            "While traveling alone at night, you see an elderly person who has fallen and needs help." to SRTCategory.RESPONSIBILITY,
            "Your team is losing a crucial match, and team morale is very low." to SRTCategory.TEAMWORK,
            "You discover that your close friend has been cheating in exams." to SRTCategory.ETHICAL_DILEMMA,
            "During a group trek, one member gets injured and cannot walk." to SRTCategory.CRISIS_MANAGEMENT,
            "You have to choose between attending an important family function or a crucial team practice." to SRTCategory.DECISION_MAKING,
            "A stranger asks you to lend them money for emergency medical treatment." to SRTCategory.INTERPERSONAL,
            "You see a group of people harassing someone on the street." to SRTCategory.COURAGE,
            "Your subordinate makes a serious mistake that could impact the entire project." to SRTCategory.LEADERSHIP
        )
        
        // Repeat and shuffle to get 60 situations
        return (situations + situations + situations + situations + situations + situations)
            .take(60)
            .mapIndexed { index, (situation, category) ->
                SRTSituation(
                    id = "srt_s_${index + 1}",
                    situation = "$situation What would you do?",
                    sequenceNumber = index + 1,
                    category = category,
                    timeAllowedSeconds = 30
                )
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
    val resetsAt: String = ""
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
            val minLength = config?.minResponseLength ?: 20
            val maxLength = config?.maxResponseLength ?: 200
            return currentResponse.length in minLength..maxLength
        }
}

