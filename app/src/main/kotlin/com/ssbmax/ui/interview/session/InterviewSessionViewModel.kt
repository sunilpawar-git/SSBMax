package com.ssbmax.ui.interview.session

import android.content.Context
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.ssbmax.BuildConfig
import com.ssbmax.R
import com.ssbmax.core.data.analytics.AnalyticsManager
import com.ssbmax.core.data.util.trackMemoryLeaks
import com.ssbmax.core.domain.repository.AuthRepository
import com.ssbmax.core.domain.repository.InterviewRepository
import com.ssbmax.core.domain.repository.UserProfileRepository
import com.ssbmax.utils.ErrorLogger
import com.ssbmax.utils.tts.AndroidTTS
import com.ssbmax.utils.tts.TTSService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Interview Session screen
 *
 * Delegates to helper classes for single responsibility:
 * - TTSManager: TTS service selection, speech control
 * - SessionManager: Session and question state management
 * - InterviewCompleter: Interview completion and background analysis
 *
 * OPTIMIZATION: Uses BACKGROUND AI analysis via WorkManager.
 */
    @HiltViewModel
class InterviewSessionViewModel @Inject constructor(
    interviewRepository: InterviewRepository,
    authRepository: AuthRepository,
    userProfileRepository: UserProfileRepository,
    workManager: WorkManager,
    analyticsManager: AnalyticsManager,
    @AndroidTTS androidTTSService: TTSService,
    @ApplicationContext private val context: Context,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val TAG = "VoiceInterviewVM"
    }

    private val sessionId: String = checkNotNull(savedStateHandle.get<String>("sessionId"))

    private val _uiState = MutableStateFlow(InterviewSessionUiState())
    val uiState: StateFlow<InterviewSessionUiState> = _uiState.asStateFlow()

    @Volatile
    private var isExiting: Boolean = false

    // Delegate TTS management
    private val ttsManager = TTSManager(
        androidTTSService = androidTTSService,
        scope = viewModelScope
    )

    // Delegate session management
    private val sessionManager = SessionManager(interviewRepository)

    // Delegate interview completion
    private val interviewCompleter = InterviewCompleter(interviewRepository, workManager)

    init {
        Log.d(TAG, "🚀 ViewModel initializing for session: $sessionId")
        trackMemoryLeaks("InterviewSessionViewModel")
        initializeTTS()
        observeTTSState()
        observeSessionState()
        loadSession()
    }

    private fun initializeTTS() {
        viewModelScope.launch {
            ttsManager.initialize()
        }
    }

    private fun observeTTSState() {
        viewModelScope.launch {
            ttsManager.isTTSReady.collect { ready ->
                _uiState.update { it.copy(isTTSReady = ready) }
                if (ready && !isExiting) {
                    _uiState.value.currentQuestion?.let { speakQuestion(it.questionText) }
                }
            }
        }
        viewModelScope.launch {
            ttsManager.isTTSSpeaking.collect { speaking ->
                _uiState.update { it.copy(isTTSSpeaking = speaking) }
            }
        }
        viewModelScope.launch {
            ttsManager.isTTSMuted.collect { muted ->
                _uiState.update { it.copy(isTTSMuted = muted) }
            }
        }
    }

    private fun observeSessionState() {
        viewModelScope.launch {
            sessionManager.session.collect { session ->
                _uiState.update { it.copy(session = session, totalQuestions = sessionManager.totalQuestions) }
            }
        }
        viewModelScope.launch {
            sessionManager.currentQuestion.collect { question ->
                _uiState.update { it.copy(currentQuestion = question) }
            }
        }
        viewModelScope.launch {
            sessionManager.currentIndex.collect { index ->
                _uiState.update { it.copy(currentQuestionIndex = index) }
            }
        }
    }

    private fun speakQuestion(questionText: String) {
        if (isExiting) return
        ttsManager.speak(questionText)
    }

    private fun loadSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = context.getString(R.string.interview_loading_session)) }

            val result = sessionManager.loadSession(sessionId)
            if (result.isFailure) {
                handleError(result.exceptionOrNull() ?: Exception("Unknown"), R.string.interview_error_load_session)
                return@launch
            }

            _uiState.update { it.copy(isLoading = false, loadingMessage = null, thinkingStartTime = System.currentTimeMillis()) }

            if (!isExiting) {
                sessionManager.currentQuestion.value?.let { speakQuestion(it.questionText) }
            }
        }
    }

    fun updateResponseText(text: String) {
        _uiState.update { it.copy(responseText = text) }
    }

    /** Toggle TTS mute/unmute. Delegated to TTSManager. */
    fun toggleTTSMute() {
        val currentQuestionText = _uiState.value.currentQuestion?.questionText
        ttsManager.toggleMute(currentQuestionText)
    }

    /**
     * Submit current response and move to next question INSTANTLY.
     * Stores response locally, NO AI analysis here.
     */
    fun submitResponse() {
        if (!_uiState.value.canSubmitResponse()) {
            Log.d(TAG, "⚠️ Cannot submit response - validation failed")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingResponse = true, error = null) }

            try {
                val state = _uiState.value
                val currentQuestion = state.currentQuestion ?: return@launch

                Log.d(TAG, "⚡ Storing response locally - Question: ${currentQuestion.id}")
                val pendingResponse = PendingResponse(
                    questionId = currentQuestion.id,
                    questionText = currentQuestion.questionText,
                    responseText = state.responseText,
                    thinkingTimeSec = state.getThinkingTimeSeconds()
                )

                val updatedPending = state.pendingResponses + pendingResponse
                Log.d(TAG, "📝 Stored locally (${updatedPending.size}/${state.totalQuestions} responses)")

                if (sessionManager.hasMoreQuestions()) {
                    _uiState.update { it.copy(pendingResponses = updatedPending) }
                    loadNextQuestion()
                } else {
                    Log.d(TAG, "🏁 Last question - completing with ${updatedPending.size} responses")
                    completeInterview(updatedPending)
                }
            } catch (e: Exception) {
                ErrorLogger.log(e, "Exception during interview response submission")
                handleError(e, R.string.interview_error_generic)
                _uiState.update { it.copy(isSubmittingResponse = false) }
            }
        }
    }

    private suspend fun loadNextQuestion() {
        val result = sessionManager.loadNextQuestion()
        if (result.isFailure) {
            handleError(result.exceptionOrNull() ?: Exception("Unknown"), R.string.interview_error_load_next_question)
            _uiState.update { it.copy(isSubmittingResponse = false) }
            return
        }

        val question = result.getOrNull()
        _uiState.update { it.copy(isSubmittingResponse = false, thinkingStartTime = System.currentTimeMillis(), responseText = "") }
        question?.let { speakQuestion(it.questionText) }
    }

    /** Complete interview with BACKGROUND analysis. Delegated to InterviewCompleter. */
    private suspend fun completeInterview(pendingResponses: List<PendingResponse>) {
        val state = _uiState.value
        val session = state.session ?: return

        _uiState.update { it.copy(isSubmittingResponse = true, loadingMessage = context.getString(R.string.interview_submitting_answers)) }

        val result = interviewCompleter.complete(sessionId, session, pendingResponses, state.mode)

        if (result.isSuccess) {
            _uiState.update {
                it.copy(isSubmittingResponse = false, loadingMessage = null, isCompleted = true, isResultPending = true, resultId = sessionId)
            }
        } else {
            ErrorLogger.log(result.exceptionOrNull() ?: Exception("Unknown"), "Exception completing voice interview")
            handleError(result.exceptionOrNull() ?: Exception("Unknown"), R.string.interview_error_complete_interview)
            _uiState.update { it.copy(isSubmittingResponse = false, loadingMessage = null) }
        }
    }

    private fun handleError(error: Throwable, stringResId: Int) {
        ErrorLogger.log(error, "Voice interview error")
        setError(stringResId)
    }

    private fun setError(stringResId: Int) {
        _uiState.update { it.copy(isLoading = false, loadingMessage = null, error = context.getString(stringResId)) }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    fun stopAll() {
        Log.d(TAG, "🛑 stopAll() called")
        isExiting = true
        ttsManager.stop()
    }

    override fun onCleared() {
        Log.d(TAG, "🧹 onCleared()")
        super.onCleared()
        isExiting = true
        ttsManager.release()
    }
}
