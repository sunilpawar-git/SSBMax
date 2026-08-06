package com.ssbmax.shared.presentation.oir

import com.ssbmax.shared.domain.model.OIRAnswer
import com.ssbmax.shared.domain.model.OIRTestConfig
import com.ssbmax.shared.domain.model.OIRTestSession
import com.ssbmax.shared.domain.model.SubscriptionTier
import com.ssbmax.shared.domain.model.TestEligibility
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.repository.TestContentRepository
import com.ssbmax.shared.domain.repository.TestSessionRepository
import com.ssbmax.shared.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.shared.domain.usecase.oir.OIRTestScoreCalculator
import com.ssbmax.shared.domain.usecase.oir.SubmitOIRTestUseCase
import com.ssbmax.shared.domain.usecase.subscription.CheckTestEligibilityUseCase
import com.ssbmax.shared.domain.usecase.subscription.GetSubscriptionTierUseCase
import com.ssbmax.shared.domain.util.AnalyticsTracker
import com.ssbmax.shared.domain.util.DomainLogger
import com.ssbmax.shared.domain.util.SecurityEvents
import com.ssbmax.shared.domain.validation.OIRQuestionValidator
import com.ssbmax.shared.domain.validation.OIRTestQuestionSetValidator
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
import kotlin.time.Clock

/** Coordinates eligibility, question readiness, answering, timing, and submission for OIR. */
class OIRTestViewModel(
    private val testContentRepository: TestContentRepository,
    private val testSessionRepository: TestSessionRepository,
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val checkTestEligibility: CheckTestEligibilityUseCase,
    private val getSubscriptionTier: GetSubscriptionTierUseCase,
    private val scoreCalculator: OIRTestScoreCalculator,
    private val submitOIRTestUseCase: SubmitOIRTestUseCase,
    private val logger: DomainLogger,
    private val analyticsTracker: AnalyticsTracker
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
                analyticsTracker.trackEvent(SecurityEvents.UNAUTHENTICATED_ACCESS, mapOf("test_type" to "OIR"))
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
                _uiState.update { it.copy(isLoading = false, errorType = OIRErrorType.QUESTIONS_UNAVAILABLE) }
                return@launch
            }
            var createdSessionId: String? = null
            try {
                val config = OIRTestConfig(testId = testId)
                val sessionId = testSessionRepository
                    .createTestSession(userId, testId, TestType.OIR)
                    .getOrElse {
                        logger.e(tag, "Failed to create durable OIR test session", it)
                        _uiState.update { state ->
                            state.copy(isLoading = false, errorType = OIRErrorType.SESSION_UNAVAILABLE)
                        }
                        return@launch
                    }
                createdSessionId = sessionId
                val questions = testContentRepository.getOIRTestQuestions(
                    count = config.totalQuestions,
                    difficulty = null
                ).getOrElse { throw it }
                val validatedQuestions = OIRQuestionValidator.validateAndFilter(questions) { inv ->
                    logger.e(tag, "OIR question validation failed: ${inv.toLogString()}", null)
                }
                OIRTestQuestionSetValidator.validate(validatedQuestions, config).getOrElse { throw it }
                val newSession = OIRTestSession(
                    sessionId = sessionId,
                    userId = userId, testId = testId,
                    questions = validatedQuestions, answers = emptyMap(),
                    currentQuestionIndex = 0, startTime = Clock.System.now().toEpochMilliseconds(),
                    timeRemainingSeconds = config.totalTimeMinutes * 60,
                    expiresAt = Clock.System.now().toEpochMilliseconds() + config.totalTimeMinutes * 60_000L
                )
                _uiState.update { it.copy(session = newSession) }
                updateUiFromSession()
                _uiState.update { it.copy(questionStartTimeMs = Clock.System.now().toEpochMilliseconds()) }
                startTimer()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                createdSessionId?.let { testSessionRepository.abandonTestSession(it) }
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
        if (_uiState.value.isSubmitting || _uiState.value.isCompleted) return
        _uiState.update { it.copy(isTimerActive = false, isSubmitting = true) }
        timerJob?.cancel()
        val session = _uiState.value.session ?: run {
            logger.e(tag, "OIR test session null during test submission", null)
            return
        }
        viewModelScope.launch {
            try {
                val subscriptionType = getSubscriptionTier(session.userId).getOrDefault(SubscriptionTier.FREE)
                val submissionId = submitOIRTestUseCase(session).getOrThrow()
                // Note: served questions are marked used inside SubmitOIRTestUseCase --
                // the single source of truth for submission orchestration. Do NOT mark
                // them again here (that caused a duplicate write per submit on Android).
                testContentRepository.clearCache()
                _uiState.update {
                    it.copy(
                        session = session.copy(isCompleted = true),
                        isSubmitting = false,
                        isCompleted = true, sessionId = submissionId,
                        subscriptionType = subscriptionType,
                        testResult = scoreCalculator.calculate(session)
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                testSessionRepository.expireTestSession(session.sessionId)
                logger.e(tag, "OIR test submission failed", e)
                _uiState.update { it.copy(isSubmitting = false, errorType = OIRErrorType.SUBMIT_FAILED) }
            }
        }
    }

    fun pauseTest() {
        val session = _uiState.value.session ?: return
        _uiState.update { it.copy(isTimerActive = false, session = session.copy(isPaused = true)) }
        timerJob?.cancel()
        viewModelScope.launch {
            testSessionRepository.abandonTestSession(session.sessionId)
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
                    val session = _uiState.value.session ?: return@launch
                    val absoluteRemaining = ((session.expiresAt - Clock.System.now().toEpochMilliseconds()) / 1000L)
                        .toInt().coerceAtLeast(0)
                    // The local tick keeps virtual/test clocks deterministic; the absolute
                    // expiry is the upper bound and prevents background drift extending a test.
                    val newTime = minOf(_uiState.value.timeRemainingSeconds - 1, absoluteRemaining)
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


    override fun onCleared() {
        timerJob?.cancel()
    }
}
