package com.ssbmax.ui.interview.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
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
 * 
 * Features:
 * - Auto-restarts STT on silence (user can pause to think during recording)
 * - Accumulates transcription across STT sessions
 * - Allows manual transcription editing
 * - Proper cleanup with cancellable handlers
 */
class VoiceRecordingHelper(private val context: Context) {

    companion object {
        private const val TAG = "VoiceRecordingHelper"
    }

    private val audioRecorder = AudioRecorder(context)
    private var speechToTextManager: SpeechToTextManager? = null
    
    /** Accumulated transcription across multiple STT sessions */
    private val accumulatedTranscription = StringBuilder()
    
    /** Handler for STT restart delays - uses single instance for proper cancellation */
    private val handler = Handler(Looper.getMainLooper())
    
    /** Pending STT restart runnable - tracked for cancellation */
    private var pendingRestartRunnable: Runnable? = null
    
    /** Flag to prevent callbacks after release */
    @Volatile
    private var isReleased = false

    private val _state = MutableStateFlow(VoiceRecordingState())
    val state: StateFlow<VoiceRecordingState> = _state.asStateFlow()

    /**
     * Start recording audio with simultaneous speech-to-text
     * @return file path if recording started successfully, null otherwise
     */
    fun startRecording(): String? {
        if (isReleased) {
            Log.w(TAG, "⚠️ startRecording called after release, ignoring")
            return null
        }
        
        Log.d(TAG, "🎤 Starting recording...")
        val filePath = audioRecorder.startRecording() ?: return null
        
        // Clear accumulated transcription for new recording
        accumulatedTranscription.clear()

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
        Log.d(TAG, "✅ Recording started: $filePath")
        return filePath
    }

    private fun startSpeechToText() {
        if (isReleased) {
            Log.w(TAG, "⚠️ startSpeechToText called after release, ignoring")
            return
        }
        
        Log.d(TAG, "🎙️ Starting speech-to-text...")
        speechToTextManager?.release()
        speechToTextManager = SpeechToTextManager(
            context = context,
            onResult = { transcription ->
                if (isReleased) {
                    Log.w(TAG, "⚠️ STT onResult after release, ignoring")
                    return@SpeechToTextManager
                }
                
                Log.d(TAG, "📝 STT result: ${transcription.take(50)}...")
                // Accumulate transcription (user may speak in segments)
                if (transcription.isNotBlank()) {
                    if (accumulatedTranscription.isNotEmpty()) {
                        accumulatedTranscription.append(" ")
                    }
                    accumulatedTranscription.append(transcription)
                }
                
                _state.update {
                    it.copy(
                        finalTranscription = accumulatedTranscription.toString(),
                        transcriptionState = TranscriptionState.COMPLETED,
                        liveTranscription = "",
                        transcriptionError = null
                    )
                }
                
                // Auto-restart STT if still recording (user may continue speaking)
                if (_state.value.recordingState == RecordingState.RECORDING && !isReleased) {
                    restartSpeechToText()
                }
            },
            onPartialResult = { partial ->
                if (isReleased) return@SpeechToTextManager
                
                // Show accumulated + current partial
                val fullText = if (accumulatedTranscription.isNotEmpty()) {
                    "${accumulatedTranscription} $partial"
                } else {
                    partial
                }
                _state.update {
                    it.copy(
                        liveTranscription = fullText,
                        transcriptionState = TranscriptionState.LISTENING,
                        transcriptionError = null
                    )
                }
            },
            onError = { error ->
                if (isReleased) {
                    Log.w(TAG, "⚠️ STT onError after release, ignoring: $error")
                    return@SpeechToTextManager
                }
                
                Log.e(TAG, "❌ STT error: $error")
                ErrorLogger.log(
                    throwable = Exception("Speech-to-text error: $error"),
                    description = "STT failed during voice interview"
                )
                _state.update {
                    it.copy(
                        transcriptionError = error,
                        transcriptionState = TranscriptionState.ERROR,
                        // Keep accumulated transcription available for editing
                        finalTranscription = accumulatedTranscription.toString().ifEmpty { 
                            it.finalTranscription 
                        }
                    )
                }
            },
            onSilenceTimeout = {
                if (isReleased) {
                    Log.w(TAG, "⚠️ STT onSilenceTimeout after release, ignoring")
                    return@SpeechToTextManager
                }
                
                Log.d(TAG, "⏳ STT silence timeout, checking if should restart...")
                // Auto-restart STT if still recording - user may be pausing to think
                if (_state.value.recordingState == RecordingState.RECORDING && !isReleased) {
                    _state.update {
                        it.copy(
                            transcriptionState = TranscriptionState.LISTENING,
                            transcriptionError = null
                        )
                    }
                    restartSpeechToText()
                }
            }
        )

        if (speechToTextManager?.isAvailable() == true) {
            speechToTextManager?.startListening()
            Log.d(TAG, "✅ STT started listening")
        } else {
            Log.w(TAG, "⚠️ STT not available")
            _state.update {
                it.copy(
                    transcriptionState = TranscriptionState.ERROR,
                    transcriptionError = "Speech recognition not available - you can type your response"
                )
            }
        }
    }
    
