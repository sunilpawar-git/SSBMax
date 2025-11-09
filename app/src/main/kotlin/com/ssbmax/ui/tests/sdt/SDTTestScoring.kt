package com.ssbmax.ui.tests.sdt

import com.ssbmax.core.domain.model.ResponseQuality
import com.ssbmax.core.domain.model.SDTAIScore
import com.ssbmax.core.domain.model.SDTQuestionResponse

/**
 * Mock AI scoring utilities for SDT test
 * TODO: Replace with actual AI scoring when implemented
 */
object SDTTestScoring {
    
    fun generateMockAIScore(responses: List<SDTQuestionResponse>): SDTAIScore {
        val validResponses = responses.filter { it.isValidResponse }
        val avgWordCount = if (validResponses.isNotEmpty()) {
            validResponses.map { it.wordCount }.average()
        } else 0.0
        
        // Score based on response quality
        val baseScore = when {
            avgWordCount > 200 -> 78f
            avgWordCount > 100 -> 70f
            else -> 60f
        }
        
        return SDTAIScore(
            overallScore = baseScore,
            selfAwarenessScore = baseScore * 0.25f,
            emotionalMaturityScore = baseScore * 0.25f,
            socialPerceptionScore = baseScore * 0.25f,
            introspectionScore = baseScore * 0.25f,
            feedback = "Good self-awareness and balanced perspective. Shows maturity in self-reflection.",
            positiveTraits = listOf(
                "Self-aware",
                "Balanced perspective",
                "Honest introspection",
                "Social awareness"
            ),
            concerningPatterns = emptyList(),
            responseQuality = when {
                baseScore >= 80 -> ResponseQuality.EXCELLENT
                baseScore >= 70 -> ResponseQuality.GOOD
                baseScore >= 60 -> ResponseQuality.AVERAGE
                else -> ResponseQuality.BELOW_AVERAGE
            },
            strengths = listOf(
                "Clear self-understanding",
                "Awareness of others' perceptions",
                "Balanced self-assessment"
            ),
            areasForImprovement = listOf(
                "Could provide more specific examples",
                "Deeper introspection recommended"
            )
        )
    }
}

