package com.ssbmax.core.domain.model

import com.ssbmax.core.domain.model.scoring.AnalysisStatus
import com.ssbmax.core.domain.model.scoring.OLQAnalysisResult
import java.util.UUID

/**
 * SDT (Self Description Test) - Phase 2 Psychology Test
 * 
 * Student answers 4 reflective questions about how others perceive them
 * and how they perceive themselves.
 * Time: 15 minutes total (900 seconds)
 * Max words per question: 1000 words
 */

/**
 * Represents a single question in SDT
 */
data class SDTQuestion(
    val id: String = UUID.randomUUID().toString(),
    val question: String,
    val sequenceNumber: Int, // 1-4
    val description: String = "",
    val maxWords: Int = 1000
)

/**
 * Student's answer to a single SDT question
 */
data class SDTQuestionResponse(
    val questionId: String,
    val question: String,
    val answer: String,
    val charCount: Int,
    val timeTakenSeconds: Int,
    val submittedAt: Long,
    val isSkipped: Boolean = false
) {
    val isValidResponse: Boolean get() = answer.isNotBlank() && !isSkipped
}

/**
 * Complete SDT test submission (all 4 responses)
 */
data class SDTSubmission(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val testId: String,
    val responses: List<SDTQuestionResponse>,
    val totalTimeTakenMinutes: Int,
    val submittedAt: Long,
    val status: SubmissionStatus = SubmissionStatus.SUBMITTED_PENDING_REVIEW,
    val instructorScore: SDTInstructorScore? = null,
    val gradedByInstructorId: String? = null,
    val gradingTimestamp: Long? = null,
    // OLQ-based analysis fields (Phase 1)
    val analysisStatus: AnalysisStatus = AnalysisStatus.PENDING_ANALYSIS,
    val olqResult: OLQAnalysisResult? = null
) {
    val totalResponses: Int get() = responses.size
    val validResponses: Int get() = responses.count { it.isValidResponse }
    val skippedResponses: Int get() = responses.count { it.isSkipped }
    val averageCharCount: Int get() = responses.filter { it.isValidResponse }
        .map { it.charCount }.average().toInt()
    val isComplete: Boolean get() = responses.size == 4
}



/**
 * Instructor's final grading for SDT
 */
data class SDTInstructorScore(
    val overallScore: Float, // 0-100
    val selfAwarenessScore: Float,
    val emotionalMaturityScore: Float,
    val socialPerceptionScore: Float,
    val introspectionScore: Float,
    val feedback: String,
    val flaggedResponses: List<String> = emptyList(), // Question IDs of concerning responses
    val exemplaryResponses: List<String> = emptyList(), // Question IDs of excellent responses
    val gradedByInstructorId: String,
    val gradedByInstructorName: String,
    val gradedAt: Long,
    val agreedWithAI: Boolean = false
)

/**
 * SDT test session tracking
 */
data class SDTTestSession(
    val sessionId: String = UUID.randomUUID().toString(),
    val userId: String,
    val testId: String,
    val questions: List<SDTQuestion>,
    val currentQuestionIndex: Int = 0,
    val responses: List<SDTQuestionResponse> = emptyList(),
    val currentAnswer: String = "",
    val phase: SDTPhase = SDTPhase.INSTRUCTIONS,
    val startTime: Long,
    val totalTimeRemaining: Int = 900, // 15 minutes in seconds
    val isCompleted: Boolean = false
) {
    val currentQuestion: SDTQuestion?
        get() = questions.getOrNull(currentQuestionIndex)
    
    val progress: Float
        get() = if (questions.isEmpty()) 0f else (currentQuestionIndex.toFloat() / questions.size)
    
    val completedQuestions: Int
        get() = responses.size
    
    val remainingQuestions: Int
        get() = questions.size - responses.size
}

/**
 * SDT test phases
 */
enum class SDTPhase {
    INSTRUCTIONS,
    IN_PROGRESS,
    REVIEW,
    COMPLETED,
    SUBMITTED;
    
    val displayName: String
        get() = when (this) {
            INSTRUCTIONS -> "Instructions"
            IN_PROGRESS -> "Test in Progress"
            REVIEW -> "Review Answers"
            COMPLETED -> "Test Completed"
            SUBMITTED -> "Submitted"
        }
}

/**
 * SDT test configuration
 */
data class SDTTestConfig(
    val testId: String = "sdt_standard",
    val title: String = "SDT - Self Description Test",
    val description: String = "Describe yourself from different perspectives",
    val totalQuestions: Int = 4,
    val totalTimeMinutes: Int = 15,
    val totalTimeSeconds: Int = 900, // 15 minutes
    val minCharsPerQuestion: Int = 50,
    val maxCharsPerQuestion: Int = 1500,
    val showAIScore: Boolean = true,
    val requiresInstructorReview: Boolean = true,
    val allowSkip: Boolean = true,
    val allowReviewBeforeSubmit: Boolean = true
)

/**
 * SDT result for display
 */
data class SDTResult(
    val sessionId: String,
    val userId: String,
    val testId: String,
    val submission: SDTSubmission,
    val overallScore: Float?,
    val status: SubmissionStatus,
    val attemptedAt: Long
)

/**
 * Factory method to create the 4 standard SDT questions
 */
fun createStandardSDTQuestions(): List<SDTQuestion> {
    return listOf(
        SDTQuestion(
            id = "sdt_q_1",
            question = "What do your parents think about you?",
            sequenceNumber = 1,
            description = "Describe how your parents perceive you - your qualities, behavior, and character",
            maxWords = 1000
        ),
        SDTQuestion(
            id = "sdt_q_2",
            question = "What do your teachers/seniors (for service/working candidates only) think about you?",
            sequenceNumber = 2,
            description = "Describe how your teachers or seniors/colleagues perceive you professionally and personally",
            maxWords = 1000
        ),
        SDTQuestion(
            id = "sdt_q_3",
            question = "What do your friends think about you?",
            sequenceNumber = 3,
            description = "Describe how your friends perceive you - your personality, behavior, and qualities",
            maxWords = 1000
        ),
        SDTQuestion(
            id = "sdt_q_4",
            question = "What do you think about yourself?",
            sequenceNumber = 4,
            description = "Describe your own perception of yourself - your strengths, weaknesses, and character",
            maxWords = 1000
        )
    )
}

