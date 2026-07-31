package com.ssbmax.shared.presentation.piq

import com.ssbmax.shared.domain.model.SubmissionStatus
import com.ssbmax.shared.domain.model.TestEligibility
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.repository.SubmissionRepository
import com.ssbmax.shared.domain.repository.TestUsageRecorder
import com.ssbmax.shared.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.shared.domain.usecase.subscription.CheckTestEligibilityUseCase
import com.ssbmax.shared.domain.util.DomainLogger
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock

/**
 * KMP port of `app/.../ui/tests/piq/PIQTestViewModel.kt`. A real
 * `androidx.lifecycle.ViewModel` using `viewModelScope`, same shape as
 * [com.ssbmax.shared.presentation.sdt.SDTTestViewModel] (see that file for the
 * fuller writeup of shared deviations: `SubscriptionManager` ->
 * [CheckTestEligibilityUseCase]; `System.currentTimeMillis()` ->
 * `kotlinx.datetime.Clock.System`).
 *
 * **Real finding, stated per this session's brief: PIQ does NOT use the
 * [com.ssbmax.shared.domain.service.SubmissionAnalysisTrigger] seam.** Unlike
 * every prior vertical (OIR/PPDT/TAT/WAT/SRT/SDT), PIQ is reference data for
 * assessors, not an AI-graded test -- its "AI quality score"
 * ([com.ssbmax.shared.domain.model.PIQAIScore], see [buildMockPIQAIScore]) is a
 * synchronous, purely local mock computation, not a real Gemini call and not
 * enqueued via any background worker.
 *
 * **Deliberately dropped, confirmed by reading the real Android
 * `PIQTestViewModel.submitTest()`:** `triggerBackgroundQuestionGeneration()`,
 * which enqueues `InterviewQuestionGenerationWorker` via `WorkManager` to
 * pre-generate Interview-module questions from the submitted PIQ data. This is
 * a genuine WorkManager background job (plan's blocker #4 -- needs its own
 * `expect`/`actual` shim) but it belongs to the not-yet-ported Interview
 * vertical's infrastructure, not to PIQ's own submission contract; dropping it
 * here means a freshly-submitted PIQ won't have pre-warmed interview questions
 * on this KMP build until the Interview vertical is ported with its own
 * background-task shim. `getOLQDashboard.invalidateCache()` is also dropped,
 * same precedent as WAT/SRT/SDT (none of those invalidate the dashboard cache
 * on submit either). `DifficultyProgressionManager.recordPerformance` and
 * `SecurityEventLogger` are dropped, same precedent as every other ported
 * vertical (local analytics / Android-only security telemetry, not part of
 * the submission contract).
 *
 * Structurally distinct from every timed test ported so far: two freely
 * navigable pages (no forward-only phase machine), continuous 2-second-debounce
 * autosave-as-draft (`DRAFT` status) rather than a single terminal submit, and
 * `canSubmit` is always `true` (every field optional, matches Android's lenient
 * form). [SubmissionRepository.submitPIQ] is called directly (no
 * `SubmitPIQTestUseCase` exists in `core:domain`/`shared` -- confirmed by
 * search -- the Android original also calls the repository directly).
 *
 * `UserProfileRepository`/`subscriptionType` also dropped: the Android
 * original fetches the user's profile mid-submit but the resulting
 * `subscriptionType` local is never read afterward (PIQ's own
 * `onNavigateToResult(submissionId)` callback doesn't need it, unlike SDT's
 * `onTestComplete(submissionId, subscriptionType)`) -- genuinely dead code in
 * the Android original, not ported.
 */
