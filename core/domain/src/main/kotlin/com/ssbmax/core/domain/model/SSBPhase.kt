package com.ssbmax.core.domain.model

/**
 * SSB Test Phases
 */
enum class TestPhase {
    PHASE_1,
    PHASE_2;
    
    val displayName: String
        get() = when (this) {
            PHASE_1 -> "Phase 1 - Screening"
            PHASE_2 -> "Phase 2 - Main Assessment"
        }
}

/**
 * Test types within each phase
 */
enum class TestType {
    // Phase 1
    OIR,        // Officer Intelligence Rating
    PPDT,       // Picture Perception & Description Test
    
    // Phase 2
    TAT,        // Thematic Apperception Test
    WAT,        // Word Association Test
    SRT,        // Situation Reaction Test
    SD,         // Self Description
    GTO,        // Group Testing Officer
    IO;         // Interview Officer
    
    val phase: TestPhase
        get() = when (this) {
            OIR, PPDT -> TestPhase.PHASE_1
            TAT, WAT, SRT, SD, GTO, IO -> TestPhase.PHASE_2
        }
    
    val displayName: String
        get() = when (this) {
            OIR -> "OIR Test"
            PPDT -> "PPDT"
            TAT -> "TAT"
            WAT -> "WAT"
            SRT -> "SRT"
            SD -> "Self Description"
            GTO -> "GTO Tasks"
            IO -> "Interview"
        }
    
    val fullName: String
        get() = when (this) {
            OIR -> "Officer Intelligence Rating"
            PPDT -> "Picture Perception & Description Test"
            TAT -> "Thematic Apperception Test"
            WAT -> "Word Association Test"
            SRT -> "Situation Reaction Test"
            SD -> "Self Description"
            GTO -> "Group Testing Officer Tasks"
            IO -> "Interview Officer Assessment"
        }
    
    val description: String
        get() = when (this) {
            OIR -> "Test your reasoning, verbal, and non-verbal intelligence"
            PPDT -> "Observe a picture and describe what you see"
            TAT -> "Create stories based on ambiguous pictures"
            WAT -> "Write first response to 60 words in 15 minutes"
            SRT -> "React to 60 practical situations"
            SD -> "Describe yourself from different perspectives"
            GTO -> "Participate in group discussions and tasks"
            IO -> "Personal interview with the Interviewing Officer"
        }
    
    val hasAutoGrading: Boolean
        get() = this == OIR
    
    val requiresInstructorReview: Boolean
        get() = this in listOf(PPDT, TAT, WAT, SRT, SD, GTO, IO)
}

/**
 * Test status for a student
 */
enum class TestStatus {
    NOT_ATTEMPTED,
    IN_PROGRESS,
    SUBMITTED_PENDING_REVIEW,
    GRADED,
    COMPLETED;
    
    val displayName: String
        get() = when (this) {
            NOT_ATTEMPTED -> "Not Attempted"
            IN_PROGRESS -> "In Progress"
            SUBMITTED_PENDING_REVIEW -> "Under Review"
            GRADED -> "Graded"
            COMPLETED -> "Completed"
        }
}

/**
 * Phase progress tracking
 */
data class PhaseProgress(
    val phase: TestPhase,
    val totalTests: Int,
    val completedTests: Int,
    val testsInProgress: Int = 0,
    val testsPendingReview: Int = 0,
    val averageScore: Float = 0f,
    val subTests: List<SubTestProgress> = emptyList()
) {
    val completionPercentage: Float
        get() = if (totalTests > 0) (completedTests.toFloat() / totalTests) * 100 else 0f
}

/**
 * Individual sub-test progress
 */
data class SubTestProgress(
    val testType: TestType,
    val status: TestStatus,
    val latestScore: Float? = null,
    val aiPreliminaryScore: Float? = null,
    val instructorScore: Float? = null,
    val attemptsCount: Int = 0,
    val lastAttemptedAt: Long? = null,
    val bestScore: Float? = null
)

