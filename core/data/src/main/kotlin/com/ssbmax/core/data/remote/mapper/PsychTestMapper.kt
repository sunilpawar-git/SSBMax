package com.ssbmax.core.data.remote.mapper

import com.ssbmax.shared.domain.model.*
import com.ssbmax.shared.domain.model.scoring.AnalysisStatus
import com.ssbmax.shared.domain.model.scoring.OLQAnalysisResult

/**
 * Mapper for parsing Psychology Test Submissions from Firestore data.
 * Extracted from PsychTestSubmissionRepository to adhere to Single Responsibility Principle.
 */
object PsychTestMapper {

    fun parseTATSubmission(data: Map<*, *>): TATSubmission {
        val storiesList = data["stories"] as? List<*> ?: emptyList<Any>()
        val stories = storiesList.mapNotNull { storyData ->
            (storyData as? Map<*, *>)?.let {
                TATStoryResponse(
                    questionId = it["questionId"] as? String ?: "",
                    story = it["story"] as? String ?: "",
                    charactersCount = (it["charactersCount"] as? Number)?.toInt() ?: 0,
                    viewingTimeTakenSeconds = (it["viewingTimeTakenSeconds"] as? Number)?.toInt() ?: 0,
                    writingTimeTakenSeconds = (it["writingTimeTakenSeconds"] as? Number)?.toInt() ?: 0,
                    submittedAt = (it["submittedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
            }
        }

        val instructorScoreMap = data["instructorScore"] as? Map<*, *>
        val instructorScore = instructorScoreMap?.let {
            TATInstructorScore(
                overallScore = (it["overallScore"] as? Number)?.toFloat() ?: 0f,
                thematicPerceptionScore = (it["thematicPerceptionScore"] as? Number)?.toFloat() ?: 0f,
                imaginationScore = (it["imaginationScore"] as? Number)?.toFloat() ?: 0f,
                characterDepictionScore = (it["characterDepictionScore"] as? Number)?.toFloat() ?: 0f,
                emotionalToneScore = (it["emotionalToneScore"] as? Number)?.toFloat() ?: 0f,
                narrativeStructureScore = (it["narrativeStructureScore"] as? Number)?.toFloat() ?: 0f,
                feedback = it["feedback"] as? String ?: "",
                storyWiseComments = (it["storyWiseComments"] as? Map<*, *>)?.mapNotNull { (k, v) ->
                    (k as? String)?.let { key -> key to (v as? String ?: "") }
                }?.toMap() ?: emptyMap(),
                gradedByInstructorId = it["gradedByInstructorId"] as? String ?: "",
                gradedByInstructorName = it["gradedByInstructorName"] as? String ?: "",
                gradedAt = (it["gradedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                agreedWithAI = it["agreedWithAI"] as? Boolean ?: false
            )
        }

        val olqResultMap = data["olqResult"] as? Map<*, *>
        val olqResult = parseOLQResult(olqResultMap)
        val analysisStatusStr = data["analysisStatus"] as? String
        val analysisStatus = try {
            if (analysisStatusStr != null) AnalysisStatus.valueOf(analysisStatusStr)
            else AnalysisStatus.PENDING_ANALYSIS
        } catch (e: Exception) { AnalysisStatus.PENDING_ANALYSIS }

        return TATSubmission(
            id = data["id"] as? String ?: "",
            userId = data["userId"] as? String ?: "",
            testId = data["testId"] as? String ?: "",
            stories = stories,
            totalTimeTakenMinutes = (data["totalTimeTakenMinutes"] as? Number)?.toInt() ?: 0,
            submittedAt = (data["submittedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            status = try { SubmissionStatus.valueOf(data["status"] as? String ?: "SUBMITTED_PENDING_REVIEW") }
                     catch (e: Exception) { SubmissionStatus.SUBMITTED_PENDING_REVIEW },
            instructorScore = instructorScore,
            gradedByInstructorId = data["gradedByInstructorId"] as? String,
            gradingTimestamp = (data["gradingTimestamp"] as? Number)?.toLong(),
            analysisStatus = analysisStatus,
            olqResult = olqResult
        )
    }

    fun parseWATSubmission(data: Map<*, *>): WATSubmission {
        val responsesList = data["responses"] as? List<*> ?: emptyList<Any>()
        val responses = responsesList.mapNotNull { responseData ->
            (responseData as? Map<*, *>)?.let {
                WATWordResponse(
                    wordId = it["wordId"] as? String ?: "",
                    word = it["word"] as? String ?: "",
                    response = it["response"] as? String ?: "",
                    timeTakenSeconds = (it["timeTakenSeconds"] as? Number)?.toInt() ?: 0,
                    submittedAt = (it["submittedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    isSkipped = it["isSkipped"] as? Boolean ?: false
                )
            }
        }

        val instructorScoreMap = data["instructorScore"] as? Map<*, *>
        val instructorScore = instructorScoreMap?.let {
            WATInstructorScore(
                overallScore = (it["overallScore"] as? Number)?.toFloat() ?: 0f,
                positivityScore = (it["positivityScore"] as? Number)?.toFloat() ?: 0f,
                creativityScore = (it["creativityScore"] as? Number)?.toFloat() ?: 0f,
                speedScore = (it["speedScore"] as? Number)?.toFloat() ?: 0f,
                relevanceScore = (it["relevanceScore"] as? Number)?.toFloat() ?: 0f,
                emotionalMaturityScore = (it["emotionalMaturityScore"] as? Number)?.toFloat() ?: 0f,
                feedback = it["feedback"] as? String ?: "",
                flaggedResponses = (it["flaggedResponses"] as? List<*>)?.mapNotNull { r -> r as? String } ?: emptyList(),
                notableResponses = (it["notableResponses"] as? List<*>)?.mapNotNull { r -> r as? String } ?: emptyList(),
                gradedByInstructorId = it["gradedByInstructorId"] as? String ?: "",
                gradedByInstructorName = it["gradedByInstructorName"] as? String ?: "",
                gradedAt = (it["gradedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                agreedWithAI = it["agreedWithAI"] as? Boolean ?: false
            )
        }

        val olqResultMap = data["olqResult"] as? Map<*, *>
        val olqResult = parseOLQResult(olqResultMap)
        val analysisStatusStr = data["analysisStatus"] as? String
        val analysisStatus = try {
            if (analysisStatusStr != null) AnalysisStatus.valueOf(analysisStatusStr)
            else AnalysisStatus.PENDING_ANALYSIS
        } catch (e: Exception) { AnalysisStatus.PENDING_ANALYSIS }

        return WATSubmission(
            id = data["id"] as? String ?: "",
            userId = data["userId"] as? String ?: "",
            testId = data["testId"] as? String ?: "",
            responses = responses,
            totalTimeTakenMinutes = (data["totalTimeTakenMinutes"] as? Number)?.toInt() ?: 0,
            submittedAt = (data["submittedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            status = try { SubmissionStatus.valueOf(data["status"] as? String ?: "SUBMITTED_PENDING_REVIEW") }
                     catch (e: Exception) { SubmissionStatus.SUBMITTED_PENDING_REVIEW },
            instructorScore = instructorScore,
            gradedByInstructorId = data["gradedByInstructorId"] as? String,
            gradingTimestamp = (data["gradingTimestamp"] as? Number)?.toLong(),
            analysisStatus = analysisStatus,
            olqResult = olqResult
        )
    }

    fun parseSRTSubmission(data: Map<*, *>): SRTSubmission {
        val responsesList = data["responses"] as? List<*> ?: emptyList<Any>()
        val responses = responsesList.mapNotNull { responseData ->
            (responseData as? Map<*, *>)?.let {
                SRTSituationResponse(
                    situationId = it["situationId"] as? String ?: "",
                    situation = it["situation"] as? String ?: "",
                    response = it["response"] as? String ?: "",
                    charactersCount = (it["charactersCount"] as? Number)?.toInt() ?: 0,
                    timeTakenSeconds = (it["timeTakenSeconds"] as? Number)?.toInt() ?: 0,
                    submittedAt = (it["submittedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    isSkipped = it["isSkipped"] as? Boolean ?: false
                )
            }
        }

        val instructorScoreMap = data["instructorScore"] as? Map<*, *>
        val instructorScore = instructorScoreMap?.let {
            val categoryWiseCommentsMap = it["categoryWiseComments"] as? Map<*, *> ?: emptyMap<Any, Any>()
            val categoryWiseComments = categoryWiseCommentsMap.mapNotNull { (k, v) ->
                try {
                    val category = SRTCategory.valueOf(k as? String ?: "GENERAL")
                    category to (v as? String ?: "")
                } catch (e: Exception) { null }
            }.toMap()
            SRTInstructorScore(
                overallScore = (it["overallScore"] as? Number)?.toFloat() ?: 0f,
                leadershipScore = (it["leadershipScore"] as? Number)?.toFloat() ?: 0f,
                decisionMakingScore = (it["decisionMakingScore"] as? Number)?.toFloat() ?: 0f,
                practicalityScore = (it["practicalityScore"] as? Number)?.toFloat() ?: 0f,
                initiativeScore = (it["initiativeScore"] as? Number)?.toFloat() ?: 0f,
                socialResponsibilityScore = (it["socialResponsibilityScore"] as? Number)?.toFloat() ?: 0f,
                feedback = it["feedback"] as? String ?: "",
                categoryWiseComments = categoryWiseComments,
                flaggedResponses = (it["flaggedResponses"] as? List<*>)?.mapNotNull { r -> r as? String } ?: emptyList(),
                exemplaryResponses = (it["exemplaryResponses"] as? List<*>)?.mapNotNull { r -> r as? String } ?: emptyList(),
                gradedByInstructorId = it["gradedByInstructorId"] as? String ?: "",
                gradedByInstructorName = it["gradedByInstructorName"] as? String ?: "",
                gradedAt = (it["gradedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                agreedWithAI = it["agreedWithAI"] as? Boolean ?: false
            )
        }

        val olqResultMap = data["olqResult"] as? Map<*, *>
        val olqResult = parseOLQResult(olqResultMap)
        val analysisStatusStr = data["analysisStatus"] as? String
        val analysisStatus = try {
            if (analysisStatusStr != null) AnalysisStatus.valueOf(analysisStatusStr)
            else AnalysisStatus.PENDING_ANALYSIS
        } catch (e: Exception) { AnalysisStatus.PENDING_ANALYSIS }

        return SRTSubmission(
            id = data["id"] as? String ?: "",
            userId = data["userId"] as? String ?: "",
            testId = data["testId"] as? String ?: "",
            responses = responses,
            totalTimeTakenMinutes = (data["totalTimeTakenMinutes"] as? Number)?.toInt() ?: 0,
            submittedAt = (data["submittedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            status = try { SubmissionStatus.valueOf(data["status"] as? String ?: "SUBMITTED_PENDING_REVIEW") }
                     catch (e: Exception) { SubmissionStatus.SUBMITTED_PENDING_REVIEW },
            instructorScore = instructorScore,
            gradedByInstructorId = data["gradedByInstructorId"] as? String,
            gradingTimestamp = (data["gradingTimestamp"] as? Number)?.toLong(),
            analysisStatus = analysisStatus,
            olqResult = olqResult
        )
    }

    fun parseSDTSubmission(data: Map<*, *>): SDTSubmission {
        val responsesList = data["responses"] as? List<*> ?: emptyList<Any>()
        val responses = responsesList.mapNotNull { responseData ->
            (responseData as? Map<*, *>)?.let {
                SDTQuestionResponse(
                    questionId = it["questionId"] as? String ?: "",
                    question = it["question"] as? String ?: "",
                    answer = it["answer"] as? String ?: "",
                    charCount = (it["charCount"] as? Number)?.toInt() ?: 0,
                    timeTakenSeconds = (it["timeTakenSeconds"] as? Number)?.toInt() ?: 0,
                    submittedAt = (it["submittedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    isSkipped = it["isSkipped"] as? Boolean ?: false
                )
            }
        }

        val instructorScoreMap = data["instructorScore"] as? Map<*, *>
        val instructorScore = instructorScoreMap?.let {
            SDTInstructorScore(
                overallScore = (it["overallScore"] as? Number)?.toFloat() ?: 0f,
                selfAwarenessScore = (it["selfAwarenessScore"] as? Number)?.toFloat() ?: 0f,
                emotionalMaturityScore = (it["emotionalMaturityScore"] as? Number)?.toFloat() ?: 0f,
                socialPerceptionScore = (it["socialPerceptionScore"] as? Number)?.toFloat() ?: 0f,
                introspectionScore = (it["introspectionScore"] as? Number)?.toFloat() ?: 0f,
                feedback = it["feedback"] as? String ?: "",
                flaggedResponses = (it["flaggedResponses"] as? List<*>)?.mapNotNull { r -> r as? String } ?: emptyList(),
                exemplaryResponses = (it["exemplaryResponses"] as? List<*>)?.mapNotNull { r -> r as? String } ?: emptyList(),
                gradedByInstructorId = it["gradedByInstructorId"] as? String ?: "",
                gradedByInstructorName = it["gradedByInstructorName"] as? String ?: "",
                gradedAt = (it["gradedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                agreedWithAI = it["agreedWithAI"] as? Boolean ?: false
            )
        }

        val olqResultMap = data["olqResult"] as? Map<*, *>
        val olqResult = parseOLQResult(olqResultMap)
        val analysisStatusStr = data["analysisStatus"] as? String
        val analysisStatus = try {
            if (analysisStatusStr != null) AnalysisStatus.valueOf(analysisStatusStr)
            else AnalysisStatus.PENDING_ANALYSIS
        } catch (e: Exception) { AnalysisStatus.PENDING_ANALYSIS }

        return SDTSubmission(
            id = data["id"] as? String ?: "",
            userId = data["userId"] as? String ?: "",
            testId = data["testId"] as? String ?: "",
            responses = responses,
            totalTimeTakenMinutes = (data["totalTimeTakenMinutes"] as? Number)?.toInt() ?: 0,
            submittedAt = (data["submittedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            status = try { SubmissionStatus.valueOf(data["status"] as? String ?: "SUBMITTED_PENDING_REVIEW") }
                     catch (e: Exception) { SubmissionStatus.SUBMITTED_PENDING_REVIEW },
            instructorScore = instructorScore,
            gradedByInstructorId = data["gradedByInstructorId"] as? String,
            gradingTimestamp = (data["gradingTimestamp"] as? Number)?.toLong(),
            analysisStatus = analysisStatus,
            olqResult = olqResult
        )
    }

    fun parseOLQResult(data: Map<*, *>?): OLQAnalysisResult? = parseSharedOLQResult(data)
}
