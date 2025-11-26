package com.ssbmax.core.data.ai

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.generationConfig
import com.ssbmax.core.domain.model.interview.InterviewQuestion
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.service.AIService
import com.ssbmax.core.domain.service.OLQScoreWithReasoning
import com.ssbmax.core.domain.service.ResponseAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gemini AI service implementation
 *
 * Uses Google's Gemini 2.5 Flash model for interview question generation
 * and response analysis
 *
 * Model: gemini-2.5-flash
 * - Best price-performance for large-scale processing
 * - Low latency, high volume task support
 * - Thinking capability for complex reasoning
 * - 1M token context window
 *
 * Note: Using Google AI Client SDK (generativeai:0.9.0).
 * For structured outputs with JSON Schema, consider migrating to Firebase AI Logic SDK.
 *
 * @param apiKey Gemini API key (injected from BuildConfig)
 */
@Singleton
class GeminiAIService @Inject constructor(
    private val apiKey: String
) : AIService {

    init {
        Log.d(TAG, "🏗️ GeminiAIService constructed with API key: ${apiKey.take(10)}...${apiKey.takeLast(4)}")
    }

    companion object {
        private const val TAG = "GeminiAIService"
        // Using gemini-2.5-flash: Latest production flash model
        // Note: gemini-1.5-flash is RETIRED since late 2024
        private const val MODEL_NAME = "gemini-2.5-flash"
        private const val TEMPERATURE = 0.7f
        // Balanced: Enough for 10 questions but still fast
        private const val MAX_TOKENS = 4096

        // Timeout values (milliseconds) per guidelines
        // Increased for high-latency networks (76-96ms observed)
        private const val QUESTION_GENERATION_TIMEOUT = 30_000L  // 30 seconds
        private const val RESPONSE_ANALYSIS_TIMEOUT = 20_000L     // 20 seconds
        private const val FEEDBACK_GENERATION_TIMEOUT = 30_000L   // 30 seconds
        private const val HEALTH_CHECK_TIMEOUT = 10_000L          // 10 seconds
    }

    private val model: GenerativeModel by lazy {
        Log.d(TAG, "🤖 Initializing GenerativeModel (model: $MODEL_NAME, temp: $TEMPERATURE, maxTokens: $MAX_TOKENS)")
        GenerativeModel(
            modelName = MODEL_NAME,
            apiKey = apiKey,
            generationConfig = generationConfig {
                temperature = TEMPERATURE
                maxOutputTokens = MAX_TOKENS
            }
        ).also {
            Log.d(TAG, "✅ GenerativeModel initialized successfully")
        }
    }

    override suspend fun generatePIQBasedQuestions(
        piqData: String,
        targetOLQs: List<OLQ>?,
        count: Int,
        difficulty: Int
    ): Result<List<InterviewQuestion>> = withContext(Dispatchers.IO) {
        try {
            withTimeout(QUESTION_GENERATION_TIMEOUT) {
                val prompt = buildPIQQuestionPrompt(piqData, targetOLQs, count, difficulty)
                val response = model.generateContent(prompt)

                parseQuestionResponse(response)
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
            withTimeout(QUESTION_GENERATION_TIMEOUT) {
                val prompt = buildAdaptiveQuestionPrompt(
                    previousQuestions,
                    previousResponses,
                    weakOLQs,
                    count
                )
                val response = model.generateContent(prompt)

                parseQuestionResponse(response)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate adaptive questions", e)
            Result.failure(e)
        }
    }

    override suspend fun analyzeResponse(
        question: InterviewQuestion,
        response: String,
        responseMode: String
    ): Result<ResponseAnalysis> = withContext(Dispatchers.IO) {
        Log.d(TAG, "🚀 analyzeResponse() called - Question: ${question.id}, Response length: ${response.length}, Mode: $responseMode")
        try {
            Log.d(TAG, "⏱️ Starting withTimeout block (${RESPONSE_ANALYSIS_TIMEOUT}ms timeout)")
            withTimeout(RESPONSE_ANALYSIS_TIMEOUT) {
                Log.d(TAG, "📝 Building analysis prompt...")
                val prompt = buildResponseAnalysisPrompt(question, response, responseMode)

                Log.d(TAG, "📤 Sending request to Gemini API (model: $MODEL_NAME)...")
                val startTime = System.currentTimeMillis()
                val aiResponse = model.generateContent(prompt)
                val duration = System.currentTimeMillis() - startTime

                Log.d(TAG, "✅ Received response from Gemini in ${duration}ms")
                Log.d(TAG, "🔍 Parsing analysis response...")

                val result = parseAnalysisResponse(aiResponse)
                Log.d(TAG, "✨ Analysis complete: ${if (result.isSuccess) "SUCCESS" else "FAILED"}")
                result
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to analyze response: ${e.javaClass.simpleName}: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun generateFeedback(
        questions: List<InterviewQuestion>,
        responses: List<String>,
        olqScores: Map<OLQ, Float>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            withTimeout(FEEDBACK_GENERATION_TIMEOUT) {
                val prompt = buildFeedbackPrompt(questions, responses, olqScores)
                val response = model.generateContent(prompt)

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

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            withTimeout(HEALTH_CHECK_TIMEOUT) {
                // Simple health check with minimal prompt
                val response = model.generateContent("Reply with OK")
                response.text?.isNotEmpty() == true
            }
        } catch (e: Exception) {
            Log.e(TAG, "AI service health check failed", e)
            false
        }
    }

    /**
     * Build prompt for PIQ-based question generation
     */
    private fun buildPIQQuestionPrompt(
        piqData: String,
        targetOLQs: List<OLQ>?,
        count: Int,
        difficulty: Int
    ): String {
        return """
Generate $count SSB questions from PIQ. Return JSON only.

PIQ: $piqData

Format: [{"id":"q1","questionText":"...?","targetOLQs":["INITIATIVE","COURAGE"],"reasoning":"..."}]

OLQs: EFFECTIVE_INTELLIGENCE,REASONING_ABILITY,ORGANIZING_ABILITY,POWER_OF_EXPRESSION,SOCIAL_ADJUSTMENT,COOPERATION,SENSE_OF_RESPONSIBILITY,INITIATIVE,SELF_CONFIDENCE,SPEED_OF_DECISION,INFLUENCE_GROUP,LIVELINESS,DETERMINATION,COURAGE,STAMINA
        """.trimIndent()
    }

    /**
     * Build prompt for adaptive follow-up questions
     */
    private fun buildAdaptiveQuestionPrompt(
        previousQuestions: List<InterviewQuestion>,
        previousResponses: List<String>,
        weakOLQs: List<OLQ>,
        count: Int
    ): String {
        val qaHistory = previousQuestions.zip(previousResponses)
            .joinToString("\n\n") { (q, a) ->
                "Q: ${q.questionText}\nA: $a"
            }

        val weakOLQNames = weakOLQs.joinToString(", ") { it.displayName }

        return """
You are an SSB interviewing officer conducting a follow-up assessment.

**PREVIOUS Q&A**:
$qaHistory

**WEAK OLQs NEEDING ASSESSMENT**: $weakOLQNames

**TASK**: Generate $count adaptive follow-up questions that:
1. Probe deeper into the weak OLQs identified
2. Challenge assumptions or generalizations from previous answers
3. Present situational scenarios requiring those specific OLQs
4. Are more challenging than initial questions (difficulty 4-5)

**OUTPUT FORMAT** (JSON array):
[
  {
    "id": "unique-id",
    "questionText": "The follow-up question",
    "targetOLQs": ["COURAGE", "DETERMINATION"],
    "reasoning": "Why this follow-up is needed"
  }
]

Generate exactly $count questions as a valid JSON array.
        """.trimIndent()
    }

    /**
     * Build prompt for response analysis
     */
    private fun buildResponseAnalysisPrompt(
        question: InterviewQuestion,
        response: String,
        responseMode: String
    ): String {
        val expectedOLQs = question.expectedOLQs.joinToString(", ") { it.name }

        return """
Analyze SSB response. Return JSON only.

Q: ${question.questionText}
OLQs: $expectedOLQs
A: $response

Format: {"olqScores":[{"olq":"INITIATIVE","score":5.5,"reasoning":"...","evidence":["..."]}],"overallConfidence":85,"keyInsights":["..."],"suggestedFollowUp":"..."}

Score 1-10 (SSB): 1-3=Exceptional, 4=Excellent, 5=Very Good, 6=Good, 7=Average, 8=Below Avg, 9-10=Poor. LOWER is BETTER. Use decimals (e.g. 5.5, 6.5).

OLQs: EFFECTIVE_INTELLIGENCE,REASONING_ABILITY,ORGANIZING_ABILITY,POWER_OF_EXPRESSION,SOCIAL_ADJUSTMENT,COOPERATION,SENSE_OF_RESPONSIBILITY,INITIATIVE,SELF_CONFIDENCE,SPEED_OF_DECISION,INFLUENCE_GROUP,LIVELINESS,DETERMINATION,COURAGE,STAMINA
        """.trimIndent()
    }

    /**
     * Build prompt for comprehensive feedback generation
     */
    private fun buildFeedbackPrompt(
        questions: List<InterviewQuestion>,
        responses: List<String>,
        olqScores: Map<OLQ, Float>
    ): String {
        val qaHistory = questions.zip(responses)
            .joinToString("\n\n") { (q, a) ->
                "Q: ${q.questionText}\nA: $a"
            }

        val scoresSummary = olqScores.entries
            .sortedByDescending { it.value }
            .joinToString("\n") { (olq, score) ->
                "- ${olq.displayName}: ${"%.1f".format(score)}/5"
            }

        return """
You are an SSB interviewing officer providing final interview feedback.

**INTERVIEW TRANSCRIPT**:
$qaHistory

**OLQ SCORES**:
$scoresSummary

**TASK**: Provide comprehensive, constructive feedback covering:

1. **Overall Performance** (2-3 sentences)
2. **Key Strengths** (top 3 OLQs with specific examples)
3. **Areas for Improvement** (3-4 OLQs with actionable advice)
4. **Recommendations** (concrete steps for development)
5. **Encouraging Conclusion** (motivational closing)

**TONE**: Professional, respectful, constructive, and encouraging.

**LENGTH**: 300-400 words.

Write the feedback directly (not as JSON).
        """.trimIndent()
    }

    /**
     * Parse question generation response
     * Handles both clean JSON and markdown-wrapped JSON
     */
    private fun parseQuestionResponse(response: GenerateContentResponse): Result<List<InterviewQuestion>> {
        return try {
            val jsonText = response.text ?: return Result.failure(
                IllegalStateException("No response text")
            )

            // Extract JSON from markdown code blocks if present
            val cleanJson = when {
                "```json" in jsonText -> {
                    // Extract content between ```json and closing ```
                    jsonText
                        .substringAfter("```json")
                        .substringBefore("```")
                        .trim()
                }
                "```" in jsonText -> {
                    // Extract content between ``` and closing ```
                    jsonText
                        .substringAfter("```")
                        .substringBefore("```")
                        .trim()
                }
                else -> {
                    // No markdown, use as-is but trim whitespace
                    jsonText.trim()
                }
            }

            val jsonArray = JSONArray(cleanJson)
            val questions = mutableListOf<InterviewQuestion>()

            for (i in 0 until jsonArray.length()) {
                val questionJson = jsonArray.getJSONObject(i)
                questions.add(parseQuestion(questionJson))
            }

            Result.success(questions)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse question response: ${response.text}", e)
            Result.failure(e)
        }
    }

    /**
     * Parse individual question JSON
     */
    private fun parseQuestion(json: JSONObject): InterviewQuestion {
        val targetOLQsArray = json.getJSONArray("targetOLQs")
        val expectedOLQs = mutableListOf<OLQ>()

        for (i in 0 until targetOLQsArray.length()) {
            val olqName = targetOLQsArray.getString(i)
            OLQ.entries.find { it.name == olqName }?.let { expectedOLQs.add(it) }
        }

        return InterviewQuestion(
            id = json.optString("id", UUID.randomUUID().toString()),
            questionText = json.getString("questionText"),
            expectedOLQs = expectedOLQs,
            context = json.optString("reasoning", null),
            source = com.ssbmax.core.domain.model.interview.QuestionSource.AI_GENERATED
        )
    }

    /**
     * Parse response analysis
     * Handles both clean JSON and markdown-wrapped JSON
     */
    private fun parseAnalysisResponse(response: GenerateContentResponse): Result<ResponseAnalysis> {
        return try {
            val jsonText = response.text ?: return Result.failure(
                IllegalStateException("No response text")
            )

            // Extract JSON from markdown code blocks if present
            val cleanJson = when {
                "```json" in jsonText -> {
                    // Extract content between ```json and closing ```
                    jsonText
                        .substringAfter("```json")
                        .substringBefore("```")
                        .trim()
                }
                "```" in jsonText -> {
                    // Extract content between ``` and closing ```
                    jsonText
                        .substringAfter("```")
                        .substringBefore("```")
                        .trim()
                }
                else -> {
                    // No markdown, use as-is but trim whitespace
                    jsonText.trim()
                }
            }

            val json = JSONObject(cleanJson)

            val olqScoresArray = json.getJSONArray("olqScores")
            val olqScores = mutableMapOf<OLQ, OLQScoreWithReasoning>()

            for (i in 0 until olqScoresArray.length()) {
                val scoreJson = olqScoresArray.getJSONObject(i)
                val olqName = scoreJson.getString("olq")
                // Match by displayName (e.g., "Self Confidence") or enum name (e.g., "SELF_CONFIDENCE")
                val olq = OLQ.entries.find {
                    it.displayName.equals(olqName, ignoreCase = true) ||
                    it.name.equals(olqName, ignoreCase = true)
                } ?: continue

                val evidenceArray = scoreJson.optJSONArray("evidence")
                val evidence = mutableListOf<String>()
                evidenceArray?.let {
                    for (j in 0 until it.length()) {
                        evidence.add(it.getString(j))
                    }
                }

                olqScores[olq] = OLQScoreWithReasoning(
                    olq = olq,
                    score = scoreJson.getDouble("score").toFloat(),
                    reasoning = scoreJson.getString("reasoning"),
                    evidence = evidence
                )
            }

            val insightsArray = json.getJSONArray("keyInsights")
            val insights = mutableListOf<String>()
            for (i in 0 until insightsArray.length()) {
                insights.add(insightsArray.getString(i))
            }

            val analysis = ResponseAnalysis(
                olqScores = olqScores,
                overallConfidence = json.getInt("overallConfidence"),
                keyInsights = insights,
                suggestedFollowUp = json.optString("suggestedFollowUp", null)
            )

            Result.success(analysis)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse analysis response: ${response.text}", e)
            Result.failure(e)
        }
    }
}
