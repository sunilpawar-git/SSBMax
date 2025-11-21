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
        // API key will be provided by BuildConfig
        // This is a placeholder - actual implementation will use BuildConfig.GEMINI_API_KEY
        val apiKey = System.getenv("GEMINI_API_KEY") ?: ""

        if (apiKey.isBlank()) {
            throw IllegalStateException(
                "Gemini API key not configured. Please set GEMINI_API_KEY in environment " +
                "or add it to local.properties: GEMINI_API_KEY=your_key_here"
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
