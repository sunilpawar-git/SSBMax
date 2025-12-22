package com.ssbmax.ui.tests.gto.gd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.data.util.trackMemoryLeaks
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.model.gto.GTOSubmission
import com.ssbmax.core.domain.model.gto.GTOTestType
import com.ssbmax.core.domain.repository.GTORepository
import com.ssbmax.ui.tests.gto.common.GTOTestEligibilityChecker
import com.ssbmax.ui.tests.gto.common.GTOTestSubmissionHelper
import com.ssbmax.ui.tests.gto.common.GTOTestUtils
import com.ssbmax.utils.ErrorLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val testContentRepository: com.ssbmax.core.domain.repository.TestContentRepository,
    private val eligibilityChecker: GTOTestEligibilityChecker,
    private val submissionHelper: GTOTestSubmissionHelper
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(GDTestUiState())
    val uiState: StateFlow<GDTestUiState> = _uiState.asStateFlow()
    
    // Track timer job to prevent duplicate coroutines
    private var timerJob: Job? = null
    
    init {
        trackMemoryLeaks("GDTestViewModel")
        android.util.Log.d(TAG, "🚀 GDTestViewModel initialized")
    }
    
    companion object {
        private const val TAG = "GDTestViewModel"
        private const val DISCUSSION_TIME_SECONDS = 1200 // 20 minutes
        private const val MIN_CHARS = 50
        private const val MAX_CHARS = 1500
    }
    
    /**
     * Load GD test (check eligibility, fetch topic)
     */
    fun loadTest(testId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Checking eligibility...") }
            try {
                val eligibilityResult = eligibilityChecker.checkEligibility(
                    testType = TestType.GTO_GD,
                    gtoTestType = GTOTestType.GROUP_DISCUSSION
                )
                when (eligibilityResult) {
                    is GTOTestEligibilityChecker.EligibilityResult.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = eligibilityResult.message) }
                        return@launch
                    }
                    is GTOTestEligibilityChecker.EligibilityResult.LimitReached -> {
                        _uiState.update { it.copy(
                            isLoading = false,
                            showLimitDialog = true,
                            limitMessage = eligibilityResult.message
                        ) }
                        return@launch
                    }
                    is GTOTestEligibilityChecker.EligibilityResult.Eligible -> {
                        _uiState.update { it.copy(loadingMessage = "Loading topic...") }
                        
                        val topicResult = testContentRepository.getRandomGDTopic()
                        if (topicResult.isFailure) {
                            _uiState.update { it.copy(
                                isLoading = false,
                                error = "Failed to load test topic. Please try again."
                            ) }
                            return@launch
                        }
                        val topic = topicResult.getOrElse {
                            _uiState.update { it.copy(
                                isLoading = false,
                                error = "Failed to load test topic. Please try again."
                            ) }
                            return@launch
                        }
                        
                        _uiState.update { it.copy(
                            isLoading = false,
                            loadingMessage = null,
                            testId = testId,
                            userId = eligibilityResult.userId,
                            topic = topic,
                            subscriptionType = eligibilityResult.subscriptionType,
                            phase = GDPhase.INSTRUCTIONS
                        ) }
                    }
                }
            } catch (e: Exception) {
                ErrorLogger.log(e, "Failed to load GD test")
                _uiState.update { it.copy(isLoading = false, error = "Failed to load test. Please try again.") }
            }
        }
    }
    
    fun startDiscussion() {
        _uiState.update { it.copy(
            phase = GDPhase.DISCUSSION,
            discussionStartTime = System.currentTimeMillis(),
            timeRemaining = DISCUSSION_TIME_SECONDS
        ) }
        startTimer()
    }
    
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.timeRemaining > 0 && _uiState.value.phase == GDPhase.DISCUSSION) {
                delay(1000)
                _uiState.update { it.copy(
                    timeRemaining = (it.timeRemaining - 1).coerceAtLeast(0)
                ) }
            }
            if (_uiState.value.phase == GDPhase.DISCUSSION && _uiState.value.timeRemaining == 0) {
                proceedToReview()
            }
        }
    }
    
    fun onResponseChanged(newResponse: String) {
        _uiState.update { it.copy(
            response = newResponse,
            charCount = newResponse.trim().length
        ) }
    }
    
    fun proceedToReview() {
        val charCount = _uiState.value.charCount
        if (charCount < MIN_CHARS) {
            _uiState.update { it.copy(
                validationError = "Response must be at least $MIN_CHARS characters (currently $charCount)"
            ) }
            return
        }
        if (charCount > MAX_CHARS) {
            _uiState.update { it.copy(
                validationError = "Response must not exceed $MAX_CHARS characters (currently $charCount)"
            ) }
            return
        }
        _uiState.update { it.copy(phase = GDPhase.REVIEW, validationError = null) }
    }
    
    fun backToDiscussion() {
        _uiState.update { it.copy(phase = GDPhase.DISCUSSION) }
        startTimer()
    }
    
    fun submitTest() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, submitError = null) }
            val state = _uiState.value
            val submissionId = UUID.randomUUID().toString()
            val timeSpent = ((System.currentTimeMillis() - state.discussionStartTime) / 1000).toInt()
            val submission = GTOSubmission.GDSubmission(
                id = submissionId,
                userId = state.userId,
                testId = state.testId,
                topic = state.topic,
                response = state.response,
                charCount = state.charCount,
                submittedAt = System.currentTimeMillis(),
                timeSpent = timeSpent
            )
            submissionHelper.submitTest(
                submission = submission,
                testType = GTOTestType.GROUP_DISCUSSION,
                userId = state.userId,
                onSuccess = { id ->
                    _uiState.update { it.copy(
                        isSubmitting = false,
                        phase = GDPhase.SUBMITTED,
                        submissionId = id,
                        isCompleted = true
                    ) }
                },
                onError = { error ->
                    _uiState.update { it.copy(isSubmitting = false, submitError = error) }
                }
            )
        }
    }
    
    fun dismissError() {
        _uiState.update { it.copy(error = null, validationError = null, submitError = null) }
    }
    
    fun dismissLimitDialog() {
        _uiState.update { it.copy(showLimitDialog = false) }
    }
    
    fun dismissUpgradeDialog() {
        _uiState.update { it.copy(showUpgradeDialog = false) }
    }
    
    
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

