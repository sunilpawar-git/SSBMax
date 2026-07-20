package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.CategoryScore
import com.ssbmax.shared.domain.model.DifficultyScore
import com.ssbmax.shared.domain.model.OIRQuestionType
import com.ssbmax.shared.domain.model.OIRSubmission
import com.ssbmax.shared.domain.model.OIRTestResult
import com.ssbmax.shared.domain.model.PPDTDetailedScores
import com.ssbmax.shared.domain.model.PPDTInstructorReview
import com.ssbmax.shared.domain.model.PPDTSubmission
import com.ssbmax.shared.domain.model.QuestionDifficulty
import com.ssbmax.shared.domain.model.SubmissionStatus
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.model.scoring.AnalysisStatus
import com.ssbmax.shared.domain.model.scoring.OLQAnalysisResult
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.Source
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

/**
 * GitLive-Firebase-backed port of the Android `PersonalTestSubmissionRepository` facade + its
 * `PPDTPersonalSubmissionDataSource`/`OIRPersonalSubmissionDataSource` delegates (PPDT and OIR
 * only — see class doc below for why PIQ is deferred).
 *
 * **PIQ explicitly NOT ported this slice.** `PIQPersonalSubmissionDataSource` + `PIQSubmission`
 * (`core/domain/model/PIQTest.kt`) is a ~90-field flat form model (personal/family/education/
 * NCC/interview-history sections) with a matching ~90-key `toMap()`/`parsePIQSubmission()` pair.
 * The port pattern is entirely mechanical (identical to the OIR/PPDT DTOs below, just far larger),
 * so it isn't blocked on anything — it's deferred because rushing a 90-field DTO+mapper under this
 * slice's remaining budget risks silent field-name typos that would only surface as missing PIQ
 * data in production, which is a worse outcome than an honest, named deferral. `submitPIQ`/
 * `getLatestPIQSubmission` are not present on this class; the facade
 * (`GitLiveSubmissionRepository`) returns `Result.failure` with this same explanation for both.
 */
class GitLivePersonalTestSubmissionRepository {

    private val submissionsCollection = Firebase.firestore.collection(SUBMISSIONS_COLLECTION)
    private val ppdtResultsCollection = Firebase.firestore.collection(PPDT_RESULTS_COLLECTION)

    // ===========================
    // PPDT
    // ===========================

    suspend fun submitPPDT(submission: PPDTSubmission, batchId: String?): Result<String> = try {
        val doc = SubmissionDocDto(
            id = submission.submissionId,
            userId = submission.userId,
            testId = submission.questionId,
            testType = TestType.PPDT.name,
            status = submission.status.name,
            submittedAt = submission.submittedAt,
            batchId = batchId,
            data = submission.toDataDto()
        )
        submissionsCollection.document(submission.submissionId).set(doc)
        Result.success(submission.submissionId)
    } catch (e: Exception) {
        Result.failure(Exception("Failed to submit PPDT: ${e.message}", e))
    }

    suspend fun getPPDTSubmission(submissionId: String): Result<PPDTSubmission?> = try {
        val snapshot = getWithServerThenCacheFallback { submissionsCollection.document(submissionId).get(it) }
        if (!snapshot.exists) Result.success(null)
        else Result.success(snapshot.data(SubmissionDocDto.serializer(PPDTDataDto.serializer())).data.toDomain())
    } catch (e: Exception) {
        Result.failure(Exception("Failed to get PPDT submission: ${e.message}", e))
    }

    suspend fun getLatestPPDTSubmission(userId: String): Result<PPDTSubmission?> = try {
        val query = submissionsCollection
            .where { FIELD_USER_ID equalTo userId }
            .where { FIELD_TEST_TYPE equalTo TestType.PPDT.name }
            .orderBy(FIELD_SUBMITTED_AT, Direction.DESCENDING)
            .limit(1)
        val snapshot = getWithServerThenCacheFallback { query.get(it) }
        val doc = snapshot.documents.firstOrNull()
        if (doc == null) {
            Result.success(null)
        } else {
            Result.success(doc.data(SubmissionDocDto.serializer(PPDTDataDto.serializer())).data.toDomain())
        }
    } catch (e: Exception) {
        Result.failure(Exception("Failed to get latest PPDT submission: ${e.message}", e))
    }

    suspend fun updatePPDTAnalysisStatus(submissionId: String, status: AnalysisStatus): Result<Unit> = try {
        submissionsCollection.document(submissionId).update("$FIELD_DATA.analysisStatus" to status.name)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(Exception("Failed to update PPDT status: ${e.message}", e))
    }

