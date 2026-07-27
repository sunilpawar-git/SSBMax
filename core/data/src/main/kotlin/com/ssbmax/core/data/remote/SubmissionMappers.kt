package com.ssbmax.core.data.remote

import android.util.Log
import com.google.firebase.firestore.SnapshotMetadata
import com.ssbmax.shared.domain.model.*
import com.ssbmax.shared.domain.model.interview.OLQ
import com.ssbmax.shared.domain.model.interview.OLQScore
import com.ssbmax.shared.domain.model.scoring.AnalysisStatus
import com.ssbmax.shared.domain.model.scoring.OLQAnalysisResult

/**
 * Shared submission mappers for Firestore serialization.
 * 
 * These functions have `internal` visibility so they can be shared across
 * repositories within the core:data module while not being exposed to other modules.
 * 
 * Extracted from FirestoreSubmissionRepository during Phase 3 refactoring.
 */

private const val TAG = "SubmissionMappers"

// ===========================
// Shared Helper Classes
// ===========================

/**
 * Helper class for OLQ regression protection in Firestore observers.
 * Prevents stale Firestore cache snapshots from overwriting completed OLQ analysis data.
 */
class OLQRegressionFilter {
    private var hasSeenCompleteAnalysis = false
    
    fun shouldFilterSnapshot(
        data: Map<*, *>,
        metadata: SnapshotMetadata,
        testType: String,
        submissionId: String
    ): Boolean {
        val analysisStatus = data["analysisStatus"] as? String
        val hasOlqResult = data["olqResult"] != null
        val isComplete = analysisStatus == SubmissionConstants.ANALYSIS_STATUS_COMPLETED && hasOlqResult
        
        if (isComplete) {
            hasSeenCompleteAnalysis = true
        }
        
        val isFromCacheOnly = metadata.isFromCache && !metadata.hasPendingWrites()
        val wouldRegress = hasSeenCompleteAnalysis && !isComplete && isFromCacheOnly
        
        if (wouldRegress) {
            Log.d(TAG, "⚠️ Ignoring stale cache for $testType $submissionId (would regress)")
        }
        
        return wouldRegress
    }
}

/**
 * Constants for submission analysis status values and field names.
 */
object SubmissionConstants {
    const val ANALYSIS_STATUS_COMPLETED = "COMPLETED"
    const val ANALYSIS_STATUS_PENDING = "PENDING_ANALYSIS"
    const val ANALYSIS_STATUS_ANALYZING = "ANALYZING"
    
    const val FIELD_ID = "id"
    const val FIELD_USER_ID = "userId"
    const val FIELD_TEST_ID = "testId"
    const val FIELD_TEST_TYPE = "testType"
    const val FIELD_STATUS = "status"
    const val FIELD_SUBMITTED_AT = "submittedAt"
    const val FIELD_GRADED_BY_INSTRUCTOR_ID = "gradedByInstructorId"
    const val FIELD_GRADING_TIMESTAMP = "gradingTimestamp"
    const val FIELD_BATCH_ID = "batchId"
    const val FIELD_DATA = "data"
}

/**
 * Helper object for mapping OLQ results to Firestore format.
 */
object OLQMapper {
    fun toFirestoreMap(olqResult: OLQAnalysisResult): Map<String, Any?> {
        return mapOf(
            "submissionId" to olqResult.submissionId,
            "testType" to olqResult.testType.name,
            "olqScores" to olqResult.olqScores.mapKeys { it.key.name }.mapValues { (_, score) ->
                mapOf("score" to score.score, "confidence" to score.confidence, "reasoning" to score.reasoning)
            },
            "overallScore" to olqResult.overallScore,
            "overallRating" to olqResult.overallRating,
            "strengths" to olqResult.strengths,
            "weaknesses" to olqResult.weaknesses,
            "recommendations" to olqResult.recommendations,
            "analyzedAt" to olqResult.analyzedAt,
            "aiConfidence" to olqResult.aiConfidence,
            "validStoriesCount" to olqResult.validStoriesCount,
            "failedStoriesCount" to olqResult.failedStoriesCount,
            "usedPartialAssessment" to olqResult.usedPartialAssessment
        )
    }
}

