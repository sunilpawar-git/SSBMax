package com.ssbmax.core.data.service

import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.service.AIScoringService
import com.ssbmax.core.domain.service.ScoringState
import com.ssbmax.core.domain.service.ScoringStatus
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Mock AI Scoring Service
 * Provides realistic mock scoring for development/testing
 * 
 * In production, this would be replaced with actual AI/ML service
 */
@Singleton
class MockAIScoringService @Inject constructor() : AIScoringService {
    
    override suspend fun scoreTAT(submission: TATSubmission): Result<TATAIScore> {
        return try {
            // Simulate API delay
            delay(1500)
            
            val score = generateTATScore(submission)
            Result.success(score)
        } catch (e: Exception) {
            Result.failure(Exception("AI scoring failed: ${e.message}", e))
        }
    }
    
    override suspend fun scoreWAT(submission: WATSubmission): Result<WATAIScore> {
        return try {
            delay(1000)
            
            val score = generateWATScore(submission)
            Result.success(score)
        } catch (e: Exception) {
            Result.failure(Exception("AI scoring failed: ${e.message}", e))
        }
    }
    
    override suspend fun scoreSRT(submission: SRTSubmission): Result<SRTAIScore> {
        return try {
            delay(1000)
            
            val score = generateSRTScore(submission)
            Result.success(score)
        } catch (e: Exception) {
            Result.failure(Exception("AI scoring failed: ${e.message}", e))
        }
    }
    
    override suspend fun getScoringStatus(submissionId: String): Result<ScoringStatus> {
        return Result.success(
            ScoringStatus(
                submissionId = submissionId,
                status = ScoringState.COMPLETED,
                progress = 1f
            )
        )
    }
    
    // Mock scoring algorithms - replace with actual AI in production
    
    private fun generateTATScore(submission: TATSubmission): TATAIScore {
        val stories = submission.stories
        val avgLength = stories.map { it.charactersCount }.average()
        
        // Base score on story length and count
        val baseScore = when {
            avgLength > 400 -> Random.nextFloat() * 15 + 75 // 75-90
            avgLength > 250 -> Random.nextFloat() * 15 + 65 // 65-80
            else -> Random.nextFloat() * 15 + 55 // 55-70
        }
        
        return TATAIScore(
            overallScore = baseScore,
            thematicPerceptionScore = baseScore * 0.2f,
            imaginationScore = baseScore * 0.2f,
            characterDepictionScore = baseScore * 0.2f,
            emotionalToneScore = baseScore * 0.2f,
            narrativeStructureScore = baseScore * 0.2f,
            feedback = generateTATFeedback(baseScore),
            storyWiseAnalysis = stories.mapIndexed { index, story ->
                StoryAnalysis(
                    questionId = story.questionId,
                    sequenceNumber = index + 1,
                    score = baseScore + Random.nextFloat() * 10 - 5,
                    themes = listOf("Leadership", "Courage", "Initiative", "Teamwork").shuffled().take(2),
                    sentimentScore = Random.nextFloat() * 0.4f + 0.5f,
                    keyInsights = listOf("Shows initiative", "Positive resolution")
                )
            },
            strengths = generateStrengths(baseScore),
            areasForImprovement = generateImprovements(baseScore)
        )
    }
    
    private fun generateWATScore(submission: WATSubmission): WATAIScore {
        val responses = submission.responses
        val validCount = responses.count { it.isValidResponse }
        
        val baseScore = (validCount.toFloat() / responses.size) * 100
        
        // Simple sentiment analysis
        val positiveCount = responses.count { 
            it.response.contains("good", ignoreCase = true) ||
            it.response.contains("success", ignoreCase = true)
        }
        
        return WATAIScore(
            overallScore = baseScore,
            positivityScore = baseScore * 0.2f,
            creativityScore = baseScore * 0.2f,
            speedScore = baseScore * 0.2f,
            relevanceScore = baseScore * 0.2f,
            emotionalMaturityScore = baseScore * 0.2f,
            feedback = generateWATFeedback(baseScore),
            positiveWords = positiveCount,
            negativeWords = responses.size - positiveCount - validCount / 2,
            neutralWords = validCount / 2,
            uniqueResponsesCount = responses.map { it.response }.toSet().size,
            repeatedPatterns = listOf("Action-oriented", "Positive associations"),
            strengths = generateStrengths(baseScore),
            areasForImprovement = generateImprovements(baseScore)
        )
    }
    