    /**
     * Writes to `ppdt_results` and flips the submission to COMPLETED in one batch — same "GTO
     * pattern" (a dedicated results collection, not a nested `data.olqResult`) the Android
     * original uses for PPDT specifically, distinct from TAT/WAT/SRT/SDT's `psych_results`.
     */
    suspend fun updatePPDTOLQResult(submissionId: String, olqResult: OLQAnalysisResult): Result<Unit> = try {
        val submissionDoc = submissionsCollection.document(submissionId).get()
        val userId = submissionDoc.data(SubmissionDocDto.serializer(PPDTDataDto.serializer())).userId
        if (userId.isEmpty()) {
            Result.failure(Exception("userId not found in submission"))
        } else {
            val batch = Firebase.firestore.batch()
            batch.set(ppdtResultsCollection.document(submissionId), olqResult.toDto(userId = userId))
            batch.update(
                submissionsCollection.document(submissionId),
                "$FIELD_DATA.analysisStatus" to SubmissionConstants.ANALYSIS_STATUS_COMPLETED,
                FIELD_STATUS to "COMPLETED"
            )
            batch.commit()
            Result.success(Unit)
        }
    } catch (e: Exception) {
        Result.failure(Exception("Failed to update PPDT OLQ result: ${e.message}", e))
    }

    suspend fun getPPDTResult(submissionId: String): Result<OLQAnalysisResult?> = try {
        val doc = getWithServerThenCacheFallback { ppdtResultsCollection.document(submissionId).get(it) }
        if (!doc.exists) Result.success(null)
        else Result.success(doc.data(OLQAnalysisResultDto.serializer()).toDomain())
    } catch (e: Exception) {
        Result.failure(Exception("Error fetching PPDT result: ${e.message}", e))
    }

    fun observePPDTSubmission(submissionId: String): Flow<PPDTSubmission?> =
        submissionsCollection.document(submissionId).snapshots
            .map { snapshot ->
                if (!snapshot.exists) return@map null
                val dto = snapshot.data(SubmissionDocDto.serializer(PPDTDataDto.serializer()))
                val filter = ppdtRegressionFilters.getOrPut(submissionId) { OLQRegressionFilter() }
                if (filter.shouldFilterSnapshot(dto.data.analysisStatus, dto.data.olqResult != null, snapshot.metadata)) return@map null
                dto.data.toDomain()
            }
            .catch { emit(null) }

    // ===========================
    // OIR
    // ===========================

    suspend fun submitOIR(submission: OIRSubmission, batchId: String?): Result<String> = try {
        val doc = SubmissionDocDto(
            id = submission.id,
            userId = submission.userId,
            testId = submission.testId,
            testType = TestType.OIR.name,
            status = submission.status.name,
            submittedAt = submission.submittedAt,
            gradedByInstructorId = submission.gradedByInstructorId,
            gradingTimestamp = submission.gradingTimestamp,
            batchId = batchId,
            data = submission.toDataDto()
        )
        submissionsCollection.document(submission.id).set(doc, merge = true)
        Result.success(submission.id)
    } catch (e: Exception) {
        Result.failure(Exception("Failed to submit OIR: ${e.message}", e))
    }

    suspend fun getLatestOIRSubmission(userId: String): Result<OIRSubmission?> = try {
        val query = submissionsCollection
            .where { FIELD_USER_ID equalTo userId }
            .where { FIELD_TEST_TYPE equalTo TestType.OIR.name }
            .orderBy(FIELD_SUBMITTED_AT, Direction.DESCENDING)
            .limit(1)
        val snapshot = getWithServerThenCacheFallback { query.get(it) }
        val doc = snapshot.documents.firstOrNull()
        if (doc == null) {
            Result.success(null)
        } else {
            Result.success(doc.data(SubmissionDocDto.serializer(OIRDataDto.serializer())).data.toDomain())
        }
    } catch (e: Exception) {
        Result.failure(Exception("Failed to get latest OIR submission: ${e.message}", e))
    }

    // ===========================
    // Shared helpers
    // ===========================

    /**
     * Same server-then-cache-fallback pattern as the Android original (a plain `.get()` prefers
     * server data but silently returns cache on any offline/timeout failure).
     */
    private suspend fun <T> getWithServerThenCacheFallback(fetch: suspend (Source) -> T): T =
        try {
            fetch(Source.SERVER)
        } catch (e: Exception) {
            fetch(Source.CACHE)
        }

    private val ppdtRegressionFilters = mutableMapOf<String, OLQRegressionFilter>()

    private companion object {
        const val SUBMISSIONS_COLLECTION = "submissions"
        const val PPDT_RESULTS_COLLECTION = "ppdt_results"
        const val FIELD_USER_ID = "userId"
        const val FIELD_TEST_TYPE = "testType"
        const val FIELD_SUBMITTED_AT = "submittedAt"
        const val FIELD_STATUS = "status"
        const val FIELD_DATA = "data"
    }
}

