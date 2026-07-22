package com.ssbmax.shared.presentation.lecturette

import com.ssbmax.shared.domain.model.SubscriptionType
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.model.gto.GTOSubmission
import com.ssbmax.shared.domain.model.gto.GTOTestType
import com.ssbmax.shared.domain.repository.SubmissionRepository
import com.ssbmax.shared.domain.repository.TestContentRepository
import com.ssbmax.shared.domain.util.DomainLogger
import com.ssbmax.shared.presentation.gto.common.GTOEligibilityChecker
import com.ssbmax.shared.presentation.gto.common.GTOSubmissionCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Lecturette test phases. */
enum class LecturettePhase { INSTRUCTIONS, TOPIC_SELECTION, SPEECH, REVIEW, SUBMITTED }

/**
 * KMP port of `app/.../ui/tests/gto/lecturette/LecturetteTestUiState.kt` +
 * `LecturetteTestViewModel.kt`. Same shape as
 * [com.ssbmax.shared.presentation.gd.GDTestViewModel] -- structurally a
 * near-twin of GD (same eligibility/submission-coordinator reuse, same
 * async-analysis seam finding: Lecturette's Android original also enqueues
 * `GTOAnalysisWorker`), differing only in an extra TOPIC_SELECTION phase (4
 * random topics, no prep time before the 3-minute speech timer starts) and a
 * 180-second (not 1200-second) countdown.
 */
class LecturetteTestViewModel(
    private val testContentRepository: TestContentRepository,
    private val submissionRepository: SubmissionRepository,
    private val eligibilityChecker: GTOEligibilityChecker,
    private val submissionCoordinator: GTOSubmissionCoordinator,
    private val logger: DomainLogger
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val tag = "LecturetteTestViewModel"

    private val _uiState = MutableStateFlow(LecturetteTestUiState())
    val uiState: StateFlow<LecturetteTestUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    private companion object {
        const val SPEECH_TIME_SECONDS = 180
        const val MIN_CHARS = 50
        const val MAX_CHARS = 1500
        const val TOPIC_COUNT = 4
    }

    fun loadTest(testId: String = "gto_lecturette_standard") {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Checking eligibility...") }
            when (val result = eligibilityChecker.checkEligibility(TestType.GTO_LECTURETTE, GTOTestType.LECTURETTE)) {
                is GTOEligibilityChecker.Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                is GTOEligibilityChecker.Result.LimitReached -> {
                    _uiState.update { it.copy(isLoading = false, showLimitDialog = true, limitMessage = result.message) }
                }
                is GTOEligibilityChecker.Result.Eligible -> {
                    _uiState.update { it.copy(loadingMessage = "Loading topics...") }
                    testContentRepository.getRandomLecturetteTopics(TOPIC_COUNT)
                        .onSuccess { topics ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false, loadingMessage = null,
                                    testId = testId, userId = result.userId, topicChoices = topics,
                                    subscriptionType = result.subscriptionType, phase = LecturettePhase.INSTRUCTIONS
                                )
                            }
                        }
                        .onFailure { e ->
                            logger.e(tag, "Failed to load Lecturette topics", e)
                            _uiState.update { it.copy(isLoading = false, error = "Failed to load test topics. Please try again.") }
                        }
                }
            }
        }
    }

    fun proceedToTopicSelection() {
        _uiState.update { it.copy(phase = LecturettePhase.TOPIC_SELECTION) }
    }

    fun selectTopic(topic: String) {
        _uiState.update {
            it.copy(selectedTopic = topic, phase = LecturettePhase.SPEECH, speechStartTime = Clock.System.now().toEpochMilliseconds(), timeRemaining = SPEECH_TIME_SECONDS)
        }
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive && _uiState.value.timeRemaining > 0 && _uiState.value.phase == LecturettePhase.SPEECH) {
                delay(1000)
                _uiState.update { it.copy(timeRemaining = (it.timeRemaining - 1).coerceAtLeast(0)) }
            }
            if (_uiState.value.phase == LecturettePhase.SPEECH && _uiState.value.timeRemaining == 0) {
                proceedToReview()
            }
        }
    }

    fun onTranscriptChanged(newTranscript: String) {
        _uiState.update { it.copy(speechTranscript = newTranscript, charCount = newTranscript.trim().length) }
    }

    fun proceedToReview() {
        val charCount = _uiState.value.charCount
        when {
            charCount < MIN_CHARS -> _uiState.update { it.copy(validationError = "Speech must be at least $MIN_CHARS characters (currently $charCount)") }
            charCount > MAX_CHARS -> _uiState.update { it.copy(validationError = "Speech must not exceed $MAX_CHARS characters (currently $charCount)") }
            else -> _uiState.update { it.copy(phase = LecturettePhase.REVIEW, validationError = null) }
        }
    }

    fun backToSpeech() {
        _uiState.update { it.copy(phase = LecturettePhase.SPEECH) }
        startTimer()
    }

    @OptIn(ExperimentalUuidApi::class)
    fun submitTest() {
        scope.launch {
            _uiState.update { it.copy(isSubmitting = true, submitError = null) }
            val state = _uiState.value
            val timeSpent = ((Clock.System.now().toEpochMilliseconds() - state.speechStartTime) / 1000).toInt()
            val submission = GTOSubmission.LecturetteSubmission(
                id = Uuid.random().toString(),
                userId = state.userId,
                testId = state.testId,
                topicChoices = state.topicChoices,
                selectedTopic = state.selectedTopic,
                speechTranscript = state.speechTranscript,
                charCount = state.charCount,
                submittedAt = Clock.System.now().toEpochMilliseconds(),
                timeSpent = timeSpent
            )
            submissionRepository.submitLecturette(submission)
                .onSuccess { submissionId ->
                    submissionCoordinator.onSubmitted(submissionId, TestType.GTO_LECTURETTE, GTOTestType.LECTURETTE, state.userId)
                    _uiState.update { it.copy(isSubmitting = false, phase = LecturettePhase.SUBMITTED, submissionId = submissionId, isCompleted = true) }
                }
                .onFailure { e ->
                    logger.e(tag, "Failed to submit Lecturette test", e)
                    _uiState.update { it.copy(isSubmitting = false, submitError = "Failed to submit test. Please try again.") }
                }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null, validationError = null, submitError = null) }
    }

    fun dismissLimitDialog() {
        _uiState.update { it.copy(showLimitDialog = false) }
    }

    fun close() {
        timerJob?.cancel()
        scope.cancel()
    }
}

data class LecturetteTestUiState(
    val isLoading: Boolean = false,
    val loadingMessage: String? = null,
    val error: String? = null,
    val testId: String = "",
    val userId: String = "",
    val topicChoices: List<String> = emptyList(),
    val selectedTopic: String = "",
    val subscriptionType: SubscriptionType = SubscriptionType.FREE,
    val phase: LecturettePhase = LecturettePhase.INSTRUCTIONS,
    val speechStartTime: Long = 0L,
    val timeRemaining: Int = 180,
    val speechTranscript: String = "",
    val charCount: Int = 0,
    val validationError: String? = null,
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val submissionId: String? = null,
    val isCompleted: Boolean = false,
    val showLimitDialog: Boolean = false,
    val limitMessage: String? = null
) {
    val isTimeLow: Boolean get() = timeRemaining in 1..29

    val formattedTime: String
        get() {
            val minutes = timeRemaining / 60
            val seconds = timeRemaining % 60
            val secStr = if (seconds < 10) "0$seconds" else "$seconds"
            return "$minutes:$secStr"
        }
}
