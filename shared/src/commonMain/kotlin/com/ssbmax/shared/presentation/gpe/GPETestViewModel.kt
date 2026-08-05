package com.ssbmax.shared.presentation.gpe

import com.ssbmax.shared.domain.model.GPEPhase
import com.ssbmax.shared.domain.model.GPEQuestion
import com.ssbmax.shared.domain.model.SubscriptionTier
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.model.gto.GTOSubmission
import com.ssbmax.shared.domain.model.gto.GTOTestType
import com.ssbmax.shared.domain.repository.SubmissionRepository
import com.ssbmax.shared.domain.repository.TestContentRepository
import com.ssbmax.shared.domain.util.DomainLogger
import com.ssbmax.shared.presentation.gto.common.GTOEligibilityChecker
import com.ssbmax.shared.presentation.gto.common.GTOSubmissionCoordinator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * KMP port of `app/.../ui/tests/gpe/GPETestViewModel.kt`. Same
 * eligibility/submission-coordinator reuse as
 * [com.ssbmax.shared.presentation.gd.GDTestViewModel] /
 * [com.ssbmax.shared.presentation.lecturette.LecturetteTestViewModel] -- GPE
 * is confirmed (by reading `GTOTestSubmissionHelper.submitTest`) to enqueue
 * the same `GTOAnalysisWorker` via WorkManager after submission, so it uses
 * the same async-analysis seam finding as GD/Lecturette.
 *
 * Reuses the domain-model [GPEPhase] (already ported in `GPETest.kt`) rather
 * than defining a presentation-layer phase enum like GD/Lecturette do --
 * GPE's phase enum already exists as the SSOT in `core:domain`'s original,
 * so redefining it here would violate the single-definition rule.
 *
 * Two real quirks preserved faithfully from the Android original rather than
 * "fixed" during this port (out of scope -- porting behavior, not correcting
 * it):
 * - `GPEPhase.IMAGE_VIEWING` is dead in the actual runtime flow: `startTest()`
 *   jumps directly from INSTRUCTIONS to PLANNING, and `proceedToNextPhase()`'s
 *   IMAGE_VIEWING branch is unreachable (only auto-forwards if somehow
 *   reached). The Android screen also renders an empty `{}` block for that
 *   phase. So `GPEImageViewingPhase`/`GPETopBar`'s IMAGE_VIEWING branch are
 *   NOT ported here -- they are unreachable dead code in the original.
 * - The planning timer is hardcoded to 600s (10 minutes) in both `startTest()`
 *   and `proceedToNextPhase()`, despite `GPEQuestion.planningTimeSeconds`
 *   defaulting to 1740 (29 min) and the instructions copy still saying
 *   "29 minutes". This is a real, pre-existing discrepancy in the Android
 *   original, not introduced by this port.
 */
