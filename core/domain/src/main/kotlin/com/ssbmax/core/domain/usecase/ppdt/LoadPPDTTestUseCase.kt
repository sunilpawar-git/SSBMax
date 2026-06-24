package com.ssbmax.core.domain.usecase.ppdt

import com.ssbmax.core.domain.model.Gender
import com.ssbmax.core.domain.model.GenderTag
import com.ssbmax.core.domain.model.PPDTPhase
import com.ssbmax.core.domain.model.PPDTTestSession
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.repository.TestContentRepository
import com.ssbmax.core.domain.repository.TestSessionRepository
import com.ssbmax.core.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Resolves gender, creates the test session, and fetches the PPDT question.
 * Returns ProfileIncompleteException when the profile gate blocks the test.
 */
class LoadPPDTTestUseCase @Inject constructor(
    private val testContentRepository: TestContentRepository,
    private val testSessionRepository: TestSessionRepository,
    private val userProfileRepository: UserProfileRepository
) {

    class ProfileIncompleteException : Exception("User profile is incomplete or not found")

    suspend operator fun invoke(userId: String, testId: String): Result<PPDTTestSession> = runCatching {
        val profileResult = userProfileRepository.getUserProfile(userId).first()
        if (profileResult.isSuccess && profileResult.getOrNull() == null) {
            throw ProfileIncompleteException()
        }
        val genderTag = when (profileResult.getOrNull()?.gender) {
            Gender.MALE -> GenderTag.MALE
            Gender.FEMALE -> GenderTag.FEMALE
            else -> null
        }

        val sessionId = testSessionRepository
            .createTestSession(userId, testId, TestType.PPDT)
            .getOrThrow()

        val question = testContentRepository
            .getPPDTQuestion(genderTag = genderTag)
            .getOrThrow()

        PPDTTestSession(
            sessionId = sessionId,
            userId = userId,
            questionId = question.id,
            question = question,
            startTime = System.currentTimeMillis(),
            imageViewingStartTime = null,
            writingStartTime = null,
            currentPhase = PPDTPhase.INSTRUCTIONS,
            story = "",
            isCompleted = false,
            isPaused = false
        )
    }
}
