package com.ssbmax.ui.tests.ppdt

import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ssbmax.core.data.repository.DifficultyProgressionManager
import com.ssbmax.core.data.repository.SubscriptionManager
import com.ssbmax.core.data.repository.TestEligibility
import com.ssbmax.core.data.security.SecurityEventLogger
import com.ssbmax.core.data.util.MemoryLeakTracker
import com.ssbmax.core.data.util.trackMemoryLeaks
import com.ssbmax.shared.domain.model.*
import com.ssbmax.shared.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.shared.domain.usecase.ppdt.LoadPPDTTestUseCase
import com.ssbmax.shared.domain.usecase.ppdt.SubmitPPDTTestUseCase
import com.ssbmax.ui.tests.common.BaseTestViewModel
import com.ssbmax.ui.tests.common.TestNavigationEvent
import com.ssbmax.utils.ErrorLogger
import com.ssbmax.workers.PPDTAnalysisWorker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PPDTTestViewModel(
    observeCurrentUser: ObserveCurrentUserUseCase,
    private val loadPPDTTest: LoadPPDTTestUseCase,
    private val submitPPDTTest: SubmitPPDTTestUseCase,
    private val difficultyManager: DifficultyProgressionManager,
    subscriptionManager: SubscriptionManager,
    securityLogger: SecurityEventLogger,
    workManager: WorkManager
) : BaseTestViewModel(observeCurrentUser, subscriptionManager, securityLogger, workManager) {

    private val _uiState = MutableStateFlow(PPDTTestUiState())
    val uiState: StateFlow<PPDTTestUiState> = _uiState.asStateFlow()

    init {
        trackMemoryLeaks("PPDTTestViewModel")
        // loadTest() is called by PPDTTestScreen's LaunchedEffect(testId) — SSOT call site
        restoreTimerIfNeeded()
    }

    private fun shouldRestoreTimer(s: PPDTTestUiState): Boolean {
        val isInProgress = !s.isLoading && !s.isSubmitted && !s.isTimerActive
        val isTimerablePhase = s.currentPhase == PPDTPhase.IMAGE_VIEWING ||
            s.currentPhase == PPDTPhase.WRITING
        return isInProgress && isTimerablePhase && s.timeRemainingSeconds > 0
    }

    private fun restoreTimerIfNeeded() {
        viewModelScope.launch {
            val s = _uiState.value
            if (shouldRestoreTimer(s)) startTimer(s.timeRemainingSeconds)
        }
    }

    fun loadTest(testId: String = "ppdt_standard") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Checking eligibility...", error = null) }

            val userId = observeCurrentUser().first()?.id ?: run {
                ErrorLogger.logTestError(
                    throwable = IllegalStateException("Unauthenticated PPDT test access"),
                    description = "PPDT test access without authentication",
                    testType = "PPDT"
                )
                securityLogger.logUnauthenticatedAccess(testType = TestType.PPDT, context = "PPDTTestViewModel.loadTest")
                _uiState.update {
                    it.copy(isLoading = false, loadingMessage = null, error = "Authentication required. Please login to continue.")
                }
                return@launch
            }

            when (val eligibility = subscriptionManager.canTakeTest(TestType.PPDT, userId)) {
                is TestEligibility.LimitReached -> {
                    _uiState.update { it.copy(
                        isLoading = false, loadingMessage = null, error = null,
                        isLimitReached = true, subscriptionTier = eligibility.tier,
                        testsLimit = eligibility.limit, testsUsed = eligibility.usedCount,
                        resetsAt = eligibility.resetsAt
                    ) }
                    return@launch
                }
                is TestEligibility.NetworkError -> {
                    _uiState.update { it.copy(isLoading = false, loadingMessage = null, error = "No connection. Please check your network and try again.") }
                    return@launch
                }
                is TestEligibility.Eligible -> Unit
            }

            _uiState.update { it.copy(loadingMessage = "Fetching questions from cloud...") }

            loadPPDTTest(userId, testId)
                .onSuccess { session ->
                    _uiState.update { it.copy(session = session) }
                    updateUiFromSession()
                }
                .onFailure { e ->
                    when (e) {
                        is LoadPPDTTestUseCase.ProfileIncompleteException ->
                            _uiState.update { it.copy(isLoading = false, loadingMessage = null, isProfileIncomplete = true) }
                        else -> {
                            ErrorLogger.log(e, "PPDT loadTest failed: ${e.message}")
                            _uiState.update { it.copy(isLoading = false, loadingMessage = null, error = mapLoadError(e)) }
                        }
                    }
                }
        }
    }

    private fun mapLoadError(e: Throwable): String = when {
        e.message?.contains("Firestore", ignoreCase = true) == true -> "Firestore connection failed: ${e.message}"
        e.message?.contains("database", ignoreCase = true) == true -> "Database error: ${e.message}"
        e.message?.contains("Cache", ignoreCase = true) == true -> "Cache initialization failed: ${e.message}"
        else -> "Failed to load test. ${e.message ?: "Check your internet connection."}"
    }

    fun startTest() {
        val session = _uiState.value.session ?: return
        _uiState.update {
            it.copy(session = session.copy(
                currentPhase = PPDTPhase.IMAGE_VIEWING,
                imageViewingStartTime = System.currentTimeMillis()
            ))
        }
        updateUiFromSession()
        startTimer(30)
    }

    fun proceedToNextPhase() {
        val session = _uiState.value.session ?: return
        when (session.currentPhase) {
            PPDTPhase.IMAGE_VIEWING -> {
                _uiState.update {
                    it.copy(
                        isTimerActive = false,
                        session = session.copy(
                            currentPhase = PPDTPhase.WRITING,
                            writingStartTime = System.currentTimeMillis()
                        )
                    )
                }
                updateUiFromSession()
                startTimer(session.question.writingTimeMinutes * 60)
            }
            PPDTPhase.WRITING -> {
                if (_uiState.value.story.length >= session.question.minCharacters) {
                    _uiState.update { it.copy(isTimerActive = false, session = session.copy(currentPhase = PPDTPhase.REVIEW)) }
                    updateUiFromSession()
                }
            }
            else -> {}
        }
    }

    fun returnToWriting() {
        val session = _uiState.value.session ?: return
        _uiState.update { it.copy(session = session.copy(currentPhase = PPDTPhase.WRITING)) }
        updateUiFromSession()
        startTimer(session.question.writingTimeMinutes * 60)
    }

    fun updateStory(newStory: String) {
        val session = _uiState.value.session ?: return
        _uiState.update { it.copy(
            session = session.copy(story = newStory),
            story = newStory,
            charactersCount = newStory.length,
            canProceedToNextPhase = newStory.length >= session.question.minCharacters
        ) }
    }

    fun submitTest() {
        _uiState.update { it.copy(isTimerActive = false) }
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            submitPPDTTest(session)
                .onSuccess { result ->
                    enqueuePPDTAnalysisWorker(result.submissionId)
                    val isValid = session.story.length >= session.question.minCharacters
                    val difficulty = difficultyManager.getRecommendedDifficulty("PPDT")
                    difficultyManager.recordPerformance(
                        testType = "PPDT", difficulty = difficulty,
                        score = if (isValid) 100f else 0f,
                        correctAnswers = if (isValid) 1 else 0, totalQuestions = 1,
                        timeSeconds = (4 * 60).toFloat()
                    )
                    _uiState.update { it.copy(
                        session = session.copy(currentPhase = PPDTPhase.SUBMITTED, isCompleted = true),
                        isSubmitted = true, submissionId = result.submissionId,
                        subscriptionType = result.subscriptionType, submission = result.submission
                    ) }
                    sendNavigationEvent(TestNavigationEvent.NavigateToResult(
                        submissionId = result.submissionId, subscriptionType = result.subscriptionType
                    ))
                }
                .onFailure { error ->
                    ErrorLogger.log(error, "Failed to submit PPDT test for user: ${session.userId}")
                    _uiState.update { it.copy(error = "Failed to submit: ${error.message}") }
                }
        }
    }

    fun pauseTest() {
        val session = _uiState.value.session ?: return
        _uiState.update { it.copy(isTimerActive = false, session = session.copy(isPaused = true)) }
    }

    private fun startTimer(seconds: Int) {
        val myGeneration = ++timerGeneration
        _uiState.update { it.copy(timeRemainingSeconds = seconds, isTimerActive = true, timerStartTime = myGeneration) }
        viewModelScope.launch {
            try {
                while (isActive && _uiState.value.isTimerActive && _uiState.value.timeRemainingSeconds > 0) {
                    delay(1000)
                    if (!isActive || !_uiState.value.isTimerActive) return@launch
                    val newTime = _uiState.value.timeRemainingSeconds - 1
                    _uiState.update { it.copy(timeRemainingSeconds = newTime) }
                    // IMAGE_VIEWING hands off to a new 240s timer; return so this coroutine's
                    // finally{} sees a newer timerGeneration and is a no-op (generation-token invariant).
                    if (newTime == 0 && advancePhaseOnTimeout()) return@launch
                }
            } finally {
                _uiState.update { current ->
                    if (current.timerStartTime == myGeneration) current.copy(isTimerActive = false) else current
                }
            }
        }.trackMemoryLeaks("PPDTTestViewModel", "phase-timer")
    }

    private fun advancePhaseOnTimeout(): Boolean = when (_uiState.value.currentPhase) {
        PPDTPhase.IMAGE_VIEWING -> { proceedToNextPhase(); true }
        PPDTPhase.WRITING -> { proceedToNextPhase(); false }
        else -> false
    }

    private fun updateUiFromSession() {
        val session = _uiState.value.session ?: return
        _uiState.update { it.copy(
            isLoading = false, loadingMessage = null,
            currentPhase = session.currentPhase, imageUrl = session.question.imageUrl,
            story = session.story, charactersCount = session.story.length,
            minCharacters = session.question.minCharacters, maxCharacters = session.question.maxCharacters,
            canProceedToNextPhase = session.story.length >= session.question.minCharacters
        ) }
    }

    private fun enqueuePPDTAnalysisWorker(submissionId: String) {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val workRequest = OneTimeWorkRequestBuilder<PPDTAnalysisWorker>()
            .setInputData(workDataOf(PPDTAnalysisWorker.KEY_SUBMISSION_ID to submissionId))
            .setConstraints(constraints).build()
        enqueueAnalysisWork("ppdt_analysis_$submissionId", workRequest)
    }

    override fun onCleared() {
        super.onCleared()
        MemoryLeakTracker.unregisterViewModel("PPDTTestViewModel")
        MemoryLeakTracker.forceGcAndLog("PPDTTestViewModel-Cleared")
    }
}
