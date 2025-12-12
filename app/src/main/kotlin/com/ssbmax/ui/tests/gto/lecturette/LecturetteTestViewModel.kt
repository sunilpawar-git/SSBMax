package com.ssbmax.ui.tests.gto.lecturette

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
    private val eligibilityChecker: GTOTestEligibilityChecker,
    private val submissionHelper: GTOTestSubmissionHelper
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LecturetteTestUiState())
    val uiState: StateFlow<LecturetteTestUiState> = _uiState.asStateFlow()
    
    // Track timer job to prevent duplicate coroutines
    private var timerJob: Job? = null
    
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
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Checking eligibility...") }
            try {
                val eligibilityResult = eligibilityChecker.checkEligibility(
                    testType = TestType.GTO_LECTURETTE,
                    gtoTestType = GTOTestType.LECTURETTE
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
                        _uiState.update { it.copy(loadingMessage = "Loading topics...") }
                        val topicsResult = gtoRepository.getRandomLecturetteTopics(TOPIC_COUNT)
                        if (topicsResult.isFailure) {
                            _uiState.update { it.copy(
                                isLoading = false,
                                error = "Failed to load test topics. Please try again."
                            ) }
                            return@launch
                        }
                        val topics = topicsResult.getOrElse {
                            _uiState.update { it.copy(
                                isLoading = false,
                                error = "Failed to load test topics. Please try again."
                            ) }
                            return@launch
                        }
                        _uiState.update { it.copy(
                            isLoading = false,
                            loadingMessage = null,
                            testId = testId,
                            userId = eligibilityResult.userId,
                            topicChoices = topics,
                            subscriptionType = eligibilityResult.subscriptionType,
                            phase = LecturettePhase.INSTRUCTIONS
                        ) }
                    }
                }
            } catch (e: Exception) {
                ErrorLogger.log(e, "Failed to load Lecturette test")
                _uiState.update { it.copy(isLoading = false, error = "Failed to load test. Please try again.") }
            }
        }
    }
    
    fun proceedToTopicSelection() {
        _uiState.update { it.copy(phase = LecturettePhase.TOPIC_SELECTION) }
    }
    
    fun selectTopic(topic: String) {
        _uiState.update { it.copy(
            selectedTopic = topic,
            phase = LecturettePhase.SPEECH,
            speechStartTime = System.currentTimeMillis(),
            timeRemaining = SPEECH_TIME_SECONDS
        ) }
        startTimer()
    }
    
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.timeRemaining > 0 && _uiState.value.phase == LecturettePhase.SPEECH) {
                delay(1000)
                _uiState.update { it.copy(
                    timeRemaining = (it.timeRemaining - 1).coerceAtLeast(0)
                ) }
            }
            if (_uiState.value.phase == LecturettePhase.SPEECH && _uiState.value.timeRemaining == 0) {
                proceedToReview()
            }
        }
    }
    
    fun onTranscriptChanged(newTranscript: String) {
        _uiState.update { it.copy(
            speechTranscript = newTranscript,
            wordCount = GTOTestUtils.countWords(newTranscript)
        ) }
    }
    
    fun proceedToReview() {
        val wordCount = _uiState.value.wordCount
        if (wordCount < MIN_WORDS) {
            _uiState.update { it.copy(
                validationError = "Speech must be at least $MIN_WORDS words (currently $wordCount)"
            ) }
            return
        }
        _uiState.update { it.copy(phase = LecturettePhase.REVIEW, validationError = null) }
    }
    
    fun backToSpeech() {
        _uiState.update { it.copy(phase = LecturettePhase.SPEECH) }
        startTimer()
    }
    
    fun submitTest() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, submitError = null) }
            val state = _uiState.value
            val submissionId = UUID.randomUUID().toString()
            val timeSpent = ((System.currentTimeMillis() - state.speechStartTime) / 1000).toInt()
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
            submissionHelper.submitTest(
                submission = submission,
                testType = GTOTestType.LECTURETTE,
                userId = state.userId,
                onSuccess = { id ->
                    _uiState.update { it.copy(
                        isSubmitting = false,
                        phase = LecturettePhase.SUBMITTED,
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

