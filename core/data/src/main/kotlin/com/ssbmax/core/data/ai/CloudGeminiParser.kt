package com.ssbmax.core.data.ai

import android.util.Log
import com.ssbmax.core.domain.model.interview.InterviewQuestion
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.QuestionSource
import com.ssbmax.core.domain.service.OLQScoreWithReasoning
import com.ssbmax.core.domain.service.ResponseAnalysis
import java.util.UUID

/**
 * Pure parsing utility to decouple logic from Firebase connections
 * in CloudGeminiAIService.
 * 
 * Provides robust validation, null-safety, and clamps score values
 * to protect domain-level contracts.
 */
object CloudGeminiParser {
    private const val TAG = "CloudGeminiParser"

    /**
     * Parse questions result from Cloud Function
     */
    fun parseQuestionsResult(data: Any): Result<List<InterviewQuestion>> {
        return try {
            val map = data as? Map<*, *>
                ?: return Result.failure(IllegalStateException("Invalid response format"))

            if (map["success"] != true) {
                return Result.failure(Exception("Function returned failure"))
            }

            val questionsData = map["questions"] as? List<*>
                ?: return Result.failure(IllegalStateException("Missing questions array"))

            val questions = questionsData.mapNotNull { questionData ->
                parseQuestionData(questionData as? Map<*, *> ?: return@mapNotNull null)
            }

            Result.success(questions)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse questions result", e)
            Result.failure(e)
        }
    }

    /**
     * Parse individual question data
     */
    private fun parseQuestionData(data: Map<*, *>): InterviewQuestion? {
        return try {
            val expectedOLQNames: List<*> = data["expectedOLQs"] as? List<*> ?: emptyList<Any>()
            val expectedOLQs = expectedOLQNames.mapNotNull { name ->
                OLQ.entries.find { it.name == name.toString() }
            }

            InterviewQuestion(
                id = data["id"]?.toString() ?: UUID.randomUUID().toString(),
                questionText = data["questionText"]?.toString() ?: return null,
                expectedOLQs = expectedOLQs,
                context = data["context"]?.toString(),
                source = QuestionSource.AI_GENERATED
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse question data", e)
            null
        }
    }

    /**
     * Parse analysis result from Cloud Function
     */
    fun parseAnalysisResult(data: Any): Result<ResponseAnalysis> {
        return try {
            val map = data as? Map<*, *>
                ?: return Result.failure(IllegalStateException("Invalid response format"))

            if (map["success"] != true) {
                return Result.failure(Exception("Function returned failure"))
            }

            val analysisData = map["analysis"] as? Map<*, *>
                ?: return Result.failure(IllegalStateException("Missing analysis data"))

            val olqScoresData = analysisData["olqScores"] as? List<*>
                ?: return Result.failure(IllegalStateException("Missing olqScores"))

            val olqScores = mutableMapOf<OLQ, OLQScoreWithReasoning>()

            olqScoresData.forEach { scoreData ->
                val scoreMap = scoreData as? Map<*, *> ?: return@forEach
                val olqName = scoreMap["olq"]?.toString() ?: return@forEach
                val ol = OLQ.entries.find { it.name == olqName } ?: return@forEach

                val evidenceData: List<*> = scoreMap["evidence"] as? List<*> ?: emptyList<Any>()
                val evidence = evidenceData.mapNotNull { it?.toString() }

                // Clamp score strictly between 1f and 10f to guarantee OLQScoreWithReasoning contract is met
                val rawScore = (scoreMap["score"] as? Number)?.toFloat() ?: 5.0f
                val clampedScore = rawScore.coerceIn(1.0f, 10.0f)

                // Fallback for reasoning to guaranteeNotBlank contract requirement
                val rawReasoning = scoreMap["reasoning"]?.toString()
                val reasoning = if (!rawReasoning.isNullOrBlank()) {
                    rawReasoning
                } else {
                    "No specific reasoning provided."
                }
                
                val finalReasoning = if (reasoning.isBlank()) "No specific reasoning provided." else reasoning

                olqScores[ol] = OLQScoreWithReasoning(
                    olq = ol,
                    score = clampedScore,
                    reasoning = finalReasoning,
                    evidence = evidence
                )
            }

            val insightsData: List<*> = analysisData["keyInsights"] as? List<*> ?: emptyList<Any>()
            val insights = insightsData.mapNotNull { it?.toString() }

            val rawConfidence = (analysisData["overallConfidence"] as? Number)?.toInt() ?: 50
            val overallConfidence = rawConfidence.coerceIn(0, 100)

            val analysis = ResponseAnalysis(
                olqScores = olqScores,
                overallConfidence = overallConfidence,
                keyInsights = insights,
                suggestedFollowUp = analysisData["suggestedFollowUp"]?.toString()
            )

            Result.success(analysis)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse analysis result", e)
            Result.failure(e)
        }
    }

    /**
     * Generate mock questions as fallback
     */
    fun generateMockQuestions(count: Int): List<InterviewQuestion> {
        return List(count) { index ->
            InterviewQuestion(
                id = UUID.randomUUID().toString(),
                questionText = "Mock question ${index + 1}",
                expectedOLQs = listOf(OLQ.SELF_CONFIDENCE, OLQ.POWER_OF_EXPRESSION),
                context = "Mock context",
                source = QuestionSource.GENERIC_POOL
            )
        }
    }
}
