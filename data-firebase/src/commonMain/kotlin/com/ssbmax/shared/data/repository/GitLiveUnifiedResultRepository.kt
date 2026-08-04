package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.model.gto.GTOTestType
import com.ssbmax.shared.domain.model.interview.OLQ
import com.ssbmax.shared.domain.model.interview.OLQScore
import com.ssbmax.shared.domain.model.scoring.OLQAnalysisResult
import com.ssbmax.shared.domain.repository.GTORepository
import com.ssbmax.shared.domain.repository.InterviewRepository
import com.ssbmax.shared.domain.repository.UnifiedResultRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Port of the Android `UnifiedResultRepositoryImpl`. Unlike every other repository ported so
 * far, this one has no direct Firebase/Room coupling at all — it's pure aggregation logic over
 * the already-KMP [InterviewRepository]/[GTORepository] domain interfaces, so it can be ported
 * ahead of the concrete Firestore-backed implementations of those two repos (Koin will wire the
 * real impls once `FirestoreGTORepository`/`FirestoreInterviewRepository` land in a future slice).
 *
 * The OLQ-averaging/ranking math that was inlined in the Android original's
 * `getTopStrengths`/`getAreasForImprovement` was extracted into standalone, directly unit-tested
 * functions ([averageOlqScores], [rankOlqsAscending], [rankOlqsDescending]) rather than ported
 * as private methods, matching the extraction pattern used for `SubscriptionLimits`/
 * `gradingPriorityForHoursWaiting` in prior slices.
 */
class GitLiveUnifiedResultRepository(
    private val interviewRepository: InterviewRepository,
    private val gtoRepository: GTORepository
) : UnifiedResultRepository {

    override fun getAllResults(userId: String): Flow<List<OLQAnalysisResult>> =
        combine(
            getInterviewResults(userId),
            getGTOResultsFlow(userId)
        ) { interviewResults, gtoResults ->
            (interviewResults + gtoResults).sortedByDescending { it.analyzedAt }
        }

    override fun getRecentResults(userId: String, limit: Int): Flow<List<OLQAnalysisResult>> =
        getAllResults(userId).map { it.take(limit) }

    override fun getOverallOLQProfile(userId: String): Flow<Map<OLQ, Float>> =
        getAllResults(userId).map { results ->
            if (results.isEmpty()) {
                emptyMap()
            } else {
                OLQ.entries.associateWith { olq ->
                    val scores = results.mapNotNull { it.olqScores[olq]?.score?.toFloat() }
                    if (scores.isNotEmpty()) scores.average().toFloat() else 0f
                }
            }
        }

    override suspend fun getTopStrengths(userId: String, topN: Int): Result<List<OLQ>> =
        runCatching {
            val averages = averageOlqScores(collectAllOlqScoreMaps(userId))
            rankOlqsAscending(averages, topN) // lower score = stronger in SSB convention
        }

    override suspend fun getAreasForImprovement(userId: String, topN: Int): Result<List<OLQ>> =
        runCatching {
            val averages = averageOlqScores(collectAllOlqScoreMaps(userId))
            rankOlqsDescending(averages, topN) // higher score = weaker in SSB convention
        }

    private suspend fun collectAllOlqScoreMaps(userId: String): List<Map<OLQ, OLQScore>> {
        val gtoResults = gtoRepository.getUserResults(userId).getOrNull() ?: emptyList()
        val interviewResults = interviewRepository.getUserResults(userId).first()
        return gtoResults.map { it.olqScores } + interviewResults.map { it.overallOLQScores }
    }

    private fun getInterviewResults(userId: String): Flow<List<OLQAnalysisResult>> =
        interviewRepository.getUserResults(userId).map { results ->
            results.map { result ->
                OLQAnalysisResult(
                    submissionId = result.id,
                    testType = TestType.IO,
                    olqScores = result.overallOLQScores,
                    overallScore = result.getAverageOLQScore(),
                    overallRating = result.getPerformanceLevel().displayName,
                    strengths = result.strengths.map { it.displayName },
                    weaknesses = result.weaknesses.map { it.displayName },
                    recommendations = listOf(result.feedback),
                    analyzedAt = result.completedAt.toEpochMilliseconds(),
                    aiConfidence = result.overallConfidence
                )
            }
        }

    private fun getGTOResultsFlow(userId: String): Flow<List<OLQAnalysisResult>> = flow {
        try {
            val gdResults = gtoRepository.getUserResults(userId, GTOTestType.GROUP_DISCUSSION).getOrNull() ?: emptyList()
            val gpeResults = gtoRepository.getUserResults(userId, GTOTestType.GROUP_PLANNING_EXERCISE).getOrNull() ?: emptyList()
            val lecturetteResults = gtoRepository.getUserResults(userId, GTOTestType.LECTURETTE).getOrNull() ?: emptyList()

            val mapped = (gdResults + gpeResults + lecturetteResults).map { gtoResult ->
                OLQAnalysisResult(
                    submissionId = gtoResult.submissionId,
                    testType = gtoTestTypeToTestType(gtoResult.testType),
                    olqScores = gtoResult.olqScores,
                    overallScore = gtoResult.overallScore,
                    overallRating = gtoResult.overallRating,
                    strengths = emptyList(),
                    weaknesses = emptyList(),
                    recommendations = emptyList(),
                    analyzedAt = gtoResult.analyzedAt,
                    aiConfidence = gtoResult.aiConfidence
                )
            }
            emit(mapped)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
}

/** Same mapping as the Android original's inline `when`; extracted for direct unit testing. */
internal fun gtoTestTypeToTestType(testType: GTOTestType): TestType = when (testType) {
    GTOTestType.GROUP_DISCUSSION -> TestType.GTO_GD
    GTOTestType.GROUP_PLANNING_EXERCISE -> TestType.GTO_GPE
    GTOTestType.LECTURETTE -> TestType.GTO_LECTURETTE
    GTOTestType.PROGRESSIVE_GROUP_TASK -> TestType.GTO_PGT
    GTOTestType.HALF_GROUP_TASK -> TestType.GTO_HGT
    GTOTestType.GROUP_OBSTACLE_RACE -> TestType.GTO_GOR
    GTOTestType.INDIVIDUAL_OBSTACLES -> TestType.GTO_IO
    GTOTestType.COMMAND_TASK -> TestType.GTO_CT
}

/** Average each OLQ's score across every score-map supplied (a GTO or interview result). */
internal fun averageOlqScores(scoreMaps: List<Map<OLQ, OLQScore>>): Map<OLQ, Float> {
    val combined = mutableMapOf<OLQ, MutableList<Int>>()
    scoreMaps.forEach { scores ->
        scores.forEach { (olq, score) -> combined.getOrPut(olq) { mutableListOf() }.add(score.score) }
    }
    return combined.mapValues { (_, scores) -> scores.average().toFloat() }
}

/** Lowest average first — SSB convention where a lower OLQ score is the stronger result. */
internal fun rankOlqsAscending(averages: Map<OLQ, Float>, topN: Int): List<OLQ> =
    averages.entries.sortedBy { it.value }.take(topN).map { it.key }

/** Highest average first — SSB convention where a higher OLQ score needs the most improvement. */
internal fun rankOlqsDescending(averages: Map<OLQ, Float>, topN: Int): List<OLQ> =
    averages.entries.sortedByDescending { it.value }.take(topN).map { it.key }
