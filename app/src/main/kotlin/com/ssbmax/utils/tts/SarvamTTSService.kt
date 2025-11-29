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
 * Sarvam AI Text-to-Speech implementation using OkHttp
 *
 * Primary TTS service for Pro/Premium tier users. Provides high-quality Indian English voice.
 *
 * Features:
 * - High-quality neural voice synthesis optimized for Indian English
 * - Professional interviewer voice selection
 * - Automatic fallback to Android TTS on failure
 * - Support for Indian English (en-IN) language
 *
 * @param context Application context for audio playback
 * @param apiKey Sarvam AI API key
 */
class SarvamTTSService @Inject constructor(
    private val context: Context,
    private val apiKey: String
) : TTSService {

    companion object {
        private const val TAG = "SarvamTTS"
        
        // Base URL for Sarvam AI API
        // Note: This may need to be adjusted based on actual API documentation
        private const val BASE_URL = "https://api.sarvam.ai/v1"
        
        // TTS endpoint
        private const val TTS_ENDPOINT = "/text-to-speech"
        
        // Speaker options from Sarvam AI
        // Male voices: abhilash, karun, hitesh
        // Female voices: anushka, vidya, manisha, arya
        // Using professional male voice for SSB interviews
        private const val DEFAULT_SPEAKER = "abhilash"
        
        // Language code for Indian English
        private const val TARGET_LANGUAGE_CODE = "en-IN"
        
        // Audio format settings
        private const val SPEECH_SAMPLE_RATE = 24000 // Hz - high quality
        private const val PITCH = 0.0 // Default pitch (-0.75 to 0.75)
        private const val PACE = 1.0 // Default pace (0.5 to 2.0)
        private const val LOUDNESS = 1.0 // Default loudness (0.3 to 3.0)
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
        Log.d(TAG, "🔧 Initializing Sarvam AI TTS...")
        if (apiKey.isBlank()) {
            Log.w(TAG, "⚠️ Sarvam AI API key is blank")
            _events.tryEmit(TTSService.TTSEvent.Error("Sarvam AI API key missing", fallbackToAndroid = true))
        } else {
            Log.d(TAG, "✅ Sarvam AI TTS ready (API key: ${apiKey.take(8)}...)")
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
                    message = "Sarvam AI API key not configured",
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

            if (audioData != null && !isReleased) {
                Log.d(TAG, "✅ Playing audio (${audioData.size} bytes)")
                withContext(Dispatchers.Main) {
                    playAudio(audioData)
                }
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
                ErrorLogger.log(e, "Sarvam AI speech synthesis failed")
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
            val url = "$BASE_URL$TTS_ENDPOINT"
            Log.d(TAG, "🌐 Calling Sarvam AI API: $url")

            // Build request body according to Sarvam AI API format
            val requestBody = JSONObject().apply {
                put("text", text)
                put("target_language_code", TARGET_LANGUAGE_CODE)
                put("speaker", DEFAULT_SPEAKER)
                put("enable_preprocessing", true)
                put("pitch", PITCH)
                put("pace", PACE)
                put("loudness", LOUDNESS)
                put("speech_sample_rate", SPEECH_SAMPLE_RATE)
            }.toString()

            Log.d(TAG, "📤 Request body: $requestBody")

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "audio/wav") // Sarvam AI returns WAV format
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
                Log.e(TAG, "❌ Sarvam AI API error: ${response.code} - $errorBody")
                Log.e(TAG, "🔍 Request URL: $url")
                Log.e(TAG, "🔍 Request headers: Authorization=Bearer ${apiKey.take(8)}..., Content-Type=application/json")
                ErrorLogger.log(
                    Exception("Sarvam AI API error: ${response.code} - $errorBody"),
                    "Sarvam AI TTS API call failed"
                )
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Network error during synthesis", e)
            ErrorLogger.log(e, "Sarvam AI API call failed")
            null
        }
    }

    private fun playAudio(audioData: ByteArray) {
        try {
            stopMediaPlayer()

            // Write audio to temp file (Sarvam AI returns WAV format)
            val tempFile = File.createTempFile("sarvam_audio", ".wav", context.cacheDir)
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
                        Log.d(TAG, "▶️ Playing Sarvam AI audio")
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
                        ErrorLogger.log(Exception(errorMsg), "Sarvam AI MediaPlayer error")
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
            ErrorLogger.log(e, "Sarvam AI audio playback failed")
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
        Log.d(TAG, "🧹 Releasing Sarvam AI TTS resources...")
        try {
            isReleased = true
            stop()
            Log.d(TAG, "✅ Sarvam AI TTS resources released")
        } catch (e: Exception) {
            ErrorLogger.log(e, "Failed to release Sarvam AI TTS")
        }
    }

    override fun isReady(): Boolean = apiKey.isNotBlank() && !isReleased

    override fun isSpeaking(): Boolean = isSpeakingInternal
}

