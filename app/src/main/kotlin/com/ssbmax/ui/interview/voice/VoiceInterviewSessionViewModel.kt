package com.ssbmax.ui.interview.voice

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.data.util.trackMemoryLeaks
import com.ssbmax.core.domain.model.interview.InterviewResponse
import com.ssbmax.core.domain.model.interview.OLQScore
import com.ssbmax.core.domain.repository.InterviewRepository
import com.ssbmax.core.domain.service.AIService
import com.ssbmax.utils.AudioRecorder
import com.ssbmax.utils.ErrorLogger
import com.ssbmax.utils.SpeechToTextManager
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
 * Responsibilities:
 * - Load interview session and questions
 * - Manage voice recording lifecycle
 * - Handle transcription workflow
 * - Submit responses with AI analysis
 * - Complete interview and navigate to results
 *
 * MEMORY LEAK PREVENTION:
 * - Registers with MemoryLeakTracker
 * - Uses viewModelScope for all coroutines (auto-cancelled)
 * - AudioRecorder released in onCleared()
 * - No static references or context leaks
 */
@HiltViewModel
class VoiceInterviewSessionViewModel @Inject constructor(
    private val interviewRepository: InterviewRepository,
    private val aiService: AIService,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedStateHandle.get<String>("sessionId")) {
        "sessionId is required"
    }

    private val _uiState = MutableStateFlow(VoiceInterviewSessionUiState())
    val uiState: StateFlow<VoiceInterviewSessionUiState> = _uiState.asStateFlow()

    private val audioRecorder = AudioRecorder(context)
    private var speechToTextManager: SpeechToTextManager? = null

    init {
        // Register for memory leak tracking
        trackMemoryLeaks("VoiceInterviewSessionViewModel")

        // Load session
        loadSession()
    }

    /**
     * Load interview session and first question
     */
    private fun loadSession() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    loadingMessage = "Loading interview session..."
                )
            }

            try {
                // Get session
                val sessionResult = interviewRepository.getSession(sessionId)
                if (sessionResult.isFailure) {
                    ErrorLogger.log(
                        throwable = sessionResult.exceptionOrNull() ?: Exception("Unknown error"),
                        description = "Failed to load interview session"
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadingMessage = null,
                            error = "Failed to load session"
                        )
                    }
                    return@launch
                }

                val session = sessionResult.getOrNull()
                if (session == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadingMessage = null,
                            error = "Session not found"
                        )
                    }
                    return@launch
                }

                // Load current question
                val questionId = session.questionIds.getOrNull(session.currentQuestionIndex)
                if (questionId == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadingMessage = null,
                            error = "No questions available"
                        )
                    }
                    return@launch
                }

                val questionResult = interviewRepository.getQuestion(questionId)
                if (questionResult.isFailure) {
                    ErrorLogger.log(
                        throwable = questionResult.exceptionOrNull() ?: Exception("Unknown error"),
                        description = "Failed to load interview question"
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadingMessage = null,
                            error = "Failed to load question"
                        )
                    }
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
            } catch (e: Exception) {
                ErrorLogger.log(e, "Exception loading interview session")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingMessage = null,
                        error = "An error occurred"
                    )
                }
            }
        }
    }

    /**
     * Update permission status
     */
    fun updateRecordPermission(granted: Boolean) {
        _uiState.update { it.copy(hasRecordPermission = granted) }
    }

    /**
     * Start recording audio response (Phase 2: with simultaneous STT)
     */
    fun startRecording() {
        if (!_uiState.value.canStartRecording()) {
            return
        }

        viewModelScope.launch {
            // 1. Start audio recording
            val filePath = audioRecorder.startRecording()
            if (filePath == null) {
                _uiState.update {
                    it.copy(
                        error = "Failed to start recording"
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    recordingState = RecordingState.RECORDING,
                    audioFilePath = filePath,
                    transcriptionState = TranscriptionState.IDLE,
                    liveTranscription = "",
                    finalTranscription = "",
                    transcriptionError = null,
                    error = null
                )
            }

            // 2. Start speech-to-text simultaneously
            speechToTextManager = SpeechToTextManager(
                context = context,
                onResult = { transcription ->
                    // Final transcription complete
                    _uiState.update {
                        it.copy(
                            finalTranscription = transcription,
                            transcriptionState = TranscriptionState.COMPLETED,
                            liveTranscription = "",
                            transcriptionText = transcription // Keep deprecated field in sync
                        )
                    }
                },
                onPartialResult = { partial ->
                    // Live partial transcription (show in real-time)
                    _uiState.update {
                        it.copy(
                            liveTranscription = partial,
                            transcriptionState = TranscriptionState.LISTENING
                        )
                    }
                },
                onError = { error ->
                    // Transcription error (audio recording continues)
                    ErrorLogger.log(
                        throwable = Exception("Speech-to-text error: $error"),
                        description = "STT failed during voice interview"
                    )
                    _uiState.update {
                        it.copy(
                            transcriptionError = error,
                            transcriptionState = TranscriptionState.ERROR
                        )
                    }
                }
            )

            // Check STT availability before starting
            if (speechToTextManager?.isAvailable() == true) {
                speechToTextManager?.startListening()
            } else {
                _uiState.update {
                    it.copy(
                        transcriptionState = TranscriptionState.ERROR,
                        transcriptionError = "Speech recognition not available - you can manually type transcription"
                    )
                }
            }
        }
    }

    /**
     * Stop recording audio response (Phase 2: with simultaneous STT)
     */
    fun stopRecording() {
        if (!_uiState.value.canStopRecording()) {
            return
        }

        viewModelScope.launch {
            // 1. Stop audio recording
            val result = audioRecorder.stopRecording()
            if (result == null) {
                _uiState.update {
                    it.copy(
                        recordingState = RecordingState.IDLE,
                        audioFilePath = null,
                        error = "Failed to save recording"
                    )
                }
                // Also stop STT
                speechToTextManager?.stopListening()
                return@launch
            }

            val (filePath, duration) = result

            // 2. Stop speech-to-text
            speechToTextManager?.stopListening()

            _uiState.update {
                it.copy(
                    recordingState = RecordingState.RECORDED,
                    audioFilePath = filePath,
                    audioDurationMs = duration,
                    transcriptionState = if (it.transcriptionState == TranscriptionState.LISTENING) {
                        TranscriptionState.PROCESSING
                    } else {
                        it.transcriptionState
                    },
                    error = null
                )
            }

            // Note: User can now review/edit transcription before submitting
        }
    }

    /**
     * Cancel current recording and reset (Phase 2: also cancel STT)
     */
    fun cancelRecording() {
        audioRecorder.cancelRecording()
        speechToTextManager?.cancel()
        speechToTextManager?.release()
        speechToTextManager = null

        _uiState.update {
            it.copy(
                recordingState = RecordingState.IDLE,
                audioFilePath = null,
                audioDurationMs = 0L,
                transcriptionText = "",
                finalTranscription = "",
                liveTranscription = "",
                transcriptionState = TranscriptionState.IDLE,
                transcriptionError = null,
                error = null
            )
        }
    }

    /**
     * Update transcription text (user can edit) - Phase 2
     */
    fun updateTranscription(text: String) {
        _uiState.update {
            it.copy(
                finalTranscription = text,
                transcriptionText = text // Keep deprecated field in sync
            )
        }
    }

    /**
     * Submit current response and move to next question
     */
    fun submitResponse() {
        if (!_uiState.value.canSubmitResponse()) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSubmittingResponse = true,
                    error = null
                )
            }

            try {
                val state = _uiState.value
                val currentQuestion = state.currentQuestion ?: return@launch
                val session = state.session ?: return@launch

                // Analyze response with AI (using transcribed text) - Phase 2: use finalTranscription
                val analysisResult = aiService.analyzeResponse(
                    question = currentQuestion,
                    response = state.finalTranscription,
                    responseMode = state.mode.name
                )

                val analysis = if (analysisResult.isSuccess) {
                    analysisResult.getOrNull()
                } else {
                    ErrorLogger.log(
                        throwable = analysisResult.exceptionOrNull() ?: Exception("Unknown error"),
                        description = "AI analysis failed for interview response"
                    )
                    null
                }

                // Convert OLQScoreWithReasoning to OLQScore
                val olqScores = analysis?.olqScores?.mapValues { (_, scoreWithReasoning) ->
                    OLQScore(
                        score = scoreWithReasoning.score.toInt().coerceIn(1, 5),
                        confidence = analysis.overallConfidence,
                        reasoning = scoreWithReasoning.reasoning
                    )
                } ?: emptyMap()

                // Create response object - Phase 2: use finalTranscription
                val response = InterviewResponse(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    questionId = currentQuestion.id,
                    responseText = state.finalTranscription,
                    responseMode = state.mode,
                    respondedAt = Instant.now(),
                    thinkingTimeSec = state.getThinkingTimeSeconds(),
                    audioUrl = state.audioFilePath, // Store local path
                    olqScores = olqScores,
                    confidenceScore = analysis?.overallConfidence ?: 0
                )

                // Submit response
                val submitResult = interviewRepository.submitResponse(response)
                if (submitResult.isFailure) {
                    ErrorLogger.log(
                        throwable = submitResult.exceptionOrNull() ?: Exception("Unknown error"),
                        description = "Failed to submit interview response"
                    )
                    _uiState.update {
                        it.copy(
                            isSubmittingResponse = false,
                            error = "Failed to submit response"
                        )
                    }
                    return@launch
                }

                // Check if there are more questions
                if (state.hasMoreQuestions()) {
                    // Move to next question
                    loadNextQuestion()
                } else {
                    // Complete interview
                    completeInterview()
                }
            } catch (e: Exception) {
                ErrorLogger.log(e, "Exception submitting interview response")
                _uiState.update {
                    it.copy(
                        isSubmittingResponse = false,
                        error = "An error occurred"
                    )
                }
            }
        }
    }

    /**
     * Load next question
     */
    private suspend fun loadNextQuestion() {
        val state = _uiState.value
        val session = state.session ?: return

        val nextIndex = state.currentQuestionIndex + 1
        val nextQuestionId = session.questionIds.getOrNull(nextIndex) ?: return

        // Update session progress
        val updatedSession = session.copy(currentQuestionIndex = nextIndex)
        interviewRepository.updateSession(updatedSession)

        // Load next question
        val questionResult = interviewRepository.getQuestion(nextQuestionId)
        if (questionResult.isFailure) {
            ErrorLogger.log(
                throwable = questionResult.exceptionOrNull() ?: Exception("Unknown error"),
                description = "Failed to load next question"
            )
            _uiState.update {
                it.copy(
                    isSubmittingResponse = false,
                    error = "Failed to load next question"
                )
            }
            return
        }

        val question = questionResult.getOrNull()

        _uiState.update {
            it.copy(
                isSubmittingResponse = false,
                session = updatedSession,
                currentQuestion = question,
                currentQuestionIndex = nextIndex,
                recordingState = RecordingState.IDLE,
                audioFilePath = null,
                audioDurationMs = 0L,
                transcriptionText = "",
                finalTranscription = "",
                liveTranscription = "",
                transcriptionState = TranscriptionState.IDLE,
                transcriptionError = null,
                thinkingStartTime = System.currentTimeMillis()
            )
        }
    }

    /**
     * Complete interview and generate result
     */
    private suspend fun completeInterview() {
        _uiState.update {
            it.copy(
                isSubmittingResponse = true,
                loadingMessage = "Generating results..."
            )
        }

        try {
            val resultResult = interviewRepository.completeInterview(sessionId)
            if (resultResult.isFailure) {
                ErrorLogger.log(
                    throwable = resultResult.exceptionOrNull() ?: Exception("Unknown error"),
                    description = "Failed to complete interview"
                )
                _uiState.update {
                    it.copy(
                        isSubmittingResponse = false,
                        loadingMessage = null,
                        error = "Failed to complete interview"
                    )
                }
                return
            }

            val result = resultResult.getOrNull()
            if (result == null) {
                _uiState.update {
                    it.copy(
                        isSubmittingResponse = false,
                        loadingMessage = null,
                        error = "Failed to generate results"
                    )
                }
                return
            }

            _uiState.update {
                it.copy(
                    isSubmittingResponse = false,
                    loadingMessage = null,
                    isCompleted = true,
                    resultId = result.id
                )
            }
        } catch (e: Exception) {
            ErrorLogger.log(e, "Exception completing interview")
            _uiState.update {
                it.copy(
                    isSubmittingResponse = false,
                    loadingMessage = null,
                    error = "An error occurred"
                )
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Clean up resources - Phase 2: also release STT manager
     */
    override fun onCleared() {
        super.onCleared()
        audioRecorder.release()
        speechToTextManager?.release()
        speechToTextManager = null
    }
}
