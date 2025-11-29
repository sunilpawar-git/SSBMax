package com.ssbmax.ui.interview.voice

import android.content.Context
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.R
import com.ssbmax.core.data.util.trackMemoryLeaks
import com.ssbmax.core.domain.model.interview.InterviewQuestion
import com.ssbmax.core.domain.model.interview.InterviewResponse
import com.ssbmax.core.domain.model.interview.OLQScore
import com.ssbmax.core.domain.repository.InterviewRepository
import com.ssbmax.core.domain.service.AIService
import com.ssbmax.core.domain.service.ResponseAnalysis
import com.ssbmax.utils.ErrorLogger
import com.ssbmax.utils.TextToSpeechManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for Voice Interview Session screen
 *
 * Manages interview session state and coordinates between:
 * - VoiceRecordingHelper for audio/transcription
 * - InterviewRepository for session persistence
 * - AIService for response analysis
 *
 * @see VoiceRecordingHelper for recording logic
 */
@HiltViewModel
class VoiceInterviewSessionViewModel @Inject constructor(
    private val interviewRepository: InterviewRepository,
    private val aiService: AIService,
    @ApplicationContext private val context: Context,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val TAG = "VoiceInterviewVM"
    }

    private val sessionId: String = checkNotNull(savedStateHandle.get<String>("sessionId"))

    private val _uiState = MutableStateFlow(VoiceInterviewSessionUiState())
    val uiState: StateFlow<VoiceInterviewSessionUiState> = _uiState.asStateFlow()
    
    /** Flag to prevent state updates during exit (stopAll() called) */
    @Volatile
    private var isExiting: Boolean = false

    private val recordingHelper = VoiceRecordingHelper(context)
    
    /** Text-to-Speech manager for interviewer voice (auto-speaks questions) */
    private val ttsManager = TextToSpeechManager(
        context = context,
        onReady = {
            if (isExiting) {
                Log.d(TAG, "⚠️ TTS onReady after exit started, ignoring")
                return@TextToSpeechManager
            }
            Log.d(TAG, "🔊 TTS ready")
            _uiState.update { it.copy(isTTSReady = true) }
            // Speak current question if available when TTS becomes ready (only if not exiting)
            if (!isExiting) {
                _uiState.value.currentQuestion?.let { 
                    Log.d(TAG, "🔊 Auto-speaking question when TTS becomes ready")
                    speakQuestion(it.questionText) 
                }
            }
        },
        onSpeechComplete = {
            if (isExiting) {
                Log.d(TAG, "⚠️ TTS onSpeechComplete after exit started, ignoring")
                return@TextToSpeechManager
            }
            Log.d(TAG, "✅ TTS speech complete")
            _uiState.update { it.copy(isTTSSpeaking = false) }
        },
        onError = { error ->
            if (isExiting) {
                Log.d(TAG, "⚠️ TTS onError after exit started, ignoring: $error")
                return@TextToSpeechManager
            }
            Log.e(TAG, "❌ TTS error: $error")
            ErrorLogger.log(Exception(error), "TTS error during voice interview")
            _uiState.update { it.copy(isTTSSpeaking = false) }
        }
    )

    init {
        Log.d(TAG, "🚀 ViewModel initializing for session: $sessionId")
        trackMemoryLeaks("VoiceInterviewSessionViewModel")
        observeRecordingState()
        loadSession()
    }
    
    /**
     * Speak the question text using TTS
     */
    private fun speakQuestion(questionText: String) {
        if (isExiting) {
            Log.d(TAG, "⚠️ speakQuestion called during exit, ignoring")
            return
        }
        if (_uiState.value.isTTSReady) {
            Log.d(TAG, "🔊 Speaking question: ${questionText.take(50)}...")
            _uiState.update { it.copy(isTTSSpeaking = true) }
            ttsManager.speak(questionText)
        }
    }

    private fun observeRecordingState() {
        viewModelScope.launch {
            recordingHelper.state.collect { recording ->
                if (isExiting) {
                    Log.d(TAG, "⚠️ Recording state update during exit, ignoring")
                    return@collect
                }
                _uiState.update {
                    it.copy(
                        recordingState = recording.recordingState,
                        audioFilePath = recording.audioFilePath,
                        audioDurationMs = recording.audioDurationMs,
                        transcriptionState = recording.transcriptionState,
                        liveTranscription = recording.liveTranscription,
                        finalTranscription = recording.finalTranscription,
                        transcriptionError = recording.transcriptionError
                    )
                }
            }
        }
    }

    private fun loadSession() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    loadingMessage = context.getString(R.string.voice_interview_loading_session)
                )
            }

            try {
                val sessionResult = interviewRepository.getSession(sessionId)
                if (sessionResult.isFailure) {
                    handleError(sessionResult.exceptionOrNull() ?: Exception("Unknown"), "Failed to load interview session", R.string.voice_interview_error_load_session)
                    return@launch
                }

                val session = sessionResult.getOrNull()
                if (session == null) {
                    setError(R.string.voice_interview_error_session_not_found)
                    return@launch
                }

                val questionId = session.questionIds.getOrNull(session.currentQuestionIndex) ?: run {
                    setError(R.string.voice_interview_error_no_questions)
                    return@launch
                }

                val questionResult = interviewRepository.getQuestion(questionId)
                if (questionResult.isFailure) {
                    handleError(questionResult.exceptionOrNull() ?: Exception("Unknown"), "Failed to load question", R.string.voice_interview_error_load_question)
                    return@launch
                }
                val question = questionResult.getOrNull()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingMessage = null,
                        session = session,
                        currentQuestion = question,
                        currentQuestionIndex = session.currentQuestionIndex,
                        totalQuestions = session.questionIds.size,
                        thinkingStartTime = System.currentTimeMillis()
                    )
                }
                
                // Auto-speak the question using TTS (only if not exiting)
                if (!isExiting) {
                    question?.let { speakQuestion(it.questionText) }
                } else {
                    Log.d(TAG, "⚠️ Skipping auto-speak - exit in progress")
                }
            } catch (e: Exception) {
                handleError(e, "Exception loading session", R.string.voice_interview_error_generic)
            }
        }
    }

    fun updateRecordPermission(granted: Boolean) {
        _uiState.update { it.copy(hasRecordPermission = granted) }
    }

    fun startRecording() {
        if (!_uiState.value.canStartRecording()) return
        viewModelScope.launch {
            if (recordingHelper.startRecording() == null) {
                _uiState.update { it.copy(error = context.getString(R.string.voice_interview_error_start_recording)) }
            } else {
                _uiState.update { it.copy(error = null) }
            }
        }
    }

    fun stopRecording() {
        if (!_uiState.value.canStopRecording()) return
        viewModelScope.launch {
            if (recordingHelper.stopRecording() == null) {
                _uiState.update { it.copy(error = context.getString(R.string.voice_interview_error_save_recording)) }
            } else {
                _uiState.update { it.copy(error = null) }
            }
        }
    }

    fun cancelRecording() {
        recordingHelper.cancelRecording()
        _uiState.update { it.copy(error = null) }
    }

    fun updateTranscription(text: String) = recordingHelper.updateTranscription(text)

    fun submitResponse() {
        if (!_uiState.value.canSubmitResponse()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingResponse = true, error = null) }

            try {
                val state = _uiState.value
                val question = state.currentQuestion ?: return@launch
                val session = state.session ?: return@launch

                val analysis = analyzeResponse(question, state.finalTranscription, state.mode.name)
                val response = buildResponse(state, question, analysis)

                interviewRepository.submitResponse(response).onFailure {
                    handleSubmitError(it)
                    return@launch
                }

                if (state.hasMoreQuestions()) loadNextQuestion() else completeInterview()
            } catch (e: Exception) {
                handleError(e, "Exception submitting response", R.string.voice_interview_error_generic)
                _uiState.update { it.copy(isSubmittingResponse = false) }
            }
        }
    }

    private suspend fun analyzeResponse(question: InterviewQuestion, response: String, mode: String): ResponseAnalysis? {
        return aiService.analyzeResponse(question, response, mode).getOrElse {
            ErrorLogger.log(it, "AI analysis failed for interview response")
            null
        }
    }

    private fun buildResponse(state: VoiceInterviewSessionUiState, question: InterviewQuestion, analysis: ResponseAnalysis?): InterviewResponse {
        val olqScores = analysis?.olqScores?.mapValues { (_, s) ->
            OLQScore(score = s.score.toInt().coerceIn(1, 10), confidence = analysis.overallConfidence, reasoning = s.reasoning)
        } ?: emptyMap()

        return InterviewResponse(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            questionId = question.id,
            responseText = state.finalTranscription,
            responseMode = state.mode,
            respondedAt = Instant.now(),
            thinkingTimeSec = state.getThinkingTimeSeconds(),
            audioUrl = state.audioFilePath,
            olqScores = olqScores,
            confidenceScore = analysis?.overallConfidence ?: 0
        )
    }

    private fun handleSubmitError(error: Throwable) {
        ErrorLogger.log(error, "Failed to submit interview response")
        _uiState.update {
            it.copy(isSubmittingResponse = false, error = context.getString(R.string.voice_interview_error_submit_response))
        }
    }

    private suspend fun loadNextQuestion() {
        val state = _uiState.value
        val session = state.session ?: return
        val nextIndex = state.currentQuestionIndex + 1
        val nextQuestionId = session.questionIds.getOrNull(nextIndex) ?: return

        val updatedSession = session.copy(currentQuestionIndex = nextIndex)
        interviewRepository.updateSession(updatedSession)

        val question = interviewRepository.getQuestion(nextQuestionId).getOrElse {
            handleError(it, "Failed to load next question", R.string.voice_interview_error_load_next_question)
            _uiState.update { s -> s.copy(isSubmittingResponse = false) }
            return
        }

        recordingHelper.resetForNextQuestion()
        _uiState.update {
            it.copy(
                isSubmittingResponse = false,
                session = updatedSession,
                currentQuestion = question,
                currentQuestionIndex = nextIndex,
                thinkingStartTime = System.currentTimeMillis()
            )
        }
        
        // Auto-speak the next question using TTS
        question?.let { speakQuestion(it.questionText) }
    }

    private suspend fun completeInterview() {
        _uiState.update { it.copy(loadingMessage = context.getString(R.string.voice_interview_generating_results)) }

        try {
            val completeResult = interviewRepository.completeInterview(sessionId)
            if (completeResult.isFailure) {
                handleError(completeResult.exceptionOrNull() ?: Exception("Unknown"), "Failed to complete interview", R.string.voice_interview_error_complete_interview)
                _uiState.update { it.copy(isSubmittingResponse = false, loadingMessage = null) }
                return
            }

            val result = completeResult.getOrNull()
            if (result == null) {
                setError(R.string.voice_interview_error_generate_results)
                _uiState.update { it.copy(isSubmittingResponse = false, loadingMessage = null) }
                return
            }

            _uiState.update { it.copy(isSubmittingResponse = false, loadingMessage = null, isCompleted = true, resultId = result.id) }
        } catch (e: Exception) {
            handleError(e, "Exception completing interview", R.string.voice_interview_error_generic)
            _uiState.update { it.copy(isSubmittingResponse = false, loadingMessage = null) }
        }
    }

    private fun handleError(error: Throwable, logDescription: String, stringResId: Int) {
        ErrorLogger.log(error, logDescription)
        setError(stringResId)
    }

    private fun setError(stringResId: Int) {
        _uiState.update { it.copy(isLoading = false, loadingMessage = null, error = context.getString(stringResId)) }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
    
    /**
     * Stop all speech and recording - call before exiting.
     * Sets isExiting flag to prevent callbacks from updating state after exit.
     */
    fun stopAll() {
        Log.d(TAG, "🛑 stopAll() called")
        isExiting = true
        ttsManager.stop()
        recordingHelper.release()
        _uiState.update { 
            it.copy(
                isTTSSpeaking = false,
                recordingState = RecordingState.IDLE,
                transcriptionState = TranscriptionState.IDLE
            ) 
        }
        Log.d(TAG, "✅ stopAll() complete")
    }

    override fun onCleared() {
        Log.d(TAG, "🧹 onCleared()")
        super.onCleared()
        isExiting = true
        ttsManager.release()
        recordingHelper.release()
        Log.d(TAG, "✅ ViewModel cleared")
    }
}
