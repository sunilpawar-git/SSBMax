package com.ssbmax.utils.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import com.ssbmax.utils.ErrorLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Qwen TTS using Hugging Face Inference API - Primary TTS for Pro/Premium tier.
 * Features: ~97ms latency, $9/month HF PRO, Ryan voice, auto-retry, Android TTS fallback.
 */
class QwenTTSService @Inject constructor(
    private val context: Context,
    private val apiKey: String,
    private val okHttpClient: OkHttpClient = createDefaultClient()
) : TTSService {

    companion object {
        private const val TAG = "QwenTTS"
        private const val API_URL = "https://api-inference.huggingface.co/models/Qwen/Qwen3-TTS"
        private const val DEFAULT_SPEAKER = "Ryan"
        private const val DEFAULT_LANGUAGE = "English"
        private const val MAX_RETRIES = 2
        private const val RETRY_DELAY_MS = 500L
        private const val HTTP_OK = 200
        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_RATE_LIMIT = 429
        private const val HTTP_SERVICE_UNAVAILABLE = 503

        private fun createDefaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private var mediaPlayer: MediaPlayer? = null
    @Volatile private var isSpeakingInternal: Boolean = false
    @Volatile private var isReleased: Boolean = false

    private val _events = MutableSharedFlow<TTSService.TTSEvent>(replay = 1, extraBufferCapacity = 1)
    override val events: SharedFlow<TTSService.TTSEvent> = _events.asSharedFlow()

    init {
        Log.d(TAG, "Initializing Qwen TTS (Hugging Face)...")
        if (apiKey.isBlank()) {
            Log.w(TAG, "Hugging Face API key is blank")
            _events.tryEmit(TTSService.TTSEvent.Error("Qwen TTS API key missing", true))
        } else {
            Log.d(TAG, "Qwen TTS ready (API key: ${apiKey.take(8)}...)")
            _events.tryEmit(TTSService.TTSEvent.Ready)
        }
    }

    override suspend fun speak(text: String, flush: Boolean) {
        if (isReleased) { Log.w(TAG, "speak() called after release"); return }
        if (apiKey.isBlank()) {
            Log.w(TAG, "No API key, falling back to Android TTS")
            _events.tryEmit(TTSService.TTSEvent.Error("Qwen TTS API key not configured", true))
            return
        }
        if (flush) stop()

        Log.d(TAG, "Synthesizing speech: ${text.take(50)}...")
        isSpeakingInternal = true

        try {
            val audioData = synthesizeSpeechWithRetry(text)
            when {
                audioData != null && !isReleased && isSpeakingInternal -> {
                    Log.d(TAG, "Playing audio (${audioData.size} bytes)")
                    withContext(Dispatchers.Main) { playAudio(audioData) }
                }
                audioData != null && !isSpeakingInternal -> {
                    Log.d(TAG, "Audio ready but TTS was stopped/muted - skipping playback")
                }
                !isReleased -> {
                    Log.e(TAG, "Failed to synthesize speech")
                    isSpeakingInternal = false
                    _events.tryEmit(TTSService.TTSEvent.Error("Failed to synthesize speech", true))
                }
            }
        } catch (e: Exception) {
            if (!isReleased) {
                Log.e(TAG, "Exception in speak(): ${e.message}", e)
                ErrorLogger.log(e, "Qwen TTS speech synthesis failed")
                isSpeakingInternal = false
                _events.tryEmit(TTSService.TTSEvent.Error("Speech synthesis error: ${e.message}", true))
            }
        }
    }

    private suspend fun synthesizeSpeechWithRetry(text: String): ByteArray? {
        var lastException: Exception? = null
        repeat(MAX_RETRIES) { attempt ->
            try {
                val result = synthesizeSpeech(text)
                if (result != null) return result
                if (attempt < MAX_RETRIES - 1) delay(RETRY_DELAY_MS)
            } catch (e: Exception) {
                lastException = e
                if (attempt < MAX_RETRIES - 1) {
                    Log.w(TAG, "Retry ${attempt + 1}/$MAX_RETRIES after error: ${e.message}")
                    delay(RETRY_DELAY_MS)
                }
            }
        }
        lastException?.let { throw it }
        return null
    }

    private suspend fun synthesizeSpeech(text: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val requestBody = JSONObject().apply {
                put("inputs", text)
                put("parameters", JSONObject().apply {
                    put("speaker", DEFAULT_SPEAKER)
                    put("language", DEFAULT_LANGUAGE)
                })
            }.toString()

            val request = Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            val startTime = System.currentTimeMillis()
            val response = okHttpClient.newCall(request).execute()
            Log.d(TAG, "Response in ${System.currentTimeMillis() - startTime}ms (status: ${response.code})")

            when (response.code) {
                HTTP_OK -> response.body?.bytes()?.takeIf { it.isNotEmpty() }
                    .also { if (it != null) Log.d(TAG, "Audio synthesized: ${it.size} bytes") }
                    ?: run { Log.e(TAG, "Response body is empty"); null }
                HTTP_UNAUTHORIZED -> {
                    Log.e(TAG, "Invalid Hugging Face API key")
                    ErrorLogger.log(Exception("Qwen TTS: Invalid API key"), "Auth error")
                    null
                }
                HTTP_RATE_LIMIT -> {
                    Log.e(TAG, "Rate limit exceeded")
                    ErrorLogger.log(Exception("Qwen TTS: Rate limit"), "Rate limit error")
                    null
                }
                HTTP_SERVICE_UNAVAILABLE -> { Log.w(TAG, "Service temporarily unavailable"); null }
                else -> {
                    val errorBody = response.body?.string() ?: "No error body"
                    Log.e(TAG, "Qwen TTS API error: ${response.code} - $errorBody")
                    ErrorLogger.log(Exception("Qwen TTS API error: ${response.code}"), "API call failed")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error during synthesis", e)
            ErrorLogger.log(e, "Qwen TTS API call failed")
            throw e
        }
    }

    private fun playAudio(audioData: ByteArray) {
        try {
            stopMediaPlayer()
            val tempFile = File.createTempFile("qwen_audio", ".mp3", context.cacheDir)
            FileOutputStream(tempFile).use { it.write(audioData) }

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build())
                setDataSource(tempFile.absolutePath)
                prepareAsync()
                setOnPreparedListener { if (!isReleased) { it.start(); Log.d(TAG, "Playing Qwen TTS audio") } }
                setOnCompletionListener {
                    if (!isReleased) {
                        Log.d(TAG, "Audio playback complete")
                        isSpeakingInternal = false
                        _events.tryEmit(TTSService.TTSEvent.SpeechComplete)
                        stopMediaPlayer()
                        tempFile.delete()
                    }
                }
                setOnErrorListener { _, what, extra ->
                    if (!isReleased) {
                        val errorMsg = "MediaPlayer error: what=$what, extra=$extra"
                        Log.e(TAG, errorMsg)
                        ErrorLogger.log(Exception(errorMsg), "Qwen TTS MediaPlayer error")
                        isSpeakingInternal = false
                        _events.tryEmit(TTSService.TTSEvent.Error(errorMsg, true))
                        stopMediaPlayer()
                        tempFile.delete()
                    }
                    true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio", e)
            ErrorLogger.log(e, "Qwen TTS audio playback failed")
            isSpeakingInternal = false
            _events.tryEmit(TTSService.TTSEvent.Error("Audio playback error: ${e.message}", true))
        }
    }

    override fun stop() {
        Log.d(TAG, "Stopping speech...")
        stopMediaPlayer()
        isSpeakingInternal = false
    }

    private fun stopMediaPlayer() {
        mediaPlayer?.apply {
            try { if (isPlaying) stop(); release() }
            catch (e: Exception) { Log.e(TAG, "Error stopping/releasing media player", e) }
        }
        mediaPlayer = null
    }

    override fun release() {
        Log.d(TAG, "Releasing Qwen TTS resources...")
        try { isReleased = true; stop() }
        catch (e: Exception) { ErrorLogger.log(e, "Failed to release Qwen TTS") }
    }

    override fun isReady(): Boolean = apiKey.isNotBlank() && !isReleased
    override fun isSpeaking(): Boolean = isSpeakingInternal
}
