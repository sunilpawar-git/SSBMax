package com.ssbmax.core.domain.model.gto

import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQScore

/**
 * GTO Submission Status
 * Tracks the lifecycle of a GTO test submission
 */
enum class GTOSubmissionStatus {
    PENDING_ANALYSIS,   // Submitted, waiting for AI analysis
    ANALYZING,          // Worker is processing
    COMPLETED,          // Analysis done, results available
    FAILED              // Analysis failed after retries
}

/**
 * Base sealed class for all GTO submissions
 * Each submission type corresponds to its test type
 */
sealed class GTOSubmission {
    abstract val id: String
    abstract val userId: String
    abstract val testId: String
    abstract val testType: GTOTestType
    abstract val submittedAt: Long
    abstract val timeSpent: Int // seconds
    abstract val status: GTOSubmissionStatus
    abstract val olqScores: Map<OLQ, OLQScore>
    
    /**
     * Group Discussion Submission
     */
    data class GDSubmission(
        override val id: String,
        override val userId: String,
        override val testId: String,
        val topic: String,
        val response: String,
        val wordCount: Int,
        override val submittedAt: Long = System.currentTimeMillis(),
        override val timeSpent: Int,
        override val status: GTOSubmissionStatus = GTOSubmissionStatus.PENDING_ANALYSIS,
        override val olqScores: Map<OLQ, OLQScore> = emptyMap(),
        override val testType: GTOTestType = GTOTestType.GROUP_DISCUSSION
    ) : GTOSubmission()
    
    /**
     * Group Planning Exercise Submission
     */
    data class GPESubmission(
        override val id: String,
        override val userId: String,
        override val testId: String,
        val imageUrl: String,
        val scenario: String,
        val solution: String? = null,
        val plan: String,
        val characterCount: Int,
        override val submittedAt: Long = System.currentTimeMillis(),
        override val timeSpent: Int,
        override val status: GTOSubmissionStatus = GTOSubmissionStatus.PENDING_ANALYSIS,
        override val olqScores: Map<OLQ, OLQScore> = emptyMap(),
        override val testType: GTOTestType = GTOTestType.GROUP_PLANNING_EXERCISE
    ) : GTOSubmission()
    
    /**
     * Lecturette Submission
     */
    data class LecturetteSubmission(
        override val id: String,
        override val userId: String,
        override val testId: String,
        val topicChoices: List<String>,
        val selectedTopic: String,
        val speechTranscript: String,
        val wordCount: Int,
        override val submittedAt: Long = System.currentTimeMillis(),
        override val timeSpent: Int,
        override val status: GTOSubmissionStatus = GTOSubmissionStatus.PENDING_ANALYSIS,
        override val olqScores: Map<OLQ, OLQScore> = emptyMap(),
        override val testType: GTOTestType = GTOTestType.LECTURETTE
    ) : GTOSubmission()
    
    /**
     * Progressive Group Task Submission
     */
    data class PGTSubmission(
        override val id: String,
        override val userId: String,
        override val testId: String,
        val obstacles: List<ObstacleConfig>,
        val solutions: List<ObstacleSolution>, // One solution per obstacle
        override val submittedAt: Long = System.currentTimeMillis(),
        override val timeSpent: Int,
        override val status: GTOSubmissionStatus = GTOSubmissionStatus.PENDING_ANALYSIS,
        override val olqScores: Map<OLQ, OLQScore> = emptyMap(),
        override val testType: GTOTestType = GTOTestType.PROGRESSIVE_GROUP_TASK
    ) : GTOSubmission()
    
    /**
     * Half Group Task Submission
     */
    data class HGTSubmission(
        override val id: String,
        override val userId: String,
        override val testId: String,
        val obstacle: ObstacleConfig,
        val solution: ObstacleSolution,
        val leadershipDecisions: String,
        override val submittedAt: Long = System.currentTimeMillis(),
        override val timeSpent: Int,
        override val status: GTOSubmissionStatus = GTOSubmissionStatus.PENDING_ANALYSIS,
        override val olqScores: Map<OLQ, OLQScore> = emptyMap(),
        override val testType: GTOTestType = GTOTestType.HALF_GROUP_TASK
    ) : GTOSubmission()
    
    /**
     * Group Obstacle Race Submission
     */
    data class GORSubmission(
        override val id: String,
        override val userId: String,
        override val testId: String,
        val obstacles: List<ObstacleConfig>,
        val coordinationStrategy: String,
        override val submittedAt: Long = System.currentTimeMillis(),
        override val timeSpent: Int,
        override val status: GTOSubmissionStatus = GTOSubmissionStatus.PENDING_ANALYSIS,
        override val olqScores: Map<OLQ, OLQScore> = emptyMap(),
        override val testType: GTOTestType = GTOTestType.GROUP_OBSTACLE_RACE
    ) : GTOSubmission()
    
    /**
     * Individual Obstacles Submission
     */
    data class IOSubmission(
        override val id: String,
        override val userId: String,
        override val testId: String,
        val obstacles: List<ObstacleConfig>,
        val approach: String, // Overall approach description
        override val submittedAt: Long = System.currentTimeMillis(),
        override val timeSpent: Int,
        override val status: GTOSubmissionStatus = GTOSubmissionStatus.PENDING_ANALYSIS,
        override val olqScores: Map<OLQ, OLQScore> = emptyMap(),
        override val testType: GTOTestType = GTOTestType.INDIVIDUAL_OBSTACLES
    ) : GTOSubmission()
    
    /**
     * Command Task Submission
     */
    data class CTSubmission(
        override val id: String,
        override val userId: String,
        override val testId: String,
        val scenario: String,
        val obstacle: ObstacleConfig,
        val commandDecisions: String,
        val resourceAllocation: String,
        override val submittedAt: Long = System.currentTimeMillis(),
        override val timeSpent: Int,
        override val status: GTOSubmissionStatus = GTOSubmissionStatus.PENDING_ANALYSIS,
        override val olqScores: Map<OLQ, OLQScore> = emptyMap(),
        override val testType: GTOTestType = GTOTestType.COMMAND_TASK
    ) : GTOSubmission()
}

/**
 * Solution for a single obstacle (used in PGT, HGT, etc.)
 */
data class ObstacleSolution(
    val obstacleId: String,
    val solutionText: String,
    val resourcesUsed: List<String> = emptyList(),
    val estimatedTime: Int? = null // seconds
)
