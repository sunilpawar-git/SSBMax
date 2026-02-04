package com.ssbmax.di

import android.content.Context
import com.ssbmax.utils.tts.AndroidTTS
import com.ssbmax.utils.tts.AndroidTTSService
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
 * - AndroidTTSService: Built-in Android TTS engine
 *   Works offline, available on all devices.
 */
@Module
@InstallIn(SingletonComponent::class)
object TTSModule {

    /**
     * Provide Android TTS service
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
}
