package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.CategoryScore
import com.ssbmax.shared.domain.model.DifficultyScore
import com.ssbmax.shared.domain.model.OIRQuestionType
import com.ssbmax.shared.domain.model.OIRSubmission
import com.ssbmax.shared.domain.model.OIRTestResult
import com.ssbmax.shared.domain.model.QuestionDifficulty
import com.ssbmax.shared.domain.model.SubmissionStatus
import kotlinx.serialization.Serializable

/**
 * Wire format for [OIRSubmission], split out of `GitLivePersonalTestSubmissionRepository.kt` to
 * keep both files under the 300-line limit. Mirrors the Android `OIRPersonalSubmissionDataSource`
 * DTO shape field-for-field.
 */
@Serializable
internal data class OIRDataDto(
    val id: String = "",
    val userId: String = "",
    val testId: String = "",
    val testResult: OIRSubmissionTestResultDto = OIRSubmissionTestResultDto(),
    val submittedAt: Long = 0L,
    val status: String = "",
    val gradedByInstructorId: String? = null,
    val gradingTimestamp: Long? = null
)

@Serializable
internal data class OIRSubmissionTestResultDto(
    val testId: String = "",
    val sessionId: String = "",
    val userId: String = "",
    val totalQuestions: Int = 0,
    val correctAnswers: Int = 0,
    val incorrectAnswers: Int = 0,
    val skippedQuestions: Int = 0,
    val totalTimeSeconds: Int = 0,
    val timeTakenSeconds: Int = 0,
    val rawScore: Int = 0,
    val percentageScore: Float = 0f,
    val categoryScores: Map<String, CategoryScoreDto> = emptyMap(),
    val difficultyBreakdown: Map<String, DifficultyScoreDto> = emptyMap(),
    val completedAt: Long = 0L,
    val passed: Boolean = false,
    val grade: String = "C"
)

@Serializable
internal data class CategoryScoreDto(
    val totalQuestions: Int = 0,
    val correctAnswers: Int = 0,
    val percentage: Float = 0f,
    val averageTimeSeconds: Int = 0
)

@Serializable
internal data class DifficultyScoreDto(
    val totalQuestions: Int = 0,
    val correctAnswers: Int = 0,
    val percentage: Float = 0f
)

/**
 * Same real limitation as the Android original's `OIRPersonalSubmissionDataSource.parseOIRSubmission`:
 * per-question `answeredQuestions` are never round-tripped through this write path (the Android
 * `toFirestoreMap()` writes them, but `parseOIRSubmission` never reads them back — always
 * `emptyList()`). Ported faithfully, not fixed, since fixing it is a real behavior change out of
 * this slice's scope.
 */
internal fun OIRSubmission.toDataDto() = OIRDataDto(
    id = id,
    userId = userId,
    testId = testId,
    testResult = OIRSubmissionTestResultDto(
        testId = testResult.testId,
        sessionId = testResult.sessionId,
        userId = testResult.userId,
        totalQuestions = testResult.totalQuestions,
        correctAnswers = testResult.correctAnswers,
        incorrectAnswers = testResult.incorrectAnswers,
        skippedQuestions = testResult.skippedQuestions,
        totalTimeSeconds = testResult.totalTimeSeconds,
        timeTakenSeconds = testResult.timeTakenSeconds,
        rawScore = testResult.rawScore,
        percentageScore = testResult.percentageScore,
        categoryScores = testResult.categoryScores.entries.associate { (category, score) ->
            category.name to CategoryScoreDto(score.totalQuestions, score.correctAnswers, score.percentage, score.averageTimeSeconds)
        },
        difficultyBreakdown = testResult.difficultyBreakdown.entries.associate { (difficulty, score) ->
            difficulty.name to DifficultyScoreDto(score.totalQuestions, score.correctAnswers, score.percentage)
        },
        completedAt = testResult.completedAt,
        passed = testResult.passed,
        grade = testResult.grade.name
    ),
    submittedAt = submittedAt,
    status = status.name,
    gradedByInstructorId = gradedByInstructorId,
    gradingTimestamp = gradingTimestamp
)

internal fun OIRDataDto.toDomain(): OIRSubmission = OIRSubmission(
    id = id,
    userId = userId,
    testId = testId,
    testResult = OIRTestResult(
        testId = testResult.testId,
        sessionId = testResult.sessionId,
        userId = testResult.userId,
        totalQuestions = testResult.totalQuestions,
        correctAnswers = testResult.correctAnswers,
        incorrectAnswers = testResult.incorrectAnswers,
        skippedQuestions = testResult.skippedQuestions,
        totalTimeSeconds = testResult.totalTimeSeconds,
        timeTakenSeconds = testResult.timeTakenSeconds,
        rawScore = testResult.rawScore,
        percentageScore = testResult.percentageScore,
        categoryScores = testResult.categoryScores.mapNotNull { (key, score) ->
            runCatching { OIRQuestionType.valueOf(key) }.getOrNull()?.let {
                it to CategoryScore(it, score.totalQuestions, score.correctAnswers, score.percentage, score.averageTimeSeconds)
            }
        }.toMap(),
        difficultyBreakdown = testResult.difficultyBreakdown.mapNotNull { (key, score) ->
            runCatching { QuestionDifficulty.valueOf(key) }.getOrNull()?.let {
                it to DifficultyScore(it, score.totalQuestions, score.correctAnswers, score.percentage)
            }
        }.toMap(),
        answeredQuestions = emptyList(),
        completedAt = testResult.completedAt
    ),
    submittedAt = submittedAt,
    status = runCatching { SubmissionStatus.valueOf(status) }.getOrDefault(SubmissionStatus.SUBMITTED_PENDING_REVIEW),
    gradedByInstructorId = gradedByInstructorId,
    gradingTimestamp = gradingTimestamp
)
