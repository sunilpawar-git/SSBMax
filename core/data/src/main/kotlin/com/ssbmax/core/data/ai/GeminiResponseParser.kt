package com.ssbmax.core.data.ai

import android.util.Log
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.ssbmax.core.domain.model.interview.InterviewQuestion
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.QuestionSource
import com.ssbmax.core.domain.service.OLQScoreWithReasoning
import com.ssbmax.core.domain.service.ResponseAnalysis
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal object GeminiResponseParser {

    private const val TAG = "GeminiResponseParser"

    fun parseGTOAnalysisResponse(jsonText: String): Result<ResponseAnalysis> {
        return try {
            if (jsonText.isBlank()) {
                return Result.failure(IllegalStateException("Empty response from Gemini"))
            }
            val cleanJson = extractJsonFromResponse(jsonText)
            // Array format: Gemini sometimes returns [{olq, score, confidence, reasoning}, ...]
            // instead of the canonical {olqScores: {OLQ_KEY: {score, confidence, reasoning}}}
            if (cleanJson.trimStart().startsWith("[")) {
                return parseGTOArrayFormat(cleanJson)
            }
            val json = JSONObject(cleanJson)
            val olqScoresJson = json.getJSONObject("olqScores")
            val olqScores = mutableMapOf<OLQ, OLQScoreWithReasoning>()
            olqScoresJson.keys().forEach { olqKey ->
                val olq = OLQ.entries.find {
                    it.name.equals(olqKey, ignoreCase = true) ||
                        it.displayName.equals(olqKey, ignoreCase = true)
                }
                if (olq != null) {
                    val scoreObj = olqScoresJson.getJSONObject(olqKey)
                    val score = scoreObj.optDouble("score", 6.0).toFloat()
                    val reasoning = scoreObj.optString("reasoning", "")
                    olqScores[olq] = OLQScoreWithReasoning(
                        olq = olq,
                        score = score,
                        reasoning = reasoning,
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
            Log.e(TAG, "Failed to parse GTO analysis response", e)
            Result.failure(e)
        }
    }

    private fun parseGTOArrayFormat(cleanJson: String): Result<ResponseAnalysis> {
        return try {
            val arr = JSONArray(cleanJson)
            val olqScores = mutableMapOf<OLQ, OLQScoreWithReasoning>()
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val olqKey = item.optString("olq")
                val olq = OLQ.entries.find {
                    it.name.equals(olqKey, ignoreCase = true) ||
                        it.displayName.equals(olqKey, ignoreCase = true)
                }
                if (olq != null) {
                    olqScores[olq] = OLQScoreWithReasoning(
                        olq = olq,
                        score = item.optDouble("score", 6.0).toFloat(),
                        reasoning = item.optString("reasoning", ""),
                        evidence = emptyList()
                    )
                }
            }
            if (olqScores.isEmpty()) {
                return Result.failure(IllegalStateException("No OLQ scores parsed from array response"))
            }
            val avgConfidence = (0 until arr.length())
                .mapNotNull { arr.optJSONObject(it)?.optInt("confidence") }
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
            Log.e(TAG, "Failed to parse GTO array format response", e)
            Result.failure(e)
        }
    }

    fun parseQuestionResponse(response: GenerateContentResponse): Result<List<InterviewQuestion>> {
        return try {
            val jsonText = response.text ?: return Result.failure(
                IllegalStateException("No response text")
            )
            val cleanJson = extractJsonFromResponse(jsonText)
            val jsonArray = JSONArray(cleanJson)
            val questions = (0 until jsonArray.length()).map { parseQuestion(jsonArray.getJSONObject(it)) }
            Result.success(questions)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse question response", e)
            Result.failure(e)
        }
    }

    fun parseAnalysisResponse(response: GenerateContentResponse): Result<ResponseAnalysis> {
        return try {
            val jsonText = response.text ?: return Result.failure(
                IllegalStateException("No response text")
            )
            val cleanJson = extractJsonFromResponse(jsonText)
            val json = JSONObject(cleanJson)
            val olqScoresArray = json.getJSONArray("olqScores")
            val olqScores = mutableMapOf<OLQ, OLQScoreWithReasoning>()
            for (i in 0 until olqScoresArray.length()) {
                val scoreJson = olqScoresArray.getJSONObject(i)
                val olqName = scoreJson.getString("olq")
                val olq = OLQ.entries.find {
                    it.displayName.equals(olqName, ignoreCase = true) ||
                        it.name.equals(olqName, ignoreCase = true)
                } ?: continue
                val evidenceArray = scoreJson.optJSONArray("evidence")
                val evidence = (0 until (evidenceArray?.length() ?: 0)).map {
                    evidenceArray!!.getString(it)
                }
                olqScores[olq] = OLQScoreWithReasoning(
                    olq = olq,
                    score = scoreJson.getDouble("score").toFloat(),
                    reasoning = scoreJson.optString("reasoning", ""),
                    evidence = evidence
                )
            }
            val insightsArray = json.optJSONArray("keyInsights") ?: JSONArray()
            val insights = (0 until insightsArray.length()).map { insightsArray.getString(it) }
            Result.success(
                ResponseAnalysis(
                    olqScores = olqScores,
                    overallConfidence = json.optInt("overallConfidence", 50),
                    keyInsights = insights,
                    suggestedFollowUp = json.optString("suggestedFollowUp", "").takeIf { it.isNotBlank() }
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse analysis response", e)
            Result.failure(e)
        }
    }

    fun extractJsonFromResponse(responseText: String): String {
        return when {
            "```json" in responseText -> responseText.substringAfter("```json").substringBefore("```").trim()
            "```" in responseText -> responseText.substringAfter("```").substringBefore("```").trim()
            "{" in responseText && "}" in responseText -> {
                val start = responseText.indexOf('{')
                val end = responseText.lastIndexOf('}') + 1
                responseText.substring(start, end).trim()
            }
            else -> responseText.trim()
        }
    }

    private fun parseQuestion(json: JSONObject): InterviewQuestion {
        val olqsArray = when {
            json.has("targetOLQs") -> json.getJSONArray("targetOLQs")
            json.has("expectedOLQs") -> json.getJSONArray("expectedOLQs")
            else -> JSONArray()
        }
        val expectedOLQs = (0 until olqsArray.length()).mapNotNull { i ->
            val olqName = olqsArray.getString(i)
            OLQ.entries.find {
                it.name.equals(olqName, ignoreCase = true) || it.displayName.equals(olqName, ignoreCase = true)
            }
        }
        val context = buildString {
            json.optString("reasoning", "").let { if (it.isNotBlank()) append(it) }
            json.optString("piqTouchpoint", "").let { if (it.isNotBlank()) append(" [PIQ: $it]") }
        }.ifBlank { null }
        return InterviewQuestion(
            id = json.optString("id", UUID.randomUUID().toString()),
            questionText = json.getString("questionText"),
            expectedOLQs = expectedOLQs,
            context = context,
            source = QuestionSource.AI_GENERATED
        )
    }
}
