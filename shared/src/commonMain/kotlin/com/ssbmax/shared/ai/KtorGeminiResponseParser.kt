package com.ssbmax.shared.ai

import com.ssbmax.shared.domain.model.interview.InterviewQuestion
import com.ssbmax.shared.domain.model.interview.OLQ
import com.ssbmax.shared.domain.model.interview.QuestionSource
import com.ssbmax.shared.domain.service.OLQScoreWithReasoning
import com.ssbmax.shared.domain.service.ResponseAnalysis
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * KMP-safe port of GeminiResponseParser. The Android original uses org.json
 * (Android/JVM-only, via android.util.Log too) — this uses
 * kotlinx.serialization instead, since that's the JSON library already on
 * every KMP target here (Ktor's own content negotiation depends on it).
 *
 * Covers all three response shapes the Android parser handles:
 * [parseAnalysisResponse] (single-response interview analysis, object with an
 * `olqScores` array), [parseGTOAnalysisResponse] (GTO/TAT/WAT/SRT/SD/PPDT
 * analysis, object with an `olqScores` map OR a bare array — Gemini emits
 * both shapes in practice), and [parseQuestionResponse] (question generation,
 * a JSON array of question objects).
 */
object KtorGeminiResponseParser {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class AnalysisResponseDto(
        val olqScores: List<OlqScoreDto> = emptyList(),
        val overallConfidence: Int = 50,
        val keyInsights: List<String> = emptyList(),
        val suggestedFollowUp: String? = null
    )

    @Serializable
    private data class OlqScoreDto(
        val olq: String,
        val score: Float,
        val reasoning: String = "",
        val evidence: List<String> = emptyList()
    )

    fun parseAnalysisResponse(responseText: String): Result<ResponseAnalysis> {
        return try {
            val cleanJson = extractJsonFromResponse(responseText)
            val dto = json.decodeFromString(AnalysisResponseDto.serializer(), cleanJson)

            val olqScores = dto.olqScores.mapNotNull { scoreDto ->
                val olq = OLQ.entries.find {
                    it.displayName.equals(scoreDto.olq, ignoreCase = true) ||
                        it.name.equals(scoreDto.olq, ignoreCase = true)
                } ?: return@mapNotNull null
                olq to OLQScoreWithReasoning(
                    olq = olq,
                    score = scoreDto.score,
                    reasoning = scoreDto.reasoning,
                    evidence = scoreDto.evidence
                )
            }.toMap()

            if (olqScores.isEmpty()) {
                return Result.failure(IllegalStateException("No OLQ scores parsed from response"))
            }

            Result.success(
                ResponseAnalysis(
                    olqScores = olqScores,
                    overallConfidence = dto.overallConfidence.coerceIn(0, 100),
                    keyInsights = dto.keyInsights,
                    suggestedFollowUp = dto.suggestedFollowUp?.takeIf { it.isNotBlank() }
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @Serializable
    private data class GTOAnalysisResponseDto(
        val olqScores: Map<String, GTOScoreDto> = emptyMap(),
        val notRecommended: Boolean = false
    )

    @Serializable
    private data class GTOScoreDto(
        val score: Float = 6f,
        val confidence: Int = 50,
        val reasoning: String = ""
    )

    @Serializable
    private data class GTOArrayItemDto(
        val olq: String = "",
        val score: Float = 6f,
        val confidence: Int = 50,
        val reasoning: String = ""
    )

    /**
     * Parses GTO/TAT/WAT/SRT/SD/PPDT analysis responses. Gemini emits either the
     * canonical `{olqScores: {OLQ_KEY: {score, confidence, reasoning}}}` object shape,
     * or (observed in practice) a bare `[{olq, score, confidence, reasoning}, ...]` array —
     * both are handled, matching GeminiResponseParser.parseGTOAnalysisResponse.
     */
    fun parseGTOAnalysisResponse(responseText: String): Result<ResponseAnalysis> {
        if (responseText.isBlank()) {
            return Result.failure(IllegalStateException("Empty response from Gemini"))
        }
        return try {
            val cleanJson = extractJsonFromResponse(responseText)
            if (cleanJson.trimStart().startsWith("[")) {
                parseGTOArrayFormat(cleanJson)
            } else {
                parseGTOObjectFormat(cleanJson)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseGTOObjectFormat(cleanJson: String): Result<ResponseAnalysis> {
        val dto = json.decodeFromString(GTOAnalysisResponseDto.serializer(), cleanJson)
        val olqScores = dto.olqScores.mapNotNull { (olqKey, scoreDto) ->
            val olq = findOlq(olqKey) ?: return@mapNotNull null
            olq to OLQScoreWithReasoning(olq = olq, score = scoreDto.score, reasoning = scoreDto.reasoning)
        }.toMap()
        if (olqScores.isEmpty()) {
            return Result.failure(IllegalStateException("No OLQ scores parsed from response"))
        }
        val avgConfidence = dto.olqScores.values.map { it.confidence }.average().toInt().coerceIn(0, 100)
        return Result.success(
            ResponseAnalysis(
                olqScores = olqScores,
                overallConfidence = avgConfidence,
                keyInsights = emptyList(),
                suggestedFollowUp = null,
                notRecommended = dto.notRecommended
            )
        )
    }

    private fun parseGTOArrayFormat(cleanJson: String): Result<ResponseAnalysis> {
        val items = json.decodeFromString(ListSerializer(GTOArrayItemDto.serializer()), cleanJson)
        val olqScores = items.mapNotNull { item ->
            val olq = findOlq(item.olq) ?: return@mapNotNull null
            olq to OLQScoreWithReasoning(olq = olq, score = item.score, reasoning = item.reasoning)
        }.toMap()
        if (olqScores.isEmpty()) {
            return Result.failure(IllegalStateException("No OLQ scores parsed from array response"))
        }
        val avgConfidence = items.map { it.confidence }.average().toInt().coerceIn(0, 100)
        return Result.success(
            ResponseAnalysis(
                olqScores = olqScores,
                overallConfidence = avgConfidence,
                keyInsights = emptyList(),
                suggestedFollowUp = null
            )
        )
    }

    @Serializable
    private data class QuestionDto(
        val id: String? = null,
        val questionText: String,
        val targetOLQs: List<String>? = null,
        val expectedOLQs: List<String>? = null,
        val reasoning: String? = null,
        val piqTouchpoint: String? = null
    )

    /** Parses the JSON array of generated interview questions (question-generation prompts). */
    @OptIn(ExperimentalUuidApi::class)
    fun parseQuestionResponse(responseText: String): Result<List<InterviewQuestion>> {
        return try {
            val cleanJson = extractJsonFromResponse(responseText)
            val dtos = json.decodeFromString(ListSerializer(QuestionDto.serializer()), cleanJson)
            val questions = dtos.map { dto ->
                val olqNames = dto.targetOLQs ?: dto.expectedOLQs ?: emptyList()
                val expectedOLQs = olqNames.mapNotNull { findOlq(it) }
                val context = buildString {
                    dto.reasoning?.takeIf { it.isNotBlank() }?.let { append(it) }
                    dto.piqTouchpoint?.takeIf { it.isNotBlank() }?.let { append(" [PIQ: $it]") }
                }.ifBlank { null }
                InterviewQuestion(
                    id = dto.id?.takeIf { it.isNotBlank() } ?: Uuid.random().toString(),
                    questionText = dto.questionText,
                    expectedOLQs = expectedOLQs,
                    context = context,
                    source = QuestionSource.AI_GENERATED
                )
            }
            Result.success(questions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun findOlq(name: String): OLQ? = OLQ.entries.find {
        it.displayName.equals(name, ignoreCase = true) || it.name.equals(name, ignoreCase = true)
    }

    /** Ported verbatim from GeminiResponseParser.extractJsonFromResponse. */
    fun extractJsonFromResponse(responseText: String): String {
        return when {
            "```json" in responseText -> responseText.substringAfter("```json").substringBefore("```").trim()
            "```" in responseText -> responseText.substringAfter("```").substringBefore("```").trim()
            "[" in responseText && "]" in responseText &&
                responseText.indexOf('[') < (responseText.indexOf('{').takeIf { it >= 0 } ?: Int.MAX_VALUE) -> {
                val start = responseText.indexOf('[')
                val end = responseText.lastIndexOf(']') + 1
                responseText.substring(start, end).trim()
            }
            "{" in responseText && "}" in responseText -> {
                val start = responseText.indexOf('{')
                val end = responseText.lastIndexOf('}') + 1
                responseText.substring(start, end).trim()
            }
            else -> responseText.trim()
        }
    }
}
