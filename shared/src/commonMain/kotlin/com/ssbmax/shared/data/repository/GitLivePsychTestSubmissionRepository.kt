package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.SDTInstructorScore
import com.ssbmax.shared.domain.model.SDTQuestionResponse
import com.ssbmax.shared.domain.model.SDTSubmission
import com.ssbmax.shared.domain.model.SRTCategory
import com.ssbmax.shared.domain.model.SRTInstructorScore
import com.ssbmax.shared.domain.model.SRTSituationResponse
import com.ssbmax.shared.domain.model.SRTSubmission
import com.ssbmax.shared.domain.model.SubmissionStatus
import com.ssbmax.shared.domain.model.TATInstructorScore
import com.ssbmax.shared.domain.model.TATStoryResponse
import com.ssbmax.shared.domain.model.TATSubmission
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.model.WATInstructorScore
import com.ssbmax.shared.domain.model.WATWordResponse
import com.ssbmax.shared.domain.model.WATSubmission
import com.ssbmax.shared.domain.model.scoring.AnalysisStatus
import com.ssbmax.shared.domain.model.scoring.OLQAnalysisResult
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

/**
 * GitLive-Firebase-backed port of the Android psychology-test submission cluster
 * (`PsychTestSubmissionRepository` + its four delegates `TATSubmissionRepository`/
 * `WATSubmissionRepository`/`SRTSubmissionRepository`/`SDTSubmissionRepository`, plus the shared
 * `PsychTestMapper`/`OLQMapper`). Merged into one class here — Koin doesn't need the Hilt
 * DI-boundary reason the Android originals were split for (300-line-file limit per class), same
 * merge rationale as the 6th slice's `StudyContentRepositoryImpl`+`FirestoreContentSource` merge.
 *
 * Same "submissions"/"psych_results" collections as the Android original. One real, faithfully
 * preserved quirk: the Android `toFirestoreMap()` functions duplicate `id`/`userId`/`testId`/
 * `status`/`submittedAt`/`gradedByInstructorId`/`gradingTimestamp` **inside** the nested `data`
 * sub-map as well as at the document's top level — and `PsychTestMapper.parseXxxSubmission` reads
 * exclusively from the nested copy, never the top-level one. This port reproduces that exact
 * shape (`SubmissionDocDto<XxxDataDto>` for writes/queries, decoding `.data` for the domain model)
 * rather than "fixing" the duplication, since changing it would break compatibility with documents
 * already written by the Android app.
 *
 * TAT alone uses the newer atomic `finalizeTATAnalysisResult` (writes to `psych_results` first,
 * then marks the submission COMPLETED with result metadata) — WAT/SRT/SDT still use the older
 * two-step `updateXxxOLQResult`, exactly matching the asymmetry already present in the Android
 * originals (see the domain `SubmissionRepository` interface, which only declares
 * `finalizeTATAnalysisResult` for TAT).
 */
class GitLivePsychTestSubmissionRepository {

    private val submissionsCollection = Firebase.firestore.collection(SUBMISSIONS_COLLECTION)
    private val psychResultsCollection = Firebase.firestore.collection(PSYCH_RESULTS_COLLECTION)

    // ===========================
    // TAT
    // ===========================

    suspend fun submitTAT(submission: TATSubmission, batchId: String?): Result<String> = try {
        val doc = SubmissionDocDto(
            id = submission.id,
            userId = submission.userId,
            testId = submission.testId,
            testType = TestType.TAT.name,
            status = submission.status.name,
            submittedAt = submission.submittedAt,
            gradedByInstructorId = submission.gradedByInstructorId,
            gradingTimestamp = submission.gradingTimestamp,
            batchId = batchId,
            data = TATDataDto(
                id = submission.id,
                userId = submission.userId,
                testId = submission.testId,
                stories = submission.stories.map { it.toDto() },
                totalTimeTakenMinutes = submission.totalTimeTakenMinutes,
                submittedAt = submission.submittedAt,
                status = submission.status.name,
                instructorScore = submission.instructorScore?.toDto(),
                gradedByInstructorId = submission.gradedByInstructorId,
                gradingTimestamp = submission.gradingTimestamp
            )
        )
        submissionsCollection.document(submission.id).set(doc, merge = true)
        Result.success(submission.id)
    } catch (e: Exception) {
        Result.failure(Exception("Failed to submit TAT: ${e.message}", e))
    }