@OptIn(FlowPreview::class)
class PIQTestViewModel(
    private val submissionRepository: SubmissionRepository,
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val checkTestEligibility: CheckTestEligibilityUseCase,
    private val usageRecorder: TestUsageRecorder,
    private val logger: DomainLogger
) : ViewModel() {
    private val tag = "PIQTestViewModel"

    private val _uiState = MutableStateFlow(PIQTestUiState())
    val uiState: StateFlow<PIQTestUiState> = _uiState.asStateFlow()

    private val autoSaveChannel = Channel<Unit>(Channel.CONFLATED)
    private var autoSaveJob: Job? = null
    private var capturedUserId: String? = null

    /** Called once from the screen's `LaunchedEffect(testId)`, mirrors the
     *  Android original's `init {}` block (`loadDraft()` + `setupAutoSave()`). */
    fun initialize(testId: String) {
        _uiState.update { it.copy(testId = testId) }
        setupAutoSave()
        loadDraft()
    }

    private fun setupAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            autoSaveChannel.receiveAsFlow()
                .debounce(2000L)
                .collect { saveDraft() }
        }
    }

    /** Loads the OIR-number auto-fill; unlike every other ported vertical, an
     *  unauthenticated user does NOT block the form -- matches Android's
     *  original behavior of silently starting a blank form. */
    private fun loadDraft() {
        viewModelScope.launch {
            val userId = withTimeoutOrNull(3000L) { observeCurrentUser().first()?.id }
            if (userId == null) {
                logger.w(tag, "No user logged in, skipping draft load")
                return@launch
            }
            capturedUserId = userId
            loadOIRNumber(userId)
        }
    }

    private suspend fun loadOIRNumber(userId: String) {
        submissionRepository.getUserSubmissionsByTestType(userId = userId, testType = TestType.OIR, limit = 1)
            .onSuccess { submissions ->
                val submissionId = submissions.firstOrNull()?.get("id") as? String ?: ""
                if (submissionId.isNotBlank()) {
                    _uiState.update { state ->
                        state.copy(answers = state.answers + ("oirNumber" to submissionId))
                    }
                }
            }
            .onFailure { e ->
                if (e is CancellationException) throw e
                logger.e(tag, "Failed to load OIR number for PIQ form", e)
            }
    }

    fun updateField(fieldId: String, value: String) {
        _uiState.update { state ->
            state.copy(
                answers = state.answers + (fieldId to value),
                lastModifiedAt = Clock.System.now().toEpochMilliseconds()
            )
        }
        autoSaveChannel.trySend(Unit)
    }

    fun navigateToPage(page: com.ssbmax.shared.domain.model.PIQPage) {
        _uiState.update { it.copy(currentPage = page) }
    }

    fun goToReview() {
        _uiState.update { it.copy(showReviewScreen = true) }
    }

    fun editPage(page: com.ssbmax.shared.domain.model.PIQPage) {
        _uiState.update { it.copy(showReviewScreen = false, currentPage = page) }
    }

    private fun saveDraft() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.isSaving || state.answers.isEmpty()) return@launch

            val userId = capturedUserId ?: withTimeoutOrNull(3000L) { observeCurrentUser().first()?.id }
            if (userId == null) {
                logger.w(tag, "Cannot save draft - user not logged in")
                return@launch
            }

            _uiState.update { it.copy(isSaving = true) }
            val submission = buildPIQSubmission(state, userId, SubmissionStatus.DRAFT)

            submissionRepository.submitPIQ(submission)
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false, lastSavedAt = Clock.System.now().toEpochMilliseconds()) }
                }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    logger.e(tag, "Failed to save PIQ draft for test: ${state.testId}", e)
                    _uiState.update { it.copy(isSaving = false) }
                }
        }
    }

    fun submitTest() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val state = _uiState.value
            val userId = capturedUserId ?: withTimeoutOrNull(3000L) { observeCurrentUser().first()?.id }
            if (userId == null) {
                logger.e(tag, "Unauthenticated PIQ submission blocked", null)
                _uiState.update { it.copy(isLoading = false, error = "Please login to submit PIQ") }
                return@launch
            }

            when (val eligibility = checkTestEligibility(TestType.PIQ, userId)) {
                is TestEligibility.LimitReached -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false, isLimitReached = true,
                            subscriptionTier = eligibility.tier, testsLimit = eligibility.limit,
                            testsUsed = eligibility.usedCount, resetsAt = eligibility.resetsAt
                        )
                    }
                    return@launch
                }
                is TestEligibility.NetworkError -> {
                    _uiState.update { it.copy(isLoading = false, error = "No connection. Please check your network and try again.") }
                    return@launch
                }
                is TestEligibility.Eligible -> Unit
            }

            val submission = buildPIQSubmission(state, userId, SubmissionStatus.SUBMITTED_PENDING_REVIEW)
            val aiScore = buildMockPIQAIScore(submission)
            val submissionWithAI = submission.copy(aiPreliminaryScore = aiScore)

            submissionRepository.submitPIQ(submissionWithAI, batchId = null)
                .onSuccess { submissionId ->
                    usageRecorder.recordTestUsage(TestType.PIQ, userId, submissionId)
                    _uiState.update {
                        it.copy(isLoading = false, submissionComplete = true, submissionId = submissionId)
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    logger.e(tag, "Failed to submit PIQ test for user: $userId", error)
                    _uiState.update { it.copy(isLoading = false, error = "Failed to submit: ${error.message}") }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        autoSaveJob?.cancel()
    }
}
