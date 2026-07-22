package com.ssbmax.shared.presentation.interviewsession

import com.ssbmax.shared.domain.repository.InterviewRepository
import com.ssbmax.shared.domain.service.SubmissionAnalysisTrigger
import com.ssbmax.shared.domain.util.DomainLogger
import com.ssbmax.shared.platform.tts.TTSService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/**
 * KMP port of `app/.../ui/interview/session/InterviewSessionViewModel.kt`.
 *
 * Delegates to helper classes for single responsibility, same shape as the
 * Android original: [TTSManager] (speech control), [SessionManager]
 * (session/question state), [InterviewCompleter] (completion + analysis
 * trigger -- see that class's own doc comment for the WorkManager ->
 * [SubmissionAnalysisTrigger] substitution, this vertical's async-analysis
 * finding).
 *
 * Deviations from the Android original, all deliberate and documented:
 * - `androidx.lifecycle.ViewModel` + `SavedStateHandle` -> plain class + own
 *   `CoroutineScope`, matching this phase's established pattern (see
 *   `PPDTTestViewModel`'s doc comment) -- `sessionId` is passed to [loadSession]
 *   directly instead of read from `SavedStateHandle`.
 * - `androidx.work.WorkManager` -> [SubmissionAnalysisTrigger] (delegated to
 *   [InterviewCompleter]).
 * - **Real finding, not carried forward silently:** the Android original's
 *   constructor also takes `AuthRepository`, `UserProfileRepository`, and
 *   `AnalyticsManager` (`core:data`'s `AnalyticsManager`) -- grep-confirmed
 *   none of the three is referenced anywhere in the class body beyond the
 *   constructor parameter list itself. This is dead-parameter debt in the
 *   Android original (not something this port introduces), so none of the
 *   three is carried into this port, matching this plan's own "don't
 *   manufacture new dead code, and don't port pre-existing dead code either
 *   when it's cheap to leave out" precedent (see `InterviewQuestionCacheDao`
 *   in Phase 2's history for the analogous data-layer case).
 * - `Context.getString(R.string...)` -> plain hardcoded English strings,
 *   same documented convention as every other Phase 5 ViewModel.
 * - `System.currentTimeMillis()` -> `Clock.System.now().toEpochMilliseconds()`.
 * - `trackMemoryLeaks`/`MemoryLeakTracker` dropped -- not an
 *   `androidx.lifecycle.ViewModel`; [stopAll]/[close] handle cleanup instead.
 */