    suspend fun getTATSubmission(submissionId: String): Result<TATSubmission?> = try {
        val snapshot = submissionsCollection.document(submissionId).get()
        if (!snapshot.exists) Result.success(null)
        else Result.success(snapshot.data(SubmissionDocDto.serializer(TATDataDto.serializer())).data.toDomain())
    } catch (e: Exception) {
        Result.failure(Exception("Failed to fetch TAT submission: ${e.message}", e))
    }

    suspend fun getLatestTATSubmission(userId: String): Result<TATSubmission?> = try {
        val snapshot = submissionsCollection
            .where { FIELD_USER_ID equalTo userId }
            .where { FIELD_TEST_TYPE equalTo TestType.TAT.name }
            .orderBy(FIELD_SUBMITTED_AT, Direction.DESCENDING)
            .limit(1)
            .get()
        val doc = snapshot.documents.firstOrNull()
        if (doc == null) {
            Result.success(null)
        } else {
            Result.success(doc.data(SubmissionDocDto.serializer(TATDataDto.serializer())).data.toDomain())
        }
    } catch (e: Exception) {
        Result.failure(Exception("Failed to fetch latest TAT submission: ${e.message}", e))
    }

    suspend fun updateTATAnalysisStatus(submissionId: String, status: AnalysisStatus): Result<Unit> = try {
        submissionsCollection.document(submissionId).update("$FIELD_DATA.analysisStatus" to status.name)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(Exception("Failed to update TAT analysis status: ${e.message}", e))
    }

    /**
     * Atomically finalizes TAT analysis: persist to `psych_results` first, then mark COMPLETED
     * with result metadata — same ordering guarantee as the Android original (see the domain
     * interface's doc comment on `finalizeTATAnalysisResult`).
     */
    suspend fun finalizeTATAnalysisResult(submissionId: String, olqResult: OLQAnalysisResult): Result<Unit> = try {
        val submissionDoc = submissionsCollection.document(submissionId).get()
        val userId = submissionDoc.data(SubmissionDocDto.serializer(TATDataDto.serializer())).userId
        if (userId.isEmpty()) {
            Result.failure(Exception("Cannot find userId for submission: $submissionId"))
        } else {
            psychResultsCollection.document(submissionId).set(olqResult.toDto(userId = userId), merge = true)

            submissionsCollection.document(submissionId).update(
                "$FIELD_DATA.analysisStatus" to SubmissionConstants.ANALYSIS_STATUS_COMPLETED,
                "$FIELD_DATA.resultUpdatedAt" to Clock.System.now().toEpochMilliseconds(),
                "$FIELD_DATA.hasOlqResult" to true,
                "$FIELD_DATA.resultSource" to "psych_results"
            )
            Result.success(Unit)
        }
    } catch (e: Exception) {
        Result.failure(Exception("Failed to finalize TAT analysis result: ${e.message}", e))
    }

    suspend fun getTATResult(submissionId: String): Result<OLQAnalysisResult?> = getOlqResult(submissionId)

    fun observeTATSubmission(submissionId: String): Flow<TATSubmission?> =
        submissionsCollection.document(submissionId).snapshots
            .map { snapshot ->
                if (!snapshot.exists) return@map null
                val dto = snapshot.data(SubmissionDocDto.serializer(TATDataDto.serializer()))
                val regressionFilters = tatRegressionFilters.getOrPut(submissionId) { OLQRegressionFilter() }
                if (regressionFilters.shouldFilterSnapshot(dto.data.analysisStatus, dto.data.olqResult != null, snapshot.metadata)) {
                    return@map null
                }
                dto.data.toDomain()
            }
            .catch { emit(null) }

    // ===========================
    // WAT
    // ===========================

    suspend fun submitWAT(submission: WATSubmission, batchId: String?): Result<String> = try {
        val doc = SubmissionDocDto(
            id = submission.id,
            userId = submission.userId,
            testId = submission.testId,
            testType = TestType.WAT.name,
            status = submission.status.name,
            submittedAt = submission.submittedAt,
            gradedByInstructorId = submission.gradedByInstructorId,
            gradingTimestamp = submission.gradingTimestamp,
            batchId = batchId,
            data = WATDataDto(
                id = submission.id,
                userId = submission.userId,
                testId = submission.testId,
                responses = submission.responses.map { it.toDto() },
                totalTimeTakenMinutes = submission.totalTimeTakenMinutes,
                submittedAt = submission.submittedAt,
                status = submission.status.name,
                instructorScore = submission.instructorScore?.toDto(),
                gradedByInstructorId = submission.gradedByInstructorId,
                gradingTimestamp = submission.gradingTimestamp
            )
        )
        submissionsCollection.document(submission.id).set(doc, merge = true)
        Result.success(submission.id)
    } catch (e: Exception) {
        Result.failure(Exception("Failed to submit WAT: ${e.message}", e))
    }

