package com.ssbmax.core.domain.model

/**
 * PPDT (Picture Perception & Description Test) Models
 */

/**
 * Gender classification for PPDT image routing.
 * SSOT: defined here (Phase 3); used by repository filter (Phase 6 activates it).
 */
enum class GenderTag { MALE, FEMALE, MIXED }

/** Controls how strictly story content is checked against the picture scene. */
enum class DeviationTolerance { LOW, MEDIUM, HIGH }

/**
 * Structured context for a PPDT image, produced by the offline enrichment pipeline (Phase 5).
 * Used by Phase 8 multimodal prompt builder to inject per-picture rubric into Gemini.
 * All fields default to empty so pre-Phase-6 cached images degrade gracefully.
 */
data class PPDTImageContext(
    val sceneDescription: String = "",
    val coreElements: List<String> = emptyList(),
    val ambiguousElements: List<String> = emptyList(),
    val expectedThemes: List<String> = emptyList(),
    val penalizedThemes: List<String> = emptyList(),
    val primaryOLQs: List<String> = emptyList(),
    val deviationTolerance: DeviationTolerance = DeviationTolerance.MEDIUM,
    val exemplarGoodHints: List<String> = emptyList(),
    val exemplarBadHints: List<String> = emptyList()
)

/**
 * PPDT Test Question - Contains image and prompts
 */
data class PPDTQuestion(
    val id: String,
    val imageUrl: String,
    val imageDescription: String, // Alt text for accessibility
    val imageContext: PPDTImageContext = PPDTImageContext(), // Structured context for AI analysis (Phase 6+)
    val viewingTimeSeconds: Int = 30,
    val writingTimeMinutes: Int = 4,
    val guidelines: List<String> = listOf(
        "Observe the picture carefully for 30 seconds",
        "Identify the characters (age, gender, mood)",
        "What led to this situation?",
        "What is happening now?",
        "What will be the outcome?",
        "Write a clear, positive story"
    ),
    val minCharacters: Int = 200,  // Matches UI enforcement in PPDTWritingPhase
    val maxCharacters: Int = 1500
)

/**
 * PPDT Submission - Student's story submission
 */
data class PPDTSubmission(
    val submissionId: String,
    val questionId: String,
    val userId: String,
    val userName: String,
    val userEmail: String,
    val batchId: String?,
    val story: String,
    val charactersCount: Int,
    val viewingTimeTakenSeconds: Int,
    val writingTimeTakenMinutes: Int,
    val submittedAt: Long,
    val status: SubmissionStatus,
    val instructorReview: PPDTInstructorReview?,
    // OLQ-based analysis fields (unified system)
    val analysisStatus: com.ssbmax.core.domain.model.scoring.AnalysisStatus = com.ssbmax.core.domain.model.scoring.AnalysisStatus.PENDING_ANALYSIS,
    val olqResult: com.ssbmax.core.domain.model.scoring.OLQAnalysisResult? = null
) {
    val finalScore: Float?
        get() = olqResult?.overallScore ?: instructorReview?.finalScore
    
    val isPending: Boolean
        get() = status == SubmissionStatus.SUBMITTED_PENDING_REVIEW
    
    val isGraded: Boolean
        get() = status == SubmissionStatus.GRADED
}

// Note: SubmissionStatus and PPDTAIScore are now defined in GradingModels.kt

/**
 * Instructor's review and final grading
 */
data class PPDTInstructorReview(
    val reviewId: String,
    val instructorId: String,
    val instructorName: String,
    val finalScore: Float, // 0-100
    val feedback: String,
    val detailedScores: PPDTDetailedScores,
    val agreedWithAI: Boolean, // Did instructor agree with AI suggestions?
    val reviewedAt: Long,
    val timeSpentMinutes: Int
)

/**
 * Detailed scoring criteria for PPDT
 */
data class PPDTDetailedScores(
    val perception: Float, // 0-20 - How well did they perceive the scene?
    val imagination: Float, // 0-20 - Creativity in story
    val narration: Float, // 0-20 - Story structure and flow
    val characterDepiction: Float, // 0-20 - Character development
    val positivity: Float, // 0-20 - Positive outlook
    val notes: Map<String, String> = emptyMap() // Additional notes per criterion
) {
    val total: Float
        get() = perception + imagination + narration + characterDepiction + positivity
}

/**
 * PPDT Test Session - Tracks active test
 */
data class PPDTTestSession(
    val sessionId: String,
    val userId: String,
    val questionId: String,
    val question: PPDTQuestion,
    val startTime: Long,
    val imageViewingStartTime: Long?,
    val writingStartTime: Long?,
    val currentPhase: PPDTPhase,
    val story: String = "",
    val isCompleted: Boolean = false,
    val isPaused: Boolean = false
)

/**
 * PPDT test phases
 */
enum class PPDTPhase {
    INSTRUCTIONS,
    IMAGE_VIEWING,
    WRITING,
    REVIEW,
    SUBMITTED;
    
    val displayName: String
        get() = when (this) {
            INSTRUCTIONS -> "Instructions"
            IMAGE_VIEWING -> "Image Viewing"
            WRITING -> "Writing Story"
            REVIEW -> "Review"
            SUBMITTED -> "Submitted"
        }
}

/**
 * PPDT Test Configuration
 */
data class PPDTTestConfig(
    val testId: String = "ppdt_standard",
    val title: String = "PPDT",
    val description: String = "Picture Perception & Description Test",
    val viewingTimeSeconds: Int = 30,
    val writingTimeMinutes: Int = 4,
    val minCharacters: Int = 50,
    val maxCharacters: Int = 1500,
    val showAIScore: Boolean = true,
    val requiresInstructorReview: Boolean = true
)

// Note: GradingQueueItem, GradingPriority, and InstructorGradingStats are now defined in GradingModels.kt

