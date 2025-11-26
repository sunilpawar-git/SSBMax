package com.ssbmax.ui.interview.voice

import com.ssbmax.core.domain.model.interview.InterviewMode
import com.ssbmax.core.domain.model.interview.InterviewQuestion
import com.ssbmax.core.domain.model.interview.InterviewSession

/**
 * UI state for Voice Interview Session screen
 *
 * Manages voice recording, playback, and transcription
 */
data class VoiceInterviewSessionUiState(
    val isLoading: Boolean = true,
    val loadingMessage: String? = null,
    val session: InterviewSession? = null,
    val currentQuestion: InterviewQuestion? = null,
    val currentQuestionIndex: Int = 0,
    val totalQuestions: Int = 0,
    val recordingState: RecordingState = RecordingState.IDLE,
    val audioFilePath: String? = null,
    val audioDurationMs: Long = 0L,
    val isSubmittingResponse: Boolean = false,
    val thinkingStartTime: Long? = null,
    val error: String? = null,
    val isCompleted: Boolean = false,
    val resultId: String? = null,
    val hasRecordPermission: Boolean = false,
    // Phase 2: Speech-to-Text fields
    val transcriptionState: TranscriptionState = TranscriptionState.IDLE,
    val liveTranscription: String = "",
    val finalTranscription: String = "",
    val transcriptionError: String? = null
) {
    /**
     * Get interview mode
     */
    val mode: InterviewMode
        get() = session?.mode ?: InterviewMode.VOICE_BASED

    /**
     * Calculate progress percentage (0-100)
     */
    fun getProgressPercentage(): Int {
        if (totalQuestions == 0) return 0
        return ((currentQuestionIndex.toFloat() / totalQuestions) * 100).toInt()
    }

    /**
     * Check if response is ready to submit
     * (audio recorded AND transcription available)
     */
    fun canSubmitResponse(): Boolean {
        return audioFilePath != null &&
                finalTranscription.trim().isNotBlank() &&
                !isSubmittingResponse &&
                !isLoading &&
                transcriptionState !in listOf(TranscriptionState.LISTENING, TranscriptionState.PROCESSING) &&
                currentQuestion != null &&
                recordingState == RecordingState.RECORDED
    }

    /**
     * Check if there are more questions
     */
    fun hasMoreQuestions(): Boolean {
        return currentQuestionIndex < totalQuestions - 1
    }

    /**
     * Get thinking time in seconds
     */
    fun getThinkingTimeSeconds(): Int {
        val startTime = thinkingStartTime ?: return 0
        return ((System.currentTimeMillis() - startTime) / 1000).toInt()
    }

    /**
     * Get audio duration in formatted string (MM:SS)
     */
    fun getFormattedDuration(): String {
        val seconds = (audioDurationMs / 1000).toInt()
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    /**
     * Check if can start recording
     */
    fun canStartRecording(): Boolean {
        return hasRecordPermission &&
                !isLoading &&
                recordingState == RecordingState.IDLE &&
                currentQuestion != null
    }

    /**
     * Check if can stop recording
     */
    fun canStopRecording(): Boolean {
        return recordingState == RecordingState.RECORDING
    }

    /**
     * Check if can play audio
     */
    fun canPlayAudio(): Boolean {
        return audioFilePath != null &&
                recordingState == RecordingState.RECORDED &&
                transcriptionState !in listOf(TranscriptionState.LISTENING, TranscriptionState.PROCESSING)
    }

    /**
     * Check if can re-record
     */
    fun canReRecord(): Boolean {
        return recordingState == RecordingState.RECORDED &&
                !isSubmittingResponse &&
                transcriptionState !in listOf(TranscriptionState.LISTENING, TranscriptionState.PROCESSING)
    }
}

/**
 * Audio recording states
 */
enum class RecordingState {
    IDLE,           // No recording yet
    RECORDING,      // Currently recording
    RECORDED,       // Recording complete, ready for playback/transcription
    PLAYING         // Playing back recorded audio
}

/**
 * Speech-to-text transcription states (Phase 2)
 */
enum class TranscriptionState {
    IDLE,           // No transcription activity
    LISTENING,      // SpeechRecognizer active, receiving audio
    PROCESSING,     // Finalizing transcription
    COMPLETED,      // Transcription complete
    ERROR           // Transcription failed
}