    suspend fun getWATSubmission(submissionId: String): Result<WATSubmission?> = try {
        val snapshot = submissionsCollection.document(submissionId).get()
        if (!snapshot.exists) Result.success(null)
        else Result.success(snapshot.data(SubmissionDocDto.serializer(WATDataDto.serializer())).data.toDomain())
    } catch (e: Exception) {
        Result.failure(Exception("Failed to fetch WAT submission: ${e.message}", e))
    }

    suspend fun getLatestWATSubmission(userId: String): Result<WATSubmission?> = try {
        val snapshot = submissionsCollection
            .where { FIELD_USER_ID equalTo userId }
            .where { FIELD_TEST_TYPE equalTo TestType.WAT.name }
            .orderBy(FIELD_SUBMITTED_AT, Direction.DESCENDING)
            .limit(1)
            .get()
        val doc = snapshot.documents.firstOrNull()
        if (doc == null) {
            Result.success(null)
        } else {
            Result.success(doc.data(SubmissionDocDto.serializer(WATDataDto.serializer())).data.toDomain())
        }
    } catch (e: Exception) {
        Result.failure(Exception("Failed to fetch latest WAT submission: ${e.message}", e))
    }

    suspend fun updateWATAnalysisStatus(submissionId: String, status: AnalysisStatus): Result<Unit> = try {
        submissionsCollection.document(submissionId).update("$FIELD_DATA.analysisStatus" to status.name)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(Exception("Failed to update WAT status: ${e.message}", e))
    }

    suspend fun updateWATOLQResult(submissionId: String, olqResult: OLQAnalysisResult): Result<Unit> =
        updateOlqResultTwoStep(submissionId, olqResult)

    suspend fun getWATResult(submissionId: String): Result<OLQAnalysisResult?> = getOlqResult(submissionId)

    fun observeWATSubmission(submissionId: String): Flow<WATSubmission?> =
        submissionsCollection.document(submissionId).snapshots
            .map { snapshot ->
                if (!snapshot.exists) return@map null
                val dto = snapshot.data(SubmissionDocDto.serializer(WATDataDto.serializer()))
                val filter = watRegressionFilters.getOrPut(submissionId) { OLQRegressionFilter() }
                if (filter.shouldFilterSnapshot(dto.data.analysisStatus, dto.data.olqResult != null, snapshot.metadata)) return@map null
                dto.data.toDomain()
            }
            .catch { emit(null) }

    // ===========================
    // SRT
    // ===========================

    suspend fun submitSRT(submission: SRTSubmission, batchId: String?): Result<String> = try {
        val doc = SubmissionDocDto(
            id = submission.id,
            userId = submission.userId,
            testId = submission.testId,
            testType = TestType.SRT.name,
            status = submission.status.name,
            submittedAt = submission.submittedAt,
            gradedByInstructorId = submission.gradedByInstructorId,
            gradingTimestamp = submission.gradingTimestamp,
            batchId = batchId,
            data = SRTDataDto(
                id = submission.id,
                userId = submission.userId,
                testId = submission.testId,
                responses = submission.responses.map { it.toDto() },
                totalTimeTakenMinutes = submission.totalTimeTakenMinutes,
                submittedAt = submission.submittedAt,
                status = submission.status.name,
                instructorScore = submission.instructorScore?.toDto(),
                gradedByInstructorId = submission.gradedByInstructorId,
                gradingTimestamp = submission.gradingTimestamp
            )
        )
        submissionsCollection.document(submission.id).set(doc, merge = true)
        Result.success(submission.id)
    } catch (e: Exception) {
        Result.failure(Exception("Failed to submit SRT: ${e.message}", e))
    }

    suspend fun getSRTSubmission(submissionId: String): Result<SRTSubmission?> = try {
        val snapshot = submissionsCollection.document(submissionId).get()
        if (!snapshot.exists) Result.success(null)
        else Result.success(snapshot.data(SubmissionDocDto.serializer(SRTDataDto.serializer())).data.toDomain())
    } catch (e: Exception) {
        Result.failure(Exception("Failed to fetch SRT submission: ${e.message}", e))
    }

