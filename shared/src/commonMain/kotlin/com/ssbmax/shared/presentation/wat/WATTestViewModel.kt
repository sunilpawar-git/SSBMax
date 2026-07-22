package com.ssbmax.shared.presentation.wat

import com.ssbmax.shared.domain.model.SubscriptionType
import com.ssbmax.shared.domain.model.TestEligibility
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.model.WATPhase
import com.ssbmax.shared.domain.model.WATSubmission
import com.ssbmax.shared.domain.model.WATTestConfig
import com.ssbmax.shared.domain.model.WATWordResponse
import com.ssbmax.shared.domain.model.scoring.AnalysisStatus
import com.ssbmax.shared.domain.repository.TestContentRepository
import com.ssbmax.shared.domain.repository.TestSessionRepository
import com.ssbmax.shared.domain.repository.TestUsageRecorder
import com.ssbmax.shared.domain.repository.UserProfileRepository
import com.ssbmax.shared.domain.service.SubmissionAnalysisTrigger
import com.ssbmax.shared.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.shared.domain.usecase.submission.SubmitWATTestUseCase
import com.ssbmax.shared.domain.usecase.subscription.CheckTestEligibilityUseCase
import com.ssbmax.shared.domain.util.DomainLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/**
 * KMP port of `app/.../ui/tests/wat/WATTestViewModel.kt`.
 *
 * Plain-class + own-`CoroutineScope` pattern, same shape as
 * [com.ssbmax.shared.presentation.tat.TATTestViewModel] (see that file for
 * the fuller writeup of shared deviations: `WorkManager` -> [SubmissionAnalysisTrigger]
 * -- a submission persists but is NOT AI-analyzed until a real platform
 * trigger exists; `SubscriptionManager` -> [CheckTestEligibilityUseCase];
 * `DifficultyProgressionManager`/`MemoryLeakTracker`/`SecurityEventLogger`
 * dropped, no KMP port needed; `System.currentTimeMillis()` ->
 * `kotlinx.datetime.Clock.System`).
 *
 * Real finding for this session (per the task brief): WAT genuinely DOES use
 * the async-analysis seam -- the Android original enqueues `WATAnalysisWorker`
 * (WorkManager) after a successful submission, exactly like TAT/PPDT's
 * `TATAnalysisPipelineOrchestrator`/`WorkManager` calls. It is NOT
 * synchronous/rule-based scoring. So [SubmissionAnalysisTrigger] applies here
 * unchanged, same as TAT/PPDT.
 *
 * Also unlike TAT: WAT has no gender-tag/profile-completeness gate in the
 * Android original (`WATTestViewModel.loadTest` calls
 * `testContentRepository.getWATQuestions(testId)` directly, no
 * `userProfileRepository` read before loading) -- so no `LoadWATTestUseCase`
 * was introduced; this ViewModel calls [TestSessionRepository]/
 * [TestContentRepository] directly, one call fewer than TAT's
 * `LoadTATTestUseCase` wraps.
 *
 * Structurally simpler than TAT's 12-picture/3-phase-per-question flow, as
 * expected from reading the real Android state machine first: one phase
 * (`IN_PROGRESS`) covers 60 words x a single 15s writing window each, no
 * separate viewing phase. The single `startWordTimer()`/`timerJob` here is
 * TAT's two-timer (`startViewingTimer`/`startWritingTimer`) collapsed to one,
 * but keeps the exact same generation-token discipline
 * (`docs/architecture/TAT_Pipeline.md` §5) so a stale timer's `finally` block
 * can't clobber `isTimerActive` after a newer timer already took over --
 * relevant here too since `startTest()` and the auto-skip path both restart
 * the timer.
 *
 * This ViewModel builds its own [WATSubmission] and calls
 * [UserProfileRepository]/[TestUsageRecorder] directly (matching the Android
 * original's own `submitTest()` shape) -- [SubmitWATTestUseCase] stays the
 * thin pass-through it already was (no widening attempted, since TAT's
 * session already demonstrated that would only benefit a PPDT-shaped call
 * site).
 */
