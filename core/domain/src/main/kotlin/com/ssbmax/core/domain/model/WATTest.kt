package com.ssbmax.core.domain.model

import com.ssbmax.core.domain.model.scoring.AnalysisStatus
import com.ssbmax.core.domain.model.scoring.OLQAnalysisResult
import java.util.UUID

/**
 * WAT (Word Association Test) - Phase 2 Psychology Test
 * 
 * Student is shown 60 words one at a time (15 seconds each).
 * They must write the first word/phrase that comes to mind.
 * Total time: 15 minutes (60 words × 15 seconds)
 */

/**
 * Represents a single word prompt in WAT
 */
data class WATWord(
    val id: String = UUID.randomUUID().toString(),
    val word: String,
    val sequenceNumber: Int, // 1-60
    val timeAllowedSeconds: Int = 15
)

/**
 * Student's response to a single word
 */
data class WATWordResponse(
    val wordId: String,
    val word: String,
    val response: String,
    val timeTakenSeconds: Int,
    val submittedAt: Long,
    val isSkipped: Boolean = false
) {
    val responseLength: Int get() = response.length
    val isValidResponse: Boolean get() = response.isNotBlank() && !isSkipped
}

/**
 * Complete WAT test submission (all 60 responses)
 */
data class WATSubmission(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val testId: String,
    val responses: List<WATWordResponse>,
    val totalTimeTakenMinutes: Int,
    val submittedAt: Long,
    val status: SubmissionStatus = SubmissionStatus.SUBMITTED_PENDING_REVIEW,
    val instructorScore: WATInstructorScore? = null,
    val gradedByInstructorId: String? = null,
    val gradingTimestamp: Long? = null,
    // OLQ-based analysis fields (Phase 1)
    val analysisStatus: AnalysisStatus = AnalysisStatus.PENDING_ANALYSIS,
    val olqResult: OLQAnalysisResult? = null
) {
    val totalResponses: Int get() = responses.size
    val validResponses: Int get() = responses.count { it.isValidResponse }
    val skippedResponses: Int get() = responses.count { it.isSkipped }
    val averageResponseTime: Float get() = responses.map { it.timeTakenSeconds }.average().toFloat()
    val isComplete: Boolean get() = responses.size == 60
}



/**
 * Instructor's final grading for WAT
 */
data class WATInstructorScore(
    val overallScore: Float, // 0-100
    val positivityScore: Float,
    val creativityScore: Float,
    val speedScore: Float,
    val relevanceScore: Float,
    val emotionalMaturityScore: Float,
    val feedback: String,
    val flaggedResponses: List<String> = emptyList(), // Word IDs of concerning responses
    val notableResponses: List<String> = emptyList(), // Word IDs of impressive responses
    val gradedByInstructorId: String,
    val gradedByInstructorName: String,
    val gradedAt: Long,
    val agreedWithAI: Boolean = false
)

/**
 * WAT test session tracking
 */
data class WATTestSession(
    val sessionId: String = UUID.randomUUID().toString(),
    val userId: String,
    val testId: String,
    val words: List<WATWord>,
    val currentWordIndex: Int = 0,
    val responses: List<WATWordResponse> = emptyList(),
    val currentResponse: String = "",
    val phase: WATPhase = WATPhase.INSTRUCTIONS,
    val startTime: Long,
    val wordStartTime: Long? = null,
    val isPaused: Boolean = false,
    val isCompleted: Boolean = false
) {
    val currentWord: WATWord?
        get() = words.getOrNull(currentWordIndex)
    
    val progress: Float
        get() = if (words.isEmpty()) 0f else (currentWordIndex.toFloat() / words.size)
    
    val completedWords: Int
        get() = responses.size
    
    val remainingWords: Int
        get() = words.size - responses.size
    
    val timeElapsedForCurrentWord: Long
        get() = if (wordStartTime != null) {
            (System.currentTimeMillis() - wordStartTime) / 1000
        } else 0L
}

/**
 * WAT test phases
 */
enum class WATPhase {
    INSTRUCTIONS,
    IN_PROGRESS,
    COMPLETED,
    SUBMITTED;
    
    val displayName: String
        get() = when (this) {
            INSTRUCTIONS -> "Instructions"
            IN_PROGRESS -> "Test in Progress"
            COMPLETED -> "Test Completed"
            SUBMITTED -> "Submitted"
        }
}

/**
 * WAT test configuration
 */
data class WATTestConfig(
    val testId: String = "wat_standard",
    val title: String = "WAT - Word Association Test",
    val description: String = "Write the first word that comes to mind for each prompt",
    val totalWords: Int = 60,
    val timePerWordSeconds: Int = 15,
    val minResponseLength: Int = 0,
    val maxResponseLength: Int = 150,
    val showAIScore: Boolean = true,
    val requiresInstructorReview: Boolean = true,
    val allowSkip: Boolean = true, // Allow skipping if no response comes to mind
    val autoSubmitOnTimeout: Boolean = true // Auto-move to next word after timeout
)

/**
 * WAT result for display
 */
data class WATResult(
    val sessionId: String,
    val userId: String,
    val testId: String,
    val submission: WATSubmission,
    val overallScore: Float?,
    val status: SubmissionStatus,
    val attemptedAt: Long
)

