package com.ssbmax.utils.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import com.ssbmax.utils.ErrorLogger
import kotlinx.coroutines.Dispatchers
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
 * ElevenLabs Text-to-Speech implementation using OkHttp
 *
 * Used for Pro/Premium tier users. Provides human-like, natural voice.
 *
 * Features:
 * - High-quality neural voice synthesis
 * - Streaming audio playback for low latency
 * - Automatic fallback to Android TTS on failure
 * - Professional interviewer voice selection
 *
 * @param context Application context for audio playback
 * @param apiKey ElevenLabs API key
 */
class ElevenLabsTTSService @Inject constructor(
    private val context: Context,
    private val apiKey: String
) : TTSService {

    companion object {
        private const val TAG = "ElevenLabsTTS"
        private const val BASE_URL = "https://api.elevenlabs.io/v1"

        // Voice IDs from ElevenLabs
        // Indian Male - Professional Indian English accent (ideal for SSB interviews)
        private const val VOICE_ID_INDIAN_MALE = "8sWH93U9U0KfEJZdaI8Z"
        // George - British male voice (professional, formal - good for SSB interview)
        private const val VOICE_ID_GEORGE = "JBFqnCBsd6RMkjVDRZzb"

        // Using Indian Male voice as default interviewer voice
        private const val DEFAULT_VOICE_ID = VOICE_ID_INDIAN_MALE

        // Model ID for multilingual support and quality
        private const val MODEL_ID = "eleven_multilingual_v2"

        // Output format for Android MediaPlayer compatibility
        private const val OUTPUT_FORMAT = "mp3_44100_128"
    }

    private var mediaPlayer: MediaPlayer? = null

    @Volatile
    private var isSpeakingInternal: Boolean = false

    @Volatile
    private var isReleased: Boolean = false

    private val _events = MutableSharedFlow<TTSService.TTSEvent>(extraBufferCapacity = 1)
    override val events: SharedFlow<TTSService.TTSEvent> = _events.asSharedFlow()

    // OkHttp client with appropriate timeouts for TTS synthesis
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    init {
        Log.d(TAG, "🔧 Initializing ElevenLabs TTS...")
        if (apiKey.isBlank()) {
            Log.w(TAG, "⚠️ ElevenLabs API key is blank")
            _events.tryEmit(TTSService.TTSEvent.Error("ElevenLabs API key missing", fallbackToAndroid = true))
        } else {
            Log.d(TAG, "✅ ElevenLabs TTS ready (API key: ${apiKey.take(8)}...)")
            _events.tryEmit(TTSService.TTSEvent.Ready)
        }
    }

    override suspend fun speak(text: String, flush: Boolean) {
        if (isReleased) {
            Log.w(TAG, "⚠️ speak() called after release")
            return
        }

        if (apiKey.isBlank()) {
            Log.w(TAG, "⚠️ No API key, falling back to Android TTS")
            _events.tryEmit(
                TTSService.TTSEvent.Error(
                    message = "ElevenLabs API key not configured",
                    fallbackToAndroid = true
                )
            )
            return
        }

        if (flush) {
            stop()
        }

        Log.d(TAG, "📢 Synthesizing speech: ${text.take(50)}...")
        isSpeakingInternal = true

        try {
            val audioData = synthesizeSpeech(text)

            // Check isSpeakingInternal to respect mute/stop during API call
            if (audioData != null && !isReleased && isSpeakingInternal) {
                Log.d(TAG, "✅ Playing audio (${audioData.size} bytes)")
                withContext(Dispatchers.Main) {
                    playAudio(audioData)
                }
            } else if (audioData != null && !isSpeakingInternal) {
                Log.d(TAG, "🔕 Audio ready but TTS was stopped/muted - skipping playback")
                // Audio synthesized but user muted/stopped during API call - don't play
            } else if (!isReleased) {
                Log.e(TAG, "❌ Failed to synthesize speech")
                isSpeakingInternal = false
                _events.tryEmit(
                    TTSService.TTSEvent.Error(
                        message = "Failed to synthesize speech",
                        fallbackToAndroid = true
                    )
                )
            }
        } catch (e: Exception) {
            if (!isReleased) {
                Log.e(TAG, "❌ Exception in speak(): ${e.message}", e)
                ErrorLogger.log(e, "ElevenLabs speech synthesis failed")
                isSpeakingInternal = false
                _events.tryEmit(
                    TTSService.TTSEvent.Error(
                        message = "Speech synthesis error: ${e.message}",
                        fallbackToAndroid = true
                    )
                )
            }
        }
    }

    private suspend fun synthesizeSpeech(text: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/text-to-speech/$DEFAULT_VOICE_ID"

            // Build request body
            val requestBody = JSONObject().apply {
                put("text", text)
                put("model_id", MODEL_ID)
                put("output_format", OUTPUT_FORMAT)
                put("voice_settings", JSONObject().apply {
                    put("stability", 0.5)
                    put("similarity_boost", 0.75)
                    put("style", 0.0)
                    put("use_speaker_boost", true)
                })
            }.toString()

            val request = Request.Builder()
                .url(url)
                .addHeader("xi-api-key", apiKey)
                .addHeader("Accept", "audio/mpeg")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            val startTime = System.currentTimeMillis()
            val response = okHttpClient.newCall(request).execute()
            val duration = System.currentTimeMillis() - startTime

            Log.d(TAG, "📥 Response in ${duration}ms (status: ${response.code})")

            if (response.isSuccessful) {
                val audioBytes = response.body?.bytes()
                if (audioBytes != null) {
                    Log.d(TAG, "✅ Audio synthesized: ${audioBytes.size} bytes")
                    audioBytes
                } else {
                    Log.e(TAG, "❌ Response body is null")
                    null
                }
            } else {
                val errorBody = response.body?.string() ?: "No error body"
                Log.e(TAG, "❌ ElevenLabs API error: ${response.code} - $errorBody")
                ErrorLogger.log(
                    Exception("ElevenLabs API error: ${response.code} - $errorBody"),
                    "ElevenLabs TTS API call failed"
                )
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Network error during synthesis", e)
            ErrorLogger.log(e, "ElevenLabs API call failed")
            null
        }
    }

    private fun playAudio(audioData: ByteArray) {
        try {
            stopMediaPlayer()

            // Write audio to temp file
            val tempFile = File.createTempFile("elevenlabs_audio", ".mp3", context.cacheDir)
            FileOutputStream(tempFile).use { it.write(audioData) }

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(tempFile.absolutePath)
                prepareAsync()
                setOnPreparedListener {
                    if (!isReleased) {
                        it.start()
                        Log.d(TAG, "▶️ Playing ElevenLabs audio")
                    }
                }
                setOnCompletionListener {
                    if (!isReleased) {
                        Log.d(TAG, "✅ Audio playback complete")
                        isSpeakingInternal = false
                        _events.tryEmit(TTSService.TTSEvent.SpeechComplete)
                        stopMediaPlayer()
                        tempFile.delete()
                    }
                }
                setOnErrorListener { _, what, extra ->
                    if (!isReleased) {
                        val errorMsg = "MediaPlayer error: what=$what, extra=$extra"
                        Log.e(TAG, "❌ $errorMsg")
                        ErrorLogger.log(Exception(errorMsg), "ElevenLabs MediaPlayer error")
                        isSpeakingInternal = false
                        _events.tryEmit(TTSService.TTSEvent.Error(errorMsg, fallbackToAndroid = true))
                        stopMediaPlayer()
                        tempFile.delete()
                    }
                    true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error playing audio", e)
            ErrorLogger.log(e, "ElevenLabs audio playback failed")
            isSpeakingInternal = false
            _events.tryEmit(TTSService.TTSEvent.Error("Audio playback error: ${e.message}", fallbackToAndroid = true))
        }
    }

    override fun stop() {
        Log.d(TAG, "⏹️ Stopping speech...")
        stopMediaPlayer()
        isSpeakingInternal = false
        Log.d(TAG, "✅ Speech stopped")
    }

    private fun stopMediaPlayer() {
        mediaPlayer?.apply {
            try {
                if (isPlaying) {
                    stop()
                }
                release()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping/releasing media player", e)
            }
        }
        mediaPlayer = null
    }

    override fun release() {
        Log.d(TAG, "🧹 Releasing ElevenLabs TTS resources...")
        try {
            isReleased = true
            stop()
            Log.d(TAG, "✅ ElevenLabs TTS resources released")
        } catch (e: Exception) {
            ErrorLogger.log(e, "Failed to release ElevenLabs TTS")
        }
    }

    override fun isReady(): Boolean = apiKey.isNotBlank() && !isReleased

    override fun isSpeaking(): Boolean = isSpeakingInternal
}