class InterviewSessionViewModel(
    interviewRepository: InterviewRepository,
    ttsService: TTSService,
    analysisTrigger: SubmissionAnalysisTrigger,
    private val logger: DomainLogger
) {
    private val tag = "InterviewSessionViewModel"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _uiState = MutableStateFlow(InterviewSessionUiState())
    val uiState: StateFlow<InterviewSessionUiState> = _uiState.asStateFlow()

    private var isExiting: Boolean = false

    private val ttsManager = TTSManager(ttsService = ttsService, scope = scope, logger = logger)
    private val sessionManager = SessionManager(interviewRepository, logger)
    private val interviewCompleter = InterviewCompleter(interviewRepository, analysisTrigger, logger)

    private var sessionId: String? = null

    /** Starts loading the session. Must be called once, e.g. from the screen's `LaunchedEffect(sessionId)`. */
    fun loadSession(sessionId: String) {
        if (this.sessionId != null) return // already loading/loaded
        this.sessionId = sessionId

        initializeTTS()
        observeTTSState()
        observeSessionState()

        scope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Loading interview session...") }

            val result = sessionManager.loadSession(sessionId)
            if (result.isFailure) {
                handleError(result.exceptionOrNull() ?: Exception("Unknown"), "Failed to load session")
                return@launch
            }

            _uiState.update {
                it.copy(isLoading = false, loadingMessage = null, thinkingStartTime = Clock.System.now().toEpochMilliseconds())
            }

            if (!isExiting) {
                sessionManager.currentQuestion.value?.let { speakQuestion(it.questionText) }
            }
        }
    }

    private fun initializeTTS() {
        scope.launch { ttsManager.initialize() }
    }

    private fun observeTTSState() {
        scope.launch {
            ttsManager.isTTSReady.collect { ready ->
                _uiState.update { it.copy(isTTSReady = ready) }
                if (ready && !isExiting) {
                    _uiState.value.currentQuestion?.let { speakQuestion(it.questionText) }
                }
            }
        }
        scope.launch {
            ttsManager.isTTSSpeaking.collect { speaking -> _uiState.update { it.copy(isTTSSpeaking = speaking) } }
        }
        scope.launch {
            ttsManager.isTTSMuted.collect { muted -> _uiState.update { it.copy(isTTSMuted = muted) } }
        }
    }

    private fun observeSessionState() {
        scope.launch {
            sessionManager.session.collect { session ->
                _uiState.update { it.copy(session = session, totalQuestions = sessionManager.totalQuestions) }
            }
        }
        scope.launch {
            sessionManager.currentQuestion.collect { question -> _uiState.update { it.copy(currentQuestion = question) } }
        }
        scope.launch {
            sessionManager.currentIndex.collect { index -> _uiState.update { it.copy(currentQuestionIndex = index) } }
        }
    }

    private fun speakQuestion(questionText: String) {
        if (isExiting) return
        ttsManager.speak(questionText)
    }

    fun updateResponseText(text: String) {
        _uiState.update { it.copy(responseText = text) }
    }

    /** Toggle TTS mute/unmute. Delegated to [TTSManager]. */
    fun toggleTTSMute() {
        val currentQuestionText = _uiState.value.currentQuestion?.questionText
        ttsManager.toggleMute(currentQuestionText)
    }

    /**
     * Submit current response and move to next question INSTANTLY.
     * Stores response locally, NO AI analysis here.
     */
    fun submitResponse() {
        if (!_uiState.value.canSubmitResponse()) return

        scope.launch {
            _uiState.update { it.copy(isSubmittingResponse = true, error = null) }

            try {
                val state = _uiState.value
                val currentQuestion = state.currentQuestion ?: return@launch

                val pendingResponse = PendingResponse(
                    questionId = currentQuestion.id,
                    questionText = currentQuestion.questionText,
                    responseText = state.responseText,
                    thinkingTimeSec = state.getThinkingTimeSeconds()
                )

                val updatedPending = state.pendingResponses + pendingResponse

                if (sessionManager.hasMoreQuestions()) {
                    _uiState.update { it.copy(pendingResponses = updatedPending) }
                    loadNextQuestion()
                } else {
                    completeInterview(updatedPending)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.e(tag, "Exception during interview response submission", e)
                handleError(e, "An error occurred")
                _uiState.update { it.copy(isSubmittingResponse = false) }
            }
        }
    }

    private suspend fun loadNextQuestion() {
        val result = sessionManager.loadNextQuestion()
        if (result.isFailure) {
            handleError(result.exceptionOrNull() ?: Exception("Unknown"), "Failed to load next question")
            _uiState.update { it.copy(isSubmittingResponse = false) }
            return
        }

        val question = result.getOrNull()
        _uiState.update {
            it.copy(isSubmittingResponse = false, thinkingStartTime = Clock.System.now().toEpochMilliseconds(), responseText = "")
        }
        question?.let { speakQuestion(it.questionText) }
    }

    /** Complete interview with async analysis. Delegated to [InterviewCompleter]. */
    private suspend fun completeInterview(pendingResponses: List<PendingResponse>) {
        val state = _uiState.value
        val session = state.session ?: return
        val currentSessionId = sessionId ?: return

        _uiState.update { it.copy(isSubmittingResponse = true, loadingMessage = "Submitting your answers...") }

        val result = interviewCompleter.complete(currentSessionId, session, pendingResponses, state.mode)

        if (result.isSuccess) {
            _uiState.update {
                it.copy(isSubmittingResponse = false, loadingMessage = null, isCompleted = true, isResultPending = true, resultId = currentSessionId)
            }
        } else {
            logger.e(tag, "Exception completing interview", result.exceptionOrNull())
            handleError(result.exceptionOrNull() ?: Exception("Unknown"), "Failed to complete interview")
            _uiState.update { it.copy(isSubmittingResponse = false, loadingMessage = null) }
        }
    }

    private fun handleError(error: Throwable, message: String) {
        logger.e(tag, "Interview session error", error)
        _uiState.update { it.copy(isLoading = false, loadingMessage = null, error = message) }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    fun stopAll() {
        isExiting = true
        ttsManager.stop()
    }

    fun close() {
        isExiting = true
        ttsManager.release()
        scope.cancel()
    }
}