class GPETestViewModel(
    private val testContentRepository: TestContentRepository,
    private val submissionRepository: SubmissionRepository,
    private val eligibilityChecker: GTOEligibilityChecker,
    private val submissionCoordinator: GTOSubmissionCoordinator,
    private val logger: DomainLogger
) : ViewModel() {
    private val tag = "GPETestViewModel"

    private val _uiState = MutableStateFlow(GPETestUiState())
    val uiState: StateFlow<GPETestUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    private companion object {
        const val PLANNING_TIME_SECONDS = 600
    }

    fun loadTest(testId: String = "gpe_standard") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Checking eligibility...", error = null) }
            when (val result = eligibilityChecker.checkEligibility(TestType.GTO_GPE, GTOTestType.GROUP_PLANNING_EXERCISE)) {
                is GTOEligibilityChecker.Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, loadingMessage = null, error = result.message) }
                }
                is GTOEligibilityChecker.Result.LimitReached -> {
                    _uiState.update { it.copy(isLoading = false, loadingMessage = null, showLimitDialog = true, limitMessage = result.message) }
                }
                is GTOEligibilityChecker.Result.Eligible -> {
                    _uiState.update { it.copy(loadingMessage = "Fetching scenario from cloud...") }
                    testContentRepository.getGPEQuestions(testId)
                        .onSuccess { questions ->
                            val question = questions.firstOrNull()
                            if (question == null) {
                                _uiState.update { it.copy(isLoading = false, loadingMessage = null, error = "No scenarios found for this test") }
                                return@onSuccess
                            }
                            _uiState.update {
                                it.copy(
                                    isLoading = false, loadingMessage = null,
                                    testId = testId, userId = result.userId, question = question,
                                    subscriptionType = result.subscriptionType, phase = GPEPhase.INSTRUCTIONS
                                )
                            }
                        }
                        .onFailure { e ->
                            logger.e(tag, "Failed to load GPE scenario", e)
                            _uiState.update { it.copy(isLoading = false, loadingMessage = null, error = "Cloud connection required. Please check your internet connection.") }
                        }
                }
            }
        }
    }

    fun startTest() {
        val question = _uiState.value.question ?: return
        _uiState.update {
            it.copy(
                phase = GPEPhase.PLANNING,
                planningStartTime = Clock.System.now().toEpochMilliseconds(),
                timeRemaining = PLANNING_TIME_SECONDS
            )
        }
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive && _uiState.value.timeRemaining > 0 && _uiState.value.phase == GPEPhase.PLANNING) {
                delay(1000)
                _uiState.update { it.copy(timeRemaining = (it.timeRemaining - 1).coerceAtLeast(0)) }
            }
            if (_uiState.value.phase == GPEPhase.PLANNING && _uiState.value.timeRemaining == 0) {
                proceedToReview()
            }
        }
    }

    fun onPlanningResponseChanged(newResponse: String) {
        val question = _uiState.value.question
        _uiState.update {
            it.copy(
                planningResponse = newResponse,
                charactersCount = newResponse.length,
                canProceedToNextPhase = question != null && newResponse.length >= question.minCharacters
            )
        }
    }

    fun proceedToReview() {
        val state = _uiState.value
        val question = state.question ?: return
        if (state.planningResponse.length < question.minCharacters) return
        timerJob?.cancel()
        _uiState.update { it.copy(phase = GPEPhase.REVIEW) }
    }

    fun returnToPlanning() {
        _uiState.update { it.copy(phase = GPEPhase.PLANNING, timeRemaining = PLANNING_TIME_SECONDS) }
        startTimer()
    }

    @OptIn(ExperimentalUuidApi::class)
    fun submitTest() {
        timerJob?.cancel()
        val state = _uiState.value
        val question = state.question ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, submitError = null) }
            val submission = GTOSubmission.GPESubmission(
                id = Uuid.random().toString(),
                userId = state.userId,
                testId = state.testId,
                imageUrl = question.imageUrl,
                scenario = question.scenario,
                solution = question.solution,
                plan = state.planningResponse,
                characterCount = state.planningResponse.length,
                submittedAt = Clock.System.now().toEpochMilliseconds(),
                timeSpent = 30 * 60 // approx 30 min total, matches Android original's hardcoded estimate
            )
            submissionRepository.submitGPE(submission)
                .onSuccess { submissionId ->
                    submissionCoordinator.onSubmitted(submissionId, TestType.GTO_GPE, GTOTestType.GROUP_PLANNING_EXERCISE, state.userId)
                    _uiState.update { it.copy(isSubmitting = false, phase = GPEPhase.SUBMITTED, submissionId = submissionId, isCompleted = true) }
                }
                .onFailure { e ->
                    logger.e(tag, "Failed to submit GPE test", e)
                    _uiState.update { it.copy(isSubmitting = false, submitError = "Failed to submit test. Please try again.") }
                }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null, submitError = null) }
    }

    fun dismissLimitDialog() {
        _uiState.update { it.copy(showLimitDialog = false) }
    }

    override fun onCleared() {
        timerJob?.cancel()
    }
}

data class GPETestUiState(
    val isLoading: Boolean = false,
    val loadingMessage: String? = null,
    val error: String? = null,
    val testId: String = "",
    val userId: String = "",
    val question: GPEQuestion? = null,
    val subscriptionType: SubscriptionTier = SubscriptionTier.FREE,
    val phase: GPEPhase = GPEPhase.INSTRUCTIONS,
    val planningStartTime: Long = 0L,
    val timeRemaining: Int = 600,
    val planningResponse: String = "",
    val charactersCount: Int = 0,
    val canProceedToNextPhase: Boolean = false,
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val submissionId: String? = null,
    val isCompleted: Boolean = false,
    val showLimitDialog: Boolean = false,
    val limitMessage: String? = null
) {
    val formattedTime: String
        get() {
            val minutes = timeRemaining / 60
            val seconds = timeRemaining % 60
            val secStr = if (seconds < 10) "0$seconds" else "$seconds"
            return "$minutes:$secStr"
        }
}
