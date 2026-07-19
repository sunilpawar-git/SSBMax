package com.ssbmax.shared.ai

import com.ssbmax.shared.domain.model.interview.OLQ
import com.ssbmax.shared.domain.service.OLQScoreWithReasoning
import com.ssbmax.shared.domain.service.ResponseAnalysis
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * KMP-safe port of GeminiResponseParser.parseAnalysisResponse. The Android
 * original uses org.json (Android/JVM-only, via android.util.Log too) — this
 * uses kotlinx.serialization instead, since that's the JSON library already
 * on every KMP target here (Ktor's own content negotiation depends on it).
 * Only parseAnalysisResponse is ported (matches
 * InterviewResponseAnalysisService's narrower scope); parseGTOAnalysisResponse
 * and parseQuestionResponse stay in core:data until their callers are ported.
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
