package com.ssbmax.di

import com.ssbmax.utils.tts.AndroidTTSService
import com.ssbmax.utils.tts.TTSService
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Dependency injection module for Text-to-Speech services.
 *
 * Only one implementation is bound today (AndroidTTSService, Android's
 * built-in offline TTS engine). Hilt's `@AndroidTTS` qualifier existed to
 * disambiguate a future Sarvam/ElevenLabs premium TTS implementation that
 * was never added; with a single binding, no Koin qualifier is needed.
 */
val ttsModule = module {
    single<TTSService> { AndroidTTSService(androidContext()) }
}