// ===========================
// OIR Submission Mappers
// ===========================

internal fun OIRSubmission.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "id" to id,
        "userId" to userId,
        "testId" to testId,
        "testResult" to mapOf(
            "testId" to testResult.testId,
            "sessionId" to testResult.sessionId,
            "userId" to testResult.userId,
            "totalQuestions" to testResult.totalQuestions,
            "correctAnswers" to testResult.correctAnswers,
            "incorrectAnswers" to testResult.incorrectAnswers,
            "skippedQuestions" to testResult.skippedQuestions,
            "totalTimeSeconds" to testResult.totalTimeSeconds,
            "timeTakenSeconds" to testResult.timeTakenSeconds,
            "rawScore" to testResult.rawScore,
            "percentageScore" to testResult.percentageScore,
            "categoryScores" to testResult.categoryScores.entries.associate { (category, score) ->
                category.name to mapOf(
                    "category" to score.category.name,
                    "totalQuestions" to score.totalQuestions,
                    "correctAnswers" to score.correctAnswers,
                    "percentage" to score.percentage,
                    "averageTimeSeconds" to score.averageTimeSeconds
                )
            },
            "difficultyBreakdown" to testResult.difficultyBreakdown.entries.associate { (difficulty, score) ->
                difficulty.name to mapOf(
                    "difficulty" to score.difficulty.name,
                    "totalQuestions" to score.totalQuestions,
                    "correctAnswers" to score.correctAnswers,
                    "percentage" to score.percentage
                )
            },
            "answeredQuestions" to testResult.answeredQuestions.map { aq ->
                mapOf(
                    "questionId" to aq.question.id,
                    "questionNumber" to aq.question.questionNumber,
                    "questionType" to aq.question.type.name,
                    "questionText" to aq.question.questionText,
                    "difficulty" to aq.question.difficulty.name,
                    "correctAnswerId" to aq.question.correctAnswerId,
                    "selectedOptionId" to aq.userAnswer.selectedOptionId,
                    "isCorrect" to aq.isCorrect,
                    "timeTakenSeconds" to aq.userAnswer.timeTakenSeconds,
                    "skipped" to aq.userAnswer.skipped
                )
            },
            "completedAt" to testResult.completedAt,
            "passed" to testResult.passed,
            "grade" to testResult.grade.name
        ),
        "submittedAt" to submittedAt,
        "status" to status.name,
        "gradedByInstructorId" to gradedByInstructorId,
        "gradingTimestamp" to gradingTimestamp
    )
}

// ===========================
// PPDT Submission Mappers
// ===========================

internal fun PPDTSubmission.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "submissionId" to submissionId,
        "questionId" to questionId,
        "userId" to userId,
        "userName" to userName,
        "userEmail" to userEmail,
        "batchId" to batchId,
        "story" to story,
        "charactersCount" to charactersCount,
        "viewingTimeTakenSeconds" to viewingTimeTakenSeconds,
        "writingTimeTakenMinutes" to writingTimeTakenMinutes,
        "submittedAt" to submittedAt,
        "status" to status.name,
        // IMPORTANT: Do NOT include analysisStatus or olqResult here!
        // These fields are written ONLY by PPDTAnalysisWorker.
        "instructorReview" to instructorReview?.let {
            mapOf(
                "reviewId" to it.reviewId,
                "instructorId" to it.instructorId,
                "instructorName" to it.instructorName,
                "finalScore" to it.finalScore,
                "feedback" to it.feedback,
                "detailedScores" to mapOf(
                    "perception" to it.detailedScores.perception,
                    "imagination" to it.detailedScores.imagination,
                    "narration" to it.detailedScores.narration,
                    "characterDepiction" to it.detailedScores.characterDepiction,
                    "positivity" to it.detailedScores.positivity
                ),
                "agreedWithAI" to it.agreedWithAI,
                "reviewedAt" to it.reviewedAt,
                "timeSpentMinutes" to it.timeSpentMinutes
            )
        }
    )
}

// ===========================
// TAT Submission Mappers
// ===========================