    suspend fun getLatestSRTSubmission(userId: String): Result<SRTSubmission?> = try {
        val snapshot = submissionsCollection
            .where { FIELD_USER_ID equalTo userId }
            .where { FIELD_TEST_TYPE equalTo TestType.SRT.name }
            .orderBy(FIELD_SUBMITTED_AT, Direction.DESCENDING)
            .limit(1)
            .get()
        val doc = snapshot.documents.firstOrNull()
        if (doc == null) {
            Result.success(null)
        } else {
            Result.success(doc.data(SubmissionDocDto.serializer(SRTDataDto.serializer())).data.toDomain())
        }
    } catch (e: Exception) {
        Result.failure(Exception("Failed to fetch latest SRT submission: ${e.message}", e))
    }

    suspend fun updateSRTAnalysisStatus(submissionId: String, status: AnalysisStatus): Result<Unit> = try {
        submissionsCollection.document(submissionId).update("$FIELD_DATA.analysisStatus" to status.name)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(Exception("Failed to update SRT status: ${e.message}", e))
    }

    suspend fun updateSRTOLQResult(submissionId: String, olqResult: OLQAnalysisResult): Result<Unit> =
        updateOlqResultTwoStep(submissionId, olqResult)

    suspend fun getSRTResult(submissionId: String): Result<OLQAnalysisResult?> = getOlqResult(submissionId)

    fun observeSRTSubmission(submissionId: String): Flow<SRTSubmission?> =
        submissionsCollection.document(submissionId).snapshots
            .map { snapshot ->
                if (!snapshot.exists) return@map null
                val dto = snapshot.data(SubmissionDocDto.serializer(SRTDataDto.serializer()))
                val filter = srtRegressionFilters.getOrPut(submissionId) { OLQRegressionFilter() }
                if (filter.shouldFilterSnapshot(dto.data.analysisStatus, dto.data.olqResult != null, snapshot.metadata)) return@map null
                dto.data.toDomain()
            }
            .catch { emit(null) }

    // ===========================
    // SDT
    // ===========================

    suspend fun submitSDT(submission: SDTSubmission, batchId: String?): Result<String> = try {
        val doc = SubmissionDocDto(
            id = submission.id,
            userId = submission.userId,
            testId = submission.testId,
            testType = TestType.SD.name,
            status = submission.status.name,
            submittedAt = submission.submittedAt,
            gradedByInstructorId = submission.gradedByInstructorId,
            gradingTimestamp = submission.gradingTimestamp,
            batchId = batchId,
            data = SDTDataDto(
                id = submission.id,
                userId = submission.userId,
                testId = submission.testId,
                responses = submission.responses.map { it.toDto() },
                totalTimeTakenMinutes = submission.totalTimeTakenMinutes,
                submittedAt = submission.submittedAt,
                status = submission.status.name,
                instructorScore = submission.instructorScore?.toDto(),
                gradedByInstructorId = submission.gradedByInstructorId,
                gradingTimestamp = submission.gradingTimestamp
            )
        )
        submissionsCollection.document(submission.id).set(doc, merge = true)
        Result.success(submission.id)
    } catch (e: Exception) {
        Result.failure(Exception("Failed to submit SDT: ${e.message}", e))
    }

    suspend fun getSDTSubmission(submissionId: String): Result<SDTSubmission?> = try {
        val snapshot = submissionsCollection.document(submissionId).get()
        if (!snapshot.exists) Result.success(null)
        else Result.success(snapshot.data(SubmissionDocDto.serializer(SDTDataDto.serializer())).data.toDomain())
    } catch (e: Exception) {
        Result.failure(Exception("Failed to fetch SDT submission: ${e.message}", e))
    }

    suspend fun getLatestSDTSubmission(userId: String): Result<SDTSubmission?> = try {
        val snapshot = submissionsCollection
            .where { FIELD_USER_ID equalTo userId }
            .where { FIELD_TEST_TYPE equalTo TestType.SD.name }
            .orderBy(FIELD_SUBMITTED_AT, Direction.DESCENDING)
            .limit(1)
            .get()
        val doc = snapshot.documents.firstOrNull()
        if (doc == null) {
            Result.success(null)
        } else {
            Result.success(doc.data(SubmissionDocDto.serializer(SDTDataDto.serializer())).data.toDomain())
        }
    } catch (e: Exception) {
        Result.failure(Exception("Failed to fetch latest SDT submission: ${e.message}", e))
    }