// ===========================
// PPDT DTOs
// ===========================

@Serializable
internal data class PPDTDataDto(
    val submissionId: String = "",
    val questionId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val batchId: String? = null,
    val story: String = "",
    val charactersCount: Int = 0,
    val viewingTimeTakenSeconds: Int = 0,
    val writingTimeTakenMinutes: Int = 0,
    val submittedAt: Long = 0L,
    val status: String = "",
    val instructorReview: PPDTInstructorReviewDto? = null,
    // Not written by submitPPDT (see the Android original's "IMPORTANT: do NOT include" comment) —
    // only ever populated later, in place, by updatePPDTAnalysisStatus.
    val analysisStatus: String? = null,
    val olqResult: OLQAnalysisResultDto? = null
)

@Serializable
internal data class PPDTInstructorReviewDto(
    val reviewId: String = "",
    val instructorId: String = "",
    val instructorName: String = "",
    val finalScore: Float = 0f,
    val feedback: String = "",
    val detailedScores: PPDTDetailedScoresDto = PPDTDetailedScoresDto(),
    val agreedWithAI: Boolean = false,
    val reviewedAt: Long = 0L,
    val timeSpentMinutes: Int = 0
)

@Serializable
internal data class PPDTDetailedScoresDto(
    val perception: Float = 0f,
    val imagination: Float = 0f,
    val narration: Float = 0f,
    val characterDepiction: Float = 0f,
    val positivity: Float = 0f
)

internal fun PPDTSubmission.toDataDto() = PPDTDataDto(
    submissionId = submissionId,
    questionId = questionId,
    userId = userId,
    userName = userName,
    userEmail = userEmail,
    batchId = batchId,
    story = story,
    charactersCount = charactersCount,
    viewingTimeTakenSeconds = viewingTimeTakenSeconds,
    writingTimeTakenMinutes = writingTimeTakenMinutes,
    submittedAt = submittedAt,
    status = status.name,
    instructorReview = instructorReview?.let {
        PPDTInstructorReviewDto(
            reviewId = it.reviewId,
            instructorId = it.instructorId,
            instructorName = it.instructorName,
            finalScore = it.finalScore,
            feedback = it.feedback,
            detailedScores = PPDTDetailedScoresDto(
                perception = it.detailedScores.perception,
                imagination = it.detailedScores.imagination,
                narration = it.detailedScores.narration,
                characterDepiction = it.detailedScores.characterDepiction,
                positivity = it.detailedScores.positivity
            ),
            agreedWithAI = it.agreedWithAI,
            reviewedAt = it.reviewedAt,
            timeSpentMinutes = it.timeSpentMinutes
        )
    }
)

internal fun PPDTDataDto.toDomain(): PPDTSubmission = PPDTSubmission(
    submissionId = submissionId,
    questionId = questionId,
    userId = userId,
    userName = userName,
    userEmail = userEmail,
    batchId = batchId,
    story = story,
    charactersCount = charactersCount,
    viewingTimeTakenSeconds = viewingTimeTakenSeconds,
    writingTimeTakenMinutes = writingTimeTakenMinutes,
    submittedAt = submittedAt,
    status = runCatching { SubmissionStatus.valueOf(status) }.getOrDefault(SubmissionStatus.SUBMITTED_PENDING_REVIEW),
    instructorReview = instructorReview?.let {
        PPDTInstructorReview(
            reviewId = it.reviewId,
            instructorId = it.instructorId,
            instructorName = it.instructorName,
            finalScore = it.finalScore,
            feedback = it.feedback,
            detailedScores = PPDTDetailedScores(
                perception = it.detailedScores.perception,
                imagination = it.detailedScores.imagination,
                narration = it.detailedScores.narration,
                characterDepiction = it.detailedScores.characterDepiction,
                positivity = it.detailedScores.positivity
            ),
            agreedWithAI = it.agreedWithAI,
            reviewedAt = it.reviewedAt,
            timeSpentMinutes = it.timeSpentMinutes
        )
    },
    analysisStatus = analysisStatus?.let { runCatching { AnalysisStatus.valueOf(it) }.getOrDefault(AnalysisStatus.PENDING_ANALYSIS) } ?: AnalysisStatus.PENDING_ANALYSIS,
    olqResult = olqResult?.toDomain()
)

// ===========================
// OIR DTOs
// ===========================

@Serializable
internal data class OIRDataDto(
    val id: String = "",
    val userId: String = "",
    val testId: String = "",
    val testResult: OIRSubmissionTestResultDto = OIRSubmissionTestResultDto(),
    val submittedAt: Long = 0L,
    val status: String = "",
    val gradedByInstructorId: String? = null,
    val gradingTimestamp: Long? = null
)

