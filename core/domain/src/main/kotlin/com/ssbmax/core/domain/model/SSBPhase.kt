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
 * 
 * 🔒 SECURITY TODO: When implementing GTO and IO (Interview) ViewModels:
 * 
 * CRITICAL - All new test ViewModels MUST implement these security measures:
 * 
 * 1. AUTHENTICATION GUARD:
 *    ```kotlin
 *    val user = observeCurrentUser().first()
 *    val userId = user?.id ?: run {
 *        securityLogger.logUnauthenticatedAccess(testType = TestType.GTO, context = "GTOViewModel.loadTest")
 *        _uiState.update { it.copy(error = "Authentication required. Please login to continue.") }
 *        return@launch
 *    }
 *    ```
 * 
 * 2. SUBSCRIPTION LIMIT CHECK:
 *    ```kotlin
 *    private suspend fun checkTestEligibility(userId: String): TestEligibility {
 *        return subscriptionManager.canTakeTest(userId, TestType.GTO)
 *    }
 *    ```
 * 
 * 3. USAGE RECORDING AFTER SUBMISSION:
 *    ```kotlin
 *    subscriptionManager.recordTestUsage(userId, TestType.GTO, submissionId)
 *    ```
 * 
 * 4. INJECT THESE DEPENDENCIES:
 *    - ObserveCurrentUserUseCase
 *    - DifficultyProgressionManager
 *    - SubscriptionManager
 *    - SecurityEventLogger
 * 
 * 5. REFERENCE IMPLEMENTATIONS:
 *    - See: app/src/main/kotlin/com/ssbmax/ui/tests/tat/TATTestViewModel.kt
 *    - See: app/src/main/kotlin/com/ssbmax/ui/tests/ppdt/PPDTTestViewModel.kt
 * 
 * ⚠️ WARNING: Skipping these steps will create a security vulnerability allowing
 *    unlimited test attempts and bypassing subscription limits!
 * 
 * ✅ VERIFIED SECURE: OIR, WAT, SRT, TAT, PPDT
 * ❌ PENDING: GTO, IO (SD not implemented yet)
 */
enum class TestType {
    // Phase 1
    OIR,        // Officer Intelligence Rating
    PPDT,       // Picture Perception & Description Test
    
    // Phase 2
    PIQ,        // Personal Information Questionnaire
    TAT,        // Thematic Apperception Test
    WAT,        // Word Association Test
    SRT,        // Situation Reaction Test
    SD,         // Self Description
    GTO,        // Group Testing Officer - TODO: Implement ViewModel with security measures above
    IO;         // Interview Officer - TODO: Implement ViewModel with security measures above
    
    val phase: TestPhase
        get() = when (this) {
            OIR, PPDT -> TestPhase.PHASE_1
            PIQ, TAT, WAT, SRT, SD, GTO, IO -> TestPhase.PHASE_2
        }
    
    val displayName: String
        get() = when (this) {
            OIR -> "OIR Test"
            PPDT -> "PPDT"
            PIQ -> "PIQ"
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
            PIQ -> "Personal Information Questionnaire"
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
            PIQ -> "Complete your personal information form"
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
        get() = this in listOf(PPDT, TAT, WAT, SRT, SD, GTO, IO) // PIQ is for reference, not graded
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

