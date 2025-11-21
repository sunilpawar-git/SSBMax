package com.ssbmax.core.data.di

import com.ssbmax.core.data.ai.GeminiAIService
import com.ssbmax.core.domain.service.AIService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for AI services
 */
@Module
@InstallIn(SingletonComponent::class)
object AIModule {

    /**
     * Provide Gemini AI service implementation
     *
     * API key is read from BuildConfig (set via local.properties)
     */
    @Provides
    @Singleton
    fun provideAIService(
        @GeminiApiKey apiKey: String
    ): AIService {
        return GeminiAIService(apiKey)
    }

    /**
     * Provide Gemini API key from BuildConfig
     *
     * The key is injected at compile time from local.properties:
     * GEMINI_API_KEY=your_key_here
     */
    @Provides
    @Singleton
    @GeminiApiKey
    fun provideGeminiApiKey(): String {
        val apiKey = com.ssbmax.core.data.BuildConfig.GEMINI_API_KEY

        if (apiKey.isBlank() || apiKey == "your_api_key_here") {
            throw IllegalStateException(
                "Gemini API key not configured. Please add it to local.properties:\n" +
                "GEMINI_API_KEY=your_actual_key_here\n\n" +
                "Get your key from: https://makersuite.google.com/app/apikey"
            )
        }

        return apiKey
    }
}

/**
 * Qualifier annotation for Gemini API key
 */
@Retention(AnnotationRetention.BINARY)
@javax.inject.Qualifier
annotation class GeminiApiKey
