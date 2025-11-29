package com.ssbmax.di

import android.content.Context
import com.ssbmax.BuildConfig
import com.ssbmax.utils.tts.AndroidTTS
import com.ssbmax.utils.tts.AndroidTTSService
import com.ssbmax.utils.tts.ElevenLabsTTS
import com.ssbmax.utils.tts.ElevenLabsTTSService
import com.ssbmax.utils.tts.SarvamTTS
import com.ssbmax.utils.tts.SarvamTTSService
import com.ssbmax.utils.tts.TTSService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for Text-to-Speech services
 *
 * Provides:
 * - AndroidTTSService: Built-in Android TTS (Free tier)
 * - SarvamTTSService: Primary TTS service - High-quality Indian English from Sarvam AI (Pro/Premium)
 * - ElevenLabsTTSService: Fallback TTS service - Human-like voice from ElevenLabs API (Pro/Premium)
 *
 * The appropriate service is selected based on user's subscription tier
 * in VoiceInterviewSessionViewModel.
 */
@Module
@InstallIn(SingletonComponent::class)
object TTSModule {

    /**
     * Provide Sarvam AI API key from BuildConfig
     *
     * Read from local.properties at compile time:
     * SARVAM_API_KEY=your_key_here
     *
     * Returns empty string if not configured (will fallback to ElevenLabs or Android TTS)
     */
    @Provides
    @Singleton
    @SarvamApiKey
    fun provideSarvamApiKey(): String {
        return BuildConfig.SARVAM_API_KEY
    }

    /**
     * Provide ElevenLabs API key from BuildConfig
     *
     * Read from local.properties at compile time:
     * ELEVENLABS_API_KEY=your_key_here
     *
     * Returns empty string if not configured (will fallback to Android TTS)
     */
    @Provides
    @Singleton
    @ElevenLabsApiKey
    fun provideElevenLabsApiKey(): String {
        return BuildConfig.ELEVENLABS_API_KEY
    }

    /**
     * Provide Android TTS service (Free tier)
     *
     * Uses Android's built-in TextToSpeech engine.
     * Functional but robotic-sounding.
     */
    @Provides
    @Singleton
    @AndroidTTS
    fun provideAndroidTTSService(
        @ApplicationContext context: Context
    ): TTSService {
        return AndroidTTSService(context)
    }

    /**
     * Provide Sarvam AI TTS service (Primary Pro/Premium tier)
     *
     * Uses Sarvam AI API for high-quality Indian English voice synthesis.
     * Uses OkHttp for API calls (no Ktor to avoid conflicts with Gemini SDK).
     * Falls back to ElevenLabs TTS if API key not configured or API fails.
     */
    @Provides
    @Singleton
    @SarvamTTS
    fun provideSarvamTTSService(
        @ApplicationContext context: Context,
        @SarvamApiKey apiKey: String
    ): TTSService {
        return SarvamTTSService(context, apiKey)
    }

    /**
     * Provide ElevenLabs TTS service (Fallback Pro/Premium tier)
     *
     * Uses ElevenLabs API for human-like voice synthesis.
     * Uses OkHttp for API calls (no Ktor to avoid conflicts with Gemini SDK).
     * Falls back to Android TTS if API key not configured or API fails.
     */
    @Provides
    @Singleton
    @ElevenLabsTTS
    fun provideElevenLabsTTSService(
        @ApplicationContext context: Context,
        @ElevenLabsApiKey apiKey: String
    ): TTSService {
        return ElevenLabsTTSService(context, apiKey)
    }
}

/**
 * Qualifier annotation for Sarvam AI API key
 */
@Retention(AnnotationRetention.BINARY)
@javax.inject.Qualifier
annotation class SarvamApiKey

/**
 * Qualifier annotation for ElevenLabs API key
 */
@Retention(AnnotationRetention.BINARY)
@javax.inject.Qualifier
annotation class ElevenLabsApiKey
