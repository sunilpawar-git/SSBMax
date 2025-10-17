package com.ssbmax.core.domain.model

/**
 * Represents a grading queue item visible to instructors
 */
data class GradingQueueItem(
    val submissionId: String,
    val studentId: String,
    val studentName: String,
    val testType: TestType,
    val testName: String,
    val submittedAt: Long,
    val status: SubmissionStatus,
    val priority: GradingPriority,
    val batchName: String? = null,
    val aiScore: Float? = null,
    val hasAISuggestions: Boolean = false
) {
    val timeAgo: String
        get() {
            val diff = System.currentTimeMillis() - submittedAt
            val hours = diff / (1000 * 60 * 60)
            val minutes = (diff / (1000 * 60)) % 60
            
            return when {
                hours > 24 -> "${hours / 24}d ago"
                hours > 0 -> "${hours}h ago"
                else -> "${minutes}m ago"
            }
        }
}

/**
 * Priority levels for grading queue
 */
enum class GradingPriority {
    URGENT,     // Submitted >24h ago
    HIGH,       // Submitted >12h ago
    NORMAL,     // Submitted >6h ago
    LOW;        // Recently submitted
    
    val displayName: String
        get() = when (this) {
            URGENT -> "Urgent"
            HIGH -> "High"
            NORMAL -> "Normal"
            LOW -> "Low"
        }
}

/**
 * Submission status for grading workflow
 */
enum class SubmissionStatus {
    DRAFT,                      // Student hasn't submitted yet
    SUBMITTED_PENDING_REVIEW,   // Submitted, waiting for instructor
    UNDER_REVIEW,               // Instructor is currently grading
    GRADED,                     // Graded and feedback provided
    RETURNED_FOR_REVISION;      // Sent back to student for improvement
    
    val displayName: String
        get() = when (this) {
            DRAFT -> "Draft"
            SUBMITTED_PENDING_REVIEW -> "Pending Review"
            UNDER_REVIEW -> "Under Review"
            GRADED -> "Graded"
            RETURNED_FOR_REVISION -> "Needs Revision"
        }
}

/**
 * Statistics for instructor grading dashboard
 */
data class InstructorGradingStats(
    val totalPending: Int,
    val totalGraded: Int,
    val averageGradingTimeMinutes: Int,
    val todayGraded: Int,
    val weekGraded: Int,
    val pendingByTestType: Map<TestType, Int>,
    val averageScoreGiven: Float
)

/**
 * Detailed PPDT submission with additional context for grading
 */
data class PPDTSubmissionWithDetails(
    val submissionId: String,
    val userId: String,
    val userName: String,
    val testId: String,
    val story: String,
    val charactersCount: Int,
    val submittedAt: Long,
    val status: SubmissionStatus,
    val aiPreliminaryScore: PPDTAIScore? = null,
    val instructorScore: PPDTInstructorScore? = null,
    val batchName: String? = null
)

/**
 * AI-generated PPDT score breakdown
 */
data class PPDTAIScore(
    val perceptionScore: Float,      // Out of 20
    val imaginationScore: Float,     // Out of 20
    val narrationScore: Float,       // Out of 20
    val characterDepictionScore: Float, // Out of 20
    val positivityScore: Float,      // Out of 20
    val overallScore: Float,         // Total out of 100
    val feedback: String? = null,
    val strengths: List<String> = emptyList(),
    val areasForImprovement: List<String> = emptyList()
)

/**
 * Instructor-provided PPDT score
 */
data class PPDTInstructorScore(
    val perceptionScore: Float,
    val imaginationScore: Float,
    val narrationScore: Float,
    val characterDepictionScore: Float,
    val positivityScore: Float,
    val overallScore: Float,
    val feedback: String,
    val gradedByInstructorId: String,
    val gradedByInstructorName: String,
    val gradedAt: Long
)

/**
 * Grading action result
 */
sealed class GradingResult {
    data class Success(val submissionId: String) : GradingResult()
    data class Error(val message: String) : GradingResult()
}

