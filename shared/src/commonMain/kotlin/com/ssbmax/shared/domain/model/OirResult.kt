package com.ssbmax.shared.domain.model

/**
 * Ported (trimmed) from core/domain/model/OIRTest.kt as part of the Phase 0
 * KMP spike vertical slice ("login + one result screen"). Only the fields
 * needed to render a result summary are kept — question/answer review
 * (OIRAnsweredQuestion, OIROption, etc.) is out of scope for this spike and
 * belongs to Phase 1 (full domain port), not Phase 0.
 */
enum class OIRQuestionType {
    VERBAL_REASONING,
    NON_VERBAL_REASONING,
    NUMERICAL_ABILITY,
    SPATIAL_REASONING;

    val displayName: String
        get() = when (this) {
            VERBAL_REASONING -> "Verbal Reasoning"
            NON_VERBAL_REASONING -> "Non-Verbal Reasoning"
            NUMERICAL_ABILITY -> "Numerical Ability"
            SPATIAL_REASONING -> "Spatial Reasoning"
        }
}

data class CategoryScore(
    val category: OIRQuestionType,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val percentage: Float
)

enum class TestGrade {
    EXCELLENT,
    VERY_GOOD,
    GOOD,
    AVERAGE,
    NEEDS_IMPROVEMENT;

    val displayName: String
        get() = when (this) {
            EXCELLENT -> "Excellent"
            VERY_GOOD -> "Very Good"
            GOOD -> "Good"
            AVERAGE -> "Average"
            NEEDS_IMPROVEMENT -> "Needs Improvement"
        }
}

data class OIRTestResult(
    val testId: String,
    val sessionId: String,
    val userId: String,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val incorrectAnswers: Int,
    val skippedQuestions: Int,
    val timeTakenSeconds: Int,
    val rawScore: Int,
    val percentageScore: Float,
    val categoryScores: Map<OIRQuestionType, CategoryScore>,
    val completedAt: Long
) {
    val passed: Boolean
        get() = percentageScore >= 50f

    val grade: TestGrade
        get() = when {
            percentageScore >= 90 -> TestGrade.EXCELLENT
            percentageScore >= 75 -> TestGrade.VERY_GOOD
            percentageScore >= 60 -> TestGrade.GOOD
            percentageScore >= 50 -> TestGrade.AVERAGE
            else -> TestGrade.NEEDS_IMPROVEMENT
        }
}
