package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.interview.InterviewMode
import com.ssbmax.shared.domain.model.interview.InterviewResponse
import com.ssbmax.shared.domain.model.interview.InterviewResult
import com.ssbmax.shared.domain.model.interview.InterviewSession
import com.ssbmax.shared.domain.model.interview.InterviewStatus
import com.ssbmax.shared.domain.model.interview.OLQ
import com.ssbmax.shared.domain.model.interview.OLQCategory
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Firestore DTOs + mappers for the interview vertical slice, replacing the Android original's
 * `InterviewFirestoreMappers` (a `Map<String, Any?>` <-> domain object mapper built on
 * `DocumentSnapshot.getData()`). Ported as typed `@Serializable` DTOs instead — same choice
 * [GitLiveGTORepository] made for its own fixed-schema documents — because these four documents
 * (session/question/response/result) have a known, stable shape, unlike the dynamic
 * per-test-type `submissions` documents [FirestoreRawMapSerializer] exists for. Reuses
 * [OLQScoreDto]/`OLQScore.toDto()`/`OLQScoreDto.toDomain()` already defined in
 * `GitLiveGTORepository.kt` (same package) rather than redefining them.
 */
@Serializable
internal data class InterviewSessionDto(
    val id: String = "",
    val userId: String = "",
    val mode: String = "",
    val status: String = "",
    val startedAt: Long = 0L,
    val completedAt: Long? = null,
    val piqSnapshotId: String = "",
    val consentGiven: Boolean = false,
    val questionIds: List<String> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val estimatedDuration: Int = 30
)

internal fun InterviewSession.toDto(): InterviewSessionDto = InterviewSessionDto(
    id = id,
    userId = userId,
    mode = mode.name,
    status = status.name,
    startedAt = startedAt.toEpochMilliseconds(),
    completedAt = completedAt?.toEpochMilliseconds(),
    piqSnapshotId = piqSnapshotId,
    consentGiven = consentGiven,
    questionIds = questionIds,
    currentQuestionIndex = currentQuestionIndex,
    estimatedDuration = estimatedDuration
)

internal fun InterviewSessionDto.toDomain(): InterviewSession = InterviewSession(
    id = id,
    userId = userId,
    mode = InterviewMode.valueOf(mode),
    status = InterviewStatus.valueOf(status),
    startedAt = Instant.fromEpochMilliseconds(startedAt),
    completedAt = completedAt?.let { Instant.fromEpochMilliseconds(it) },
    piqSnapshotId = piqSnapshotId,
    consentGiven = consentGiven,
    questionIds = questionIds,
    currentQuestionIndex = currentQuestionIndex,
    estimatedDuration = estimatedDuration
)

// InterviewQuestionDto + InterviewQuestion.toDto()/InterviewQuestionDto.toDomain() already exist
// in GitLiveQuestionCacheRepository.kt (same package) — reused as-is here rather than redefined.

/**
 * `userId` is not part of the [InterviewResponse] domain model; it is carried on the document only
 * so [GitLiveInterviewRepository.getResponses] can filter by it, mirroring the Android original's
 * `responseMap["userId"] = session.userId` side-channel field.
 */
@Serializable
internal data class InterviewResponseDto(
    val id: String = "",
    val sessionId: String = "",
    val questionId: String = "",
    val userId: String = "",
    val responseText: String = "",
    val responseMode: String = "",
    val respondedAt: Long = 0L,
    val thinkingTimeSec: Int = 0,
    val audioUrl: String? = null,
    val olqScores: Map<String, OLQScoreDto> = emptyMap(),
    val confidenceScore: Int = 0
)

internal fun InterviewResponse.toDto(userId: String): InterviewResponseDto = InterviewResponseDto(
    id = id,
    sessionId = sessionId,
    questionId = questionId,
    userId = userId,
    responseText = responseText,
    responseMode = responseMode.name,
    respondedAt = respondedAt.toEpochMilliseconds(),
    thinkingTimeSec = thinkingTimeSec,
    audioUrl = audioUrl,
    olqScores = olqScores.entries.associate { (olq, score) -> olq.name to score.toDto() },
    confidenceScore = confidenceScore
)

