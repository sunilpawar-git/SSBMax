package com.ssbmax.core.domain.service

import com.ssbmax.core.domain.model.interview.InterviewQuestion
import com.ssbmax.core.domain.model.interview.OLQ

/**
 * AI service interface for interview-related operations
 *
 * This interface abstracts the AI provider (Gemini, OpenAI, etc.)
 * to maintain clean architecture and testability
 */
interface AIService {

    /**
     * Generate personalized interview questions based on PIQ data
     *
     * Analyzes candidate's PIQ responses and generates targeted questions
     * to assess specific Officer-Like Qualities (OLQs)
     *
     * @param piqData Candidate's PIQ submission data (as JSON string)
     * @param targetOLQs Specific OLQs to assess (optional, generates balanced set if null)
     * @param count Number of questions to generate
     * @param difficulty Question difficulty level (1-5)
     * @return Generated interview questions
     */
    suspend fun generatePIQBasedQuestions(
        piqData: String,
        targetOLQs: List<OLQ>? = null,
        count: Int = 5,
        difficulty: Int = 3
    ): Result<List<InterviewQuestion>>

    /**
     * Generate adaptive follow-up questions based on previous responses
     *
     * Analyzes candidate's responses and generates deeper probing questions
     * to explore specific OLQs that need further assessment
     *
     * @param previousQuestions Questions asked so far
     * @param previousResponses Candidate's responses
     * @param weakOLQs OLQs needing more assessment (identified from low scores)
     * @param count Number of follow-up questions to generate
     * @return Adaptive follow-up questions
     */
    suspend fun generateAdaptiveQuestions(
        previousQuestions: List<InterviewQuestion>,
        previousResponses: List<String>,
        weakOLQs: List<OLQ>,
        count: Int = 2
    ): Result<List<InterviewQuestion>>

    /**
     * Analyze candidate response and generate OLQ scores
     *
     * Uses AI to evaluate response quality and assess demonstrated
     * Officer-Like Qualities with reasoning
     *
     * @param question The interview question
     * @param response Candidate's answer
     * @param responseMode How the response was provided (text/voice)
     * @return OLQ scores with confidence and reasoning
     */
    suspend fun analyzeResponse(
        question: InterviewQuestion,
        response: String,
        responseMode: String
    ): Result<ResponseAnalysis>

    /**
     * Generate comprehensive interview feedback
     *
     * Analyzes all responses and generates personalized feedback
     * with strengths, weaknesses, and improvement recommendations
     *
     * @param questions All questions asked
     * @param responses All candidate responses
     * @param olqScores Aggregated OLQ scores
     * @return Comprehensive feedback text
     */
    suspend fun generateFeedback(
        questions: List<InterviewQuestion>,
        responses: List<String>,
        olqScores: Map<OLQ, Float>
    ): Result<String>

    /**
     * Health check for AI service availability
     *
     * @return True if service is available and API key is valid
     */
    suspend fun isAvailable(): Boolean
}

/**
 * Response analysis result from AI
 *
 * @param olqScores Map of OLQ to score (1-5) and reasoning
 * @param overallConfidence AI confidence in the assessment (0-100)
 * @param keyInsights Notable observations about the response
 * @param suggestedFollowUp Recommended follow-up question (optional)
 */
data class ResponseAnalysis(
    val olqScores: Map<OLQ, OLQScoreWithReasoning>,
    val overallConfidence: Int,
    val keyInsights: List<String>,
    val suggestedFollowUp: String? = null
) {
    init {
        require(overallConfidence in 0..100) { "Confidence must be between 0 and 100" }
        require(olqScores.isNotEmpty()) { "Must have at least one OLQ score" }
    }
}

/**
 * OLQ score with AI reasoning
 *
 * @param olq The Officer-Like Quality being assessed
 * @param score Score from 1 (poor) to 5 (excellent)
 * @param reasoning AI's explanation for the score
 * @param evidence Specific phrases/behaviors from response supporting the score
 */
data class OLQScoreWithReasoning(
    val olq: OLQ,
    val score: Float,
    val reasoning: String,
    val evidence: List<String> = emptyList()
) {
    init {
        require(score in 1f..5f) { "Score must be between 1 and 5" }
        require(reasoning.isNotBlank()) { "Reasoning cannot be blank" }
    }
}
