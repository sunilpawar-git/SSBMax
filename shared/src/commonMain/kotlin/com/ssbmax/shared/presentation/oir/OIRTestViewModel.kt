package com.ssbmax.shared.presentation.oir

import com.ssbmax.shared.domain.model.OIRAnswer
import com.ssbmax.shared.domain.model.OIRTestConfig
import com.ssbmax.shared.domain.model.OIRTestSession
import com.ssbmax.shared.domain.model.SubscriptionType
import com.ssbmax.shared.domain.model.TestEligibility
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.repository.TestContentRepository
import com.ssbmax.shared.domain.repository.TestSessionRepository
import com.ssbmax.shared.domain.repository.UserProfileRepository
import com.ssbmax.shared.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.shared.domain.usecase.oir.OIRTestScoreCalculator
import com.ssbmax.shared.domain.usecase.oir.SubmitOIRTestUseCase
import com.ssbmax.shared.domain.usecase.subscription.CheckTestEligibilityUseCase
import com.ssbmax.shared.domain.util.DomainLogger
import com.ssbmax.shared.domain.validation.OIRQuestionValidator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.random.Random

/**
 * KMP port of the Android `app/.../ui/tests/oir/OIRTestViewModel.kt`.
 *
 * Phase 1 of the KMP-convergence plan: a real `androidx.lifecycle.ViewModel`
 * using `viewModelScope`, converged with `app`'s existing 57-call-site idiom
 * and this module's own DI (`viewModelOf`) / screen (`koinViewModel()`)
 * conventions — no more manual `CoroutineScope` + `close()`.
 *
 * Deviations from the Android original, all deliberate and documented (none silent):
 * - `subscriptionManager.canTakeTest`/`TestUsageRecorder` (Android `core:data`
 *   `SubscriptionManager`) replaced by [CheckTestEligibilityUseCase] — see
 *   that use case's own doc comment for what it does and doesn't carry
 *   forward (debug bypass, Room mirror, security-event logging).
 * - `SecurityEventLogger` (Android-only: Firebase Analytics) dropped;
 *   [DomainLogger] used for the equivalent "blocked unauthenticated access"
 *   log line, same seam every other ported ViewModel in this phase uses.
 * - `MemoryLeakTracker`/`trackMemoryLeaks` (Android-only, wraps
 *   `androidx.lifecycle.ViewModel` lifecycle + `java.lang.ref.WeakReference`)
 *   dropped entirely — there is no KMP equivalent, and `viewModelScope`
 *   already makes the leak class it targeted (timer coroutine outliving the
 *   screen) structurally impossible: it is cancelled automatically in
 *   [onCleared], with no manual bookkeeping needed.
 * - `coil.ImageLoader`/`android.content.Context`-based next-question image
 *   prefetch dropped — [com.ssbmax.shared.ui.oir.components.OIRQuestionView]
 *   uses Coil3's `AsyncImage` directly with its default (already-caching)
 *   `SingletonImageLoader`; the prefetch was a perceived-latency
 *   optimization, not a correctness requirement, and re-adding it would mean
 *   injecting a platform image loader into presentation code Coil3 already
 *   makes unnecessary.
 * - `java.util.UUID.randomUUID()` (JVM-only) replaced with
 *   `kotlin.random.Random`-based ID generation (same pattern already used
 *   elsewhere in this migration for ID generation — see this plan's Phase 2
 *   note on the `String.format`/UUID-style JVM-only gotchas).
 * - `com.ssbmax.time.Clock` (Android app-local abstraction) replaced with
 *   `kotlinx.datetime.Clock.System`, matching every other ported ViewModel.
 *
 * At 311 lines this is ~11 lines over this repo's 300-line Quality Limit —
 * flagged rather than silently ignored. Not split: this is one cohesive
 * state machine (load -> answer -> navigate -> submit -> timer), same
 * shape and similar size as the Android original; splitting it across
 * files the way the UI layer's delegate-composable files do would scatter
 * a single StateFlow's mutations across multiple files rather than
 * localizing a genuinely reusable chunk, which this plan's own "simplicity
 * first" rule weighs against doing just to hit a line count.
 */
