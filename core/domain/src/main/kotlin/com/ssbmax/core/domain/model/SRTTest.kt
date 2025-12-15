package com.ssbmax.core.domain.model

import com.ssbmax.core.domain.model.scoring.AnalysisStatus
import com.ssbmax.core.domain.model.scoring.OLQAnalysisResult
import java.util.UUID

/**
 * SRT (Situation Reaction Test) - Phase 2 Psychology Test
 * 
 * Student is presented with 60 practical situations and must write
 * how they would react in each situation.
 * Time: 30 minutes (60 situations × 30 seconds avg)
 */

/**
 * Represents a single situation in SRT
 */
data class SRTSituation(
    val id: String = UUID.randomUUID().toString(),
    val situation: String,
    val sequenceNumber: Int, // 1-60
    val category: SRTCategory,
    val timeAllowedSeconds: Int = 30
)

/**
 * Categories of situations in SRT
 */
enum class SRTCategory {
    LEADERSHIP,
    DECISION_MAKING,
    CRISIS_MANAGEMENT,
    INTERPERSONAL,
    ETHICAL_DILEMMA,
    RESPONSIBILITY,
    TEAMWORK,
    COURAGE,
    ADVERSITY,
    CONFLICT_RESOLUTION,
    PERSONAL_SACRIFICE,
    ALERTNESS,
    GENERAL;
    
    val displayName: String
        get() = when (this) {
            LEADERSHIP -> "Leadership"
            DECISION_MAKING -> "Decision Making"
            CRISIS_MANAGEMENT -> "Crisis Management"
            INTERPERSONAL -> "Interpersonal"
            ETHICAL_DILEMMA -> "Ethical Dilemma"
            RESPONSIBILITY -> "Responsibility"
            TEAMWORK -> "Teamwork"
            COURAGE -> "Courage"
            ADVERSITY -> "Adversity"
            CONFLICT_RESOLUTION -> "Conflict Resolution"
            PERSONAL_SACRIFICE -> "Personal Sacrifice"
            ALERTNESS -> "Alertness"
            GENERAL -> "General"
        }
}

/**
 * Student's response to a single situation
 */
data class SRTSituationResponse(
    val situationId: String,
    val situation: String,
    val response: String,
    val charactersCount: Int,
    val timeTakenSeconds: Int,
    val submittedAt: Long,
    val isSkipped: Boolean = false
) {
    val isValidResponse: Boolean get() = response.isNotBlank() && !isSkipped
}

/**
 * Complete SRT test submission (all 60 responses)
 */
data class SRTSubmission(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val testId: String,
    val responses: List<SRTSituationResponse>,
    val totalTimeTakenMinutes: Int,
    val submittedAt: Long,
    val status: SubmissionStatus = SubmissionStatus.SUBMITTED_PENDING_REVIEW,
    val aiPreliminaryScore: SRTAIScore? = null,
    val instructorScore: SRTInstructorScore? = null,
    val gradedByInstructorId: String? = null,
    val gradingTimestamp: Long? = null,
    // OLQ-based analysis fields (Phase 1)
    val analysisStatus: AnalysisStatus = AnalysisStatus.PENDING_ANALYSIS,
    val olqResult: OLQAnalysisResult? = null
) {
    val totalResponses: Int get() = responses.size
    val validResponses: Int get() = responses.count { it.isValidResponse }
    val skippedResponses: Int get() = responses.count { it.isSkipped }
    val averageResponseLength: Int get() = responses.filter { it.isValidResponse }
        .map { it.charactersCount }.average().toInt()
    val isComplete: Boolean get() = responses.size == 60
}

/**
 * AI-generated preliminary score for SRT
 */
data class SRTAIScore(
    val overallScore: Float, // 0-100
    val leadershipScore: Float, // 0-20
    val decisionMakingScore: Float, // 0-20
    val practicalityScore: Float, // 0-20
    val initiativeScore: Float, // 0-20
    val socialResponsibilityScore: Float, // 0-20
    val feedback: String? = null,
    val categoryWiseScores: Map<SRTCategory, Float> = emptyMap(),
    val positiveTraits: List<String> = emptyList(), // Identified positive qualities
    val concerningPatterns: List<String> = emptyList(), // Red flags
    val responseQuality: ResponseQuality,
    val strengths: List<String> = emptyList(),
    val areasForImprovement: List<String> = emptyList()
)