@Serializable
internal data class OIRSubmissionTestResultDto(
    val testId: String = "",
    val sessionId: String = "",
    val userId: String = "",
    val totalQuestions: Int = 0,
    val correctAnswers: Int = 0,
    val incorrectAnswers: Int = 0,
    val skippedQuestions: Int = 0,
    val totalTimeSeconds: Int = 0,
    val timeTakenSeconds: Int = 0,
    val rawScore: Int = 0,
    val percentageScore: Float = 0f,
    val categoryScores: Map<String, CategoryScoreDto> = emptyMap(),
    val difficultyBreakdown: Map<String, DifficultyScoreDto> = emptyMap(),
    val completedAt: Long = 0L,
    val passed: Boolean = false,
    val grade: String = "C"
)

@Serializable
internal data class CategoryScoreDto(
    val totalQuestions: Int = 0,
    val correctAnswers: Int = 0,
    val percentage: Float = 0f,
    val averageTimeSeconds: Int = 0
)

@Serializable
internal data class DifficultyScoreDto(
    val totalQuestions: Int = 0,
    val correctAnswers: Int = 0,
    val percentage: Float = 0f
)

/**
 * Same real limitation as the Android original's `OIRPersonalSubmissionDataSource.parseOIRSubmission`:
 * per-question `answeredQuestions` are never round-tripped through this write path (the Android
 * `toFirestoreMap()` writes them, but `parseOIRSubmission` never reads them back — always
 * `emptyList()`). Ported faithfully, not fixed, since fixing it is a real behavior change out of
 * this slice's scope.
 */
internal fun OIRSubmission.toDataDto() = OIRDataDto(
    id = id,
    userId = userId,
    testId = testId,
    testResult = OIRSubmissionTestResultDto(
        testId = testResult.testId,
        sessionId = testResult.sessionId,
        userId = testResult.userId,
        totalQuestions = testResult.totalQuestions,
        correctAnswers = testResult.correctAnswers,
        incorrectAnswers = testResult.incorrectAnswers,
        skippedQuestions = testResult.skippedQuestions,
        totalTimeSeconds = testResult.totalTimeSeconds,
        timeTakenSeconds = testResult.timeTakenSeconds,
        rawScore = testResult.rawScore,
        percentageScore = testResult.percentageScore,
        categoryScores = testResult.categoryScores.entries.associate { (category, score) ->
            category.name to CategoryScoreDto(score.totalQuestions, score.correctAnswers, score.percentage, score.averageTimeSeconds)
        },
        difficultyBreakdown = testResult.difficultyBreakdown.entries.associate { (difficulty, score) ->
            difficulty.name to DifficultyScoreDto(score.totalQuestions, score.correctAnswers, score.percentage)
        },
        completedAt = testResult.completedAt,
        passed = testResult.passed,
        grade = testResult.grade.name
    ),
    submittedAt = submittedAt,
    status = status.name,
    gradedByInstructorId = gradedByInstructorId,
    gradingTimestamp = gradingTimestamp
)

internal fun OIRDataDto.toDomain(): OIRSubmission = OIRSubmission(
    id = id,
    userId = userId,
    testId = testId,
    testResult = OIRTestResult(
        testId = testResult.testId,
        sessionId = testResult.sessionId,
        userId = testResult.userId,
        totalQuestions = testResult.totalQuestions,
        correctAnswers = testResult.correctAnswers,
        incorrectAnswers = testResult.incorrectAnswers,
        skippedQuestions = testResult.skippedQuestions,
        totalTimeSeconds = testResult.totalTimeSeconds,
        timeTakenSeconds = testResult.timeTakenSeconds,
        rawScore = testResult.rawScore,
        percentageScore = testResult.percentageScore,
        categoryScores = testResult.categoryScores.mapNotNull { (key, score) ->
            runCatching { OIRQuestionType.valueOf(key) }.getOrNull()?.let {
                it to CategoryScore(it, score.totalQuestions, score.correctAnswers, score.percentage, score.averageTimeSeconds)
            }
        }.toMap(),
        difficultyBreakdown = testResult.difficultyBreakdown.mapNotNull { (key, score) ->
            runCatching { QuestionDifficulty.valueOf(key) }.getOrNull()?.let {
                it to DifficultyScore(it, score.totalQuestions, score.correctAnswers, score.percentage)
            }
        }.toMap(),
        answeredQuestions = emptyList(),
        completedAt = testResult.completedAt
    ),
    submittedAt = submittedAt,
    status = runCatching { SubmissionStatus.valueOf(status) }.getOrDefault(SubmissionStatus.SUBMITTED_PENDING_REVIEW),
    gradedByInstructorId = gradedByInstructorId,
    gradingTimestamp = gradingTimestamp
)
