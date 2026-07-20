package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.model.interview.OLQ
import com.ssbmax.shared.domain.model.scoring.OLQAnalysisResult
import dev.gitlive.firebase.firestore.SnapshotMetadata
import kotlinx.serialization.Serializable

/**
 * Shared submission-cluster constants, mirroring the Android SubmissionConstants object
 * (core:data/remote/SubmissionMappers.kt). `internal` so it's shared across this package's
 * GitLive*SubmissionRepository classes without being exposed outside `shared`.
 */
internal object SubmissionConstants {
    const val ANALYSIS_STATUS_COMPLETED = "COMPLETED"
    const val ANALYSIS_STATUS_PENDING = "PENDING_ANALYSIS"
    const val ANALYSIS_STATUS_ANALYZING = "ANALYZING"
    const val FIELD_DATA = "data"
}

/**
 * Wire format for [OLQAnalysisResult], shared by every OLQ-scored test type (TAT/WAT/SRT/SDT/PPDT/GTO).
 * Mirrors the Android OLQMapper.toFirestoreMap()/parseSharedOLQResult() pair.
 *
 * `userId` is populated only when writing to `psych_results`/`ppdt_results` (required by Firestore
 * security rules there, same as the Android original) — it's not part of the domain [OLQAnalysisResult].
 */
@Serializable
internal data class OLQAnalysisResultDto(
    val submissionId: String = "",
    val testType: String = "",
    val olqScores: Map<String, OLQScoreDto> = emptyMap(),
    val overallScore: Float = 5f,
    val overallRating: String = "",
    val strengths: List<String> = emptyList(),
    val weaknesses: List<String> = emptyList(),
    val recommendations: List<String> = emptyList(),
    val analyzedAt: Long = 0L,
    val aiConfidence: Int = 0,
    val validStoriesCount: Int = 0,
    val failedStoriesCount: Int = 0,
    val usedPartialAssessment: Boolean = false,
    val userId: String? = null
)

internal fun OLQAnalysisResult.toDto(userId: String? = null): OLQAnalysisResultDto = OLQAnalysisResultDto(
    submissionId = submissionId,
    testType = testType.name,
    olqScores = olqScores.entries.associate { (olq, score) -> olq.name to score.toDto() },
    overallScore = overallScore,
    overallRating = overallRating,
    strengths = strengths,
    weaknesses = weaknesses,
    recommendations = recommendations,
    analyzedAt = analyzedAt,
    aiConfidence = aiConfidence,
    validStoriesCount = validStoriesCount,
    failedStoriesCount = failedStoriesCount,
    usedPartialAssessment = usedPartialAssessment,
    userId = userId
)

/**
 * Returns null (not throws) for an unparseable/missing test type, same defensive behavior as
 * the Android `parseSharedOLQResult`.
 */
internal fun OLQAnalysisResultDto.toDomain(): OLQAnalysisResult? {
    val parsedTestType = runCatching { TestType.valueOf(testType) }.getOrNull() ?: return null
    val parsedScores = olqScores.mapNotNull { (key, dto) ->
        runCatching { OLQ.valueOf(key) }.getOrNull()?.let { it to dto.toDomain() }
    }.toMap()
    return OLQAnalysisResult(
        submissionId = submissionId,
        testType = parsedTestType,
        olqScores = parsedScores,
        overallScore = overallScore,
        overallRating = overallRating,
        strengths = strengths,
        weaknesses = weaknesses,
        recommendations = recommendations,
        analyzedAt = analyzedAt,
        aiConfidence = aiConfidence,
        validStoriesCount = validStoriesCount,
        failedStoriesCount = failedStoriesCount,
        usedPartialAssessment = usedPartialAssessment
    )
}

/**
 * Common "submissions" collection document envelope shared by every test type in this cluster
 * (TAT/WAT/SRT/SDT/PPDT/GTO all write this same outer shape, only `data`'s inner type differs).
 * Generic so each `GitLive*SubmissionRepository` can plug in its own typed `data` payload instead
 * of duplicating this envelope per test type.
 */
@Serializable
internal data class SubmissionDocDto<T>(
    val id: String = "",
    val userId: String = "",
    val testId: String = "",
    val testType: String = "",
    val status: String = "",
    val submittedAt: Long = 0L,
    val gradedByInstructorId: String? = null,
    val gradingTimestamp: Long? = null,
    val batchId: String? = null,
    val data: T
)

/**
 * OLQ regression protection, ported from the Android OLQRegressionFilter (SubmissionMappers.kt).
 * Prevents a stale Firestore cache snapshot (delivered after reconnect) from overwriting a
 * submission's completed OLQ-analysis state on the observed [Flow] with older PENDING data.
 */
internal class OLQRegressionFilter {
    private var hasSeenCompleteAnalysis = false

    fun shouldFilterSnapshot(
        analysisStatus: String?,
        hasOlqResult: Boolean,
        metadata: SnapshotMetadata
    ): Boolean {
        val isComplete = analysisStatus == SubmissionConstants.ANALYSIS_STATUS_COMPLETED && hasOlqResult
        if (isComplete) {
            hasSeenCompleteAnalysis = true
        }
        val isFromCacheOnly = metadata.isFromCache && !metadata.hasPendingWrites
        return hasSeenCompleteAnalysis && !isComplete && isFromCacheOnly
    }
}
