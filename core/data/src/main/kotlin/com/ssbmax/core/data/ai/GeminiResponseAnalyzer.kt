package com.ssbmax.core.data.ai

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.ssbmax.core.data.ai.prompts.SSBInterviewPrompts
import com.ssbmax.core.domain.model.gto.GTOTestType
import com.ssbmax.core.domain.model.interview.InterviewQuestion
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.service.OLQScoreWithReasoning
import com.ssbmax.core.domain.service.ResponseAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiResponseAnalyzer @Inject constructor() {

    private companion object {
        const val TAG = "GeminiResponseAnalyzer"
        const val RESPONSE_ANALYSIS_TIMEOUT = 60_000L
        const val FEEDBACK_GENERATION_TIMEOUT = 40_000L
    }

    suspend fun analyzeResponse(
        model: GenerativeModel,
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
                val aiResponse = model.generateContent(prompt)
                parseAnalysisResponse(aiResponse)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to analyze response", e)
            Result.failure(e)
        }
    }

    suspend fun generateFeedback(
        model: GenerativeModel,
        questions: List<InterviewQuestion>,
        responses: List<String>,
        olqScores: Map<OLQ, Float>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            withTimeout(FEEDBACK_GENERATION_TIMEOUT) {
                val qaHistory = questions.zip(responses).map { (q, a) -> q.questionText to a }
                val prompt = SSBInterviewPrompts.buildFeedbackPrompt(
                    piqContext = "(Full PIQ context not available for feedback generation)",
                    questionAnswerPairs = qaHistory,
                    olqScores = olqScores
                )
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

    suspend fun analyzeGTOResponse(
        model: GenerativeModel,
        prompt: String,
        testType: GTOTestType
    ): Result<ResponseAnalysis> = withContext(Dispatchers.IO) {
        try {
            withTimeout(RESPONSE_ANALYSIS_TIMEOUT) {
                val response = model.generateContent(prompt)
                parseGTOAnalysisResponse(response.text ?: "")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to analyze GTO response", e)
            Result.failure(e)
        }
    }

    suspend fun analyzeTATResponse(model: GenerativeModel, prompt: String): Result<ResponseAnalysis> =
        analyzeGTOResponse(model, prompt, GTOTestType.GROUP_PLANNING_EXERCISE) // Reuses psychology parser

    suspend fun analyzeWATResponse(model: GenerativeModel, prompt: String): Result<ResponseAnalysis> =
        analyzeGTOResponse(model, prompt, GTOTestType.GROUP_PLANNING_EXERCISE)

    suspend fun analyzeSRTResponse(model: GenerativeModel, prompt: String): Result<ResponseAnalysis> =
        analyzeGTOResponse(model, prompt, GTOTestType.GROUP_PLANNING_EXERCISE)

    suspend fun analyzeSDResponse(model: GenerativeModel, prompt: String): Result<ResponseAnalysis> =
        analyzeGTOResponse(model, prompt, GTOTestType.GROUP_PLANNING_EXERCISE)

    suspend fun analyzePPDTResponse(model: GenerativeModel, prompt: String): Result<ResponseAnalysis> =
        analyzeGTOResponse(model, prompt, GTOTestType.GROUP_PLANNING_EXERCISE)

    private fun parseAnalysisResponse(response: GenerateContentResponse): Result<ResponseAnalysis> {
        return try {
            val jsonText = response.text ?: return Result.failure(IllegalStateException("No response text"))
            val cleanJson = extractJsonFromResponse(jsonText)
            val json = JSONObject(cleanJson)
            val olqScoresArray = json.getJSONArray("olqScores")
            val olqScores = mutableMapOf<OLQ, OLQScoreWithReasoning>()

            for (i in 0 until olqScoresArray.length()) {
                val scoreJson = olqScoresArray.getJSONObject(i)
                val olqName = scoreJson.getString("olq")
                val olq = OLQ.entries.find {
                    it.displayName.equals(olqName, ignoreCase = true) || it.name.equals(olqName, ignoreCase = true)
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

            Result.success(
                ResponseAnalysis(
                    olqScores = olqScores,
                    overallConfidence = json.optInt("overallConfidence", 50),
                    keyInsights = insights,
                    suggestedFollowUp = json.optString("suggestedFollowUp", "").takeIf { it.isNotBlank() }
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseGTOAnalysisResponse(jsonText: String): Result<ResponseAnalysis> {
        return try {
            val cleanJson = extractJsonFromResponse(jsonText)
            val json = JSONObject(cleanJson)
            val olqScoresJson = json.getJSONObject("olqScores")
            val olqScores = mutableMapOf<OLQ, OLQScoreWithReasoning>()

            olqScoresJson.keys().forEach { olqKey ->
                val olq = OLQ.entries.find {
                    it.name.equals(olqKey, ignoreCase = true) || it.displayName.equals(olqKey, ignoreCase = true)
                }
                if (olq != null) {
                    val scoreObj = olqScoresJson.getJSONObject(olqKey)
                    olqScores[olq] = OLQScoreWithReasoning(
                        olq = olq,
                        score = scoreObj.optDouble("score", 6.0).toFloat(),
                        reasoning = scoreObj.optString("reasoning", ""),
                        evidence = emptyList()
                    )
                }
            }

            if (olqScores.isEmpty()) {
                return Result.failure(IllegalStateException("No OLQ scores parsed from response"))
            }

            val avgConfidence = olqScoresJson.keys().asSequence()
                .mapNotNull { olqScoresJson.optJSONObject(it)?.optInt("confidence") }
                .average().toInt().coerceIn(0, 100)

            Result.success(
                ResponseAnalysis(
                    olqScores = olqScores,
                    overallConfidence = avgConfidence,
                    keyInsights = emptyList(),
                    suggestedFollowUp = null
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractJsonFromResponse(responseText: String): String {
        return when {
            "```json" in responseText -> responseText.substringAfter("```json").substringBefore("```").trim()
            "```" in responseText -> responseText.substringAfter("```").substringBefore("```").trim()
            else -> {
                val trimmed = responseText.trim()
                when {
                    trimmed.startsWith("[") -> trimmed.substringBefore("\n\n").trim()
                    trimmed.startsWith("{") -> trimmed.substringBefore("\n\n").trim()
                    else -> trimmed
                }
            }
        }
    }
}