/**
 * Quality assessment of responses
 */
enum class ResponseQuality {
    EXCELLENT,      // Thoughtful, practical, shows leadership
    GOOD,           // Reasonable, appropriate
    AVERAGE,        // Acceptable but unremarkable
    BELOW_AVERAGE,  // Passive, impractical
    CONCERNING;     // Inappropriate, violent, unethical
    
    val displayName: String
        get() = when (this) {
            EXCELLENT -> "Excellent"
            GOOD -> "Good"
            AVERAGE -> "Average"
            BELOW_AVERAGE -> "Below Average"
            CONCERNING -> "Needs Attention"
        }
}

/**
 * Instructor's final grading for SRT
 */
data class SRTInstructorScore(
    val overallScore: Float, // 0-100
    val leadershipScore: Float,
    val decisionMakingScore: Float,
    val practicalityScore: Float,
    val initiativeScore: Float,
    val socialResponsibilityScore: Float,
    val feedback: String,
    val categoryWiseComments: Map<SRTCategory, String> = emptyMap(),
    val flaggedResponses: List<String> = emptyList(), // Situation IDs of concerning responses
    val exemplaryResponses: List<String> = emptyList(), // Situation IDs of excellent responses
    val gradedByInstructorId: String,
    val gradedByInstructorName: String,
    val gradedAt: Long,
    val agreedWithAI: Boolean = false
)

/**
 * SRT test session tracking
 */
data class SRTTestSession(
    val sessionId: String = UUID.randomUUID().toString(),
    val userId: String,
    val testId: String,
    val situations: List<SRTSituation>,
    val currentSituationIndex: Int = 0,
    val responses: List<SRTSituationResponse> = emptyList(),
    val currentResponse: String = "",
    val phase: SRTPhase = SRTPhase.INSTRUCTIONS,
    val startTime: Long,
    val situationStartTime: Long? = null,
    val isPaused: Boolean = false,
    val isCompleted: Boolean = false
) {
    val currentSituation: SRTSituation?
        get() = situations.getOrNull(currentSituationIndex)
    
    val progress: Float
        get() = if (situations.isEmpty()) 0f else (currentSituationIndex.toFloat() / situations.size)
    
    val completedSituations: Int
        get() = responses.size
    
    val remainingSituations: Int
        get() = situations.size - responses.size
    
    val timeElapsedForCurrentSituation: Long
        get() = if (situationStartTime != null) {
            (System.currentTimeMillis() - situationStartTime) / 1000
        } else 0L
}

/**
 * SRT test phases
 */
enum class SRTPhase {
    INSTRUCTIONS,
    IN_PROGRESS,
    REVIEW,
    COMPLETED,
    SUBMITTED;
    
    val displayName: String
        get() = when (this) {
            INSTRUCTIONS -> "Instructions"
            IN_PROGRESS -> "Test in Progress"
            REVIEW -> "Review Responses"
            COMPLETED -> "Test Completed"
            SUBMITTED -> "Submitted"
        }
}

/**
 * SRT test configuration
 */
data class SRTTestConfig(
    val testId: String = "srt_standard",
    val title: String = "SRT - Situation Reaction Test",
    val description: String = "Describe how you would react in each situation",
    val totalSituations: Int = 60,
    val totalTimeMinutes: Int = 30,
    val minResponseLength: Int = 20,
    val maxResponseLength: Int = 200,
    val showAIScore: Boolean = true,
    val requiresInstructorReview: Boolean = true,
    val allowSkip: Boolean = true,
    val allowReviewBeforeSubmit: Boolean = true,
    val showCategoryLabels: Boolean = false // Don't show category to avoid bias
)

/**
 * SRT result for display
 */
data class SRTResult(
    val sessionId: String,
    val userId: String,
    val testId: String,
    val submission: SRTSubmission,
    val overallScore: Float?,
    val status: SubmissionStatus,
    val attemptedAt: Long
)

