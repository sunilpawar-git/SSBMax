package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.gto.GTOAnalysisUnavailableException
import com.ssbmax.shared.domain.model.gto.GTOResult
import com.ssbmax.shared.domain.model.gto.GTOResultStatus
import com.ssbmax.shared.domain.model.gto.GTOSubmissionStatus
import com.ssbmax.shared.domain.model.gto.GTOTestType
import com.ssbmax.shared.domain.model.interview.OLQ
import dev.gitlive.firebase.firestore.Direction
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * GTO results/analysis cluster (`getTestResult`/`getLatestResult`/`getUserResults`/
 * `getAverageOLQScores`), split out of the former single `GitLiveGTORepository` god-class
 * (300-line-file limit). Depends on [GitLiveGTOSubmissionDelegate] for submission lookups
 * (`getSubmission`/`getUserSubmissions`) rather than duplicating that logic. Pure structural
 * split — no behavior change from the original merged class.
 */
internal class GitLiveGTOResultsDelegate(
    private val collections: GitLiveGTOCollections,
    private val submissionDelegate: GitLiveGTOSubmissionDelegate
) {

    private val submissionsCollection get() = collections.submissions
    private val resultsCollection get() = collections.results

    suspend fun getTestResult(submissionId: String): Result<GTOResult> = try {
        val resultDoc = resultsCollection.document(submissionId).get()

        if (resultDoc.exists) {
            Result.success(resultDoc.data(GTOResultDto.serializer()).toDomain())
        } else {
            // Fallback: legacy data — scores embedded on the submission doc, never migrated to
            // gto_results. (New submissions store scores ONLY in gto_results, so a miss here
            // means either a legacy row or an unscored submission.)
            val submission = submissionDelegate.getSubmission(submissionId).getOrThrow()

            if (submission.olqScores.isEmpty()) {
                Result.failure(GTOAnalysisUnavailableException(submissionId))
            } else {
                val overallScore = submission.olqScores.values.map { it.score }.average().toFloat()
                val result = GTOResult(
                    submissionId = submissionId,
                    userId = submission.userId,
                    testType = submission.testType,
                    olqScores = submission.olqScores,
                    overallScore = overallScore,
                    overallRating = calculateGtoRating(overallScore),
                    aiConfidence = submission.olqScores.values.map { it.confidence }.average().toInt()
                )

                // Lazy self-heal: migrate the legacy scores into gto_results so the next read
                // hits the primary collection. Best-effort — never fail the read.
                runCatching { resultsCollection.document(submissionId).set(result.toDto()) }

                Result.success(result)
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getLatestResult(
        userId: String,
        testType: GTOTestType
    ): Result<GTOResultStatus> = try {
        // Lean path for the dashboard: fetch only the most-recent submission for this type (one
        // ordered query), instead of fetching+joining every completed submission.
        val snapshot = submissionsCollection
            .where { GTO_FIELD_USER_ID equalTo userId }
            .where { GTO_FIELD_TEST_TYPE equalTo gtoTestTypeToTestType(testType).name }
            .orderBy(GTO_FIELD_SUBMITTED_AT, Direction.DESCENDING)
            .limit(1)
            .get()

        val doc = snapshot.documents.firstOrNull()
        if (doc == null) {
            Result.success(GTOResultStatus.NotAttempted)
        } else {
            val submission = doc.data(GTOSubmissionDocDto.serializer()).toDomain()

            getTestResult(submission.id).fold(
                onSuccess = { Result.success(GTOResultStatus.Available(it)) },
                onFailure = { error ->
                    if (error is GTOAnalysisUnavailableException) {
                        Result.success(GTOResultStatus.AnalysisPending)
                    } else {
                        Result.failure(error)
                    }
                }
            )
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getUserResults(
        userId: String,
        testType: GTOTestType?
    ): Result<List<GTOResult>> = try {
        val submissions = submissionDelegate.getUserSubmissions(userId, testType).getOrThrow()
        // Resolve each submission's result concurrently instead of serially — the Android
        // original does this same async/awaitAll batching (previously a serial mapNotNull).
        val results = coroutineScope {
            submissions
                .filter { it.status == GTOSubmissionStatus.COMPLETED }
                .map { submission -> async { getTestResult(submission.id).getOrNull() } }
                .awaitAll()
                .filterNotNull()
        }

        Result.success(results)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getAverageOLQScores(userId: String): Result<Map<OLQ, Float>> = try {
        val results = getUserResults(userId, null).getOrThrow()

        if (results.isEmpty()) {
            Result.success(emptyMap())
        } else {
            val olqAverages = OLQ.entries.associateWith { olq ->
                val scores = results.mapNotNull { it.olqScores[olq]?.score }
                if (scores.isNotEmpty()) scores.average().toFloat() else 0f
            }
            Result.success(olqAverages)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
