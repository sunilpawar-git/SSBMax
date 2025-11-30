package com.ssbmax.utils.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.ssbmax.utils.ErrorLogger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

/**
 * Android's built-in Text-to-Speech implementation
 *
 * Used for Free tier users. Provides functional but robotic-sounding voice.
 *
 * Features:
 * - English (India) locale optimized for SSB interviews
 * - Automatic voice selection for best available quality
 * - Proper resource cleanup
 *
 * @param context Application context for TTS initialization
 */
class AndroidTTSService @Inject constructor(
    private val context: Context
) : TTSService {

    companion object {
        private const val TAG = "AndroidTTSService"
        private const val SPEECH_RATE = 0.95f
        private const val SPEECH_PITCH = 1.0f
    }

    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    private var initializationFailed = false
    private var pendingText: String? = null

    @Volatile
    private var isReleased = false

    private val _events = MutableSharedFlow<TTSService.TTSEvent>(replay = 1)
    override val events: SharedFlow<TTSService.TTSEvent> = _events.asSharedFlow()

    init {
        initializeTTS()
    }

    private fun initializeTTS() {
        Log.d(TAG, "🔧 Initializing Android TTS...")
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                setupTTS()
            } else {
                val errorMsg = "Failed to initialize Android TTS engine"
                Log.e(TAG, "❌ $errorMsg (status: $status)")
                ErrorLogger.log(
                    throwable = Exception("TTS initialization failed with status: $status"),
                    description = errorMsg
                )
                initializationFailed = true
                _events.tryEmit(TTSService.TTSEvent.Error(errorMsg))
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

            selectBestVoice(tts)
            tts.setSpeechRate(SPEECH_RATE)
            tts.setPitch(SPEECH_PITCH)

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
                    _events.tryEmit(TTSService.TTSEvent.SpeechComplete)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    if (isReleased) return
                    Log.e(TAG, "❌ Speech error: $utteranceId")
                    _events.tryEmit(TTSService.TTSEvent.Error("Speech synthesis error"))
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    if (isReleased) return
                    val errorMsg = mapErrorCode(errorCode)
                    Log.e(TAG, "❌ $errorMsg")
                    ErrorLogger.log(Exception("TTS error: $errorCode"), errorMsg)
                    _events.tryEmit(TTSService.TTSEvent.Error(errorMsg))
                }
            })

            isInitialized = true
            Log.d(TAG, "✅ Android TTS initialized successfully")

            if (!isReleased) {
                _events.tryEmit(TTSService.TTSEvent.Ready)
                pendingText?.let { text ->
                    speakInternal(text, flush = true)
                    pendingText = null
                }
            }
        }
    }

    private fun mapErrorCode(errorCode: Int): String = when (errorCode) {
        TextToSpeech.ERROR_SYNTHESIS -> "Speech synthesis failed"
        TextToSpeech.ERROR_SERVICE -> "TTS service error"
        TextToSpeech.ERROR_OUTPUT -> "Audio output error"
        TextToSpeech.ERROR_NETWORK -> "Network error"
        TextToSpeech.ERROR_NETWORK_TIMEOUT -> "Network timeout"
        TextToSpeech.ERROR_INVALID_REQUEST -> "Invalid request"
        TextToSpeech.ERROR_NOT_INSTALLED_YET -> "TTS not installed"
        else -> "Unknown TTS error (code: $errorCode)"
    }

    override suspend fun speak(text: String, flush: Boolean) {
        if (!isInitialized) {
            Log.d(TAG, "📝 TTS not initialized yet, queuing text")
            pendingText = text
            return
        }
        speakInternal(text, flush)
    }

    private fun speakInternal(text: String, flush: Boolean) {
        val utteranceId = UUID.randomUUID().toString()
        val queueMode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD

        val result = textToSpeech?.speak(text, queueMode, null, utteranceId)

        if (result == TextToSpeech.ERROR) {
            Log.e(TAG, "❌ Failed to queue speech")
            _events.tryEmit(TTSService.TTSEvent.Error("Failed to speak text"))
        } else {
            Log.d(TAG, "📢 Queued speech: ${text.take(50)}...")
        }
    }

    override fun stop() {
        Log.d(TAG, "⏹️ Stopping speech...")
        textToSpeech?.stop()
        Log.d(TAG, "✅ Speech stopped")
    }

    override fun release() {
        Log.d(TAG, "🧹 Releasing Android TTS resources...")
        try {
            isReleased = true
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
            isInitialized = false
            pendingText = null
            Log.d(TAG, "✅ Android TTS resources released")
        } catch (e: Exception) {
            ErrorLogger.log(e, "Failed to release Android TTS")
        }
    }

    override fun isReady(): Boolean = !isReleased && !initializationFailed

    override fun isSpeaking(): Boolean = textToSpeech?.isSpeaking == true

    private fun selectBestVoice(tts: TextToSpeech) {
        try {
            val voices = tts.voices ?: return

            // Prioritize Indian English voices for SSB interviews
            val indianEnglishVoices = voices.filter { voice ->
                voice.locale.language == "en" &&
                voice.locale.country == "IN" &&
                !voice.isNetworkConnectionRequired
            }

            // Fallback to other English voices if Indian English not available
            val englishVoices = if (indianEnglishVoices.isNotEmpty()) {
                indianEnglishVoices
            } else {
                voices.filter { voice ->
                    voice.locale.language == "en" &&
                    !voice.isNetworkConnectionRequired
                }
            }

            if (englishVoices.isEmpty()) {
                Log.d(TAG, "📢 No English voices available, using default")
                return
            }

            // Sort by quality: Indian English → High quality → Other English
            val sortedVoices = englishVoices.sortedWith(
                compareBy(
                    { voice -> if (voice.locale.country == "IN") 0 else 1 }, // Prioritize IN locale
                    { voice -> !voice.features.contains("legacySetLanguageVoice") },
                    { voice -> voice.quality },
                    { voice -> voice.name.contains("male", ignoreCase = true).let { if (it) 0 else 1 } } // Prefer male voices for SSB
                )
            )

            val bestVoice = sortedVoices.firstOrNull { voice ->
                voice.quality >= Voice.QUALITY_NORMAL
            } ?: sortedVoices.firstOrNull()

            bestVoice?.let { voice ->
                val result = tts.setVoice(voice)
                if (result == TextToSpeech.SUCCESS) {
                    val voiceType = when {
                        voice.locale.country == "IN" -> "Indian English"
                        voice.name.contains("male", ignoreCase = true) -> "Male English"
                        voice.name.contains("female", ignoreCase = true) -> "Female English"
                        else -> "English"
                    }
                    Log.d(TAG, "📢 Selected $voiceType voice: ${voice.name} (${voice.locale}, quality=${voice.quality})")
                } else {
                    Log.w(TAG, "⚠️ Failed to set voice: ${voice.name}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Error selecting voice, using default: ${e.message}")
        }
    }
}

