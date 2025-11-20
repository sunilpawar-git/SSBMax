package com.ssbmax.core.domain.model.interview

import java.time.Instant

/**
 * Candidate's response to an interview question
 *
 * @param id Unique response identifier
 * @param sessionId Parent interview session ID
 * @param questionId Question being answered
 * @param responseText Candidate's answer (text or transcribed from voice)
 * @param responseMode How the response was provided
 * @param respondedAt When the response was submitted
 * @param thinkingTimeSec Time taken to formulate response (seconds)
 * @param audioUrl URL to audio recording (for voice mode, if consent given)
 * @param olqScores AI-generated OLQ scores for this response
 * @param confidenceScore Overall AI confidence in the assessment (0-100)
 */
data class InterviewResponse(
    val id: String,
    val sessionId: String,
    val questionId: String,
    val responseText: String,
    val responseMode: InterviewMode,
    val respondedAt: Instant,
    val thinkingTimeSec: Int,
    val audioUrl: String? = null,
    val olqScores: Map<OLQ, OLQScore> = emptyMap(),
    val confidenceScore: Int = 0
) {
    init {
        require(id.isNotBlank()) { "Response ID cannot be blank" }
        require(sessionId.isNotBlank()) { "Session ID cannot be blank" }
        require(questionId.isNotBlank()) { "Question ID cannot be blank" }
        require(responseText.isNotBlank()) { "Response text cannot be blank" }
        require(thinkingTimeSec >= 0) { "Thinking time cannot be negative" }
        require(confidenceScore in 0..100) { "Confidence score must be between 0 and 100" }

        if (responseMode == InterviewMode.VOICE_BASED) {
            require(audioUrl != null) { "Voice mode responses must have audio URL" }
        }
    }

    /**
     * Get average OLQ score across all qualities assessed
     */
    fun getAverageOLQScore(): Float {
        if (olqScores.isEmpty()) return 0f
        return olqScores.values.map { it.score }.average().toFloat()
    }

    /**
     * Get highest scored OLQ
     */
    fun getHighestScoredOLQ(): Pair<OLQ, OLQScore>? {
        return olqScores.maxByOrNull { it.value.score }?.toPair()
    }

    /**
     * Get lowest scored OLQ
     */
    fun getLowestScoredOLQ(): Pair<OLQ, OLQScore>? {
        return olqScores.minByOrNull { it.value.score }?.toPair()
    }

    /**
     * Check if response is high quality (avg score >= 4)
     */
    fun isHighQuality(): Boolean = getAverageOLQScore() >= 4.0f

    /**
     * Get word count of response
     */
    fun getWordCount(): Int = responseText.trim().split("\\s+".toRegex()).size
}

/**
 * Response statistics for analysis
 */
data class ResponseStats(
    val totalResponses: Int,
    val averageThinkingTime: Float,
    val averageWordCount: Float,
    val averageConfidence: Float,
    val highQualityCount: Int
) {
    init {
        require(totalResponses >= 0) { "Total responses cannot be negative" }
        require(averageThinkingTime >= 0) { "Average thinking time cannot be negative" }
        require(averageWordCount >= 0) { "Average word count cannot be negative" }
        require(averageConfidence in 0f..100f) { "Average confidence must be between 0 and 100" }
        require(highQualityCount >= 0) { "High quality count cannot be negative" }
        require(highQualityCount <= totalResponses) { "High quality count cannot exceed total responses" }
    }

    /**
     * Calculate high quality percentage
     */
    fun getHighQualityPercentage(): Float {
        if (totalResponses == 0) return 0f
        return (highQualityCount.toFloat() / totalResponses) * 100
    }
}
