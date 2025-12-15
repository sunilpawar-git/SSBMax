package com.ssbmax.core.data.ai

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.generationConfig
import com.ssbmax.core.data.ai.prompts.SSBInterviewPrompts
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
 * and response analysis with comprehensive SSB-focused prompts.
 *
 * Model: gemini-2.5-flash
 * - Best price-performance for large-scale processing
 * - Low latency, high volume task support
 * - Thinking capability for complex reasoning
 * - 1M token context window
 *
 * Enhanced Features:
 * - Uses SSBInterviewPrompts for comprehensive OLQ-focused prompts
 * - Full PIQ context extraction for personalized questions
 * - Difficulty-based question strategies
 * - Detailed response analysis with evidence
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
        // Increased for comprehensive prompts with OLQ definitions
        private const val MAX_TOKENS = 8192

        // Timeout values (milliseconds) per guidelines
        // Increased for high-latency networks and comprehensive prompts
        private const val QUESTION_GENERATION_TIMEOUT = 45_000L  // 45 seconds (increased for larger prompts)
        private const val RESPONSE_ANALYSIS_TIMEOUT = 25_000L    // 25 seconds
        private const val FEEDBACK_GENERATION_TIMEOUT = 40_000L  // 40 seconds
        private const val HEALTH_CHECK_TIMEOUT = 10_000L         // 10 seconds
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
            Log.d(TAG, "📝 Generating $count PIQ-based questions (difficulty: $difficulty)")
            withTimeout(QUESTION_GENERATION_TIMEOUT) {
                // Use enhanced SSB prompts for comprehensive question generation
                val prompt = SSBInterviewPrompts.buildQuestionGenerationPrompt(
                    piqContext = piqData,
                    count = count,
                    difficulty = difficulty,
                    targetOLQs = targetOLQs
                )

                Log.d(TAG, "📤 Sending comprehensive question generation request to Gemini...")
                val startTime = System.currentTimeMillis()
                val response = model.generateContent(prompt)
                val duration = System.currentTimeMillis() - startTime
                Log.d(TAG, "✅ Received question generation response in ${duration}ms")

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
            Log.d(TAG, "🔄 Generating $count adaptive follow-up questions for weak OLQs: ${weakOLQs.map { it.name }}")
            withTimeout(QUESTION_GENERATION_TIMEOUT) {
                // Build Q&A pairs for context
                val qaHistory = previousQuestions.zip(previousResponses)
                    .map { (q, a) -> q.questionText to a }

                // Use enhanced adaptive question prompt
                // Note: piqContext would ideally be passed here too for full personalization
                // For now, we build from Q&A history
                val prompt = SSBInterviewPrompts.buildAdaptiveQuestionPrompt(
                    piqContext = buildContextFromQA(previousQuestions),
                    previousQA = qaHistory,
                    weakOLQs = weakOLQs,
                    count = count
                )

                val response = model.generateContent(prompt)
                parseQuestionResponse(response)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate adaptive questions", e)
            Result.failure(e)
        }
    }

    /**
     * Build minimal context from previous questions when full PIQ not available
     */
    private fun buildContextFromQA(questions: List<InterviewQuestion>): String {
        return """
(Context derived from previous questions asked)

Topics covered so far:
${questions.mapIndexed { index, q -> 
    "- Question ${index + 1}: ${q.questionText.take(100)}..."
}.joinToString("\n")}

OLQs assessed:
${questions.flatMap { it.expectedOLQs }.distinct().joinToString(", ") { it.displayName }}
        """.trimIndent()
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
                Log.d(TAG, "📝 Building comprehensive analysis prompt...")

                // Use enhanced response analysis prompt
                val prompt = SSBInterviewPrompts.buildResponseAnalysisPrompt(
                    questionText = question.questionText,
                    responseText = response,
                    expectedOLQs = question.expectedOLQs,
                    responseMode = responseMode
                )

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
            Log.d(TAG, "📊 Generating comprehensive feedback for ${questions.size} Q&A pairs")
            withTimeout(FEEDBACK_GENERATION_TIMEOUT) {
                // Build Q&A pairs
                val qaHistory = questions.zip(responses)
                    .map { (q, a) -> q.questionText to a }

                // Use enhanced feedback prompt
                // Note: piqContext would ideally be passed for full personalization
                val prompt = SSBInterviewPrompts.buildFeedbackPrompt(
                    piqContext = "(Full PIQ context not available for feedback generation)",
                    questionAnswerPairs = qaHistory,
                    olqScores = olqScores
                )

                val response = model.generateContent(prompt)

                val feedbackText = response.text ?: return@withTimeout Result.failure(
                    IllegalStateException("No feedback generated")
                )

                Log.d(TAG, "✅ Feedback generated successfully (${feedbackText.length} chars)")
                Result.success(feedbackText.trim())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate feedback", e)
            Result.failure(e)
        }
    }
    
    override suspend fun analyzeGTOResponse(
        prompt: String,
        testType: com.ssbmax.core.domain.model.gto.GTOTestType
    ): Result<ResponseAnalysis> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🎯 Analyzing GTO $testType response")
            withTimeout(RESPONSE_ANALYSIS_TIMEOUT) {
                Log.d(TAG, "📤 Sending GTO analysis request to Gemini...")
                val startTime = System.currentTimeMillis()
                val response = model.generateContent(prompt)
                val duration = System.currentTimeMillis() - startTime

                Log.d(TAG, "✅ Received GTO analysis response in ${duration}ms")

                // Parse GTO response to ResponseAnalysis format
                parseGTOAnalysisResponse(response.text ?: "")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to analyze GTO response", e)
            Result.failure(e)
        }
    }

    override suspend fun analyzeTATResponse(prompt: String): Result<ResponseAnalysis> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "📖 Analyzing TAT submission")
                withTimeout(RESPONSE_ANALYSIS_TIMEOUT) {
                    Log.d(TAG, "📤 Sending TAT analysis request to Gemini...")
                    val startTime = System.currentTimeMillis()
                    val response = model.generateContent(prompt)
                    val duration = System.currentTimeMillis() - startTime

                    Log.d(TAG, "✅ Received TAT analysis response in ${duration}ms")

                    // Parse psychology test response (same format as GTO)
                    parseGTOAnalysisResponse(response.text ?: "")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to analyze TAT response", e)
                Result.failure(e)
            }
        }

    override suspend fun analyzeWATResponse(prompt: String): Result<ResponseAnalysis> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "💬 Analyzing WAT submission")
                withTimeout(RESPONSE_ANALYSIS_TIMEOUT) {
                    Log.d(TAG, "📤 Sending WAT analysis request to Gemini...")
                    val startTime = System.currentTimeMillis()
                    val response = model.generateContent(prompt)
                    val duration = System.currentTimeMillis() - startTime

                    Log.d(TAG, "✅ Received WAT analysis response in ${duration}ms")

                    // Parse psychology test response (same format as GTO)
                    parseGTOAnalysisResponse(response.text ?: "")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to analyze WAT response", e)
                Result.failure(e)
            }
        }

    override suspend fun analyzeSRTResponse(prompt: String): Result<ResponseAnalysis> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "⚡ Analyzing SRT submission")
                withTimeout(RESPONSE_ANALYSIS_TIMEOUT) {
                    Log.d(TAG, "📤 Sending SRT analysis request to Gemini...")
                    val startTime = System.currentTimeMillis()
                    val response = model.generateContent(prompt)
                    val duration = System.currentTimeMillis() - startTime

                    Log.d(TAG, "✅ Received SRT analysis response in ${duration}ms")

                    // Parse psychology test response (same format as GTO)
                    parseGTOAnalysisResponse(response.text ?: "")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to analyze SRT response", e)
                Result.failure(e)
            }
        }

    override suspend fun analyzeSDResponse(prompt: String): Result<ResponseAnalysis> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🪞 Analyzing SD (Self Description) submission")
                withTimeout(RESPONSE_ANALYSIS_TIMEOUT) {
                    Log.d(TAG, "📤 Sending SD analysis request to Gemini...")
                    val startTime = System.currentTimeMillis()
                    val response = model.generateContent(prompt)
                    val duration = System.currentTimeMillis() - startTime

                    Log.d(TAG, "✅ Received SD analysis response in ${duration}ms")

                    // Parse psychology test response (same format as GTO)
                    parseGTOAnalysisResponse(response.text ?: "")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to analyze SD response", e)
                Result.failure(e)
            }
        }
    
    /**
     * Parse GTO-specific JSON response to standard ResponseAnalysis format
     * 
     * GTO uses different JSON structure:
     * {
     *   "olqScores": {
     *     "EFFECTIVE_INTELLIGENCE": {"score": 5, "confidence": 80, "reasoning": "..."},
     *     ...
     *   }
     * }
     */
    private fun parseGTOAnalysisResponse(jsonText: String): Result<ResponseAnalysis> {
        return try {
            if (jsonText.isBlank()) {
                return Result.failure(IllegalStateException("Empty response from Gemini"))
            }
            
            // Log first 200 chars for debugging
            Log.d(TAG, "Raw response preview: ${jsonText.take(200)}")
            
            // Extract JSON from response - handle various formats
            val cleanJson = when {
                // Look for JSON in markdown code blocks first
                "```json" in jsonText -> {
                    jsonText.substringAfter("```json")
                        .substringBefore("```")
                        .trim()
                }
                "```" in jsonText -> {
                    jsonText.substringAfter("```")
                        .substringBefore("```")
                        .trim()
                }
                // Look for JSON object boundaries
                "{" in jsonText && "}" in jsonText -> {
                    val startIndex = jsonText.indexOf('{')
                    val endIndex = jsonText.lastIndexOf('}') + 1
                    jsonText.substring(startIndex, endIndex).trim()
                }
                else -> jsonText.trim()
            }
            
            Log.d(TAG, "Extracted JSON preview: ${cleanJson.take(200)}")
            
            val json = org.json.JSONObject(cleanJson)
            val olqScoresJson = json.getJSONObject("olqScores")
            val olqScores = mutableMapOf<OLQ, OLQScoreWithReasoning>()
            
            // Parse each OLQ score
            olqScoresJson.keys().forEach { olqKey ->
                // Match by enum name OR display name (robust lookup)
                val olq = OLQ.entries.find { 
                    it.name.equals(olqKey, ignoreCase = true) || 
                    it.displayName.equals(olqKey, ignoreCase = true)
                }
                
                if (olq != null) {
                    val scoreObj = olqScoresJson.getJSONObject(olqKey)
                    val score = scoreObj.optDouble("score", 6.0).toFloat()
                    val confidence = scoreObj.optInt("confidence", 50)
                    val reasoning = scoreObj.optString("reasoning", "")
                    
                    olqScores[olq] = OLQScoreWithReasoning(
                        olq = olq,
                        score = score,
                        reasoning = reasoning,
                        evidence = emptyList() // GTO doesn't provide evidence field
                    )
                }
            }
            
            if (olqScores.isEmpty()) {
                Log.e(TAG, "No OLQ scores found in response")
                return Result.failure(IllegalStateException("No OLQ scores parsed from response"))
            }
            
            // Calculate average confidence from individual scores
            val avgConfidence = olqScoresJson.keys().asSequence()
                .mapNotNull {
                    olqScoresJson.optJSONObject(it)?.optInt("confidence")
                }
                .average()
                .toInt()
                .coerceIn(0, 100)
            
            val analysis = ResponseAnalysis(
                olqScores = olqScores,
                overallConfidence = avgConfidence,
                keyInsights = emptyList(), // Not provided in GTO response
                suggestedFollowUp = null
            )
            
            Log.d(TAG, "✅ Parsed ${olqScores.size} OLQ scores from GTO analysis")
            Result.success(analysis)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse GTO analysis response", e)
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
     * Parse question generation response
     * Handles both clean JSON and markdown-wrapped JSON
     */
    private fun parseQuestionResponse(response: GenerateContentResponse): Result<List<InterviewQuestion>> {
        return try {
            val jsonText = response.text ?: return Result.failure(
                IllegalStateException("No response text")
            )

            Log.d(TAG, "🔍 Parsing question response (${jsonText.length} chars)")

            // Extract JSON from markdown code blocks if present
            val cleanJson = extractJsonFromResponse(jsonText)

            val jsonArray = JSONArray(cleanJson)
            val questions = mutableListOf<InterviewQuestion>()

            for (i in 0 until jsonArray.length()) {
                val questionJson = jsonArray.getJSONObject(i)
                questions.add(parseQuestion(questionJson))
            }

            Log.d(TAG, "✅ Parsed ${questions.size} questions successfully")
            Result.success(questions)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse question response: ${response.text?.take(500)}", e)
            Result.failure(e)
        }
    }

    /**
     * Extract JSON from response, handling markdown code blocks
     */
    private fun extractJsonFromResponse(responseText: String): String {
        return when {
            "```json" in responseText -> {
                responseText
                    .substringAfter("```json")
                    .substringBefore("```")
                    .trim()
            }
            "```" in responseText -> {
                responseText
                    .substringAfter("```")
                    .substringBefore("```")
                    .trim()
            }
            else -> {
                // Try to find JSON array or object
                val trimmed = responseText.trim()
                when {
                    trimmed.startsWith("[") -> trimmed.substringBefore("\n\n").trim()
                    trimmed.startsWith("{") -> trimmed.substringBefore("\n\n").trim()
                    else -> trimmed
                }
            }
        }
    }

    /**
     * Parse individual question JSON
     */
    private fun parseQuestion(json: JSONObject): InterviewQuestion {
        // Handle both "targetOLQs" and "expectedOLQs" field names
        val olqsArray = when {
            json.has("targetOLQs") -> json.getJSONArray("targetOLQs")
            json.has("expectedOLQs") -> json.getJSONArray("expectedOLQs")
            else -> JSONArray()
        }

        val expectedOLQs = mutableListOf<OLQ>()
        for (i in 0 until olqsArray.length()) {
            val olqName = olqsArray.getString(i)
            // Match by name or display name
            OLQ.entries.find {
                it.name.equals(olqName, ignoreCase = true) ||
                    it.displayName.equals(olqName, ignoreCase = true)
            }?.let { expectedOLQs.add(it) }
        }

        // Build context from available fields
        val context = buildString {
            json.optString("reasoning", "").let { if (it.isNotBlank()) append(it) }
            json.optString("piqTouchpoint", "").let {
                if (it.isNotBlank()) append(" [PIQ: $it]")
            }
        }.ifBlank { null }

        return InterviewQuestion(
            id = json.optString("id", UUID.randomUUID().toString()),
            questionText = json.getString("questionText"),
            expectedOLQs = expectedOLQs,
            context = context,
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

            Log.d(TAG, "🔍 Parsing analysis response (${jsonText.length} chars)")

            val cleanJson = extractJsonFromResponse(jsonText)
            val json = JSONObject(cleanJson)

            val olqScoresArray = json.getJSONArray("olqScores")
            val olqScores = mutableMapOf<OLQ, OLQScoreWithReasoning>()

            for (i in 0 until olqScoresArray.length()) {
                val scoreJson = olqScoresArray.getJSONObject(i)
                val olqName = scoreJson.getString("olq")

                // Match by displayName or enum name
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
                    reasoning = scoreJson.optString("reasoning", ""),
                    evidence = evidence
                )
            }

            val insightsArray = json.optJSONArray("keyInsights") ?: JSONArray()
            val insights = mutableListOf<String>()
            for (i in 0 until insightsArray.length()) {
                insights.add(insightsArray.getString(i))
            }

            val analysis = ResponseAnalysis(
                olqScores = olqScores,
                overallConfidence = json.optInt("overallConfidence", 50),
                keyInsights = insights,
                suggestedFollowUp = json.optString("suggestedFollowUp", "").takeIf { it.isNotBlank() }
            )

            Log.d(TAG, "✅ Parsed analysis with ${olqScores.size} OLQ scores")
            Result.success(analysis)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse analysis response: ${response.text?.take(500)}", e)
            Result.failure(e)
        }
    }
}

