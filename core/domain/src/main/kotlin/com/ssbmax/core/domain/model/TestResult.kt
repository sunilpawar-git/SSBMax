package com.ssbmax.core.domain.model

/**
 * Test submission and result model
 */
data class TestSubmission(
    val id: String,
    val testId: String,
    val userId: String,
    val testType: TestType,
    val phase: TestPhase,
    val submittedAt: Long,
    val responses: List<TestResponse>,
    val aiPreliminaryScore: Float? = null,
    val instructorScore: Float? = null,
    val finalScore: Float? = null,
    val gradingStatus: GradingStatus = GradingStatus.PENDING,
    val instructorId: String? = null,
    val instructorFeedback: String? = null,
    val gradedAt: Long? = null,
    val timeSpent: Long = 0, // milliseconds
    val batchId: String? = null
)

/**
 * Individual question/task response
 */
sealed class TestResponse {
    abstract val questionId: String
    abstract val timestamp: Long
    
    data class MultipleChoice(
        override val questionId: String,
        override val timestamp: Long,
        val selectedOption: Int,
        val isCorrect: Boolean? = null
    ) : TestResponse()
    
    data class TextResponse(
        override val questionId: String,
        override val timestamp: Long,
        val answer: String,
        val wordCount: Int = answer.split("\\s+".toRegex()).size
    ) : TestResponse()
    
    data class ImageBasedResponse(
        override val questionId: String,
        override val timestamp: Long,
        val imageUrl: String,
        val description: String
    ) : TestResponse()
    
    data class RatingResponse(
        override val questionId: String,
        override val timestamp: Long,
        val rating: Int,
        val comment: String? = null
    ) : TestResponse()
}

/**
 * Grading status for instructor workflow
 */
enum class GradingStatus {
    PENDING,            // Awaiting instructor review
    IN_REVIEW,          // Instructor is currently grading
    GRADED,             // Graded by instructor
    AUTO_GRADED,        // Automatically graded (OIR)
    NEEDS_REVISION;     // Instructor requires student to revise
    
    val displayName: String
        get() = when (this) {
            PENDING -> "Pending Review"
            IN_REVIEW -> "Under Review"
            GRADED -> "Graded"
            AUTO_GRADED -> "Completed"
            NEEDS_REVISION -> "Needs Revision"
        }
}

/**
 * AI grading suggestions for instructors
 */
data class AIGradingSuggestion(
    val submissionId: String,
    val suggestedScore: Float,
    val confidence: Float, // 0.0 to 1.0
    val strengths: List<String>,
    val weaknesses: List<String>,
    val keywordAnalysis: Map<String, Int>,
    val sentimentScore: Float? = null,
    val structureScore: Float? = null,
    val contentRelevanceScore: Float? = null
)

/**
 * Detailed test result with analytics
 */
data class DetailedTestResult(
    val submissionId: String,
    val testType: TestType,
    val finalScore: Float,
    val maxScore: Float = 100f,
    val percentile: Float? = null,
    val timeTaken: Long,
    val correctAnswers: Int = 0,
    val totalQuestions: Int,
    val strengths: List<String> = emptyList(),
    val areasForImprovement: List<String> = emptyList(),
    val instructorComments: String? = null,
    val detailedBreakdown: Map<String, Float> = emptyMap() // e.g., "Reasoning": 85.0
) {
    val percentage: Float
        get() = (finalScore / maxScore) * 100
    
    val grade: String
        get() = when {
            percentage >= 90 -> "Excellent"
            percentage >= 75 -> "Very Good"
            percentage >= 60 -> "Good"
            percentage >= 50 -> "Average"
            else -> "Needs Improvement"
        }
}

