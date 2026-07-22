package com.ssbmax.shared.presentation.gd

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

/**
 * GD (Group Discussion) test phases.
 */
enum class GDPhase { INSTRUCTIONS, DISCUSSION, REVIEW, SUBMITTED }

/**
 * KMP port of `app/.../ui/tests/gto/gd/GDTestUiState.kt` + `GDTestViewModel.kt`.
 * Plain-class + own-`CoroutineScope` pattern, same shape as
 * [com.ssbmax.shared.presentation.srt.SRTTestViewModel]. Real finding for
 * this session: GD genuinely enqueues a real background analysis job
 * (`GTOTestSubmissionHelper` -> `WorkManager` + `GTOAnalysisWorker`), so
 * [com.ssbmax.shared.domain.service.SubmissionAnalysisTrigger] applies --
 * fired via [GTOSubmissionCoordinator], not synchronous/rule-based scoring.
 *
 * `GTOTestEligibilityChecker`/`GTOSequentialAccessManager` (Android
 * app-layer helpers) are replaced by [GTOEligibilityChecker], which composes
 * the already-ported [com.ssbmax.shared.domain.usecase.subscription.CheckTestEligibilityUseCase]
 * with [com.ssbmax.shared.domain.repository.GTORepository.canUserTakeTest]
 * for the sequential-access check -- same real logic, no Android
 * `SecurityEventLogger` dependency.
 */
class GDTestViewModel(
    private val testContentRepository: TestContentRepository,
    private val submissionRepository: SubmissionRepository,
    private val eligibilityChecker: GTOEligibilityChecker,
    private val submissionCoordinator: GTOSubmissionCoordinator,
    private val logger: DomainLogger
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val tag = "GDTestViewModel"

    private val _uiState = MutableStateFlow(GDTestUiState())
    val uiState: StateFlow<GDTestUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    private companion object {
        const val DISCUSSION_TIME_SECONDS = 1200
        const val MIN_CHARS = 50
        const val MAX_CHARS = 1500
    }

    fun loadTest(testId: String = "gto_gd_standard") {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Checking eligibility...") }
            when (val result = eligibilityChecker.checkEligibility(TestType.GTO_GD, GTOTestType.GROUP_DISCUSSION)) {
                is GTOEligibilityChecker.Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                is GTOEligibilityChecker.Result.LimitReached -> {
                    _uiState.update { it.copy(isLoading = false, showLimitDialog = true, limitMessage = result.message) }
                }
                is GTOEligibilityChecker.Result.Eligible -> {
                    _uiState.update { it.copy(loadingMessage = "Loading topic...") }
                    testContentRepository.getRandomGDTopic()
                        .onSuccess { topic ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false, loadingMessage = null,
                                    testId = testId, userId = result.userId, topic = topic,
                                    subscriptionType = result.subscriptionType, phase = GDPhase.INSTRUCTIONS
                                )
                            }
                        }
                        .onFailure { e ->
                            logger.e(tag, "Failed to load GD topic", e)
                            _uiState.update { it.copy(isLoading = false, error = "Failed to load test topic. Please try again.") }
                        }
                }
            }
        }
    }

    fun startDiscussion() {
        _uiState.update {
            it.copy(phase = GDPhase.DISCUSSION, discussionStartTime = Clock.System.now().toEpochMilliseconds(), timeRemaining = DISCUSSION_TIME_SECONDS)
        }
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive && _uiState.value.timeRemaining > 0 && _uiState.value.phase == GDPhase.DISCUSSION) {
                delay(1000)
                _uiState.update { it.copy(timeRemaining = (it.timeRemaining - 1).coerceAtLeast(0)) }
            }
            if (_uiState.value.phase == GDPhase.DISCUSSION && _uiState.value.timeRemaining == 0) {
                proceedToReview()
            }
        }
    }

    fun onResponseChanged(newResponse: String) {
        _uiState.update { it.copy(response = newResponse, charCount = newResponse.trim().length) }
    }

    fun proceedToReview() {
        val charCount = _uiState.value.charCount
        when {
            charCount < MIN_CHARS -> _uiState.update { it.copy(validationError = "Response must be at least $MIN_CHARS characters (currently $charCount)") }
            charCount > MAX_CHARS -> _uiState.update { it.copy(validationError = "Response must not exceed $MAX_CHARS characters (currently $charCount)") }
            else -> _uiState.update { it.copy(phase = GDPhase.REVIEW, validationError = null) }
        }
    }

    fun backToDiscussion() {
        _uiState.update { it.copy(phase = GDPhase.DISCUSSION) }
        startTimer()
    }

    @OptIn(ExperimentalUuidApi::class)
    fun submitTest() {
        scope.launch {
            _uiState.update { it.copy(isSubmitting = true, submitError = null) }
            val state = _uiState.value
            val timeSpent = ((Clock.System.now().toEpochMilliseconds() - state.discussionStartTime) / 1000).toInt()
            val submission = GTOSubmission.GDSubmission(
                id = Uuid.random().toString(),
                userId = state.userId,
                testId = state.testId,
                topic = state.topic,
                response = state.response,
                charCount = state.charCount,
                submittedAt = Clock.System.now().toEpochMilliseconds(),
                timeSpent = timeSpent
            )
            submissionRepository.submitGD(submission)
                .onSuccess { submissionId ->
                    submissionCoordinator.onSubmitted(submissionId, TestType.GTO_GD, GTOTestType.GROUP_DISCUSSION, state.userId)
                    _uiState.update { it.copy(isSubmitting = false, phase = GDPhase.SUBMITTED, submissionId = submissionId, isCompleted = true) }
                }
                .onFailure { e ->
                    logger.e(tag, "Failed to submit GD test", e)
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

data class GDTestUiState(
    val isLoading: Boolean = false,
    val loadingMessage: String? = null,
    val error: String? = null,
    val testId: String = "",
    val userId: String = "",
    val topic: String = "",
    val subscriptionType: SubscriptionType = SubscriptionType.FREE,
    val phase: GDPhase = GDPhase.INSTRUCTIONS,
    val discussionStartTime: Long = 0L,
    val timeRemaining: Int = 1200,
    val response: String = "",
    val charCount: Int = 0,
    val validationError: String? = null,
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val submissionId: String? = null,
    val isCompleted: Boolean = false,
    val showLimitDialog: Boolean = false,
    val limitMessage: String? = null
) {
    val isTimeLow: Boolean get() = timeRemaining in 1..119

    val formattedTime: String
        get() {
            val minutes = timeRemaining / 60
            val seconds = timeRemaining % 60
            val secStr = if (seconds < 10) "0$seconds" else "$seconds"
            return "$minutes:$secStr"
        }
}
