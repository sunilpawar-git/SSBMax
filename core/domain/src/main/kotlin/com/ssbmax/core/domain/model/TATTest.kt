package com.ssbmax.core.domain.model

import java.util.UUID

/**
 * TAT (Thematic Apperception Test) - Phase 2 Psychology Test
 * 
 * Student is shown a series of ambiguous pictures (typically 11-12 pictures)
 * and must write a story for each picture within 30 seconds viewing + 4 minutes writing.
 */

/**
 * Represents a single TAT image/picture card
 */
data class TATQuestion(
    val id: String = UUID.randomUUID().toString(),
    val imageUrl: String,
    val sequenceNumber: Int, // 1-12
    val prompt: String = "Write a story about what you see in the picture",
    val viewingTimeSeconds: Int = 30,
    val writingTimeMinutes: Int = 4,
    val minCharacters: Int = 150,
    val maxCharacters: Int = 800
)

/**
 * Student's response to a single TAT picture
 */
data class TATStoryResponse(
    val questionId: String,
    val story: String,
    val charactersCount: Int,
    val viewingTimeTakenSeconds: Int,
    val writingTimeTakenSeconds: Int,
    val submittedAt: Long
)

/**
 * Complete TAT test submission (all 11-12 stories)
 */
data class TATSubmission(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val testId: String,
    val stories: List<TATStoryResponse>,
    val totalTimeTakenMinutes: Int,
    val submittedAt: Long,
    val status: SubmissionStatus = SubmissionStatus.SUBMITTED_PENDING_REVIEW,
    val aiPreliminaryScore: TATAIScore? = null,
    val instructorScore: TATInstructorScore? = null,
    val gradedByInstructorId: String? = null,
    val gradingTimestamp: Long? = null
) {
    val totalStories: Int get() = stories.size
    val isComplete: Boolean get() = stories.size >= 11
    val averageStoryLength: Int get() = stories.map { it.charactersCount }.average().toInt()
}

/**
 * AI-generated preliminary score for TAT
 */
data class TATAIScore(
    val overallScore: Float, // 0-100
    val thematicPerceptionScore: Float, // 0-20
    val imaginationScore: Float, // 0-20
    val characterDepictionScore: Float, // 0-20
    val emotionalToneScore: Float, // 0-20
    val narrativeStructureScore: Float, // 0-20
    val feedback: String? = null,
    val storyWiseAnalysis: List<StoryAnalysis> = emptyList(),
    val strengths: List<String> = emptyList(),
    val areasForImprovement: List<String> = emptyList()
)

/**
 * Per-story analysis from AI
 */
data class StoryAnalysis(
    val questionId: String,
    val sequenceNumber: Int,
    val score: Float, // 0-100 for this story
    val themes: List<String>, // Identified themes (leadership, courage, etc.)
    val sentimentScore: Float, // -1 to +1 (negative to positive)
    val keyInsights: List<String>
)

/**
 * Instructor's final grading for TAT
 */
data class TATInstructorScore(
    val overallScore: Float, // 0-100
    val thematicPerceptionScore: Float,
    val imaginationScore: Float,
    val characterDepictionScore: Float,
    val emotionalToneScore: Float,
    val narrativeStructureScore: Float,
    val feedback: String,
    val storyWiseComments: Map<String, String> = emptyMap(), // questionId -> comment
    val gradedByInstructorId: String,
    val gradedByInstructorName: String,
    val gradedAt: Long,
    val agreedWithAI: Boolean = false
)

/**
 * TAT test session tracking
 */
data class TATTestSession(
    val sessionId: String = UUID.randomUUID().toString(),
    val userId: String,
    val testId: String,
    val questions: List<TATQuestion>,
    val currentQuestionIndex: Int = 0,
    val responses: List<TATStoryResponse> = emptyList(),
    val currentPhase: TATPhase = TATPhase.INSTRUCTIONS,
    val currentStory: String = "",
    val startTime: Long,
    val viewingStartTime: Long? = null,
    val writingStartTime: Long? = null,
    val isPaused: Boolean = false,
    val isCompleted: Boolean = false
) {
    val currentQuestion: TATQuestion?
        get() = questions.getOrNull(currentQuestionIndex)
    
    val progress: Float
        get() = if (questions.isEmpty()) 0f else (currentQuestionIndex.toFloat() / questions.size)
    
    val completedStories: Int
        get() = responses.size
    
    val remainingStories: Int
        get() = questions.size - responses.size
}

/**
 * TAT test phases
 */
enum class TATPhase {
    INSTRUCTIONS,
    IMAGE_VIEWING,
    WRITING,
    REVIEW_CURRENT,
    SUBMITTED;
    
    val displayName: String
        get() = when (this) {
            INSTRUCTIONS -> "Instructions"
            IMAGE_VIEWING -> "Viewing Image"
            WRITING -> "Writing Story"
            REVIEW_CURRENT -> "Review"
            SUBMITTED -> "Submitted"
        }
}

/**
 * TAT test configuration
 */
data class TATTestConfig(
    val testId: String = "tat_standard",
    val title: String = "TAT - Thematic Apperception Test",
    val description: String = "Write stories based on ambiguous pictures",
    val totalPictures: Int = 12,
    val viewingTimePerPictureSeconds: Int = 30,
    val writingTimePerPictureMinutes: Int = 4,
    val minCharactersPerStory: Int = 150,
    val maxCharactersPerStory: Int = 800,
    val showAIScore: Boolean = true,
    val requiresInstructorReview: Boolean = true,
    val allowSkip: Boolean = false, // Force sequential completion
    val allowReviewBeforeSubmit: Boolean = true
)

/**
 * TAT result for display
 */
data class TATResult(
    val sessionId: String,
    val userId: String,
    val testId: String,
    val submission: TATSubmission,
    val overallScore: Float?,
    val status: SubmissionStatus,
    val attemptedAt: Long
)

