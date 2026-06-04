package com.ssbmax.ui.tests.oir

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.ssbmax.R
import com.ssbmax.core.data.security.SecurityEventLogger
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.TestContentRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.core.domain.usecase.oir.OIRTestScoreCalculator
import com.ssbmax.core.domain.usecase.oir.SubmitOIRTestUseCase
import com.ssbmax.core.domain.validation.OIRQuestionValidator
import com.ssbmax.core.data.util.MemoryLeakTracker
import com.ssbmax.core.data.util.trackMemoryLeaks
import com.ssbmax.utils.ErrorLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
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
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for OIR Test Screen
 * Loads test questions from cloud via TestContentRepository
 */
@HiltViewModel
class OIRTestViewModel @Inject constructor(
    private val testContentRepository: TestContentRepository,
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val userProfileRepository: com.ssbmax.core.domain.repository.UserProfileRepository,
    private val subscriptionManager: com.ssbmax.core.data.repository.SubscriptionManager,
    private val securityLogger: SecurityEventLogger,
    private val scoreCalculator: OIRTestScoreCalculator,
    private val submitOIRTestUseCase: SubmitOIRTestUseCase,
    @ApplicationContext private val appContext: Context,
    private val imageLoader: ImageLoader
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(OIRTestUiState())
    val uiState: StateFlow<OIRTestUiState> = _uiState.asStateFlow()

    init {
        trackMemoryLeaks("OIRTestViewModel")
        loadTest()
        restoreTimerIfNeeded()
    }

    private fun restoreTimerIfNeeded() {
        viewModelScope.launch {
            val state = _uiState.value
            if (!state.isLoading && !state.isCompleted &&
                state.timeRemainingSeconds > 0 && state.totalQuestions > 0 && !state.isTimerActive) {
                startTimer()
            }
        }
    }

    private suspend fun checkTestEligibility(userId: String): com.ssbmax.core.data.repository.TestEligibility =
        subscriptionManager.canTakeTest(TestType.OIR, userId)

    fun loadTest(testId: String = "oir_standard") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorResId = null) }
            val userId = observeCurrentUser().first()?.id ?: run {
                ErrorLogger.log(Exception("Unauthenticated OIR test access"), "SECURITY: Unauthenticated test access attempt blocked")
                securityLogger.logUnauthenticatedAccess(testType = TestType.OIR, context = "OIRTestViewModel.loadTest")
                _uiState.update { it.copy(isLoading = false, errorResId = R.string.oir_error_auth_required) }
                return@launch
            }
            try {
                when (val eligibility = checkTestEligibility(userId)) {
                    is com.ssbmax.core.data.repository.TestEligibility.LimitReached -> {
                        _uiState.update { it.copy(
                            isLoading = false, errorResId = null, isLimitReached = true,
                            subscriptionTier = eligibility.tier, testsLimit = eligibility.limit,
                            testsUsed = eligibility.usedCount, resetsAt = eligibility.resetsAt
                        ) }
                        return@launch
                    }
                    is com.ssbmax.core.data.repository.TestEligibility.Eligible -> Unit
                }
            } catch (e: Exception) {
                ErrorLogger.log(e, "Exception checking OIR test eligibility")
            }
            try {
                val questions = testContentRepository.getOIRTestQuestions(count = 50, difficulty = null)
                    .getOrElse { throw it }
                    .takeIf { it.isNotEmpty() } ?: throw Exception("No questions available.")
                val validatedQuestions = OIRQuestionValidator.validateAndFilter(questions) { inv ->
                    ErrorLogger.log(Exception("Invalid OIR question: ${inv.questionId}"), "OIR question validation failed: ${inv.toLogString()}")
                }
                if (validatedQuestions.isEmpty()) throw Exception("All questions failed validation.")
                val config = OIRTestConfig()
                val newSession = OIRTestSession(
                    sessionId = UUID.randomUUID().toString(),
                    userId = userId, testId = testId,
                    questions = validatedQuestions, answers = emptyMap(),
                    currentQuestionIndex = 0, startTime = System.currentTimeMillis(),
                    timeRemainingSeconds = config.totalTimeMinutes * 60
                )
                _uiState.update { it.copy(session = newSession) }
                updateUiFromSession()
                startTimer()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ErrorLogger.log(e, "Exception loading OIR test")
                _uiState.update { it.copy(isLoading = false, errorResId = R.string.oir_error_load_failed) }
            }
        }
    }

    fun selectOption(optionId: String) {
        val session = _uiState.value.session ?: run {
            ErrorLogger.log(Exception("Session is null in selectOption"), "OIR test session null during option selection")
            return
        }
        val question = session.currentQuestion ?: run {
            ErrorLogger.log(Exception("Current question is null in selectOption"), "OIR current question null during option selection")
            return
        }
        val validationResult = OIRQuestionValidator.validate(question)
        if (!validationResult.isValid) {
            ErrorLogger.log(Exception("Invalid question during runtime validation: ${question.id}"), "OIR question runtime validation failed: ${validationResult.toLogString()}")
        }
        val answer = OIRAnswer(
            questionId = question.id,
            selectedOptionId = optionId,
            isCorrect = optionId == question.correctAnswerId,
            timeTakenSeconds = 60 - _uiState.value.timeRemainingSeconds,
            skipped = false
        )
        _uiState.update { state ->
            state.copy(
                session = session.copy(answers = session.answers + (question.id to answer)),
                selectedOptionId = optionId,
                showFeedback = true,
                isCurrentAnswerCorrect = answer.isCorrect,
                currentQuestionAnswered = true
            )
        }
    }
    
    fun nextQuestion() {
        val session = _uiState.value.session ?: return
        if (session.currentQuestionIndex < session.questions.size - 1) {
            _uiState.update { it.copy(session = session.copy(currentQuestionIndex = session.currentQuestionIndex + 1)) }
            updateUiFromSession()
        }
    }

    fun previousQuestion() {
        val session = _uiState.value.session ?: return
        if (session.currentQuestionIndex > 0) {
            _uiState.update { it.copy(session = session.copy(currentQuestionIndex = session.currentQuestionIndex - 1)) }
            updateUiFromSession()
        }
    }

    fun submitTest() {
        _uiState.update { it.copy(isTimerActive = false) }
        val session = _uiState.value.session ?: run {
            ErrorLogger.log(Exception("Session is null in submitTest"), "OIR test session null during test submission")
            return
        }
        viewModelScope.launch {
            try {
                val subscriptionType = userProfileRepository.getUserProfile(session.userId).first()
                    .getOrNull()?.subscriptionType ?: com.ssbmax.core.domain.model.SubscriptionType.FREE
                val submissionId = submitOIRTestUseCase(session).getOrThrow()
                testContentRepository.clearCache()
                _uiState.update { it.copy(
                    session = session.copy(isCompleted = true),
                    isCompleted = true, sessionId = submissionId,
                    subscriptionType = subscriptionType,
                    testResult = scoreCalculator.calculate(session)
                ) }
            } catch (e: Exception) {
                ErrorLogger.logTestError(throwable = e, description = "OIR test submission failed",
                    testType = "OIR", userId = observeCurrentUser().first()?.id)
                _uiState.update { it.copy(errorResId = R.string.oir_error_submit_failed) }
            }
        }
    }
    
    fun pauseTest() {
        val session = _uiState.value.session ?: return
        _uiState.update { it.copy(isTimerActive = false, session = session.copy(isPaused = true)) }
    }

    private fun startTimer() {
        _uiState.update { it.copy(isTimerActive = true, timerStartTime = System.currentTimeMillis()) }
        viewModelScope.launch {
            try {
                while (isActive && _uiState.value.isTimerActive &&
                       _uiState.value.timeRemainingSeconds > 0 && !_uiState.value.isCompleted) {
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
        }.trackMemoryLeaks("OIRTestViewModel", "test-timer")
    }
    
    private fun updateUiFromSession() {
        val session = _uiState.value.session ?: return
        val currentQuestion = session.currentQuestion
        if (currentQuestion == null) {
            val exception = Exception("Current question null at invalid index: ${session.currentQuestionIndex}/${session.questions.size}")
            ErrorLogger.log(exception, "OIR test data inconsistency: currentQuestion is null")
            _uiState.update { it.copy(isLoading = false, errorResId = R.string.oir_error_invalid_question) }
            return
        }
        val existingAnswer = session.answers[currentQuestion.id]
        _uiState.update { it.copy(
            isLoading = false,
            errorResId = null,
            currentQuestion = currentQuestion,
            currentQuestionIndex = session.currentQuestionIndex,
            totalQuestions = session.questions.size,
            timeRemainingSeconds = session.timeRemainingSeconds,
            selectedOptionId = existingAnswer?.selectedOptionId,
            showFeedback = existingAnswer != null,
            isCurrentAnswerCorrect = existingAnswer?.isCorrect ?: false,
            currentQuestionAnswered = existingAnswer != null
        ) }
        prefetchNextImage(session.questions, session.currentQuestionIndex)
    }

    /**
     * Enqueues a background Coil preload for the image of question[currentIndex + 1].
     * No-op if on the last question or the next question has no image.
     */
    private fun prefetchNextImage(questions: List<OIRQuestion>, currentIndex: Int) {
        val nextUrl = questions.getOrNull(currentIndex + 1)?.questionImageUrl ?: return
        val request = ImageRequest.Builder(appContext)
            .data(nextUrl)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
        imageLoader.enqueue(request)
    }
    
    // Scoring logic extracted to OIRTestScoreCalculator (single responsibility, testable)

    override fun onCleared() {
        super.onCleared()
        MemoryLeakTracker.unregisterViewModel("OIRTestViewModel")
        MemoryLeakTracker.forceGcAndLog("OIRTestViewModel-Cleared")
    }
}