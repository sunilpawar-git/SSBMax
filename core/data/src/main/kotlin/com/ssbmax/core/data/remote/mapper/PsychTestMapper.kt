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
        val stories = parseTATStories(data)
        val instructorScore = (data["instructorScore"] as? Map<*, *>)?.let { parseTATInstructorScore(it) }
        val olqResult = parseOLQResult(data["olqResult"] as? Map<*, *>)
        val analysisStatus = parseAnalysisStatus(data)

        return TATSubmission(
            id = data["id"] as? String ?: "",
            userId = data["userId"] as? String ?: "",
            testId = data["testId"] as? String ?: "",
            stories = stories,
            totalTimeTakenMinutes = (data["totalTimeTakenMinutes"] as? Number)?.toInt() ?: 0,
            submittedAt = (data["submittedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            status = parseSubmissionStatus(data),
            instructorScore = instructorScore,
            gradedByInstructorId = data["gradedByInstructorId"] as? String,
            gradingTimestamp = (data["gradingTimestamp"] as? Number)?.toLong(),
            analysisStatus = analysisStatus,
            olqResult = olqResult
        )
    }

    private fun parseTATStories(data: Map<*, *>): List<TATStoryResponse> {
        val storiesList = data["stories"] as? List<*> ?: emptyList<Any>()
        return storiesList.mapNotNull { storyData ->
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
    }

    private fun parseTATStoryWiseComments(it: Map<*, *>): Map<String, String> =
        (it["storyWiseComments"] as? Map<*, *>)?.mapNotNull { (k, v) ->
            (k as? String)?.let { key -> key to (v as? String ?: "") }
        }?.toMap() ?: emptyMap()

    private fun parseTATInstructorScore(it: Map<*, *>): TATInstructorScore = TATInstructorScore(
        overallScore = (it["overallScore"] as? Number)?.toFloat() ?: 0f,
        thematicPerceptionScore = (it["thematicPerceptionScore"] as? Number)?.toFloat() ?: 0f,
        imaginationScore = (it["imaginationScore"] as? Number)?.toFloat() ?: 0f,
        characterDepictionScore = (it["characterDepictionScore"] as? Number)?.toFloat() ?: 0f,
        emotionalToneScore = (it["emotionalToneScore"] as? Number)?.toFloat() ?: 0f,
        narrativeStructureScore = (it["narrativeStructureScore"] as? Number)?.toFloat() ?: 0f,
        feedback = it["feedback"] as? String ?: "",
        storyWiseComments = parseTATStoryWiseComments(it),
        gradedByInstructorId = it["gradedByInstructorId"] as? String ?: "",
        gradedByInstructorName = it["gradedByInstructorName"] as? String ?: "",
        gradedAt = (it["gradedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        agreedWithAI = it["agreedWithAI"] as? Boolean ?: false
    )

    fun parseWATSubmission(data: Map<*, *>): WATSubmission {
        val responses = parseWATResponses(data)
        val instructorScore = (data["instructorScore"] as? Map<*, *>)?.let { parseWATInstructorScore(it) }
        val olqResult = parseOLQResult(data["olqResult"] as? Map<*, *>)
        val analysisStatus = parseAnalysisStatus(data)

        return WATSubmission(
            id = data["id"] as? String ?: "",
            userId = data["userId"] as? String ?: "",
            testId = data["testId"] as? String ?: "",
            responses = responses,
            totalTimeTakenMinutes = (data["totalTimeTakenMinutes"] as? Number)?.toInt() ?: 0,
            submittedAt = (data["submittedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            status = parseSubmissionStatus(data),
            instructorScore = instructorScore,
            gradedByInstructorId = data["gradedByInstructorId"] as? String,
            gradingTimestamp = (data["gradingTimestamp"] as? Number)?.toLong(),
            analysisStatus = analysisStatus,
            olqResult = olqResult
        )
    }

    private fun parseWATResponses(data: Map<*, *>): List<WATWordResponse> {
        val responsesList = data["responses"] as? List<*> ?: emptyList<Any>()
        return responsesList.mapNotNull { responseData ->
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
    }

    private fun parseWATInstructorScore(it: Map<*, *>): WATInstructorScore = WATInstructorScore(
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

    fun parseSRTSubmission(data: Map<*, *>): SRTSubmission {
        val responses = parseSRTResponses(data)
        val instructorScore = (data["instructorScore"] as? Map<*, *>)?.let { parseSRTInstructorScore(it) }
        val olqResult = parseOLQResult(data["olqResult"] as? Map<*, *>)
        val analysisStatus = parseAnalysisStatus(data)

        return SRTSubmission(
            id = data["id"] as? String ?: "",
            userId = data["userId"] as? String ?: "",
            testId = data["testId"] as? String ?: "",
            responses = responses,
            totalTimeTakenMinutes = (data["totalTimeTakenMinutes"] as? Number)?.toInt() ?: 0,
            submittedAt = (data["submittedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            status = parseSubmissionStatus(data),
            instructorScore = instructorScore,
            gradedByInstructorId = data["gradedByInstructorId"] as? String,
            gradingTimestamp = (data["gradingTimestamp"] as? Number)?.toLong(),
            analysisStatus = analysisStatus,
            olqResult = olqResult
        )
    }

    private fun parseSRTResponses(data: Map<*, *>): List<SRTSituationResponse> {
        val responsesList = data["responses"] as? List<*> ?: emptyList<Any>()
        return responsesList.mapNotNull { responseData ->
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
    }

    private fun parseSRTCategoryWiseComments(categoryWiseCommentsMap: Map<*, *>): Map<SRTCategory, String> =
        categoryWiseCommentsMap.mapNotNull { (k, v) ->
            try {
                val category = SRTCategory.valueOf(k as? String ?: "GENERAL")
                category to (v as? String ?: "")
            } catch (e: Exception) { null }
        }.toMap()

    private data class SRTScoreFields(
        val overallScore: Float,
        val leadershipScore: Float,
        val decisionMakingScore: Float,
        val practicalityScore: Float,
        val initiativeScore: Float,
        val socialResponsibilityScore: Float
    )

    private fun parseSRTScoreFields(it: Map<*, *>) = SRTScoreFields(
        overallScore = (it["overallScore"] as? Number)?.toFloat() ?: 0f,
        leadershipScore = (it["leadershipScore"] as? Number)?.toFloat() ?: 0f,
        decisionMakingScore = (it["decisionMakingScore"] as? Number)?.toFloat() ?: 0f,
        practicalityScore = (it["practicalityScore"] as? Number)?.toFloat() ?: 0f,
        initiativeScore = (it["initiativeScore"] as? Number)?.toFloat() ?: 0f,
        socialResponsibilityScore = (it["socialResponsibilityScore"] as? Number)?.toFloat() ?: 0f
    )

    private fun parseSRTInstructorScore(it: Map<*, *>): SRTInstructorScore {
        val categoryWiseComments = parseSRTCategoryWiseComments(it["categoryWiseComments"] as? Map<*, *> ?: emptyMap<Any, Any>())
        val scores = parseSRTScoreFields(it)
        return SRTInstructorScore(
            overallScore = scores.overallScore,
            leadershipScore = scores.leadershipScore,
            decisionMakingScore = scores.decisionMakingScore,
            practicalityScore = scores.practicalityScore,
            initiativeScore = scores.initiativeScore,
            socialResponsibilityScore = scores.socialResponsibilityScore,
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

    fun parseSDTSubmission(data: Map<*, *>): SDTSubmission {
        val responses = parseSDTResponses(data)
        val instructorScore = (data["instructorScore"] as? Map<*, *>)?.let { parseSDTInstructorScore(it) }
        val olqResult = parseOLQResult(data["olqResult"] as? Map<*, *>)
        val analysisStatus = parseAnalysisStatus(data)

        return SDTSubmission(
            id = data["id"] as? String ?: "",
            userId = data["userId"] as? String ?: "",
            testId = data["testId"] as? String ?: "",
            responses = responses,
            totalTimeTakenMinutes = (data["totalTimeTakenMinutes"] as? Number)?.toInt() ?: 0,
            submittedAt = (data["submittedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            status = parseSubmissionStatus(data),
            instructorScore = instructorScore,
            gradedByInstructorId = data["gradedByInstructorId"] as? String,
            gradingTimestamp = (data["gradingTimestamp"] as? Number)?.toLong(),
            analysisStatus = analysisStatus,
            olqResult = olqResult
        )
    }

    private fun parseSDTResponses(data: Map<*, *>): List<SDTQuestionResponse> {
        val responsesList = data["responses"] as? List<*> ?: emptyList<Any>()
        return responsesList.mapNotNull { responseData ->
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
    }

    private fun parseSDTInstructorScore(it: Map<*, *>): SDTInstructorScore = SDTInstructorScore(
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

    private fun parseAnalysisStatus(data: Map<*, *>): AnalysisStatus {
        val analysisStatusStr = data["analysisStatus"] as? String
        return try {
            if (analysisStatusStr != null) AnalysisStatus.valueOf(analysisStatusStr)
            else AnalysisStatus.PENDING_ANALYSIS
        } catch (e: Exception) { AnalysisStatus.PENDING_ANALYSIS }
    }

    private fun parseSubmissionStatus(data: Map<*, *>): SubmissionStatus =
        try { SubmissionStatus.valueOf(data["status"] as? String ?: "SUBMITTED_PENDING_REVIEW") }
        catch (e: Exception) { SubmissionStatus.SUBMITTED_PENDING_REVIEW }

    fun parseOLQResult(data: Map<*, *>?): OLQAnalysisResult? = parseSharedOLQResult(data)
}