internal fun TATSubmission.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "id" to id,
        "userId" to userId,
        "testId" to testId,
        "stories" to stories.map { it.toFirestoreMap() },
        "totalTimeTakenMinutes" to totalTimeTakenMinutes,
        "submittedAt" to submittedAt,
        "status" to status.name,
        // IMPORTANT: Do NOT include analysisStatus or olqResult here!
        "instructorScore" to instructorScore?.toFirestoreMap(),
        "gradedByInstructorId" to gradedByInstructorId,
        "gradingTimestamp" to gradingTimestamp
    )
}

internal fun TATStoryResponse.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "questionId" to questionId,
        "story" to story,
        "charactersCount" to charactersCount,
        "viewingTimeTakenSeconds" to viewingTimeTakenSeconds,
        "writingTimeTakenSeconds" to writingTimeTakenSeconds,
        "submittedAt" to submittedAt
    )
}

internal fun TATInstructorScore.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "overallScore" to overallScore,
        "thematicPerceptionScore" to thematicPerceptionScore,
        "imaginationScore" to imaginationScore,
        "characterDepictionScore" to characterDepictionScore,
        "emotionalToneScore" to emotionalToneScore,
        "narrativeStructureScore" to narrativeStructureScore,
        "feedback" to feedback,
        "storyWiseComments" to storyWiseComments,
        "gradedByInstructorId" to gradedByInstructorId,
        "gradedByInstructorName" to gradedByInstructorName,
        "gradedAt" to gradedAt,
        "agreedWithAI" to agreedWithAI
    )
}

// ===========================
// WAT Submission Mappers
// ===========================

internal fun WATSubmission.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "id" to id,
        "userId" to userId,
        "testId" to testId,
        "responses" to responses.map { it.toFirestoreMap() },
        "totalTimeTakenMinutes" to totalTimeTakenMinutes,
        "submittedAt" to submittedAt,
        "status" to status.name,
        // IMPORTANT: Do NOT include analysisStatus or olqResult here!
        "instructorScore" to instructorScore?.toFirestoreMap(),
        "gradedByInstructorId" to gradedByInstructorId,
        "gradingTimestamp" to gradingTimestamp
    )
}

internal fun WATWordResponse.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "wordId" to wordId,
        "word" to word,
        "response" to response,
        "timeTakenSeconds" to timeTakenSeconds,
        "submittedAt" to submittedAt,
        "isSkipped" to isSkipped
    )
}

internal fun WATInstructorScore.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "overallScore" to overallScore,
        "positivityScore" to positivityScore,
        "creativityScore" to creativityScore,
        "speedScore" to speedScore,
        "relevanceScore" to relevanceScore,
        "emotionalMaturityScore" to emotionalMaturityScore,
        "feedback" to feedback,
        "gradedByInstructorId" to gradedByInstructorId,
        "gradedByInstructorName" to gradedByInstructorName,
        "gradedAt" to gradedAt
    )
}

// ===========================
// SRT Submission Mappers
// ===========================

internal fun SRTSubmission.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "id" to id,
        "userId" to userId,
        "testId" to testId,
        "responses" to responses.map { it.toFirestoreMap() },
        "totalTimeTakenMinutes" to totalTimeTakenMinutes,
        "submittedAt" to submittedAt,
        "status" to status.name,
        // IMPORTANT: Do NOT include analysisStatus or olqResult here!
        "instructorScore" to instructorScore?.toFirestoreMap(),
        "gradedByInstructorId" to gradedByInstructorId,
        "gradingTimestamp" to gradingTimestamp
    )
}

internal fun SRTSituationResponse.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "situationId" to situationId,
        "situation" to situation,
        "response" to response,
        "charactersCount" to charactersCount,
        "timeTakenSeconds" to timeTakenSeconds,
        "submittedAt" to submittedAt,
        "isSkipped" to isSkipped
    )
}