    suspend fun updateSDTAnalysisStatus(submissionId: String, status: AnalysisStatus): Result<Unit> = try {
        submissionsCollection.document(submissionId).update("$FIELD_DATA.analysisStatus" to status.name)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(Exception("Failed to update SDT status: ${e.message}", e))
    }

    suspend fun updateSDTOLQResult(submissionId: String, olqResult: OLQAnalysisResult): Result<Unit> =
        updateOlqResultTwoStep(submissionId, olqResult)

    suspend fun getSDTResult(submissionId: String): Result<OLQAnalysisResult?> = getOlqResult(submissionId)

    fun observeSDTSubmission(submissionId: String): Flow<SDTSubmission?> =
        submissionsCollection.document(submissionId).snapshots
            .map { snapshot ->
                if (!snapshot.exists) return@map null
                val dto = snapshot.data(SubmissionDocDto.serializer(SDTDataDto.serializer()))
                val filter = sdtRegressionFilters.getOrPut(submissionId) { OLQRegressionFilter() }
                if (filter.shouldFilterSnapshot(dto.data.analysisStatus, dto.data.olqResult != null, snapshot.metadata)) return@map null
                dto.data.toDomain()
            }
            .catch { emit(null) }

    // ===========================
    // Shared helpers
    // ===========================

    /** WAT/SRT/SDT's older two-step OLQ write (unlike TAT's atomic `finalizeTATAnalysisResult`). */
    private suspend fun updateOlqResultTwoStep(submissionId: String, olqResult: OLQAnalysisResult): Result<Unit> = try {
        val submissionDoc = submissionsCollection.document(submissionId).get()
        // userId is read generically from the top-level envelope, common to every test type's data shape.
        val userId = submissionDoc.get<String?>(FIELD_USER_ID)
        if (userId == null) {
            Result.failure(Exception("Cannot find userId for submission: $submissionId"))
        } else {
            psychResultsCollection.document(submissionId).set(olqResult.toDto(userId = userId), merge = true)
            submissionsCollection.document(submissionId).update("$FIELD_DATA.analysisStatus" to SubmissionConstants.ANALYSIS_STATUS_COMPLETED)
            Result.success(Unit)
        }
    } catch (e: Exception) {
        Result.failure(Exception("Failed to update OLQ result: ${e.message}", e))
    }

    private suspend fun getOlqResult(submissionId: String): Result<OLQAnalysisResult?> = try {
        val resultDoc = psychResultsCollection.document(submissionId).get()
        if (resultDoc.exists) {
            Result.success(resultDoc.data(OLQAnalysisResultDto.serializer()).toDomain())
        } else {
            // Fallback: legacy/transitional documents that only ever wrote to submissions.data.olqResult.
            // Best-effort — dotted nested-field decode isn't proven against GitLive, so failures here
            // fall through to "no result" rather than failing the whole call.
            val olqDto = runCatching {
                submissionsCollection.document(submissionId).get()
                    .get<OLQAnalysisResultDto?>("$FIELD_DATA.olqResult")
            }.getOrNull()
            Result.success(olqDto?.toDomain())
        }
    } catch (e: Exception) {
        Result.failure(Exception("Failed to get OLQ result: ${e.message}", e))
    }

    private val tatRegressionFilters = mutableMapOf<String, OLQRegressionFilter>()
    private val watRegressionFilters = mutableMapOf<String, OLQRegressionFilter>()
    private val srtRegressionFilters = mutableMapOf<String, OLQRegressionFilter>()
    private val sdtRegressionFilters = mutableMapOf<String, OLQRegressionFilter>()

    private companion object {
        const val SUBMISSIONS_COLLECTION = "submissions"
        const val PSYCH_RESULTS_COLLECTION = "psych_results"
        const val FIELD_USER_ID = "userId"
        const val FIELD_TEST_TYPE = "testType"
        const val FIELD_SUBMITTED_AT = "submittedAt"
        const val FIELD_DATA = "data"
    }
}

// ===========================
// TAT DTOs
// ===========================

@Serializable
internal data class TATDataDto(
    val id: String = "",
    val userId: String = "",
    val testId: String = "",
    val stories: List<TATStoryDto> = emptyList(),
    val totalTimeTakenMinutes: Int = 0,
    val submittedAt: Long = 0L,
    val status: String = "",
    val instructorScore: TATInstructorScoreDto? = null,
    val gradedByInstructorId: String? = null,
    val gradingTimestamp: Long? = null,
    val analysisStatus: String? = null,
    val olqResult: OLQAnalysisResultDto? = null
)

