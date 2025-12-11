package com.ssbmax.utils

import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale
import java.util.UUID

/**
 * Manages text-to-speech for interviewer voice in voice interviews
 *
 * Features:
 * - English (India) locale optimized for SSB interviews
 * - Callback when speech completes (for enabling recording)
 * - Queue management for sequential speech
 * - Adjustable speech rate and pitch
 * - Resource cleanup to prevent memory leaks
 *
 * Usage:
 * ```
 * val ttsManager = TextToSpeechManager(
 *     context = context,
 *     onReady = { /* TTS engine initialized */ },
 *     onSpeechComplete = { /* Speech finished, can start recording */ },
 *     onError = { error -> /* Handle error */ }
 * )
 * ttsManager.speak("Tell me about yourself")
 * // ... user listens ...
 * ttsManager.release() // When done
 * ```
 */
class TextToSpeechManager(
    context: Context,
    private val onReady: () -> Unit = {},
    private val onSpeechComplete: () -> Unit = {},
    private val onError: (String) -> Unit = {}
) {

    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    private var pendingText: String? = null
    
    /** Flag to prevent callbacks after release/stop */
    @Volatile
    private var isReleased = false

    init {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                setupTTS()
            } else {
                val errorMsg = "Failed to initialize TextToSpeech engine"
                Log.e(TAG, "❌ $errorMsg (status: $status)")
                ErrorLogger.log(
                    throwable = Exception("TTS initialization failed with status: $status"),
                    description = errorMsg
                )
                onError(errorMsg)
            }
        }
    }

    private fun setupTTS() {
        textToSpeech?.let { tts ->
            // Set English (India) locale for SSB context
            val localeResult = tts.setLanguage(Locale("en", "IN"))
            
            if (localeResult == TextToSpeech.LANG_MISSING_DATA ||
                localeResult == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                // Fallback to generic English
                val fallbackResult = tts.setLanguage(Locale.ENGLISH)
                if (fallbackResult == TextToSpeech.LANG_MISSING_DATA ||
                    fallbackResult == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    Log.w(TAG, "⚠️ English not fully supported, using default")
                } else {
                    Log.d(TAG, "📢 Using fallback English locale")
                }
            } else {
                Log.d(TAG, "📢 Using English (India) locale")
            }
            
            // Try to select the best quality voice (neural/network voices sound more natural)
            selectBestVoice(tts)

            // Set speech parameters for clear, professional delivery
            tts.setSpeechRate(SPEECH_RATE)
            tts.setPitch(SPEECH_PITCH)

            // Set up listener for speech events
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    if (isReleased) return
                    Log.d(TAG, "🗣️ Speech started: $utteranceId")
                }

                override fun onDone(utteranceId: String?) {
                    if (isReleased) {
                        Log.d(TAG, "⚠️ onDone after release, ignoring: $utteranceId")
                        return
                    }
                    Log.d(TAG, "✅ Speech completed: $utteranceId")
                    onSpeechComplete()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    if (isReleased) {
                        Log.d(TAG, "⚠️ onError after release, ignoring: $utteranceId")
                        return
                    }
                    Log.e(TAG, "❌ Speech error: $utteranceId")
                    onError("Speech synthesis error")
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    if (isReleased) {
                        Log.d(TAG, "⚠️ onError after release, ignoring: $utteranceId")
                        return
                    }
                    val errorMsg = when (errorCode) {
                        TextToSpeech.ERROR_SYNTHESIS -> "Speech synthesis failed"
                        TextToSpeech.ERROR_SERVICE -> "TTS service error"
                        TextToSpeech.ERROR_OUTPUT -> "Audio output error"
                        TextToSpeech.ERROR_NETWORK -> "Network error"
                        TextToSpeech.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        TextToSpeech.ERROR_INVALID_REQUEST -> "Invalid request"
                        TextToSpeech.ERROR_NOT_INSTALLED_YET -> "TTS not installed"
                        else -> "Unknown TTS error (code: $errorCode)"
                    }
                    Log.e(TAG, "❌ $errorMsg")
                    ErrorLogger.log(
                        throwable = Exception("TTS error: $errorCode"),
                        description = errorMsg
                    )
                    onError(errorMsg)
                }
            })

            isInitialized = true
            Log.d(TAG, "✅ TextToSpeech initialized successfully")
            
            if (!isReleased) {
                onReady()

                // Speak any pending text
                pendingText?.let { text ->
                    speak(text)
                    pendingText = null
                }
            } else {
                Log.d(TAG, "⚠️ TTS ready after release, ignoring")
            }
        }
    }

    /**
     * Speak the given text aloud
     *
     * @param text The text to speak
     * @param flush If true, stops current speech and starts new. If false, queues after current.
     */
    fun speak(text: String, flush: Boolean = true) {
        if (!isInitialized) {
            Log.d(TAG, "📝 TTS not ready, queuing text")
            pendingText = text
            return
        }

        val utteranceId = UUID.randomUUID().toString()
        val queueMode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD

        val result = textToSpeech?.speak(
            text,
            queueMode,
            null,
            utteranceId
        )

        if (result == TextToSpeech.ERROR) {
            Log.e(TAG, "❌ Failed to queue speech")
            onError("Failed to speak text")
        } else {
            Log.d(TAG, "📢 Queued speech: ${text.take(50)}...")
        }
    }

    /**
     * Stop any ongoing speech immediately
     */
    fun stop() {
        Log.d(TAG, "⏹️ Stopping speech...")
        isReleased = true  // Prevent callbacks from firing after stop
        textToSpeech?.stop()
        Log.d(TAG, "✅ Speech stopped")
    }

    /**
     * Check if TTS is currently speaking
     */
    fun isSpeaking(): Boolean {
        return textToSpeech?.isSpeaking == true
    }

    /**
     * Check if TTS engine is ready
     */
    fun isReady(): Boolean = isInitialized

    /**
     * Set speech rate (0.5 = half speed, 1.0 = normal, 2.0 = double)
     */
    fun setSpeechRate(rate: Float) {
        textToSpeech?.setSpeechRate(rate.coerceIn(0.5f, 2.0f))
    }

    /**
     * Set pitch (0.5 = lower, 1.0 = normal, 2.0 = higher)
     */
    fun setPitch(pitch: Float) {
        textToSpeech?.setPitch(pitch.coerceIn(0.5f, 2.0f))
    }

    /**
     * Release resources and cleanup
     * MUST be called when done to prevent memory leaks
     */
    fun release() {
        Log.d(TAG, "🧹 Releasing TextToSpeechManager resources...")
        try {
            // Set released flag FIRST to prevent callbacks
            isReleased = true
            
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
            isInitialized = false
            pendingText = null
            Log.d(TAG, "✅ TextToSpeechManager resources released")
        } catch (e: Exception) {
            ErrorLogger.log(e, "Failed to release TextToSpeech")
        }
    }

    /**
     * Select the best available voice for natural-sounding speech
     * 
     * Prioritizes:
     * 1. Network/neural voices (most natural, requires internet)
     * 2. High-quality local voices 
     * 3. Any English voice
     * 4. Default voice
     */
    private fun selectBestVoice(tts: TextToSpeech) {
        try {
            val voices = tts.voices ?: return
            
            // Filter to English voices only
            val englishVoices = voices.filter { voice ->
                voice.locale.language == "en" && 
                !voice.isNetworkConnectionRequired // Prefer offline for reliability
            }
            
            if (englishVoices.isEmpty()) {
                Log.d(TAG, "📢 No English voices available, using default")
                return
            }
            
            // Sort voices by quality (lower latency and features = better)
            // Prefer voices that are NOT "legacy" and have low latency
            val sortedVoices = englishVoices.sortedWith(
                compareBy(
                    // Prefer voices with "not legacy" feature
                    { voice -> !voice.features.contains("legacySetLanguageVoice") },
                    // Prefer lower quality rating (confusingly, lower = better in TTS API)
                    { voice -> voice.quality },
                    // Prefer voices matching Indian English
                    { voice -> if (voice.locale.country == "IN") 0 else 1 }
                )
            )
            
            // Try to find a high-quality voice
            val bestVoice = sortedVoices.firstOrNull { voice ->
                voice.quality >= Voice.QUALITY_NORMAL
            } ?: sortedVoices.firstOrNull()
            
            bestVoice?.let { voice ->
                val result = tts.setVoice(voice)
                if (result == TextToSpeech.SUCCESS) {
                    Log.d(TAG, "📢 Selected voice: ${voice.name} (${voice.locale}, quality=${voice.quality})")
                } else {
                    Log.w(TAG, "⚠️ Failed to set voice: ${voice.name}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Error selecting voice, using default: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "TextToSpeechManager"
        
        // Professional, clear speech rate (slightly slower for clarity)
        private const val SPEECH_RATE = 0.95f
        
        // Normal pitch for professional tone
        private const val SPEECH_PITCH = 1.0f
    }
}

