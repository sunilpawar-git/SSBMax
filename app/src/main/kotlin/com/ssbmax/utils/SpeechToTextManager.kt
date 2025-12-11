package com.ssbmax.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * Manages speech-to-text recognition for voice interview responses
 *
 * Features:
 * - Real-time partial transcription results
 * - Automatic silence detection
 * - English (India) language model optimized for SSB candidates
 * - Error handling with detailed feedback
 * - Resource cleanup to prevent memory leaks
 *
 * Usage:
 * ```
 * val sttManager = SpeechToTextManager(
 *     context = context,
 *     onResult = { transcription -> /* Use final result */ },
 *     onPartialResult = { partial -> /* Update UI with live transcription */ },
 *     onError = { error -> /* Handle error */ }
 * )
 * sttManager.startListening()
 * // ... user speaks ...
 * sttManager.stopListening()
 * sttManager.release() // When done
 * ```
 */
class SpeechToTextManager(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onPartialResult: (String) -> Unit = {},
    private val onError: (String) -> Unit,
    private val onSilenceTimeout: () -> Unit = {} // Called when no speech detected - can restart
) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var accumulatedTranscription = StringBuilder()
    
    /** Flag to prevent callbacks after release/cancel */
    @Volatile
    private var isReleased = false

    /**
     * Check if speech recognition is available on this device
     */
    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    /**
     * Start listening for speech input
     */
    fun startListening() {
        if (!isAvailable()) {
            onError("Speech recognition not available on this device")
            return
        }

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onResults(results: Bundle?) {
                        if (isReleased) {
                            Log.d(TAG, "⚠️ onResults after release, ignoring")
                            return
                        }
                        
                        val matches = results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )
                        val transcription = matches?.firstOrNull() ?: ""

                        Log.d(TAG, "📝 Transcription complete: $transcription")
                        isListening = false
                        onResult(transcription)
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        if (isReleased) return
                        
                        val matches = partialResults?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )
                        val partial = matches?.firstOrNull() ?: ""

                        if (partial.isNotBlank()) {
                            Log.d(TAG, "🔄 Partial result: $partial")
                            onPartialResult(partial)
                        }
                    }

                    override fun onError(error: Int) {
                        isListening = false
                        
                        if (isReleased) {
                            Log.d(TAG, "⚠️ onError after release, ignoring: $error")
                            return
                        }
                        
                        // Handle silence/no-match errors gracefully - these are normal during recording
                        // User may be thinking before speaking
                        if (error == SpeechRecognizer.ERROR_NO_MATCH || 
                            error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                            Log.d(TAG, "⏳ Silence detected, calling onSilenceTimeout for auto-restart")
                            onSilenceTimeout()
                            return
                        }
                        
                        val errorMessage = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Client error"
                            SpeechRecognizer.ERROR_NETWORK -> "Network error - please check your connection"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout - please try again"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy - please try again"
                            SpeechRecognizer.ERROR_SERVER -> "Server error - please try again"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Missing microphone permission"
                            else -> "Speech recognition error (code: $error)"
                        }

                        Log.e(TAG, "❌ STT Error: $errorMessage")
                        ErrorLogger.log(
                            throwable = Exception("SpeechRecognizer error: $error"),
                            description = errorMessage
                        )
                        onError(errorMessage)
                    }

                    override fun onReadyForSpeech(params: Bundle?) {
                        if (isReleased) return
                        Log.d(TAG, "🎤 Ready for speech")
                        isListening = true
                    }

                    override fun onBeginningOfSpeech() {
                        if (isReleased) return
                        Log.d(TAG, "🗣️ Speech started")
                    }

                    override fun onEndOfSpeech() {
                        if (isReleased) return
                        Log.d(TAG, "🛑 Speech ended")
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // Audio level changed - can use for visualization
                        // Not logging to avoid spam
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {
                        // Audio buffer received - not used
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {
                        // Custom events - not used
                    }
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN") // English (India)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                // Extended silence detection for interview responses (user may pause to think)
                // Complete silence = 4 seconds (after speech detected, wait 4s before finalizing)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 4000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                // Allow up to 60 seconds of recording (interview answers can be long)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 60000L)
            }

            speechRecognizer?.startListening(intent)
            Log.d(TAG, "▶️ Started listening for speech")
        } catch (e: Exception) {
            ErrorLogger.log(e, "Failed to start speech recognition")
            onError("Failed to start speech recognition")
            isListening = false
        }
    }

    /**
     * Stop listening and finalize transcription
     */
    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            Log.d(TAG, "⏹️ Stopped listening")
        } catch (e: Exception) {
            ErrorLogger.log(e, "Failed to stop speech recognition")
        }
    }

    /**
     * Cancel listening without returning results
     */
    fun cancel() {
        Log.d(TAG, "❌ Cancelling speech recognition...")
        try {
            // Set released flag FIRST to prevent callbacks
            isReleased = true
            speechRecognizer?.cancel()
            isListening = false
            Log.d(TAG, "✅ Speech recognition cancelled")
        } catch (e: Exception) {
            ErrorLogger.log(e, "Failed to cancel speech recognition")
        }
    }

    /**
     * Check if currently listening
     */
    fun isCurrentlyListening(): Boolean = isListening && !isReleased

    /**
     * Release resources and cleanup
     * MUST be called when done to prevent memory leaks
     */
    fun release() {
        Log.d(TAG, "🧹 Releasing SpeechToTextManager resources...")
        try {
            // Set released flag FIRST to prevent callbacks
            isReleased = true
            speechRecognizer?.destroy()
            speechRecognizer = null
            isListening = false
            Log.d(TAG, "✅ SpeechToTextManager resources released")
        } catch (e: Exception) {
            ErrorLogger.log(e, "Failed to release speech recognizer")
        }
    }

    companion object {
        private const val TAG = "SpeechToTextManager"
    }
}
