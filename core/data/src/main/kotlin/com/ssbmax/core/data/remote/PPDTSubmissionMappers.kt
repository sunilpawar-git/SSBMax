package com.ssbmax.core.data.remote

import android.util.Log
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.model.scoring.AnalysisStatus
import com.ssbmax.core.domain.model.scoring.OLQAnalysisResult

private const val PPDT_MAPPER_TAG = "PPDTMapper"

@Suppress("UNCHECKED_CAST")
internal fun parsePPDTSubmission(data: Map<*, *>): PPDTSubmission {
    val instructorReviewMap = data["instructorReview"] as? Map<*, *>
    val instructorReview = instructorReviewMap?.let {
        val detailedScoresMap = it["detailedScores"] as? Map<*, *> ?: emptyMap<String, Any>()
        val detailedScores = PPDTDetailedScores(
            perception = (detailedScoresMap["perception"] as? Number)?.toFloat() ?: 0f,
            imagination = (detailedScoresMap["imagination"] as? Number)?.toFloat() ?: 0f,
            narration = (detailedScoresMap["narration"] as? Number)?.toFloat() ?: 0f,
            characterDepiction = (detailedScoresMap["characterDepiction"] as? Number)?.toFloat() ?: 0f,
            positivity = (detailedScoresMap["positivity"] as? Number)?.toFloat() ?: 0f
        )
        PPDTInstructorReview(
            reviewId = it["reviewId"] as? String ?: "",
            instructorId = it["instructorId"] as? String ?: "",
            instructorName = it["instructorName"] as? String ?: "",
            finalScore = (it["finalScore"] as? Number)?.toFloat() ?: 0f,
            feedback = it["feedback"] as? String ?: "",
            detailedScores = detailedScores,
            agreedWithAI = it["agreedWithAI"] as? Boolean ?: false,
            reviewedAt = (it["reviewedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            timeSpentMinutes = (it["timeSpentMinutes"] as? Number)?.toInt() ?: 0
        )
    }

    val olqResultMap = data["olqResult"] as? Map<*, *>
    val olqResult = parseOLQResult(olqResultMap)

    val analysisStatusStr = data["analysisStatus"] as? String
    val analysisStatus = try {
        if (analysisStatusStr != null) AnalysisStatus.valueOf(analysisStatusStr)
        else AnalysisStatus.PENDING_ANALYSIS
    } catch (e: Exception) { AnalysisStatus.PENDING_ANALYSIS }

    return PPDTSubmission(
        submissionId = data["submissionId"] as? String ?: "",
        questionId = data["questionId"] as? String ?: "",
        userId = data["userId"] as? String ?: "",
        userName = data["userName"] as? String ?: "",
        userEmail = data["userEmail"] as? String ?: "",
        batchId = data["batchId"] as? String,
        story = data["story"] as? String ?: "",
        charactersCount = (data["charactersCount"] as? Number)?.toInt() ?: 0,
        viewingTimeTakenSeconds = (data["viewingTimeTakenSeconds"] as? Number)?.toInt() ?: 0,
        writingTimeTakenMinutes = (data["writingTimeTakenMinutes"] as? Number)?.toInt() ?: 0,
        submittedAt = (data["submittedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        status = SubmissionStatus.valueOf(data["status"] as? String ?: "SUBMITTED"),
        instructorReview = instructorReview,
        analysisStatus = analysisStatus,
        olqResult = olqResult
    )
}

@Suppress("UNCHECKED_CAST")
internal fun parsePPDTOLQAnalysisResult(data: Map<String, Any?>, submissionId: String): OLQAnalysisResult? {
    return try {
        val scoresMap = data["olqScores"] as? Map<String, Map<String, Any>> ?: return null

        val olqScores = scoresMap.mapNotNull { (key, value) ->
            try {
                val olq = com.ssbmax.core.domain.model.interview.OLQ.valueOf(key)
                val score = (value["score"] as? Number)?.toInt() ?: 0
                val confidence = (value["confidence"] as? Number)?.toInt() ?: 0
                val reasoning = value["reasoning"] as? String ?: ""
                olq to com.ssbmax.core.domain.model.interview.OLQScore(score, confidence, reasoning)
            } catch (e: Exception) {
                Log.w(PPDT_MAPPER_TAG, "Error parsing OLQ score: $key", e)
                null
            }
        }.toMap()

        OLQAnalysisResult(
            submissionId = submissionId,
            testType = TestType.PPDT,
            olqScores = olqScores,
            overallScore = (data["overallScore"] as? Number)?.toFloat() ?: 0f,
            overallRating = data["overallRating"] as? String ?: "",
            strengths = (data["strengths"] as? List<String>) ?: emptyList(),
            weaknesses = (data["weaknesses"] as? List<String>) ?: emptyList(),
            recommendations = (data["recommendations"] as? List<String>) ?: emptyList(),
            aiConfidence = (data["aiConfidence"] as? Number)?.toInt() ?: 0,
            analyzedAt = (data["analyzedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    } catch (e: Exception) {
        Log.e(PPDT_MAPPER_TAG, "Error parsing OLQAnalysisResult", e)
        null
    }
}
