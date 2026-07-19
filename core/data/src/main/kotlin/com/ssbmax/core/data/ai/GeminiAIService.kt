package com.ssbmax.core.data.ai

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.ssbmax.core.data.ai.prompts.SSBInterviewPrompts
import com.ssbmax.shared.domain.model.PPDTImageContext
import com.ssbmax.shared.domain.model.TATImageContext
import com.ssbmax.shared.domain.model.interview.InterviewQuestion
import com.ssbmax.shared.domain.model.interview.OLQ
import com.ssbmax.shared.domain.service.AIService
import com.ssbmax.shared.domain.service.ResponseAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiAIService @Inject constructor(
    private val apiKey: String
) : AIService {

    init {
        Log.d(TAG, "🏗️ GeminiAIService constructed")
    }

    companion object {
        private const val TAG = "GeminiAIService"
        private const val MODEL_NAME = "gemini-2.5-flash"
        const val TEMPERATURE = 0.0f  // Deterministic: identical story → identical score

        // Tier 1 — single-item analysis: TAT per-story, PPDT, WAT, SD, interview response
        private const val MAX_TOKENS = 8192
        // Tier 2 — multi-item / growing context: SRT (60 situations), interview Q-gen,
        //          adaptive Q-gen (Q&A history grows to ~6K tokens for long sessions)
        private const val MAX_LARGE_CONTEXT_TOKENS = 12288
        // Tier 3 — full-transcript synthesis: TAT synthesis (12 stories × thinking),
        //          interview feedback (full Q&A transcript + OLQ scores)
        private const val MAX_SYNTHESIS_TOKENS = 16384

        private const val RESPONSE_ANALYSIS_TIMEOUT = 60_000L
        private const val QUESTION_GENERATION_TIMEOUT = 60_000L
        private const val LARGE_CONTEXT_TIMEOUT = 90_000L
        private const val SYNTHESIS_TIMEOUT = 120_000L
        private const val HEALTH_CHECK_TIMEOUT = 10_000L
    }

    // Tier 1 — single-item analysis (worst case ~7K tokens)
    private val model: GenerativeModel by lazy {
        GenerativeModel(
            modelName = MODEL_NAME,
            apiKey = apiKey,
            generationConfig = generationConfig {
                temperature = TEMPERATURE
                maxOutputTokens = MAX_TOKENS
            }
        )
    }

    // Tier 2 — multi-item / growing context (worst case ~10K tokens)
    private val largeContextModel: GenerativeModel by lazy {
        GenerativeModel(
            modelName = MODEL_NAME,
            apiKey = apiKey,
            generationConfig = generationConfig {
                temperature = TEMPERATURE
                maxOutputTokens = MAX_LARGE_CONTEXT_TOKENS
            }
        )
    }

    // Tier 3 — full-transcript synthesis (worst case ~13K tokens)
    private val synthesisModel: GenerativeModel by lazy {
        GenerativeModel(
            modelName = MODEL_NAME,
            apiKey = apiKey,
            generationConfig = generationConfig {
                temperature = TEMPERATURE
                maxOutputTokens = MAX_SYNTHESIS_TOKENS
            }
        )
    }

    private val ppdtAnalyzer: GeminiPPDTAnalyzer by lazy {
        GeminiPPDTAnalyzer(model, RESPONSE_ANALYSIS_TIMEOUT)
    }

    private val tatStoryAnalyzer: GeminiTATStoryAnalyzer by lazy {
        GeminiTATStoryAnalyzer(model, RESPONSE_ANALYSIS_TIMEOUT)
    }

    override suspend fun generatePIQBasedQuestions(
        piqData: String,
        targetOLQs: List<OLQ>?,
        count: Int,
        difficulty: Int
    ): Result<List<InterviewQuestion>> = withContext(Dispatchers.IO) {
        try {
            withTimeout(QUESTION_GENERATION_TIMEOUT) {
                val prompt = SSBInterviewPrompts.buildQuestionGenerationPrompt(
                    piqContext = piqData,
                    count = count,
                    difficulty = difficulty,
                    targetOLQs = targetOLQs
                )
                // Tier 2: OLQ_DEFINITIONS + PIQ_TO_OLQ_MAPPING + PIQ context = ~3K tokens input
                val response = largeContextModel.generateContent(prompt)
                GeminiResponseParser.parseQuestionResponse(response)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate PIQ-based questions", e)
            Result.failure(e)
        }
    }

    override suspend fun generateAdaptiveQuestions(
        previousQuestions: List<InterviewQuestion>,
        previousResponses: List<String>,
        weakOLQs: List<OLQ>,
        count: Int
    ): Result<List<InterviewQuestion>> = withContext(Dispatchers.IO) {
        try {
            withTimeout(LARGE_CONTEXT_TIMEOUT) {
                val qaHistory = previousQuestions.zip(previousResponses).map { (q, a) -> q.questionText to a }
                val prompt = SSBInterviewPrompts.buildAdaptiveQuestionPrompt(
                    piqContext = buildContextFromQA(previousQuestions),
                    previousQA = qaHistory,
                    weakOLQs = weakOLQs,
                    count = count
                )
                // Tier 2: Q&A history grows to ~6K tokens for a 25-question session
                val response = largeContextModel.generateContent(prompt)
                GeminiResponseParser.parseQuestionResponse(response)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate adaptive questions", e)
            Result.failure(e)
        }
    }

    private fun buildContextFromQA(questions: List<InterviewQuestion>): String = buildString {
        appendLine("(Context derived from previous questions asked)")
        appendLine()
        appendLine("Topics covered so far:")
        questions.mapIndexed { i, q -> appendLine("- Question ${i + 1}: ${q.questionText.take(100)}...") }
        appendLine()
        appendLine("OLQs assessed:")
        append(questions.flatMap { it.expectedOLQs }.distinct().joinToString(", ") { it.displayName })
    }

    override suspend fun analyzeResponse(
        question: InterviewQuestion,
        response: String,
        responseMode: String
    ): Result<ResponseAnalysis> = withContext(Dispatchers.IO) {
        try {
            withTimeout(RESPONSE_ANALYSIS_TIMEOUT) {
                val prompt = SSBInterviewPrompts.buildResponseAnalysisPrompt(
                    questionText = question.questionText,
                    responseText = response,
                    expectedOLQs = question.expectedOLQs,
                    responseMode = responseMode
                )
                Log.d(TAG, "📤 Sending interview analysis to Gemini")
                val aiResponse = model.generateContent(prompt)
                GeminiResponseParser.parseAnalysisResponse(aiResponse)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to analyze interview response", e)
            Result.failure(e)
        }
    }

    override suspend fun generateFeedback(
        questions: List<InterviewQuestion>,
        responses: List<String>,
        olqScores: Map<OLQ, Float>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            withTimeout(SYNTHESIS_TIMEOUT) {
                val qaHistory = questions.zip(responses).map { (q, a) -> q.questionText to a }
                val prompt = SSBInterviewPrompts.buildFeedbackPrompt(
                    piqContext = "(Full PIQ context not available for feedback generation)",
                    questionAnswerPairs = qaHistory,
                    olqScores = olqScores
                )
                // Tier 3: full Q&A transcript — 25 questions = ~7.5K tokens input alone
                val response = synthesisModel.generateContent(prompt)
                val feedbackText = response.text ?: return@withTimeout Result.failure(
                    IllegalStateException("No feedback generated")
                )
                Result.success(feedbackText.trim())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate feedback", e)
            Result.failure(e)
        }
    }

    override suspend fun analyzeGTOResponse(
        prompt: String,
        testType: com.ssbmax.shared.domain.model.gto.GTOTestType
    ): Result<ResponseAnalysis> = withContext(Dispatchers.IO) {
        try {
            withTimeout(RESPONSE_ANALYSIS_TIMEOUT) {
                Log.d(TAG, "📤 Sending GTO $testType analysis to Gemini")
                val response = model.generateContent(prompt)
                GeminiResponseParser.parseGTOAnalysisResponse(response.text ?: "")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to analyze GTO response", e)
            Result.failure(e)
        }
    }

    override suspend fun analyzeTATResponse(prompt: String): Result<ResponseAnalysis> =
        withContext(Dispatchers.IO) {
            try {
                withTimeout(SYNTHESIS_TIMEOUT) {
                    Log.d(TAG, "📤 Sending TAT synthesis to Gemini (maxTokens=$MAX_SYNTHESIS_TOKENS)")
                    val response = synthesisModel.generateContent(prompt)
                    GeminiResponseParser.parseGTOAnalysisResponse(response.text ?: "")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to analyze TAT response", e)
                Result.failure(e)
            }
        }

    override suspend fun analyzeWATResponse(prompt: String): Result<ResponseAnalysis> =
        withContext(Dispatchers.IO) {
            try {
                withTimeout(RESPONSE_ANALYSIS_TIMEOUT) {
                    Log.d(TAG, "📤 Sending WAT analysis to Gemini")
                    val response = model.generateContent(prompt)
                    GeminiResponseParser.parseGTOAnalysisResponse(response.text ?: "")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to analyze WAT response", e)
                Result.failure(e)
            }
        }

    override suspend fun analyzeSRTResponse(prompt: String): Result<ResponseAnalysis> =
        withContext(Dispatchers.IO) {
            try {
                withTimeout(LARGE_CONTEXT_TIMEOUT) {
                    Log.d(TAG, "📤 Sending SRT analysis to Gemini")
                    // Tier 2: 60 situation+response pairs — verbose edge case pushes past 8K
                    val response = largeContextModel.generateContent(prompt)
                    GeminiResponseParser.parseGTOAnalysisResponse(response.text ?: "")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to analyze SRT response", e)
                Result.failure(e)
            }
        }

    override suspend fun analyzeSDResponse(prompt: String): Result<ResponseAnalysis> =
        withContext(Dispatchers.IO) {
            try {
                withTimeout(RESPONSE_ANALYSIS_TIMEOUT) {
                    Log.d(TAG, "📤 Sending SD analysis to Gemini")
                    val response = model.generateContent(prompt)
                    GeminiResponseParser.parseGTOAnalysisResponse(response.text ?: "")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to analyze SD response", e)
                Result.failure(e)
            }
        }

    override suspend fun analyzePPDTMultimodal(
        imageBytes: ByteArray,
        story: String,
        imageContext: PPDTImageContext,
        candidateGender: String
    ): Result<ResponseAnalysis> =
        ppdtAnalyzer.analyzePPDTMultimodal(imageBytes, story, imageContext, candidateGender)

    override suspend fun analyzeTATStoryMultimodal(
        imageBytes: ByteArray,
        story: String,
        imageContext: TATImageContext,
        candidateGender: String,
        storyIndex: Int,
        totalStories: Int,
        imageGenderTag: String
    ): Result<ResponseAnalysis> =
        tatStoryAnalyzer.analyzeTATStoryMultimodal(
            imageBytes, story, imageContext, candidateGender, storyIndex, totalStories, imageGenderTag
        )

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
