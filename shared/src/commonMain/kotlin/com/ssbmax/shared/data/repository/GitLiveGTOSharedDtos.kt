package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.gto.GTOResult
import com.ssbmax.shared.domain.model.gto.GTOTestType
import com.ssbmax.shared.domain.model.interview.OLQ
import com.ssbmax.shared.domain.model.interview.OLQScore
import kotlinx.serialization.Serializable

/**
 * DTOs and helpers shared between [GitLiveGTOSubmissionDelegate] (writes a [GTOResultDto] when
 * scoring a submission) and [GitLiveGTOResultsDelegate] (reads it back) — split out of the former
 * single `GitLiveGTORepository` god-class (300-line-file limit). Pure structural split — no
 * behavior change from the original merged class.
 */
@Serializable
internal data class GTOResultDto(
    val submissionId: String = "",
    val userId: String = "",
    val testType: String = "",
    val olqScores: Map<String, OLQScoreDto> = emptyMap(),
    val overallScore: Float = 1f,
    val overallRating: String = "",
    val aiConfidence: Int = 0,
    val analyzedAt: Long = 0L
)

@Serializable
internal data class OLQScoreDto(
    val score: Int = 6,
    val confidence: Int = 0,
    val reasoning: String = ""
)

internal fun OLQScore.toDto(): OLQScoreDto = OLQScoreDto(score, confidence, reasoning)

internal fun OLQScoreDto.toDomain(): OLQScore = OLQScore(
    score = score.coerceIn(1, 10),
    confidence = confidence.coerceIn(0, 100),
    reasoning = reasoning
)

internal fun GTOResultDto.toDomain(): GTOResult {
    val testTypeEnum = runCatching { GTOTestType.valueOf(testType) }.getOrDefault(GTOTestType.GROUP_DISCUSSION)
    val parsedScores = olqScores.mapNotNull { (key, dto) ->
        runCatching { OLQ.valueOf(key) }.getOrNull()?.let { it to dto.toDomain() }
    }.toMap()
    // Fill missing OLQs with default zero values, same as the Android original.
    val filledScores = OLQ.entries.associateWith { olq ->
        parsedScores[olq] ?: OLQScore(score = 1, confidence = 0, reasoning = "Not analyzed")
    }

    return GTOResult(
        submissionId = submissionId,
        userId = userId,
        testType = testTypeEnum,
        olqScores = filledScores,
        overallScore = overallScore.coerceIn(1f, 10f),
        overallRating = overallRating,
        aiConfidence = aiConfidence.coerceIn(0, 100),
        analyzedAt = analyzedAt
    )
}

internal fun GTOResult.toDto(): GTOResultDto = GTOResultDto(
    submissionId = submissionId,
    userId = userId,
    testType = testType.name,
    olqScores = olqScores.entries.associate { (olq, score) -> olq.name to score.toDto() },
    overallScore = overallScore,
    overallRating = overallRating,
    aiConfidence = aiConfidence,
    analyzedAt = analyzedAt
)

/** Same rating thresholds as the Android original's `calculateRating`; extracted for direct unit testing. */
internal fun calculateGtoRating(score: Float): String = when {
    score <= 3f -> "Exceptional"
    score <= 4f -> "Excellent"
    score <= 5f -> "Very Good"
    score <= 6f -> "Good"
    score <= 7f -> "Average"
    score <= 8f -> "Below Average"
    else -> "Poor"
}