    /** Restart STT for continued recording (called on silence timeout or after result) */
    private fun restartSpeechToText() {
        if (isReleased) {
            Log.w(TAG, "⚠️ restartSpeechToText called after release, ignoring")
            return
        }
        
        Log.d(TAG, "🔄 Scheduling STT restart...")
        speechToTextManager?.release()
        speechToTextManager = null
        
        // Cancel any pending restart first
        cancelPendingRestart()
        
        // Schedule restart with cancellable runnable
        pendingRestartRunnable = Runnable {
            if (!isReleased && _state.value.recordingState == RecordingState.RECORDING) {
                Log.d(TAG, "🔄 Executing STT restart")
                startSpeechToText()
            } else {
                Log.d(TAG, "⏹️ STT restart cancelled (released=$isReleased, state=${_state.value.recordingState})")
            }
            pendingRestartRunnable = null
        }
        handler.postDelayed(pendingRestartRunnable!!, 300)
    }
    
    /** Cancel any pending STT restart */
    private fun cancelPendingRestart() {
        pendingRestartRunnable?.let { runnable ->
            Log.d(TAG, "🚫 Cancelling pending STT restart")
            handler.removeCallbacks(runnable)
            pendingRestartRunnable = null
        }
    }

    /**
     * Stop recording and finalize transcription
     * @return Pair of (filePath, durationMs) if successful, null otherwise
     */
    fun stopRecording(): Pair<String, Long>? {
        if (isReleased) {
            Log.w(TAG, "⚠️ stopRecording called after release, ignoring")
            return null
        }
        
        Log.d(TAG, "⏹️ Stopping recording...")
        
        // Cancel any pending STT restart
        cancelPendingRestart()
        
        val result = audioRecorder.stopRecording()
        speechToTextManager?.stopListening()

        if (result == null) {
            Log.w(TAG, "⚠️ Failed to stop recording")
            _state.update {
                it.copy(recordingState = RecordingState.IDLE, audioFilePath = null)
            }
            return null
        }

        val (filePath, duration) = result
        Log.d(TAG, "✅ Recording stopped: ${duration}ms")
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
        Log.d(TAG, "🛑 Cancelling recording...")
        
        // Cancel pending STT restart first
        cancelPendingRestart()
        
        audioRecorder.cancelRecording()
        speechToTextManager?.cancel()
        speechToTextManager?.release()
        speechToTextManager = null
        accumulatedTranscription.clear()
        _state.value = VoiceRecordingState()
        
        Log.d(TAG, "✅ Recording cancelled")
    }

    /** Update transcription text (user manual edit) */
    fun updateTranscription(text: String) {
        if (isReleased) return
        _state.update { it.copy(finalTranscription = text) }
    }

    /** Reset for next question */
    fun resetForNextQuestion() {
        Log.d(TAG, "🔄 Resetting for next question...")
        
        // Cancel pending STT restart
        cancelPendingRestart()
        
        accumulatedTranscription.clear()
        speechToTextManager?.release()
        speechToTextManager = null
        _state.value = VoiceRecordingState()
        
        Log.d(TAG, "✅ Reset complete")
    }

    /** Release all resources - MUST be called when done */
    fun release() {
        Log.d(TAG, "🧹 Releasing all resources...")
        
        // Set released flag FIRST to prevent any callbacks
        isReleased = true
        
        // Cancel any pending restarts
        cancelPendingRestart()
        
        // Release all resources
        audioRecorder.release()
        speechToTextManager?.cancel()
        speechToTextManager?.release()
        speechToTextManager = null
        accumulatedTranscription.clear()
        
        Log.d(TAG, "✅ All resources released")
    }
}
