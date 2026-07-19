package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.CategoryScore
import com.ssbmax.shared.domain.model.OIRQuestionType
import com.ssbmax.shared.domain.model.OIRTestResult
import com.ssbmax.shared.domain.repository.OirResultRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore

/**
 * GitLive-Firebase-backed implementation for the Phase 0 KMP spike.
 * Fetches one submission document from the "submissions" collection
 * (matches the existing Android SubmissionRepository's collection —
 * confirmed via FirestoreSubmissionRepository lookup) and maps it into the
 * domain model. Mirrors OIRSubmissionResultViewModel.parseOIRTestResult's
 * defaulting behavior (missing/malformed fields -> 0/empty, never throw)
 * so behavior parity is preserved for this slice.
 */
class GitLiveOirResultRepository : OirResultRepository {

    override suspend fun getOirResult(submissionId: String): Result<OIRTestResult?> {
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

            Result.success(resultDto.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun OirTestResultDto.toDomain(): OIRTestResult {
        val categoryScores = categoryScores.mapNotNull { (key, dto) ->
            val category = runCatching { OIRQuestionType.valueOf(key) }.getOrNull()
                ?: return@mapNotNull null
            category to CategoryScore(
                category = category,
                totalQuestions = dto.totalQuestions,
                correctAnswers = dto.correctAnswers,
                percentage = dto.percentage
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
            timeTakenSeconds = timeTakenSeconds,
            rawScore = rawScore,
            percentageScore = percentageScore,
            categoryScores = categoryScores,
            completedAt = completedAt
        )
    }
}