@Serializable
internal data class TATStoryDto(
    val questionId: String = "",
    val story: String = "",
    val charactersCount: Int = 0,
    val viewingTimeTakenSeconds: Int = 0,
    val writingTimeTakenSeconds: Int = 0,
    val submittedAt: Long = 0L
)

@Serializable
internal data class TATInstructorScoreDto(
    val overallScore: Float = 0f,
    val thematicPerceptionScore: Float = 0f,
    val imaginationScore: Float = 0f,
    val characterDepictionScore: Float = 0f,
    val emotionalToneScore: Float = 0f,
    val narrativeStructureScore: Float = 0f,
    val feedback: String = "",
    val storyWiseComments: Map<String, String> = emptyMap(),
    val gradedByInstructorId: String = "",
    val gradedByInstructorName: String = "",
    val gradedAt: Long = 0L,
    val agreedWithAI: Boolean = false
)

internal fun TATStoryResponse.toDto() = TATStoryDto(questionId, story, charactersCount, viewingTimeTakenSeconds, writingTimeTakenSeconds, submittedAt)
internal fun TATStoryDto.toDomain() = TATStoryResponse(questionId, story, charactersCount, viewingTimeTakenSeconds, writingTimeTakenSeconds, submittedAt)

internal fun TATInstructorScore.toDto() = TATInstructorScoreDto(
    overallScore, thematicPerceptionScore, imaginationScore, characterDepictionScore,
    emotionalToneScore, narrativeStructureScore, feedback, storyWiseComments,
    gradedByInstructorId, gradedByInstructorName, gradedAt, agreedWithAI
)

internal fun TATInstructorScoreDto.toDomain() = TATInstructorScore(
    overallScore, thematicPerceptionScore, imaginationScore, characterDepictionScore,
    emotionalToneScore, narrativeStructureScore, feedback, storyWiseComments,
    gradedByInstructorId, gradedByInstructorName, gradedAt, agreedWithAI
)

internal fun TATDataDto.toDomain(): TATSubmission = TATSubmission(
    id = id,
    userId = userId,
    testId = testId,
    stories = stories.map { it.toDomain() },
    totalTimeTakenMinutes = totalTimeTakenMinutes,
    submittedAt = submittedAt,
    status = runCatching { SubmissionStatus.valueOf(status) }.getOrDefault(SubmissionStatus.SUBMITTED_PENDING_REVIEW),
    instructorScore = instructorScore?.toDomain(),
    gradedByInstructorId = gradedByInstructorId,
    gradingTimestamp = gradingTimestamp,
    analysisStatus = analysisStatus?.let { runCatching { AnalysisStatus.valueOf(it) }.getOrDefault(AnalysisStatus.PENDING_ANALYSIS) } ?: AnalysisStatus.PENDING_ANALYSIS,
    olqResult = olqResult?.toDomain()
)

// ===========================
// WAT DTOs
// ===========================

@Serializable
internal data class WATDataDto(
    val id: String = "",
    val userId: String = "",
    val testId: String = "",
    val responses: List<WATResponseDto> = emptyList(),
    val totalTimeTakenMinutes: Int = 0,
    val submittedAt: Long = 0L,
    val status: String = "",
    val instructorScore: WATInstructorScoreDto? = null,
    val gradedByInstructorId: String? = null,
    val gradingTimestamp: Long? = null,
    val analysisStatus: String? = null,
    val olqResult: OLQAnalysisResultDto? = null
)

@Serializable
internal data class WATResponseDto(
    val wordId: String = "",
    val word: String = "",
    val response: String = "",
    val timeTakenSeconds: Int = 0,
    val submittedAt: Long = 0L,
    val isSkipped: Boolean = false
)

@Serializable
internal data class WATInstructorScoreDto(
    val overallScore: Float = 0f,
    val positivityScore: Float = 0f,
    val creativityScore: Float = 0f,
    val speedScore: Float = 0f,
    val relevanceScore: Float = 0f,
    val emotionalMaturityScore: Float = 0f,
    val feedback: String = "",
    val flaggedResponses: List<String> = emptyList(),
    val notableResponses: List<String> = emptyList(),
    val gradedByInstructorId: String = "",
    val gradedByInstructorName: String = "",
    val gradedAt: Long = 0L,
    val agreedWithAI: Boolean = false
)

internal fun WATWordResponse.toDto() = WATResponseDto(wordId, word, response, timeTakenSeconds, submittedAt, isSkipped)
internal fun WATResponseDto.toDomain() = WATWordResponse(wordId, word, response, timeTakenSeconds, submittedAt, isSkipped)