    private fun generateSRTScore(submission: SRTSubmission): SRTAIScore {
        val responses = submission.responses
        val validCount = responses.count { it.isValidResponse }
        
        val baseScore = (validCount.toFloat() / responses.size) * 100
        
        return SRTAIScore(
            overallScore = baseScore,
            leadershipScore = baseScore * 0.2f,
            decisionMakingScore = baseScore * 0.2f,
            practicalityScore = baseScore * 0.2f,
            initiativeScore = baseScore * 0.2f,
            socialResponsibilityScore = baseScore * 0.2f,
            feedback = generateSRTFeedback(baseScore),
            categoryWiseScores = mapOf(
                SRTCategory.LEADERSHIP to baseScore + Random.nextFloat() * 10 - 5,
                SRTCategory.DECISION_MAKING to baseScore + Random.nextFloat() * 10 - 5,
                SRTCategory.CRISIS_MANAGEMENT to baseScore + Random.nextFloat() * 10 - 5
            ),
            positiveTraits = listOf("Proactive", "Ethical", "Responsible"),
            concerningPatterns = emptyList(),
            responseQuality = when {
                baseScore >= 80 -> ResponseQuality.EXCELLENT
                baseScore >= 70 -> ResponseQuality.GOOD
                baseScore >= 60 -> ResponseQuality.AVERAGE
                else -> ResponseQuality.BELOW_AVERAGE
            },
            strengths = generateStrengths(baseScore),
            areasForImprovement = generateImprovements(baseScore)
        )
    }
    
    private fun generateTATFeedback(score: Float): String {
        return when {
            score >= 80 -> "Excellent storytelling with strong themes of leadership and positive character development."
            score >= 70 -> "Good narrative skills with appropriate emotional depth."
            score >= 60 -> "Adequate responses but could benefit from more detailed character development."
            else -> "Stories need more depth and better structure. Focus on positive themes."
        }
    }
    
    private fun generateWATFeedback(score: Float): String {
        return when {
            score >= 80 -> "Excellent spontaneity and positive word associations showing emotional maturity."
            score >= 70 -> "Good response quality with mostly positive associations."
            score >= 60 -> "Acceptable responses but try to be more creative and spontaneous."
            else -> "Work on faster, more spontaneous responses with positive associations."
        }
    }
    
    private fun generateSRTFeedback(score: Float): String {
        return when {
            score >= 80 -> "Excellent practical judgment showing leadership and social responsibility."
            score >= 70 -> "Good decision-making skills with ethical awareness."
            score >= 60 -> "Acceptable responses but show more initiative and leadership."
            else -> "Focus on practical solutions that demonstrate leadership qualities."
        }
    }
    
    private fun generateStrengths(score: Float): List<String> {
        val allStrengths = listOf(
            "Creative thinking",
            "Positive outlook",
            "Leadership qualities",
            "Emotional maturity",
            "Quick responses",
            "Ethical awareness",
            "Practical approach",
            "Social responsibility"
        )
        return allStrengths.shuffled().take(if (score >= 70) 4 else 2)
    }
    
    private fun generateImprovements(score: Float): List<String> {
        val allImprovements = listOf(
            "Add more detail to responses",
            "Show more initiative",
            "Be more spontaneous",
            "Include emotional depth",
            "Demonstrate leadership",
            "Consider multiple perspectives"
        )
        return allImprovements.shuffled().take(if (score < 60) 3 else 2)
    }
}

