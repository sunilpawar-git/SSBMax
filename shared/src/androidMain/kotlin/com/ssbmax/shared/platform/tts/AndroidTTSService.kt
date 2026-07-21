package com.ssbmax.shared.platform.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.Locale
import java.util.UUID

/**
 * Android's built-in Text-to-Speech implementation.
 *
 * Moved from `app/.../utils/tts/AndroidTTSService.kt` (Phase 4 platform shim) —
 * same behavior, now the Android `actual` of the shared `TTSService` interface.
 *
 * Note: the pre-shim version additionally reported init/synthesis failures to
 * `ErrorLogger` (app-layer, Firebase Crashlytics-backed). `ErrorLogger` has no
 * `shared`-reachable equivalent yet, so this actual logs via `android.util.Log`
 * only; callers still observe failures through [TTSService.TTSEvent.Error].
 *
 * @param context Application context for TTS initialization
 */
class AndroidTTSService(
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
        Log.d(TAG, "Initializing Android TTS...")
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                setupTTS()
            } else {
                val errorMsg = "Failed to initialize Android TTS engine (status: $status)"
                Log.e(TAG, errorMsg)
                initializationFailed = true
                _events.tryEmit(TTSService.TTSEvent.Error(errorMsg))
            }
        }
    }

    private fun setupTTS() {
        textToSpeech?.let { tts ->
            val localeResult = tts.setLanguage(Locale("en", "IN"))
            if (localeResult == TextToSpeech.LANG_MISSING_DATA ||
                localeResult == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                tts.setLanguage(Locale.ENGLISH)
            }

            selectBestVoice(tts)
            tts.setSpeechRate(SPEECH_RATE)
            tts.setPitch(SPEECH_PITCH)

            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    if (isReleased) return
                    Log.d(TAG, "Speech started: $utteranceId")
                }

                override fun onDone(utteranceId: String?) {
                    if (isReleased) return
                    Log.d(TAG, "Speech completed: $utteranceId")
                    _events.tryEmit(TTSService.TTSEvent.SpeechComplete)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    if (isReleased) return
                    Log.e(TAG, "Speech error: $utteranceId")
                    _events.tryEmit(TTSService.TTSEvent.Error("Speech synthesis error"))
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    if (isReleased) return
                    val errorMsg = mapErrorCode(errorCode)
                    Log.e(TAG, errorMsg)
                    _events.tryEmit(TTSService.TTSEvent.Error(errorMsg))
                }
            })

            isInitialized = true
            Log.d(TAG, "Android TTS initialized successfully")

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
            Log.d(TAG, "TTS not initialized yet, queuing text")
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
            Log.e(TAG, "Failed to queue speech")
            _events.tryEmit(TTSService.TTSEvent.Error("Failed to speak text"))
        }
    }

    override fun stop() {
        textToSpeech?.stop()
    }

    override fun release() {
        try {
            isReleased = true
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
            isInitialized = false
            pendingText = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release Android TTS", e)
        }
    }

    override fun isReady(): Boolean = !isReleased && !initializationFailed

    override fun isSpeaking(): Boolean = textToSpeech?.isSpeaking == true

    private fun selectBestVoice(tts: TextToSpeech) {
        try {
            val voices = tts.voices ?: return

            val indianEnglishVoices = voices.filter { voice ->
                voice.locale.language == "en" &&
                    voice.locale.country == "IN" &&
                    !voice.isNetworkConnectionRequired
            }

            val englishVoices = if (indianEnglishVoices.isNotEmpty()) {
                indianEnglishVoices
            } else {
                voices.filter { voice ->
                    voice.locale.language == "en" && !voice.isNetworkConnectionRequired
                }
            }

            if (englishVoices.isEmpty()) return

            val sortedVoices = englishVoices.sortedWith(
                compareBy(
                    { voice -> if (voice.locale.country == "IN") 0 else 1 },
                    { voice -> !voice.features.contains("legacySetLanguageVoice") },
                    { voice -> voice.quality },
                    { voice -> voice.name.contains("male", ignoreCase = true).let { if (it) 0 else 1 } }
                )
            )

            val bestVoice = sortedVoices.firstOrNull { voice ->
                voice.quality >= Voice.QUALITY_NORMAL
            } ?: sortedVoices.firstOrNull()

            bestVoice?.let { voice -> tts.setVoice(voice) }
        } catch (e: Exception) {
            Log.w(TAG, "Error selecting voice, using default: ${e.message}")
        }
    }
}