internal fun SRTInstructorScore.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "overallScore" to overallScore,
        "leadershipScore" to leadershipScore,
        "decisionMakingScore" to decisionMakingScore,
        "practicalityScore" to practicalityScore,
        "initiativeScore" to initiativeScore,
        "socialResponsibilityScore" to socialResponsibilityScore,
        "feedback" to feedback,
        "categoryWiseComments" to categoryWiseComments.mapKeys { it.key.name },
        "flaggedResponses" to flaggedResponses,
        "exemplaryResponses" to exemplaryResponses,
        "gradedByInstructorId" to gradedByInstructorId,
        "gradedByInstructorName" to gradedByInstructorName,
        "gradedAt" to gradedAt,
        "agreedWithAI" to agreedWithAI
    )
}

// ===========================
// SDT Submission Mappers
// ===========================

internal fun SDTSubmission.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "id" to id,
        "userId" to userId,
        "testId" to testId,
        "responses" to responses.map { it.toFirestoreMap() },
        "totalTimeTakenMinutes" to totalTimeTakenMinutes,
        "submittedAt" to submittedAt,
        "status" to status.name,
        // IMPORTANT: Do NOT include analysisStatus or olqResult here!
        "instructorScore" to instructorScore?.toFirestoreMap(),
        "gradedByInstructorId" to gradedByInstructorId,
        "gradingTimestamp" to gradingTimestamp
    )
}

internal fun SDTQuestionResponse.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "questionId" to questionId,
        "question" to question,
        "answer" to answer,
        "charCount" to charCount,
        "timeTakenSeconds" to timeTakenSeconds,
        "submittedAt" to submittedAt,
        "isSkipped" to isSkipped
    )
}

internal fun SDTInstructorScore.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "overallScore" to overallScore,
        "selfAwarenessScore" to selfAwarenessScore,
        "emotionalMaturityScore" to emotionalMaturityScore,
        "socialPerceptionScore" to socialPerceptionScore,
        "introspectionScore" to introspectionScore,
        "feedback" to feedback,
        "flaggedResponses" to flaggedResponses,
        "exemplaryResponses" to exemplaryResponses,
        "gradedByInstructorId" to gradedByInstructorId,
        "gradedByInstructorName" to gradedByInstructorName,
        "gradedAt" to gradedAt,
        "agreedWithAI" to agreedWithAI
    )
}

// ===========================
// GPE Submission Mappers
// ===========================

internal fun GPESubmission.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "submissionId" to submissionId,
        "questionId" to questionId,
        "userId" to userId,
        "userName" to userName,
        "userEmail" to userEmail,
        "batchId" to batchId,
        "planningResponse" to planningResponse,
        "charactersCount" to charactersCount,
        "viewingTimeTakenSeconds" to viewingTimeTakenSeconds,
        "planningTimeTakenMinutes" to planningTimeTakenMinutes,
        "submittedAt" to submittedAt,
        "status" to status.name,
        "instructorReview" to instructorReview?.let {
            mapOf(
                "reviewId" to it.reviewId,
                "instructorId" to it.instructorId,
                "instructorName" to it.instructorName,
                "finalScore" to it.finalScore,
                "feedback" to it.feedback,
                "detailedScores" to mapOf(
                    "situationAnalysis" to it.detailedScores.situationAnalysis,
                    "planningQuality" to it.detailedScores.planningQuality,
                    "leadership" to it.detailedScores.leadership,
                    "resourceUtilization" to it.detailedScores.resourceUtilization,
                    "practicality" to it.detailedScores.practicality
                ),
                "agreedWithAI" to it.agreedWithAI,
                "reviewedAt" to it.reviewedAt,
                "timeSpentMinutes" to it.timeSpentMinutes
            )
        }
    )
}

// ===========================
// OLQ Parsing Helpers
// ===========================

internal fun parseAnalysisStatus(statusStr: String?): AnalysisStatus {
    return try {
        if (statusStr != null) AnalysisStatus.valueOf(statusStr)
        else AnalysisStatus.PENDING_ANALYSIS
    } catch (e: Exception) {
        AnalysisStatus.PENDING_ANALYSIS
    }
}

