package com.ssbmax.core.data.ai

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.ssbmax.core.domain.model.gto.GTOTestType
import com.ssbmax.core.domain.model.interview.InterviewQuestion
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.service.AIService
import com.ssbmax.core.domain.service.ResponseAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gemini AI service implementation acting as a facade.
 * Delegates question generation to [GeminiQuestionGenerator] and scoring/feedback to [GeminiResponseAnalyzer].
 */
@Singleton
class GeminiAIService @Inject constructor(
    private val apiKey: String,
    private val questionGenerator: GeminiQuestionGenerator,
    private val responseAnalyzer: GeminiResponseAnalyzer
) : AIService {

    init {
        Log.d(TAG, "🏗️ GeminiAIService facade constructed successfully.")
    }

    companion object {
        private const val TAG = "GeminiAIService"
        private const val MODEL_NAME = "gemini-2.5-flash"
        private const val TEMPERATURE = 0.7f
        private const val MAX_TOKENS = 8192
        private const val HEALTH_CHECK_TIMEOUT = 10_000L
    }

    private val model: GenerativeModel by lazy {
        Log.d(TAG, "🤖 Initializing GenerativeModel ($MODEL_NAME)")
        GenerativeModel(
            modelName = MODEL_NAME,
            apiKey = apiKey,
            generationConfig = generationConfig {
                temperature = TEMPERATURE
                maxOutputTokens = MAX_TOKENS
            }
        )
    }

    override suspend fun generatePIQBasedQuestions(
        piqData: String,
        targetOLQs: List<OLQ>?,
        count: Int,
        difficulty: Int
    ): Result<List<InterviewQuestion>> =
        questionGenerator.generatePIQBasedQuestions(model, piqData, targetOLQs, count, difficulty)

    override suspend fun generateAdaptiveQuestions(
        previousQuestions: List<InterviewQuestion>,
        previousResponses: List<String>,
        weakOLQs: List<OLQ>,
        count: Int
    ): Result<List<InterviewQuestion>> =
        questionGenerator.generateAdaptiveQuestions(model, previousQuestions, previousResponses, weakOLQs, count)

    override suspend fun analyzeResponse(
        question: InterviewQuestion,
        response: String,
        responseMode: String
    ): Result<ResponseAnalysis> =
        responseAnalyzer.analyzeResponse(model, question, response, responseMode)

    override suspend fun generateFeedback(
        questions: List<InterviewQuestion>,
        responses: List<String>,
        olqScores: Map<OLQ, Float>
    ): Result<String> =
        responseAnalyzer.generateFeedback(model, questions, responses, olqScores)

    override suspend fun analyzeGTOResponse(
        prompt: String,
        testType: GTOTestType
    ): Result<ResponseAnalysis> =
        responseAnalyzer.analyzeGTOResponse(model, prompt, testType)

    override suspend fun analyzeTATResponse(prompt: String): Result<ResponseAnalysis> =
        responseAnalyzer.analyzeTATResponse(model, prompt)

    override suspend fun analyzeWATResponse(prompt: String): Result<ResponseAnalysis> =
        responseAnalyzer.analyzeWATResponse(model, prompt)

    override suspend fun analyzeSRTResponse(prompt: String): Result<ResponseAnalysis> =
        responseAnalyzer.analyzeSRTResponse(model, prompt)

    override suspend fun analyzeSDResponse(prompt: String): Result<ResponseAnalysis> =
        responseAnalyzer.analyzeSDResponse(model, prompt)

    override suspend fun analyzePPDTResponse(prompt: String): Result<ResponseAnalysis> =
        responseAnalyzer.analyzePPDTResponse(model, prompt)

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            withTimeout(HEALTH_CHECK_TIMEOUT) {
                val response = model.generateContent("Reply with OK")
                response.text?.isNotEmpty() == true
            }
        } catch (e: Exception) {
            Log.e(TAG, "AI service health check failed", e)
            false
        }
    }
}