class WATTestViewModel(
    private val testContentRepository: TestContentRepository,
    private val testSessionRepository: TestSessionRepository,
    private val submitWATTest: SubmitWATTestUseCase,
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val checkTestEligibility: CheckTestEligibilityUseCase,
    private val userProfileRepository: UserProfileRepository,
    private val usageRecorder: TestUsageRecorder,
    private val analysisTrigger: SubmissionAnalysisTrigger,
    private val logger: DomainLogger
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val tag = "WATTestViewModel"

    private val _uiState = MutableStateFlow(WATTestUiState())
    val uiState: StateFlow<WATTestUiState> = _uiState.asStateFlow()

    private var timerGeneration = 0L
    private var timerJob: Job? = null
    private var capturedUserId: String? = null

    fun loadTest(testId: String = "wat_standard") {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Checking eligibility...", error = null) }

            val userId = observeCurrentUser().first()?.id ?: run {
                logger.e(tag, "SECURITY: Unauthenticated WAT test access blocked", null)
                _uiState.update {
                    it.copy(isLoading = false, loadingMessage = null, error = "Authentication required. Please login to continue.")
                }
                return@launch
            }
            capturedUserId = userId

            when (val eligibility = checkTestEligibility(TestType.WAT, userId)) {
                is TestEligibility.LimitReached -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false, loadingMessage = null, error = null,
                            isLimitReached = true, subscriptionTier = eligibility.tier,
                            testsLimit = eligibility.limit, testsUsed = eligibility.usedCount,
                            resetsAt = eligibility.resetsAt
                        )
                    }
                    return@launch
                }
                is TestEligibility.NetworkError -> {
                    _uiState.update { it.copy(isLoading = false, loadingMessage = null, error = "No connection. Please check your network and try again.") }
                    return@launch
                }
                is TestEligibility.Eligible -> Unit
            }

            _uiState.update { it.copy(loadingMessage = "Fetching questions from cloud...") }

            testSessionRepository.createTestSession(userId, testId, TestType.WAT)
                .onFailure { e ->
                    logger.e(tag, "Failed to create WAT test session: $testId", e)
                    _uiState.update { it.copy(isLoading = false, loadingMessage = null, error = "Cloud connection required. Please check your internet connection.") }
                    return@launch
                }

            testContentRepository.getWATQuestions(testId)
                .onSuccess { words ->
                    if (words.isEmpty()) {
                        logger.e(tag, "No WAT words found for test: $testId", null)
                        _uiState.update { it.copy(isLoading = false, loadingMessage = null, error = "Cloud connection required. Please check your internet connection.") }
                        return@onSuccess
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false, loadingMessage = null,
                            testId = testId, words = words, config = WATTestConfig(),
                            phase = WATPhase.INSTRUCTIONS
                        )
                    }
                }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    logger.e(tag, "Failed to load WAT test: $testId", e)
                    _uiState.update { it.copy(isLoading = false, loadingMessage = null, error = "Cloud connection required. Please check your internet connection.") }
                }
        }
    }

    fun startTest() {
        _uiState.update {
            it.copy(phase = WATPhase.IN_PROGRESS, currentWordIndex = 0, startTime = Clock.System.now().toEpochMilliseconds())
        }
        startWordTimer()
    }

    fun updateResponse(response: String) {
        val maxLength = _uiState.value.config?.maxResponseLength ?: 150
        if (response.length <= maxLength) {
            _uiState.update { it.copy(currentResponse = response) }
        }
    }

    fun submitResponse() {
        val state = _uiState.value
        val currentWord = state.currentWord ?: return
        val response = WATWordResponse(
            wordId = currentWord.id,
            word = currentWord.word,
            response = state.currentResponse,
            timeTakenSeconds = (state.config?.timePerWordSeconds ?: 15) - state.timeRemaining,
            submittedAt = Clock.System.now().toEpochMilliseconds(),
            isSkipped = false
        )
        _uiState.update { it.copy(responses = it.responses + response) }
        moveToNextWord()
    }

    fun skipWord() {
        val state = _uiState.value
        val currentWord = state.currentWord ?: return
        val response = WATWordResponse(
            wordId = currentWord.id,
            word = currentWord.word,
            response = "",
            timeTakenSeconds = 0,
            submittedAt = Clock.System.now().toEpochMilliseconds(),
            isSkipped = true
        )
        _uiState.update { it.copy(responses = it.responses + response) }
        moveToNextWord()
    }

    private fun moveToNextWord() {
        val state = _uiState.value
        _uiState.update { it.copy(currentResponse = "") }
        if (state.currentWordIndex < state.words.size - 1) {
            _uiState.update { it.copy(currentWordIndex = state.currentWordIndex + 1) }
            startWordTimer()
        } else {
            _uiState.update { it.copy(isTimerActive = false, phase = WATPhase.COMPLETED) }
            submitTest()
        }
    }

    private fun submitTest() {
        _uiState.update { it.copy(isLoading = true) }
        val state = _uiState.value
        scope.launch {
            val userId = capturedUserId ?: observeCurrentUser().first()?.id
            if (userId == null) {
                logger.e(tag, "Unauthenticated WAT submission blocked", null)
                _uiState.update { it.copy(isLoading = false, error = "Please login to submit test") }
                return@launch
            }

            val subscriptionType = userProfileRepository.getUserProfile(userId).first()
                .getOrNull()?.subscriptionType ?: SubscriptionType.FREE

            val totalTimeTakenMinutes = if (state.startTime > 0) {
                ((Clock.System.now().toEpochMilliseconds() - state.startTime) / 60000).toInt()
            } else 0

            val submission = WATSubmission(
                userId = userId,
                testId = state.testId,
                responses = state.responses,
                totalTimeTakenMinutes = totalTimeTakenMinutes,
                submittedAt = Clock.System.now().toEpochMilliseconds(),
                analysisStatus = AnalysisStatus.PENDING_ANALYSIS,
                olqResult = null
            )

            submitWATTest(submission, batchId = null)
                .onSuccess { submissionId ->
                    analysisTrigger.trigger(TestType.WAT, submissionId)
                    usageRecorder.recordTestUsage(TestType.WAT, userId, submissionId)
                    _uiState.update {
                        it.copy(
                            isLoading = false, isSubmitted = true, submissionId = submissionId,
                            subscriptionType = subscriptionType, submission = submission,
                            phase = WATPhase.SUBMITTED
                        )
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    logger.e(tag, "Failed to submit WAT test for user: $userId", error)
                    _uiState.update { it.copy(isLoading = false, error = "Failed to submit: ${error.message}") }
                }
        }
    }

    private fun startWordTimer() {
        timerJob?.cancel()
        val myGeneration = ++timerGeneration
        val timePerWord = _uiState.value.config?.timePerWordSeconds ?: 15
        _uiState.update { it.copy(timeRemaining = timePerWord, isTimerActive = true, timerStartTime = myGeneration) }
        val endTime = Clock.System.now().toEpochMilliseconds() + (timePerWord * 1000L)
        timerJob = scope.launch {
            try {
                while (isActive) {
                    val remaining = ((endTime - Clock.System.now().toEpochMilliseconds()) / 1000).toInt()
                    if (remaining <= 0) break
                    _uiState.update { it.copy(timeRemaining = remaining) }
                    delay(200)
                }
                if (isActive) {
                    skipWord()
                }
            } finally {
                _uiState.update { c -> if (c.timerStartTime == myGeneration) c.copy(isTimerActive = false) else c }
            }
        }
    }

    fun close() {
        timerJob?.cancel()
        scope.cancel()
    }
}
