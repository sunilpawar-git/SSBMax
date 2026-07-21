package com.ssbmax.core.data.di

import com.ssbmax.core.data.ai.CloudGeminiAIService
import com.ssbmax.core.data.ai.GeminiAIService
import com.ssbmax.shared.domain.service.AIService
import org.koin.dsl.module

/**
 * Dependency injection module for AI services
 *
 * **Development vs Production:**
 * - Debug builds: Use GeminiAIService (direct API calls with local API key)
 * - Release builds: Use CloudGeminiAIService (Firebase Functions, production-safe)
 *
 * Controlled by USE_CLOUD_AI build config flag. Plain object now (converted
 * from a Hilt `@Module` — see [aiModule] below for the actual Koin binding).
 */
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
    fun provideAIService(
        apiKey: String
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
     * The key is injected at compile time from local.properties
     * (set the GEMINI_API_KEY property to your key).
     *
     * Was `@GeminiApiKey`-qualified in Hilt; no Koin qualifier is needed
     * for this binding since [AIService] is its only consumer (see
     * [aiModule] below).
     */
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
                "Please add it to local.properties, setting GEMINI_API_KEY to your actual key.\n\n" +
                "Get your key from: https://makersuite.google.com/app/apikey"
            )
        }

        return apiKey
    }
}

/**
 * Koin module for AI services — binds [AIModule]'s plain factory functions
 * (converted from Hilt `@Provides`/`@Singleton`/`@GeminiApiKey`; the API key
 * qualifier is gone since it has exactly one consumer, [AIService], so no
 * Koin qualifier is needed either).
 */
val aiModule = module {
    single<AIService> { AIModule.provideAIService(AIModule.provideGeminiApiKey()) }
}
