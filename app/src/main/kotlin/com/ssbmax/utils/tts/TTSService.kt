package com.ssbmax.utils.tts

import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Qualifier

/**
 * Interface for Text-to-Speech services
 *
 * Supports multiple TTS implementations:
 * - AndroidTTSService: Free tier (built-in Android TTS)
 * - SarvamTTSService: Primary TTS service for Pro/Premium tier (high-quality Indian English)
 * - ElevenLabsTTSService: Fallback TTS service (human-like ElevenLabs voices)
 */
interface TTSService {
    /**
     * Speak the provided text
     *
     * @param text Text to synthesize and speak
     * @param flush If true, clears any queued speech first
     */
    suspend fun speak(text: String, flush: Boolean = true)

    /**
     * Stop any current speech immediately
     */
    fun stop()

    /**
     * Release all resources
     * Should be called when TTS is no longer needed
     */
    fun release()

    /**
     * Check if TTS is initialized and ready to speak
     */
    fun isReady(): Boolean

    /**
     * Check if TTS is currently speaking
     */
    fun isSpeaking(): Boolean

    /**
     * Events emitted by the TTS service
     */
    sealed class TTSEvent {
        /** TTS is initialized and ready */
        object Ready : TTSEvent()

        /** Speech completed successfully */
        object SpeechComplete : TTSEvent()

        /**
         * Error occurred during TTS
         * @param message Error description
         * @param fallbackToAndroid If true, caller should fallback to Android TTS
         */
        data class Error(
            val message: String,
            val fallbackToAndroid: Boolean = false
        ) : TTSEvent()
    }

    /**
     * Flow of TTS events
     */
    val events: SharedFlow<TTSEvent>
}

/**
 * Qualifier for Android TTS implementation (free tier)
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AndroidTTS

/**
 * Qualifier for Sarvam AI TTS implementation (Primary Pro/Premium tier)
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SarvamTTS

/**
 * Qualifier for ElevenLabs TTS implementation (Fallback Pro/Premium tier)
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ElevenLabsTTS

