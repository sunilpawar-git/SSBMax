package com.ssbmax.core.domain.model

/**
 * GPE (Group Planning Exercise) Models
 * Image-based tactical scenario planning test
 *
 * Test Format:
 * - 60 seconds: View tactical scenario image
 * - 29 minutes: Write planning response
 */

/**
 * GPE Test Question - Contains tactical scenario image and context
 */
data class GPEQuestion(
    val id: String,
    val imageUrl: String,
    val scenario: String, // Tactical scenario description
    val solution: String? = null,
    val imageDescription: String, // Alt text for accessibility
    val resources: List<String> = emptyList(), // Available resources (rope, planks, etc.)
    val viewingTimeSeconds: Int = 60,
    val planningTimeSeconds: Int = 1740, // 29 minutes
    val guidelines: List<String> = listOf(
        "Observe the scenario image carefully for 60 seconds",
        "Identify the tactical challenge and constraints",
        "Consider available resources and team capabilities",
        "Develop a clear, actionable plan",
        "Assign roles and responsibilities",
        "Consider time management and contingencies",
        "Write a structured planning response"
    ),
    val minCharacters: Int = 500,
    val maxCharacters: Int = 2000,
    val category: String? = null, // river crossing, wall climbing, ditch crossing, etc.
    val difficulty: String? = null // easy, medium, hard
)

/**
 * GPE Submission - Student's planning response
 */
data class GPESubmission(
    val submissionId: String,
    val questionId: String,
    val userId: String,
    val userName: String,
    val userEmail: String,
    val batchId: String?,
    val planningResponse: String,
    val charactersCount: Int,
    val viewingTimeTakenSeconds: Int,
    val planningTimeTakenMinutes: Int,
    val submittedAt: Long,
    val status: SubmissionStatus,
    val aiPreliminaryScore: GPEAIScore?,
    val instructorReview: GPEInstructorReview?
) {
    val finalScore: Float?
        get() = instructorReview?.finalScore ?: aiPreliminaryScore?.overallScore

    val isPending: Boolean
        get() = status == SubmissionStatus.SUBMITTED_PENDING_REVIEW

    val isGraded: Boolean
        get() = status == SubmissionStatus.GRADED
}

/**
 * AI-generated preliminary score for GPE
 */
data class GPEAIScore(
    val situationAnalysisScore: Float, // 0-20 - How well did they analyze the scenario?
    val planningQualityScore: Float, // 0-20 - Quality of tactical plan
    val leadershipScore: Float, // 0-20 - Leadership and decision-making
    val resourceUtilizationScore: Float, // 0-20 - Effective use of resources
    val practicalityScore: Float, // 0-20 - Practicality and feasibility of plan
    val overallScore: Float, // 0-100
    val feedback: String,
    val strengths: List<String> = emptyList(),
    val areasForImprovement: List<String> = emptyList()
) {
    val isPass: Boolean
        get() = overallScore >= 60f
}

/**
 * Instructor's review and final grading
 */
data class GPEInstructorReview(
    val reviewId: String,
    val instructorId: String,
    val instructorName: String,
    val finalScore: Float, // 0-100
    val feedback: String,
    val detailedScores: GPEDetailedScores,
    val agreedWithAI: Boolean, // Did instructor agree with AI suggestions?
    val reviewedAt: Long,
    val timeSpentMinutes: Int
)

/**
 * Detailed scoring criteria for GPE
 */
data class GPEDetailedScores(
    val situationAnalysis: Float, // 0-20 - Scenario understanding
    val planningQuality: Float, // 0-20 - Plan structure and clarity
    val leadership: Float, // 0-20 - Leadership qualities demonstrated
    val resourceUtilization: Float, // 0-20 - Resource management
    val practicality: Float, // 0-20 - Feasibility and realism
    val notes: Map<String, String> = emptyMap() // Additional notes per criterion
) {
    val total: Float
        get() = situationAnalysis + planningQuality + leadership + resourceUtilization + practicality
}

/**
 * GPE Test Session - Tracks active test
 */
data class GPETestSession(
    val sessionId: String,
    val userId: String,
    val questionId: String,
    val question: GPEQuestion,
    val startTime: Long,
    val imageViewingStartTime: Long?,
    val planningStartTime: Long?,
    val currentPhase: GPEPhase,
    val planningResponse: String = "",
    val isCompleted: Boolean = false,
    val isPaused: Boolean = false
)

/**
 * GPE test phases
 */
enum class GPEPhase {
    INSTRUCTIONS,
    IMAGE_VIEWING,
    PLANNING,
    REVIEW,
    SUBMITTED;

    val displayName: String
        get() = when (this) {
            INSTRUCTIONS -> "Instructions"
            IMAGE_VIEWING -> "Image Viewing"
            PLANNING -> "Planning Response"
            REVIEW -> "Review"
            SUBMITTED -> "Submitted"
        }
}

/**
 * GPE Test Configuration
 */
data class GPETestConfig(
    val testId: String = "gpe_standard",
    val title: String = "GPE",
    val description: String = "Group Planning Exercise",
    val viewingTimeSeconds: Int = 60,
    val planningTimeSeconds: Int = 1740, // 29 minutes
    val minCharacters: Int = 500,
    val maxCharacters: Int = 2000,
    val showAIScore: Boolean = true,
    val requiresInstructorReview: Boolean = true
)