class OIRTestViewModel(
    private val testContentRepository: TestContentRepository,
    private val testSessionRepository: TestSessionRepository,
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val userProfileRepository: UserProfileRepository,
    private val checkTestEligibility: CheckTestEligibilityUseCase,
    private val scoreCalculator: OIRTestScoreCalculator,
    private val submitOIRTestUseCase: SubmitOIRTestUseCase,
    private val logger: DomainLogger
) : ViewModel() {
    private val tag = "OIRTestViewModel"

    private val _uiState = MutableStateFlow(OIRTestUiState())
    val uiState: StateFlow<OIRTestUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        loadTest()
    }

    fun loadTest(testId: String = "oir_standard") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorType = null) }
            val userId = observeCurrentUser().first()?.id ?: run {
                logger.e(tag, "SECURITY: Unauthenticated OIR test access blocked", null)
                _uiState.update { it.copy(isLoading = false, errorType = OIRErrorType.AUTH_REQUIRED) }
                return@launch
            }
            try {
                when (val eligibility = checkTestEligibility(TestType.OIR, userId)) {
                    is TestEligibility.LimitReached -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false, errorType = null, isLimitReached = true,
                                subscriptionTier = eligibility.tier, testsLimit = eligibility.limit,
                                testsUsed = eligibility.usedCount, resetsAt = eligibility.resetsAt
                            )
                        }
                        return@launch
                    }
                    is TestEligibility.NetworkError -> {
                        _uiState.update { it.copy(isLoading = false, errorType = OIRErrorType.QUESTIONS_UNAVAILABLE) }
                        return@launch
                    }
                    is TestEligibility.Eligible -> Unit
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.e(tag, "Exception checking OIR test eligibility", e)
            }
            try {
                val questions = testContentRepository.getOIRTestQuestions(count = 50, difficulty = null)
                    .getOrElse { throw it }
                    .takeIf { it.isNotEmpty() } ?: throw Exception("No questions available.")
                val validatedQuestions = OIRQuestionValidator.validateAndFilter(questions) { inv ->
                    logger.e(tag, "OIR question validation failed: ${inv.toLogString()}", null)
                }
                if (validatedQuestions.isEmpty()) throw Exception("All questions failed validation.")
                val config = OIRTestConfig()
                val newSession = OIRTestSession(
                    sessionId = newSessionId(),
                    userId = userId, testId = testId,
                    questions = validatedQuestions, answers = emptyMap(),
                    currentQuestionIndex = 0, startTime = Clock.System.now().toEpochMilliseconds(),
                    timeRemainingSeconds = config.totalTimeMinutes * 60
                )
                _uiState.update { it.copy(session = newSession) }
                updateUiFromSession()
                _uiState.update { it.copy(questionStartTimeMs = Clock.System.now().toEpochMilliseconds()) }
                startTimer()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.e(tag, "Exception loading OIR test", e)
                _uiState.update { it.copy(isLoading = false, errorType = OIRErrorType.QUESTIONS_UNAVAILABLE) }
            }
        }
    }

    fun selectOption(optionId: String) {
        val session = _uiState.value.session ?: run {
            logger.e(tag, "OIR test session null during option selection", null)
            return
        }
        val question = session.currentQuestion ?: run {
            logger.e(tag, "OIR current question null during option selection", null)
            return
        }
        val validationResult = OIRQuestionValidator.validate(question)
        if (!validationResult.isValid) {
            logger.e(tag, "OIR question runtime validation failed: ${validationResult.toLogString()}", null)
        }
        val timeTaken = ((Clock.System.now().toEpochMilliseconds() - _uiState.value.questionStartTimeMs) / 1000L)
            .toInt().coerceAtLeast(0)
        val current = _uiState.value.selectedOptionIds
        val updated = when {
            !question.isMultiSelect -> setOf(optionId)
            optionId in current -> current - optionId
            current.size >= 2 -> current
            else -> current + optionId
        }
        val isAnswerCorrect = if (question.isMultiSelect) updated == question.correctAnswerIds.toSet()
        else updated.singleOrNull() == question.correctAnswerId
        val answer = OIRAnswer(
            questionId = question.id,
            selectedOptionId = updated.singleOrNull(),
            selectedOptionIds = updated,
            timeTakenSeconds = timeTaken,
            skipped = false
        )
        _uiState.update { state ->
            state.copy(
                session = session.copy(answers = session.answers + (question.id to answer)),
                selectedOptionIds = updated,
                showFeedback = true,
                isCurrentAnswerCorrect = isAnswerCorrect,
                currentQuestionAnswered = true
            )
        }
    }

    fun nextQuestion() {
        val session = _uiState.value.session ?: return
        if (session.currentQuestionIndex < session.questions.size - 1) {
            _uiState.update { it.copy(session = session.copy(currentQuestionIndex = session.currentQuestionIndex + 1)) }
            updateUiFromSession()
            _uiState.update { it.copy(questionStartTimeMs = Clock.System.now().toEpochMilliseconds()) }
        }
    }

    fun previousQuestion() {
        val session = _uiState.value.session ?: return
        if (session.currentQuestionIndex > 0) {
            _uiState.update { it.copy(session = session.copy(currentQuestionIndex = session.currentQuestionIndex - 1)) }
            updateUiFromSession()
            _uiState.update { it.copy(questionStartTimeMs = Clock.System.now().toEpochMilliseconds()) }
        }
    }

    fun submitTest() {
        _uiState.update { it.copy(isTimerActive = false) }
        timerJob?.cancel()
        val session = _uiState.value.session ?: run {
            logger.e(tag, "OIR test session null during test submission", null)
            return
        }
        viewModelScope.launch {
            try {
                val subscriptionType = userProfileRepository.getUserProfile(session.userId).first()
                    .getOrNull()?.subscriptionType ?: SubscriptionType.FREE
                val submissionId = submitOIRTestUseCase(session).getOrThrow()
                // Note: served questions are marked used inside SubmitOIRTestUseCase --
                // the single source of truth for submission orchestration. Do NOT mark
                // them again here (that caused a duplicate write per submit on Android).
                testContentRepository.clearCache()
                _uiState.update {
                    it.copy(
                        session = session.copy(isCompleted = true),
                        isCompleted = true, sessionId = submissionId,
                        subscriptionType = subscriptionType,
                        testResult = scoreCalculator.calculate(session)
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.e(tag, "OIR test submission failed", e)
                _uiState.update { it.copy(errorType = OIRErrorType.SUBMIT_FAILED) }
            }
        }
    }

    fun pauseTest() {
        val session = _uiState.value.session ?: return
        _uiState.update { it.copy(isTimerActive = false, session = session.copy(isPaused = true)) }
        timerJob?.cancel()
        viewModelScope.launch {
            testSessionRepository.endTestSession(session.sessionId)
        }
    }

    private fun startTimer() {
        _uiState.update { it.copy(isTimerActive = true, timerStartTime = Clock.System.now().toEpochMilliseconds()) }
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            try {
                while (isActive && _uiState.value.isTimerActive &&
                    _uiState.value.timeRemainingSeconds > 0 && !_uiState.value.isCompleted
                ) {
                    delay(1000)
                    if (!isActive || !_uiState.value.isTimerActive) break
                    val newTime = _uiState.value.timeRemainingSeconds - 1
                    _uiState.update { state ->
                        state.copy(timeRemainingSeconds = newTime, session = state.session?.copy(timeRemainingSeconds = newTime))
                    }
                    if (newTime == 0 && isActive && _uiState.value.isTimerActive) submitTest()
                }
            } catch (e: CancellationException) {
                throw e
            } finally {
                _uiState.update { it.copy(isTimerActive = false) }
            }
        }
    }

    private fun updateUiFromSession() {
        val session = _uiState.value.session ?: return
        val currentQuestion = session.currentQuestion
        if (currentQuestion == null) {
            logger.e(tag, "OIR test data inconsistency: currentQuestion is null at index ${session.currentQuestionIndex}/${session.questions.size}", null)
            _uiState.update { it.copy(isLoading = false, errorType = OIRErrorType.INVALID_QUESTION) }
            return
        }
        val existingAnswer = session.answers[currentQuestion.id]
        _uiState.update {
            it.copy(
                isLoading = false,
                errorType = null,
                currentQuestion = currentQuestion,
                currentQuestionIndex = session.currentQuestionIndex,
                totalQuestions = session.questions.size,
                timeRemainingSeconds = session.timeRemainingSeconds,
                selectedOptionIds = existingAnswer?.selectedOptionIds ?: emptySet(),
                showFeedback = existingAnswer != null,
                isCurrentAnswerCorrect = existingAnswer?.isCorrect ?: false,
                currentQuestionAnswered = existingAnswer != null
            )
        }
    }

    private fun newSessionId(): String =
        "oir_${Clock.System.now().toEpochMilliseconds()}_${Random.nextInt(100000, 999999)}"

    override fun onCleared() {
        timerJob?.cancel()
    }
}