internal fun WATInstructorScore.toDto() = WATInstructorScoreDto(
    overallScore, positivityScore, creativityScore, speedScore, relevanceScore, emotionalMaturityScore,
    feedback, flaggedResponses, notableResponses, gradedByInstructorId, gradedByInstructorName, gradedAt, agreedWithAI
)

internal fun WATInstructorScoreDto.toDomain() = WATInstructorScore(
    overallScore, positivityScore, creativityScore, speedScore, relevanceScore, emotionalMaturityScore,
    feedback, flaggedResponses, notableResponses, gradedByInstructorId, gradedByInstructorName, gradedAt, agreedWithAI
)

internal fun WATDataDto.toDomain(): WATSubmission = WATSubmission(
    id = id,
    userId = userId,
    testId = testId,
    responses = responses.map { it.toDomain() },
    totalTimeTakenMinutes = totalTimeTakenMinutes,
    submittedAt = submittedAt,
    status = runCatching { SubmissionStatus.valueOf(status) }.getOrDefault(SubmissionStatus.SUBMITTED_PENDING_REVIEW),
    instructorScore = instructorScore?.toDomain(),
    gradedByInstructorId = gradedByInstructorId,
    gradingTimestamp = gradingTimestamp,
    analysisStatus = analysisStatus?.let { runCatching { AnalysisStatus.valueOf(it) }.getOrDefault(AnalysisStatus.PENDING_ANALYSIS) } ?: AnalysisStatus.PENDING_ANALYSIS,
    olqResult = olqResult?.toDomain()
)

// ===========================
// SRT DTOs
// ===========================

@Serializable
internal data class SRTDataDto(
    val id: String = "",
    val userId: String = "",
    val testId: String = "",
    val responses: List<SRTResponseDto> = emptyList(),
    val totalTimeTakenMinutes: Int = 0,
    val submittedAt: Long = 0L,
    val status: String = "",
    val instructorScore: SRTInstructorScoreDto? = null,
    val gradedByInstructorId: String? = null,
    val gradingTimestamp: Long? = null,
    val analysisStatus: String? = null,
    val olqResult: OLQAnalysisResultDto? = null
)

@Serializable
internal data class SRTResponseDto(
    val situationId: String = "",
    val situation: String = "",
    val response: String = "",
    val charactersCount: Int = 0,
    val timeTakenSeconds: Int = 0,
    val submittedAt: Long = 0L,
    val isSkipped: Boolean = false
)

@Serializable
internal data class SRTInstructorScoreDto(
    val overallScore: Float = 0f,
    val leadershipScore: Float = 0f,
    val decisionMakingScore: Float = 0f,
    val practicalityScore: Float = 0f,
    val initiativeScore: Float = 0f,
    val socialResponsibilityScore: Float = 0f,
    val feedback: String = "",
    val categoryWiseComments: Map<String, String> = emptyMap(),
    val flaggedResponses: List<String> = emptyList(),
    val exemplaryResponses: List<String> = emptyList(),
    val gradedByInstructorId: String = "",
    val gradedByInstructorName: String = "",
    val gradedAt: Long = 0L,
    val agreedWithAI: Boolean = false
)

internal fun SRTSituationResponse.toDto() = SRTResponseDto(situationId, situation, response, charactersCount, timeTakenSeconds, submittedAt, isSkipped)
internal fun SRTResponseDto.toDomain() = SRTSituationResponse(situationId, situation, response, charactersCount, timeTakenSeconds, submittedAt, isSkipped)

internal fun SRTInstructorScore.toDto() = SRTInstructorScoreDto(
    overallScore, leadershipScore, decisionMakingScore, practicalityScore, initiativeScore, socialResponsibilityScore,
    feedback, categoryWiseComments.entries.associate { (k, v) -> k.name to v }, flaggedResponses, exemplaryResponses,
    gradedByInstructorId, gradedByInstructorName, gradedAt, agreedWithAI
)

/** Unparseable category keys are dropped, same defensive behavior as the Android original. */
internal fun SRTInstructorScoreDto.toDomain() = SRTInstructorScore(
    overallScore, leadershipScore, decisionMakingScore, practicalityScore, initiativeScore, socialResponsibilityScore,
    feedback,
    categoryWiseComments.mapNotNull { (k, v) -> runCatching { SRTCategory.valueOf(k) }.getOrNull()?.let { it to v } }.toMap(),
    flaggedResponses, exemplaryResponses, gradedByInstructorId, gradedByInstructorName, gradedAt, agreedWithAI
)

