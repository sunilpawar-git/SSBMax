package com.ssbmax.core.data.remote.mapper

import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQScore
import com.ssbmax.core.domain.model.scoring.OLQAnalysisResult

/**
 * Parses a Firestore OLQ result map shared by all OLQ-based tests (TAT, WAT, SRT, SDT, PPDT).
 * Extracted from PsychTestMapper to keep it under the 300-line limit.
 */
internal fun parseSharedOLQResult(data: Map<*, *>?): OLQAnalysisResult? {
    if (data == null) return null
    return try {
        val testTypeStr = data["testType"] as? String ?: return null
        val testType = try {
            TestType.valueOf(testTypeStr)
        } catch (e: Exception) {
            // Fallback or handle null - avoiding crash for unknown types
            return null
        }
        val olqScoresMap = data["olqScores"] as? Map<*, *> ?: emptyMap<Any, Any>()
        val olqScores = olqScoresMap.mapNotNull { (k, v) ->
            try {
                val olq = OLQ.valueOf(k as? String ?: "")
                val scoreMap = v as? Map<*, *> ?: return@mapNotNull null
                olq to OLQScore(
                    score = (scoreMap["score"] as? Number)?.toInt() ?: 5,
                    confidence = (scoreMap["confidence"] as? Number)?.toInt() ?: 0,
                    reasoning = scoreMap["reasoning"] as? String ?: ""
                )
            } catch (e: Exception) { null }
        }.toMap()

        OLQAnalysisResult(
            submissionId = data["submissionId"] as? String ?: "",
            testType = testType,
            olqScores = olqScores,
            overallScore = (data["overallScore"] as? Number)?.toFloat() ?: 5f,
            overallRating = data["overallRating"] as? String ?: "",
            strengths = (data["strengths"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            weaknesses = (data["weaknesses"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            recommendations = (data["recommendations"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            analyzedAt = (data["analyzedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            aiConfidence = (data["aiConfidence"] as? Number)?.toInt() ?: 0,
            validStoriesCount = (data["validStoriesCount"] as? Number)?.toInt() ?: 0,
            failedStoriesCount = (data["failedStoriesCount"] as? Number)?.toInt() ?: 0,
            usedPartialAssessment = data["usedPartialAssessment"] as? Boolean ?: false
        )
    } catch (e: Exception) { null }
}
