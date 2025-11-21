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
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gemini AI service implementation
 *
 * Uses Google's Gemini 1.5 Flash model for interview question generation
 * and response analysis with structured JSON output
 *
 * @param apiKey Gemini API key (injected from BuildConfig)
 */
@Singleton
class GeminiAIService @Inject constructor(
    private val apiKey: String
) : AIService {

    companion object {
        private const val TAG = "GeminiAIService"
        private const val MODEL_NAME = "gemini-1.5-flash"
        private const val TEMPERATURE = 0.7f
        private const val MAX_TOKENS = 2048
        private const val TIMEOUT_SECONDS = 30L
    }

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

    override suspend fun generatePIQBasedQuestions(
        piqData: String,
        targetOLQs: List<OLQ>?,
        count: Int,
        difficulty: Int
    ): Result<List<InterviewQuestion>> = withContext(Dispatchers.IO) {
        try {
            val prompt = buildPIQQuestionPrompt(piqData, targetOLQs, count, difficulty)
            val response = model.generateContent(prompt)

            parseQuestionResponse(response)
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
            val prompt = buildAdaptiveQuestionPrompt(
                previousQuestions,
                previousResponses,
                weakOLQs,
                count
            )
            val response = model.generateContent(prompt)

            parseQuestionResponse(response)
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
        try {
            val prompt = buildResponseAnalysisPrompt(question, response, responseMode)
            val aiResponse = model.generateContent(prompt)

            parseAnalysisResponse(aiResponse)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to analyze response", e)
            Result.failure(e)
        }
    }

    override suspend fun generateFeedback(
        questions: List<InterviewQuestion>,
        responses: List<String>,
        olqScores: Map<OLQ, Float>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val prompt = buildFeedbackPrompt(questions, responses, olqScores)
            val response = model.generateContent(prompt)

            val feedbackText = response.text ?: return@withContext Result.failure(
                IllegalStateException("No feedback generated")
            )

            Result.success(feedbackText.trim())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate feedback", e)
            Result.failure(e)
        }
    }

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Simple health check with minimal prompt
            val response = model.generateContent("Reply with OK")
            response.text?.isNotEmpty() == true
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
        val olqList = targetOLQs?.joinToString(", ") { it.displayName }
            ?: "all 15 Officer-Like Qualities"

        return """
You are an SSB (Services Selection Board) interviewing officer expert in assessing candidates for Indian Armed Forces.

**TASK**: Generate $count personalized interview questions based on the candidate's PIQ (Personal Information Questionnaire) data.

**PIQ DATA**:
$piqData

**TARGET OLQs**: $olqList

**DIFFICULTY LEVEL**: $difficulty/5 (1=basic, 5=complex situational)

**REQUIREMENTS**:
1. Questions must be personalized using PIQ data (family, education, hobbies, achievements)
2. Each question should assess 2-3 OLQs simultaneously
3. Use "Why", "How would you", "Tell me about a time" formats
4. Questions should be open-ended, not yes/no
5. Difficulty should match the level specified
6. Questions should feel natural and conversational

**OUTPUT FORMAT** (JSON array):
[
  {
    "id": "unique-id",
    "questionText": "The interview question",
    "targetOLQs": ["EFFECTIVE_INTELLIGENCE", "POWER_OF_EXPRESSION"],
    "difficulty": 3,
    "expectedDuration": 120,
    "hints": ["What to listen for in response"],
    "reasoning": "Why this question is relevant to candidate"
  }
]

Generate exactly $count questions as a valid JSON array.
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
    "difficulty": 4,
    "expectedDuration": 150,
    "hints": ["What to assess"],
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
        val expectedOLQs = question.expectedOLQs.joinToString(", ") { it.displayName }

        return """
You are an SSB psychologist analyzing a candidate's interview response.

**QUESTION**: ${question.questionText}
**TARGET OLQs**: $expectedOLQs
**RESPONSE MODE**: $responseMode

**CANDIDATE'S RESPONSE**:
$response

**TASK**: Analyze the response and assess demonstrated OLQs.

**EVALUATION CRITERIA**:
- Clarity and coherence of thought
- Depth of self-awareness
- Leadership potential indicators
- Problem-solving approach
- Emotional intelligence
- Confidence and communication style

**OUTPUT FORMAT** (JSON):
{
  "olqScores": [
    {
      "olq": "EFFECTIVE_INTELLIGENCE",
      "score": 3.5,
      "reasoning": "Why this score",
      "evidence": ["Specific phrases from response"]
    }
  ],
  "overallConfidence": 85,
  "keyInsights": ["Notable observations"],
  "suggestedFollowUp": "Optional follow-up question"
}

**SCORING SCALE**:
1 = Poor (lacks quality)
2 = Below Average (shows minimal signs)
3 = Average (meets basic expectations)
4 = Good (demonstrates quality well)
5 = Excellent (exceptional demonstration)

Provide analysis as a valid JSON object.
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
     */
    private fun parseQuestionResponse(response: GenerateContentResponse): Result<List<InterviewQuestion>> {
        return try {
            val jsonText = response.text ?: return Result.failure(
                IllegalStateException("No response text")
            )

            // Extract JSON from markdown code blocks if present
            val cleanJson = jsonText
                .substringAfter("```json", jsonText)
                .substringAfter("```", jsonText)
                .substringBefore("```", jsonText)
                .trim()

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
     */
    private fun parseAnalysisResponse(response: GenerateContentResponse): Result<ResponseAnalysis> {
        return try {
            val jsonText = response.text ?: return Result.failure(
                IllegalStateException("No response text")
            )

            // Extract JSON from markdown code blocks if present
            val cleanJson = jsonText
                .substringAfter("```json", jsonText)
                .substringAfter("```", jsonText)
                .substringBefore("```", jsonText)
                .trim()

            val json = JSONObject(cleanJson)

            val olqScoresArray = json.getJSONArray("olqScores")
            val olqScores = mutableMapOf<OLQ, OLQScoreWithReasoning>()

            for (i in 0 until olqScoresArray.length()) {
                val scoreJson = olqScoresArray.getJSONObject(i)
                val olqName = scoreJson.getString("olq")
                val olq = OLQ.entries.find { it.name == olqName } ?: continue

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