internal fun SRTDataDto.toDomain(): SRTSubmission = SRTSubmission(
    id = id,
    userId = userId,
    testId = testId,
    responses = responses.map { it.toDomain() },
    totalTimeTakenMinutes = totalTimeTakenMinutes,
    submittedAt = submittedAt,
    status = runCatching { SubmissionStatus.valueOf(status) }.getOrDefault(SubmissionStatus.SUBMITTED_PENDING_REVIEW),
    instructorScore = instructorScore?.toDomain(),
    gradedByInstructorId = gradedByInstructorId,
    gradingTimestamp = gradingTimestamp,
    analysisStatus = analysisStatus?.let { runCatching { AnalysisStatus.valueOf(it) }.getOrDefault(AnalysisStatus.PENDING_ANALYSIS) } ?: AnalysisStatus.PENDING_ANALYSIS,
    olqResult = olqResult?.toDomain()
)

// ===========================
// SDT DTOs
// ===========================

@Serializable
internal data class SDTDataDto(
    val id: String = "",
    val userId: String = "",
    val testId: String = "",
    val responses: List<SDTResponseDto> = emptyList(),
    val totalTimeTakenMinutes: Int = 0,
    val submittedAt: Long = 0L,
    val status: String = "",
    val instructorScore: SDTInstructorScoreDto? = null,
    val gradedByInstructorId: String? = null,
    val gradingTimestamp: Long? = null,
    val analysisStatus: String? = null,
    val olqResult: OLQAnalysisResultDto? = null
)

@Serializable
internal data class SDTResponseDto(
    val questionId: String = "",
    val question: String = "",
    val answer: String = "",
    val charCount: Int = 0,
    val timeTakenSeconds: Int = 0,
    val submittedAt: Long = 0L,
    val isSkipped: Boolean = false
)

@Serializable
internal data class SDTInstructorScoreDto(
    val overallScore: Float = 0f,
    val selfAwarenessScore: Float = 0f,
    val emotionalMaturityScore: Float = 0f,
    val socialPerceptionScore: Float = 0f,
    val introspectionScore: Float = 0f,
    val feedback: String = "",
    val flaggedResponses: List<String> = emptyList(),
    val exemplaryResponses: List<String> = emptyList(),
    val gradedByInstructorId: String = "",
    val gradedByInstructorName: String = "",
    val gradedAt: Long = 0L,
    val agreedWithAI: Boolean = false
)

internal fun SDTQuestionResponse.toDto() = SDTResponseDto(questionId, question, answer, charCount, timeTakenSeconds, submittedAt, isSkipped)
internal fun SDTResponseDto.toDomain() = SDTQuestionResponse(questionId, question, answer, charCount, timeTakenSeconds, submittedAt, isSkipped)

internal fun SDTInstructorScore.toDto() = SDTInstructorScoreDto(
    overallScore, selfAwarenessScore, emotionalMaturityScore, socialPerceptionScore, introspectionScore,
    feedback, flaggedResponses, exemplaryResponses, gradedByInstructorId, gradedByInstructorName, gradedAt, agreedWithAI
)

internal fun SDTInstructorScoreDto.toDomain() = SDTInstructorScore(
    overallScore, selfAwarenessScore, emotionalMaturityScore, socialPerceptionScore, introspectionScore,
    feedback, flaggedResponses, exemplaryResponses, gradedByInstructorId, gradedByInstructorName, gradedAt, agreedWithAI
)

internal fun SDTDataDto.toDomain(): SDTSubmission = SDTSubmission(
    id = id,
    userId = userId,
    testId = testId,
    responses = responses.map { it.toDomain() },
    totalTimeTakenMinutes = totalTimeTakenMinutes,
    submittedAt = submittedAt,
    status = runCatching { SubmissionStatus.valueOf(status) }.getOrDefault(SubmissionStatus.SUBMITTED_PENDING_REVIEW),
    instructorScore = instructorScore?.toDomain(),
    gradedByInstructorId = gradedByInstructorId,
    gradingTimestamp = gradingTimestamp,
    analysisStatus = analysisStatus?.let { runCatching { AnalysisStatus.valueOf(it) }.getOrDefault(AnalysisStatus.PENDING_ANALYSIS) } ?: AnalysisStatus.PENDING_ANALYSIS,
    olqResult = olqResult?.toDomain()
)