@Suppress("UNCHECKED_CAST")
internal fun parseOLQResult(olqResultMap: Map<*, *>?): OLQAnalysisResult? {
    if (olqResultMap == null) return null
    return try {
        val olqScoresMap = olqResultMap["olqScores"] as? Map<*, *> ?: emptyMap<String, Any>()
        val olqScores = parseOLQScoresMap(olqScoresMap)

        val testTypeName = olqResultMap["testType"] as? String ?: "PPDT"
        val testType = try { TestType.valueOf(testTypeName) } catch (e: Exception) { TestType.PPDT }
        
        OLQAnalysisResult(
            submissionId = olqResultMap["submissionId"] as? String ?: "",
            testType = testType,
            olqScores = olqScores,
            overallScore = (olqResultMap["overallScore"] as? Number)?.toFloat() ?: 0f,
            overallRating = olqResultMap["overallRating"] as? String ?: "",
            strengths = (olqResultMap["strengths"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            weaknesses = (olqResultMap["weaknesses"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            recommendations = (olqResultMap["recommendations"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            analyzedAt = (olqResultMap["analyzedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            aiConfidence = (olqResultMap["aiConfidence"] as? Number)?.toInt() ?: 0
        )
    } catch (e: Exception) {
        Log.e(TAG, "Failed to parse OLQ result", e)
        null
    }
}

private fun parseOLQScoresMap(olqScoresMap: Map<*, *>): Map<OLQ, OLQScore> {
    val olqScores = mutableMapOf<OLQ, OLQScore>()
    for ((key, value) in olqScoresMap) {
        val olqName = key as? String ?: continue
        val scoreMap = value as? Map<*, *> ?: continue
        val olq = try { OLQ.valueOf(olqName) } catch (e: Exception) { continue }
        olqScores[olq] = OLQScore(
            score = (scoreMap["score"] as? Number)?.toInt() ?: 0,
            confidence = (scoreMap["confidence"] as? Number)?.toInt() ?: 0,
            reasoning = scoreMap["reasoning"] as? String ?: ""
        )
    }
    return olqScores
}

// ===========================
// PIQ Submission Parsers
// ===========================

/**
 * Parse PIQ submission from Firestore map.
 * Extracted from FirestoreSubmissionRepository during Phase 4 refactoring.
 */
private fun piqListOfMaps(data: Map<*, *>, key: String): List<Map<*, *>> =
    (data[key] as? List<*>)?.mapNotNull { it as? Map<*, *> } ?: emptyList()

private fun parsePIQSiblings(data: Map<*, *>): List<Sibling> = piqListOfMaps(data, "siblings").map {
    Sibling(
        id = it["id"] as? String ?: "",
        name = it["name"] as? String ?: "",
        age = it["age"] as? String ?: "",
        occupation = it["occupation"] as? String ?: "",
        education = it["education"] as? String ?: "",
        income = it["income"] as? String ?: ""
    )
}

private fun parsePIQEducation(data: Map<*, *>, key: String, level: String): Education {
    val eduMap = data[key] as? Map<*, *> ?: emptyMap<String, Any>()
    return Education(
        level = level,
        institution = eduMap["institution"] as? String ?: "",
        board = eduMap["board"] as? String ?: "",
        stream = eduMap["stream"] as? String ?: "",
        year = eduMap["year"] as? String ?: "",
        percentage = eduMap["percentage"] as? String ?: "",
        cgpa = eduMap["cgpa"] as? String ?: "",
        mediumOfInstruction = eduMap["mediumOfInstruction"] as? String ?: "",
        boarderDayScholar = eduMap["boarderDayScholar"] as? String ?: "",
        outstandingAchievement = eduMap["outstandingAchievement"] as? String ?: ""
    )
}

private fun parsePIQSportsParticipation(data: Map<*, *>): List<SportsParticipation> =
    piqListOfMaps(data, "sportsParticipation").map {
        SportsParticipation(
            id = it["id"] as? String ?: "",
            sport = it["sport"] as? String ?: "",
            period = it["period"] as? String ?: "",
            representedInstitution = it["representedInstitution"] as? String ?: "",
            outstandingAchievement = it["outstandingAchievement"] as? String ?: ""
        )
    }

private fun parsePIQExtraCurricularActivities(data: Map<*, *>): List<ExtraCurricularActivity> =
    piqListOfMaps(data, "extraCurricularActivities").map {
        ExtraCurricularActivity(
            id = it["id"] as? String ?: "",
            activityName = it["activityName"] as? String ?: "",
            duration = it["duration"] as? String ?: "",
            outstandingAchievement = it["outstandingAchievement"] as? String ?: ""
        )
    }

private fun parsePIQWorkExperience(data: Map<*, *>): List<WorkExperience> =
    piqListOfMaps(data, "workExperience").map {
        WorkExperience(
            id = it["id"] as? String ?: "",
            company = it["company"] as? String ?: "",
            role = it["role"] as? String ?: "",
            duration = it["duration"] as? String ?: "",
            description = it["description"] as? String ?: ""
        )
    }

private fun parsePIQNCCTraining(data: Map<*, *>): NCCTraining {
    val nccTrainingMap = data["nccTraining"] as? Map<*, *> ?: emptyMap<String, Any>()
    return NCCTraining(
        hasTraining = nccTrainingMap["hasTraining"] as? Boolean ?: false,
        totalTraining = nccTrainingMap["totalTraining"] as? String ?: "",
        wing = nccTrainingMap["wing"] as? String ?: "",
        division = nccTrainingMap["division"] as? String ?: "",
        certificateObtained = nccTrainingMap["certificateObtained"] as? String ?: ""
    )
}

private fun parsePIQPreviousInterviews(data: Map<*, *>): List<PreviousInterview> =
    piqListOfMaps(data, "previousInterviews").map {
        PreviousInterview(
            id = it["id"] as? String ?: "",
            typeOfEntry = it["typeOfEntry"] as? String ?: "",
            ssbNumber = it["ssbNumber"] as? String ?: "",
            ssbPlace = it["ssbPlace"] as? String ?: "",
            date = it["date"] as? String ?: "",
            chestNumber = it["chestNumber"] as? String ?: "",
            batchNumber = it["batchNumber"] as? String ?: ""
        )
    }

/** Name/contact fields — grouped (and split from location/residence) purely to keep [parsePIQSubmission] short. */
private data class PIQIdentityFields(
    val oirNumber: String,
    val selectionBoard: String,
    val batchNumber: String,
    val chestNumber: String,
    val upscRollNumber: String,
    val fullName: String,
    val dateOfBirth: String,
    val age: String,
    val gender: String,
    val phone: String,
    val email: String
)

private fun parsePIQIdentityFields(data: Map<*, *>) = PIQIdentityFields(
    oirNumber = data["oirNumber"] as? String ?: "",
    selectionBoard = data["selectionBoard"] as? String ?: "",
    batchNumber = data["batchNumber"] as? String ?: "",
    chestNumber = data["chestNumber"] as? String ?: "",
    upscRollNumber = data["upscRollNumber"] as? String ?: "",
    fullName = data["fullName"] as? String ?: "",
    dateOfBirth = data["dateOfBirth"] as? String ?: "",
    age = data["age"] as? String ?: "",
    gender = data["gender"] as? String ?: "",
    phone = data["phone"] as? String ?: "",
    email = data["email"] as? String ?: ""
)

/** Location fields — split from [PIQIdentityFields] purely to keep [parsePIQSubmission] short. */
private data class PIQLocationFields(
    val state: String,
    val district: String,
    val religion: String,
    val scStObcStatus: String,
    val motherTongue: String,
    val maritalStatus: String,
    val permanentAddress: String,
    val presentAddress: String,
    val maximumResidence: String
)

private fun parsePIQLocationFields(data: Map<*, *>) = PIQLocationFields(
    state = data["state"] as? String ?: "",
    district = data["district"] as? String ?: "",
    religion = data["religion"] as? String ?: "",
    scStObcStatus = data["scStObcStatus"] as? String ?: "",
    motherTongue = data["motherTongue"] as? String ?: "",
    maritalStatus = data["maritalStatus"] as? String ?: "",
    permanentAddress = data["permanentAddress"] as? String ?: "",
    presentAddress = data["presentAddress"] as? String ?: "",
    maximumResidence = data["maximumResidence"] as? String ?: ""
)

/** Residence-stats fields — split from [PIQLocationFields] purely to keep [parsePIQSubmission] short. */
private data class PIQResidenceStatsFields(
    val maximumResidencePopulation: String,
    val presentResidencePopulation: String,
    val permanentResidencePopulation: String,
    val isDistrictHQ: Boolean,
    val height: String,
    val weight: String
)

private fun parsePIQResidenceStatsFields(data: Map<*, *>) = PIQResidenceStatsFields(
    maximumResidencePopulation = data["maximumResidencePopulation"] as? String ?: "",
    presentResidencePopulation = data["presentResidencePopulation"] as? String ?: "",
    permanentResidencePopulation = data["permanentResidencePopulation"] as? String ?: "",
    isDistrictHQ = data["isDistrictHQ"] as? Boolean ?: false,
    height = data["height"] as? String ?: "",
    weight = data["weight"] as? String ?: ""
)

/** Parent fields — grouped (and split from guardian/sibling fields) purely to keep [parsePIQSubmission] short. */
private data class PIQParentFields(
    val fatherName: String,
    val fatherOccupation: String,
    val fatherEducation: String,
    val fatherIncome: String,
    val motherName: String,
    val motherOccupation: String,
    val motherEducation: String
)

private fun parsePIQParentFields(data: Map<*, *>) = PIQParentFields(
    fatherName = data["fatherName"] as? String ?: "",
    fatherOccupation = data["fatherOccupation"] as? String ?: "",
    fatherEducation = data["fatherEducation"] as? String ?: "",
    fatherIncome = data["fatherIncome"] as? String ?: "",
    motherName = data["motherName"] as? String ?: "",
    motherOccupation = data["motherOccupation"] as? String ?: "",
    motherEducation = data["motherEducation"] as? String ?: ""
)

/** Guardian-status fields — split from [PIQParentFields] purely to keep [parsePIQSubmission] short. */
private data class PIQGuardianFields(
    val parentsAlive: String,
    val ageAtFatherDeath: String,
    val ageAtMotherDeath: String,
    val guardianName: String,
    val guardianOccupation: String,
    val guardianEducation: String,
    val guardianIncome: String
)

private fun parsePIQGuardianFields(data: Map<*, *>) = PIQGuardianFields(
    parentsAlive = data["parentsAlive"] as? String ?: "",
    ageAtFatherDeath = data["ageAtFatherDeath"] as? String ?: "",
    ageAtMotherDeath = data["ageAtMotherDeath"] as? String ?: "",
    guardianName = data["guardianName"] as? String ?: "",
    guardianOccupation = data["guardianOccupation"] as? String ?: "",
    guardianEducation = data["guardianEducation"] as? String ?: "",
    guardianIncome = data["guardianIncome"] as? String ?: ""
)

/** Career/background fields — grouped purely to keep [parsePIQSubmission] short. */
private data class PIQBackgroundFields(
    val presentOccupation: String,
    val personalMonthlyIncome: String,
    val hobbies: String,
    val sports: String,
    val positionsOfResponsibility: String,
    val natureOfCommission: String,
    val choiceOfService: String,
    val chancesAvailed: String,
    val whyDefenseForces: String,
    val strengths: String,
    val weaknesses: String
)

private fun parsePIQBackgroundFields(data: Map<*, *>) = PIQBackgroundFields(
    presentOccupation = data["presentOccupation"] as? String ?: "",
    personalMonthlyIncome = data["personalMonthlyIncome"] as? String ?: "",
    hobbies = data["hobbies"] as? String ?: "",
    sports = data["sports"] as? String ?: "",
    positionsOfResponsibility = data["positionsOfResponsibility"] as? String ?: "",
    natureOfCommission = data["natureOfCommission"] as? String ?: "",
    choiceOfService = data["choiceOfService"] as? String ?: "",
    chancesAvailed = data["chancesAvailed"] as? String ?: "",
    whyDefenseForces = data["whyDefenseForces"] as? String ?: "",
    strengths = data["strengths"] as? String ?: "",
    weaknesses = data["weaknesses"] as? String ?: ""
)

/**
 * Parse PIQ submission from Firestore map.
 * Extracted from FirestoreSubmissionRepository during Phase 4 refactoring.
 */
internal fun parsePIQSubmission(data: Map<*, *>): PIQSubmission {
    val identity = parsePIQIdentityFields(data)
    val parents = parsePIQParentFields(data)
    val guardian = parsePIQGuardianFields(data)
    val background = parsePIQBackgroundFields(data)
    val location = parsePIQLocationFields(data)
    val residenceStats = parsePIQResidenceStatsFields(data)

    return PIQSubmission(
        id = data["id"] as? String ?: "",
        userId = data["userId"] as? String ?: "",
        testId = data["testId"] as? String ?: "piq_standard",
        oirNumber = identity.oirNumber,
        selectionBoard = identity.selectionBoard,
        batchNumber = identity.batchNumber,
        chestNumber = identity.chestNumber,
        upscRollNumber = identity.upscRollNumber,
        fullName = identity.fullName,
        dateOfBirth = identity.dateOfBirth,
        age = identity.age,
        gender = identity.gender,
        phone = identity.phone,
        email = identity.email,
        state = location.state,
        district = location.district,
        religion = location.religion,
        scStObcStatus = location.scStObcStatus,
        motherTongue = location.motherTongue,
        maritalStatus = location.maritalStatus,
        permanentAddress = location.permanentAddress,
        presentAddress = location.presentAddress,
        maximumResidence = location.maximumResidence,
        maximumResidencePopulation = residenceStats.maximumResidencePopulation,
        presentResidencePopulation = residenceStats.presentResidencePopulation,
        permanentResidencePopulation = residenceStats.permanentResidencePopulation,
        isDistrictHQ = residenceStats.isDistrictHQ,
        height = residenceStats.height,
        weight = residenceStats.weight,
        fatherName = parents.fatherName,
        fatherOccupation = parents.fatherOccupation,
        fatherEducation = parents.fatherEducation,
        fatherIncome = parents.fatherIncome,
        motherName = parents.motherName,
        motherOccupation = parents.motherOccupation,
        motherEducation = parents.motherEducation,
        parentsAlive = guardian.parentsAlive,
        ageAtFatherDeath = guardian.ageAtFatherDeath,
        ageAtMotherDeath = guardian.ageAtMotherDeath,
        guardianName = guardian.guardianName,
        guardianOccupation = guardian.guardianOccupation,
        guardianEducation = guardian.guardianEducation,
        guardianIncome = guardian.guardianIncome,
        siblings = parsePIQSiblings(data),
        presentOccupation = background.presentOccupation,
        personalMonthlyIncome = background.personalMonthlyIncome,
        education10th = parsePIQEducation(data, "education10th", "10th"),
        education12th = parsePIQEducation(data, "education12th", "12th"),
        educationGraduation = parsePIQEducation(data, "educationGraduation", "Graduation"),
        educationPostGraduation = parsePIQEducation(data, "educationPostGraduation", "Post-Graduation"),
        hobbies = background.hobbies,
        sports = background.sports,
        sportsParticipation = parsePIQSportsParticipation(data),
        extraCurricularActivities = parsePIQExtraCurricularActivities(data),
        positionsOfResponsibility = background.positionsOfResponsibility,
        workExperience = parsePIQWorkExperience(data),
        nccTraining = parsePIQNCCTraining(data),
        natureOfCommission = background.natureOfCommission,
        choiceOfService = background.choiceOfService,
        chancesAvailed = background.chancesAvailed,
        previousInterviews = parsePIQPreviousInterviews(data),
        whyDefenseForces = background.whyDefenseForces,
        strengths = background.strengths,
        weaknesses = background.weaknesses,
        status = SubmissionStatus.valueOf(data["status"] as? String ?: "SUBMITTED"),
        submittedAt = (data["submittedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        lastModifiedAt = (data["lastModifiedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        gradedByInstructorId = data["gradedByInstructorId"] as? String,
        gradingTimestamp = (data["gradingTimestamp"] as? Number)?.toLong(),
    )
}

