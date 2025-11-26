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
    private val onError: (String) -> Unit
) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

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
                        val matches = results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )
                        val transcription = matches?.firstOrNull() ?: ""

                        Log.d(TAG, "📝 Transcription complete: $transcription")
                        onResult(transcription)
                        isListening = false
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
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
                        val errorMessage = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Client error"
                            SpeechRecognizer.ERROR_NETWORK -> "Network error - please check your connection"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout - please try again"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected - please try again"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy - please try again"
                            SpeechRecognizer.ERROR_SERVER -> "Server error - please try again"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Missing microphone permission"
                            else -> "Speech recognition error (code: $error)"
                        }

                        Log.e(TAG, "❌ STT Error: $errorMessage")
                        ErrorLogger.log(
                            throwable = Exception("SpeechRecognizer error: $error"),
                            description = errorMessage
                        )
                        onError(errorMessage)
                        isListening = false
                    }

                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d(TAG, "🎤 Ready for speech")
                        isListening = true
                    }

                    override fun onBeginningOfSpeech() {
                        Log.d(TAG, "🗣️ Speech started")
                    }

                    override fun onEndOfSpeech() {
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
                // Silence detection: 2 seconds of silence completes recognition
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
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
        try {
            speechRecognizer?.cancel()
            isListening = false
            Log.d(TAG, "❌ Cancelled speech recognition")
        } catch (e: Exception) {
            ErrorLogger.log(e, "Failed to cancel speech recognition")
        }
    }

    /**
     * Check if currently listening
     */
    fun isCurrentlyListening(): Boolean = isListening

    /**
     * Release resources and cleanup
     * MUST be called when done to prevent memory leaks
     */
    fun release() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
            isListening = false
            Log.d(TAG, "🧹 Released SpeechToTextManager resources")
        } catch (e: Exception) {
            ErrorLogger.log(e, "Failed to release speech recognizer")
        }
    }

    companion object {
        private const val TAG = "SpeechToTextManager"
    }
}
