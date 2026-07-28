package com.ssbmax.core.data.remote

import android.util.Log
import com.ssbmax.shared.domain.model.PPDTDetailedScores
import com.ssbmax.shared.domain.model.PPDTInstructorReview
import com.ssbmax.shared.domain.model.PPDTSubmission
import com.ssbmax.shared.domain.model.SubmissionStatus
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.model.scoring.AnalysisStatus
import com.ssbmax.shared.domain.model.scoring.OLQAnalysisResult

private const val PPDT_MAPPER_TAG = "PPDTMapper"

@Suppress("UNCHECKED_CAST")
internal fun parsePPDTSubmission(data: Map<*, *>): PPDTSubmission {
    val instructorReview = (data["instructorReview"] as? Map<*, *>)?.let { parsePPDTInstructorReview(it) }

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

private fun parsePPDTDetailedScores(detailedScoresMap: Map<*, *>) = PPDTDetailedScores(
    perception = (detailedScoresMap["perception"] as? Number)?.toFloat() ?: 0f,
    imagination = (detailedScoresMap["imagination"] as? Number)?.toFloat() ?: 0f,
    narration = (detailedScoresMap["narration"] as? Number)?.toFloat() ?: 0f,
    characterDepiction = (detailedScoresMap["characterDepiction"] as? Number)?.toFloat() ?: 0f,
    positivity = (detailedScoresMap["positivity"] as? Number)?.toFloat() ?: 0f
)

@Suppress("UNCHECKED_CAST")
private fun parsePPDTInstructorReview(reviewMap: Map<*, *>): PPDTInstructorReview {
    val detailedScores = parsePPDTDetailedScores(reviewMap["detailedScores"] as? Map<*, *> ?: emptyMap<String, Any>())
    return PPDTInstructorReview(
        reviewId = reviewMap["reviewId"] as? String ?: "",
        instructorId = reviewMap["instructorId"] as? String ?: "",
        instructorName = reviewMap["instructorName"] as? String ?: "",
        finalScore = (reviewMap["finalScore"] as? Number)?.toFloat() ?: 0f,
        feedback = reviewMap["feedback"] as? String ?: "",
        detailedScores = detailedScores,
        agreedWithAI = reviewMap["agreedWithAI"] as? Boolean ?: false,
        reviewedAt = (reviewMap["reviewedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        timeSpentMinutes = (reviewMap["timeSpentMinutes"] as? Number)?.toInt() ?: 0
    )
}

@Suppress("UNCHECKED_CAST")
internal fun parsePPDTOLQAnalysisResult(data: Map<String, Any?>, submissionId: String): OLQAnalysisResult? {
    return try {
        val scoresMap = data["olqScores"] as? Map<String, Map<String, Any>> ?: return null

        val olqScores = scoresMap.mapNotNull { (key, value) ->
            try {
                val olq = com.ssbmax.shared.domain.model.interview.OLQ.valueOf(key)
                val score = (value["score"] as? Number)?.toInt() ?: 0
                val confidence = (value["confidence"] as? Number)?.toInt() ?: 0
                val reasoning = value["reasoning"] as? String ?: ""
                olq to com.ssbmax.shared.domain.model.interview.OLQScore(score, confidence, reasoning)
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
