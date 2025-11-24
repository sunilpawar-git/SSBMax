package com.ssbmax.core.data.di

import com.ssbmax.core.data.ai.CloudGeminiAIService
import com.ssbmax.core.data.ai.GeminiAIService
import com.ssbmax.core.domain.service.AIService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for AI services
 *
 * **Development vs Production:**
 * - Debug builds: Use GeminiAIService (direct API calls with local API key)
 * - Release builds: Use CloudGeminiAIService (Firebase Functions, production-safe)
 *
 * Controlled by USE_CLOUD_AI build config flag.
 */
@Module
@InstallIn(SingletonComponent::class)
object AIModule {

    /**
     * Provide AI Service implementation
     *
     * **Development (DEBUG)**: GeminiAIService
     * - Direct API calls to Gemini
     * - Faster iteration during development
     * - API key from local.properties
     *
     * **Production (RELEASE)**: CloudGeminiAIService
     * - Firebase Cloud Functions
     * - Secure (API key never exposed)
     * - Rate limiting and user authentication
     */
    @Provides
    @Singleton
    fun provideAIService(
        @GeminiApiKey apiKey: String
    ): AIService {
        return if (com.ssbmax.core.data.BuildConfig.USE_CLOUD_AI) {
            // Production: Use Firebase Functions (secure)
            CloudGeminiAIService()
        } else {
            // Development: Use direct API calls (fast iteration)
            GeminiAIService(apiKey)
        }
    }

    /**
     * Provide Gemini API key from BuildConfig
     *
     * Only used in development builds (direct API calls).
     * In production, this returns empty string as the API key is stored
     * securely in Firebase Functions environment.
     *
     * The key is injected at compile time from local.properties:
     * GEMINI_API_KEY=your_key_here
     */
    @Provides
    @Singleton
    @GeminiApiKey
    fun provideGeminiApiKey(): String {
        val apiKey = com.ssbmax.core.data.BuildConfig.GEMINI_API_KEY

        // In production, API key is not needed (using Cloud Functions)
        if (com.ssbmax.core.data.BuildConfig.USE_CLOUD_AI) {
            return "" // Empty string OK for production
        }

        // In development, API key is required
        if (apiKey.isBlank() || apiKey == "your_api_key_here") {
            throw IllegalStateException(
                "Gemini API key not configured for development build.\n" +
                "Please add it to local.properties:\n" +
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
