package com.ssbmax.di

import android.content.Context
import com.ssbmax.BuildConfig
import com.ssbmax.utils.tts.AndroidTTS
import com.ssbmax.utils.tts.AndroidTTSService
import com.ssbmax.utils.tts.QwenTTS
import com.ssbmax.utils.tts.QwenTTSService
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
 * - AndroidTTSService: Built-in Android TTS (Free tier, ultimate fallback)
 * - QwenTTSService: Primary premium TTS via Hugging Face (Pro/Premium)
 *
 * Priority order: Qwen TTS → Android TTS (fallback)
 * Selected based on user's subscription tier in InterviewSessionViewModel.
 */
@Module
@InstallIn(SingletonComponent::class)
object TTSModule {

    /**
     * Provide Android TTS service (Free tier, ultimate fallback)
     *
     * Uses Android's built-in TextToSpeech engine.
     * Works offline, available on all devices.
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
     * Provide Hugging Face API key from BuildConfig
     *
     * Read from local.properties at compile time:
     * HUGGINGFACE_API_KEY=hf_your_key_here
     *
     * Returns empty string if not configured (will fallback to Android TTS)
     */
    @Provides
    @Singleton
    @HuggingFaceApiKey
    fun provideHuggingFaceApiKey(): String {
        return BuildConfig.HUGGINGFACE_API_KEY
    }

    /**
     * Provide Qwen TTS service (Primary Pro/Premium tier)
     *
     * Uses Hugging Face Inference API for Qwen TTS model.
     * Low latency (~97ms), cost-effective ($9/month HF PRO).
     * Falls back to Android TTS if API key not configured or API fails.
     */
    @Provides
    @Singleton
    @QwenTTS
    fun provideQwenTTSService(
        @ApplicationContext context: Context,
        @HuggingFaceApiKey apiKey: String
    ): TTSService {
        return QwenTTSService(context, apiKey)
    }
}

/**
 * Qualifier annotation for Hugging Face API key (Qwen TTS)
 */
@Retention(AnnotationRetention.BINARY)
@javax.inject.Qualifier
annotation class HuggingFaceApiKey
