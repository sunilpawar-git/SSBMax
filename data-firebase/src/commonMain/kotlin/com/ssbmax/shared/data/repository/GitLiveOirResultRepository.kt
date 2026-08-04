package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.CategoryScore
import com.ssbmax.shared.domain.model.OIRQuestionType
import com.ssbmax.shared.domain.model.OIRTestResult
import com.ssbmax.shared.domain.repository.OirResultRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore

/**
 * GitLive-Firebase-backed implementation, with a SQLDelight read-through/
 * write-through cache (Phase 2) in front of Firestore. Fetches one submission
 * document from the "submissions" collection (matches the existing Android
 * SubmissionRepository's collection — confirmed via FirestoreSubmissionRepository
 * lookup) and maps it into the domain model. Mirrors
 * OIRSubmissionResultViewModel.parseOIRTestResult's defaulting behavior
 * (missing/malformed fields -> 0/empty, never throw) so behavior parity is
 * preserved for this slice.
 */
class GitLiveOirResultRepository(
    private val cache: OirResultCache
) : OirResultRepository {

    override suspend fun getOirResult(submissionId: String): Result<OIRTestResult?> {
        cache.get(submissionId)?.let { cached ->
            return Result.success(cached.toDomain())
        }

        return try {
            val snapshot = Firebase.firestore
                .collection("submissions")
                .document(submissionId)
                .get()

            if (!snapshot.exists) {
                return Result.success(null)
            }

            val dto = snapshot.data(OirSubmissionDocumentDto.serializer())
            val resultDto = dto.data?.testResult
                ?: return Result.success(null)

            cache.put(submissionId, resultDto)
            Result.success(resultDto.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // The Phase 1 domain move replaced the spike's trimmed OIRTestResult with the real
    // core:domain one, which additionally requires totalTimeSeconds, difficultyBreakdown,
    // and answeredQuestions (per-question review data). This DTO's Firestore read only ever
    // fetched the summary fields needed for this spike's "OIR score: X%" demo, so the
    // per-question/difficulty data defaults to empty here — real values require a fuller
    // Firestore read, which is Phase 2 (data layer) scope, not this reconciliation.
    private fun OirTestResultDto.toDomain(): OIRTestResult {
        val categoryScores = categoryScores.mapNotNull { (key, dto) ->
            val category = runCatching { OIRQuestionType.valueOf(key) }.getOrNull()
                ?: return@mapNotNull null
            category to CategoryScore(
                category = category,
                totalQuestions = dto.totalQuestions,
                correctAnswers = dto.correctAnswers,
                percentage = dto.percentage,
                averageTimeSeconds = 0
            )
        }.toMap()

        return OIRTestResult(
            testId = testId,
            sessionId = sessionId,
            userId = userId,
            totalQuestions = totalQuestions,
            correctAnswers = correctAnswers,
            incorrectAnswers = incorrectAnswers,
            skippedQuestions = skippedQuestions,
            totalTimeSeconds = timeTakenSeconds,
            timeTakenSeconds = timeTakenSeconds,
            rawScore = rawScore,
            percentageScore = percentageScore,
            categoryScores = categoryScores,
            difficultyBreakdown = emptyMap(),
            answeredQuestions = emptyList(),
            completedAt = completedAt
        )
    }
}