internal fun InterviewResponseDto.toDomain(): InterviewResponse = InterviewResponse(
    id = id,
    sessionId = sessionId,
    questionId = questionId,
    responseText = responseText,
    responseMode = InterviewMode.valueOf(responseMode),
    respondedAt = Instant.fromEpochMilliseconds(respondedAt),
    thinkingTimeSec = thinkingTimeSec,
    audioUrl = audioUrl,
    olqScores = olqScores.mapNotNull { (name, dto) ->
        OLQ.entries.find { it.name == name }?.let { it to dto.toDomain() }
    }.toMap(),
    confidenceScore = confidenceScore
)

@Serializable
internal data class InterviewResultDto(
    val id: String = "",
    val sessionId: String = "",
    val userId: String = "",
    val mode: String = "",
    val completedAt: Long = 0L,
    val durationSec: Long = 0L,
    val totalQuestions: Int = 0,
    val totalResponses: Int = 0,
    val overallOLQScores: Map<String, OLQScoreDto> = emptyMap(),
    val categoryScores: Map<String, Float> = emptyMap(),
    val overallConfidence: Int = 0,
    val strengths: List<String> = emptyList(),
    val weaknesses: List<String> = emptyList(),
    val feedback: String = "",
    val overallRating: Int = 0
)

internal fun InterviewResult.toDto(): InterviewResultDto = InterviewResultDto(
    id = id,
    sessionId = sessionId,
    userId = userId,
    mode = mode.name,
    completedAt = completedAt.toEpochMilliseconds(),
    durationSec = durationSec,
    totalQuestions = totalQuestions,
    totalResponses = totalResponses,
    overallOLQScores = overallOLQScores.entries.associate { (olq, score) -> olq.name to score.toDto() },
    categoryScores = categoryScores.entries.associate { (category, score) -> category.name to score },
    overallConfidence = overallConfidence,
    strengths = strengths.map { it.name },
    weaknesses = weaknesses.map { it.name },
    feedback = feedback,
    overallRating = overallRating
)

/**
 * Progress-tracking submission record written by [GitLiveInterviewRepository.completeInterview]
 * into the shared `submissions` collection so a completed interview shows up in "Your Progress",
 * mirroring the Android original's inline `submissionMap`. A small fixed-shape DTO rather than a
 * [FirestoreRawMapSerializer] write — that serializer exists for genuinely dynamic per-test-type
 * payloads, not this repository's own fixed 9-field record.
 */
@Serializable
internal data class InterviewProgressSubmissionDto(
    val id: String = "",
    val userId: String = "",
    val testId: String = "",
    val testType: String = "IO",
    val status: String = "COMPLETED",
    val submittedAt: Long = 0L,
    val score: Float = 0f,
    val resultId: String = "",
    val mode: String = ""
)

internal fun InterviewResultDto.toDomain(): InterviewResult = InterviewResult(
    id = id,
    sessionId = sessionId,
    userId = userId,
    mode = InterviewMode.valueOf(mode),
    completedAt = Instant.fromEpochMilliseconds(completedAt),
    durationSec = durationSec,
    totalQuestions = totalQuestions,
    totalResponses = totalResponses,
    overallOLQScores = overallOLQScores.mapNotNull { (name, dto) ->
        OLQ.entries.find { it.name == name }?.let { it to dto.toDomain() }
    }.toMap(),
    categoryScores = categoryScores.mapNotNull { (name, score) ->
        OLQCategory.entries.find { it.name == name }?.let { it to score }
    }.toMap(),
    overallConfidence = overallConfidence,
    strengths = strengths.mapNotNull { name -> OLQ.entries.find { it.name == name } },
    weaknesses = weaknesses.mapNotNull { name -> OLQ.entries.find { it.name == name } },
    feedback = feedback,
    overallRating = overallRating
)
