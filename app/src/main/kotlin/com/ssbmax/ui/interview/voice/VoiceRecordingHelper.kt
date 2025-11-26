package com.ssbmax.ui.interview.voice

import android.content.Context
import com.ssbmax.utils.AudioRecorder
import com.ssbmax.utils.ErrorLogger
import com.ssbmax.utils.SpeechToTextManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * State holder for voice recording operations
 */
data class VoiceRecordingState(
    val recordingState: RecordingState = RecordingState.IDLE,
    val audioFilePath: String? = null,
    val audioDurationMs: Long = 0L,
    val transcriptionState: TranscriptionState = TranscriptionState.IDLE,
    val liveTranscription: String = "",
    val finalTranscription: String = "",
    val transcriptionError: String? = null
)

/**
 * Helper class for managing voice recording and speech-to-text transcription
 *
 * Extracted from VoiceInterviewSessionViewModel to maintain file size limits.
 * Emits a single [VoiceRecordingState] that contains all recording state.
 */
class VoiceRecordingHelper(private val context: Context) {

    private val audioRecorder = AudioRecorder(context)
    private var speechToTextManager: SpeechToTextManager? = null

    private val _state = MutableStateFlow(VoiceRecordingState())
    val state: StateFlow<VoiceRecordingState> = _state.asStateFlow()

    /**
     * Start recording audio with simultaneous speech-to-text
     * @return file path if recording started successfully, null otherwise
     */
    fun startRecording(): String? {
        val filePath = audioRecorder.startRecording() ?: return null

        _state.update {
            it.copy(
                recordingState = RecordingState.RECORDING,
                audioFilePath = filePath,
                transcriptionState = TranscriptionState.IDLE,
                liveTranscription = "",
                finalTranscription = "",
                transcriptionError = null
            )
        }

        startSpeechToText()
        return filePath
    }

    private fun startSpeechToText() {
        speechToTextManager = SpeechToTextManager(
            context = context,
            onResult = { transcription ->
                _state.update {
                    it.copy(
                        finalTranscription = transcription,
                        transcriptionState = TranscriptionState.COMPLETED,
                        liveTranscription = ""
                    )
                }
            },
            onPartialResult = { partial ->
                _state.update {
                    it.copy(
                        liveTranscription = partial,
                        transcriptionState = TranscriptionState.LISTENING
                    )
                }
            },
            onError = { error ->
                ErrorLogger.log(
                    throwable = Exception("Speech-to-text error: $error"),
                    description = "STT failed during voice interview"
                )
                _state.update {
                    it.copy(
                        transcriptionError = error,
                        transcriptionState = TranscriptionState.ERROR
                    )
                }
            }
        )

        if (speechToTextManager?.isAvailable() == true) {
            speechToTextManager?.startListening()
        } else {
            _state.update {
                it.copy(
                    transcriptionState = TranscriptionState.ERROR,
                    transcriptionError = "Speech recognition not available - you can manually type transcription"
                )
            }
        }
    }

    /**
     * Stop recording and finalize transcription
     * @return Pair of (filePath, durationMs) if successful, null otherwise
     */
    fun stopRecording(): Pair<String, Long>? {
        val result = audioRecorder.stopRecording()
        speechToTextManager?.stopListening()

        if (result == null) {
            _state.update {
                it.copy(recordingState = RecordingState.IDLE, audioFilePath = null)
            }
            return null
        }

        val (filePath, duration) = result
        _state.update {
            it.copy(
                recordingState = RecordingState.RECORDED,
                audioFilePath = filePath,
                audioDurationMs = duration,
                transcriptionState = if (it.transcriptionState == TranscriptionState.LISTENING) {
                    TranscriptionState.PROCESSING
                } else {
                    it.transcriptionState
                }
            )
        }
        return result
    }

    /** Cancel recording and reset all state */
    fun cancelRecording() {
        audioRecorder.cancelRecording()
        speechToTextManager?.cancel()
        speechToTextManager?.release()
        speechToTextManager = null
        _state.value = VoiceRecordingState()
    }

    /** Update transcription text (user manual edit) */
    fun updateTranscription(text: String) {
        _state.update { it.copy(finalTranscription = text) }
    }

    /** Reset for next question */
    fun resetForNextQuestion() {
        _state.value = VoiceRecordingState()
    }

    /** Release all resources - MUST be called when done */
    fun release() {
        audioRecorder.release()
        speechToTextManager?.release()
        speechToTextManager = null
    }
}
