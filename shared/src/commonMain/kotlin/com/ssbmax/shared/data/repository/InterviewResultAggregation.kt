package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.interview.InterviewResponse
import com.ssbmax.shared.domain.model.interview.InterviewResult
import com.ssbmax.shared.domain.model.interview.InterviewSession
import com.ssbmax.shared.domain.model.interview.OLQ
import com.ssbmax.shared.domain.model.interview.OLQCategory
import com.ssbmax.shared.domain.model.interview.OLQScore
import kotlinx.datetime.Clock

/**
 * Same OLQ-aggregation and feedback-generation logic as the Android original's
 * `FirestoreInterviewRepository.completeInterview`/`generateFeedback`, extracted to a top-level
 * function so [GitLiveInterviewRepository] itself stays under this repo's 300-line file limit.
 */
internal fun aggregateInterviewResult(session: InterviewSession, responses: List<InterviewResponse>): InterviewResult {
    val olqScoresMap = mutableMapOf<OLQ, MutableList<OLQScore>>()
    responses.forEach { response ->
        response.olqScores.forEach { (olq, score) ->
            olqScoresMap.getOrPut(olq) { mutableListOf() }.add(score)
        }
    }

    val overallOLQScores = olqScoresMap.mapValues { (_, scores) ->
        val avgScore = scores.map { it.score }.average().toInt().coerceIn(1, 10)
        val avgConfidence = scores.map { it.confidence }.average().toInt()
        OLQScore(avgScore, avgConfidence, "Aggregated from ${scores.size} responses")
    }

    val categoryScores = OLQCategory.entries.associateWith { category ->
        val categoryOLQs = OLQ.entries.filter { it.category == category }
        val scores = categoryOLQs.mapNotNull { overallOLQScores[it]?.score }
        if (scores.isEmpty()) 0f else scores.average().toFloat()
    }

    // Identify strengths and weaknesses (lower scores = better in SSB)
    val sortedOLQs = overallOLQScores.entries.sortedBy { it.value.score }
    val strengths = sortedOLQs.take(3).map { it.key }
    val weaknesses = sortedOLQs.takeLast(3).map { it.key }

    val overallConfidence = responses.map { it.confidenceScore }.average().toInt()
    val avgScore = overallOLQScores.values.map { it.score }.average().toFloat()
    val overallRating = avgScore.toInt().coerceIn(1, 10)

    return InterviewResult(
        id = randomId(),
        sessionId = session.id,
        userId = session.userId,
        mode = session.mode,
        completedAt = Clock.System.now(),
        durationSec = session.getDurationSeconds(),
        totalQuestions = session.questionIds.size,
        totalResponses = responses.size,
        overallOLQScores = overallOLQScores,
        categoryScores = categoryScores,
        overallConfidence = overallConfidence,
        strengths = strengths,
        weaknesses = weaknesses,
        feedback = generateInterviewFeedback(strengths, weaknesses, overallRating),
        overallRating = overallRating
    )
}

internal fun generateInterviewFeedback(strengths: List<OLQ>, weaknesses: List<OLQ>, rating: Int): String {
    val strengthNames = strengths.joinToString(", ") { it.displayName }
    val weaknessNames = weaknesses.joinToString(", ") { it.displayName }

    return when {
        rating <= 5 -> "Excellent performance! Your strengths in $strengthNames stood out. " +
            "Consider developing: $weaknessNames"
        rating <= 7 -> "Good performance. Strong areas: $strengthNames. " +
            "Areas for improvement: $weaknessNames"
        else -> "Focus on developing: $weaknessNames. " +
            "Build on your strengths in: $strengthNames"
    }
}
