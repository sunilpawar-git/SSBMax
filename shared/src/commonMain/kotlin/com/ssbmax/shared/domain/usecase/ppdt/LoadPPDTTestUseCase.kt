package com.ssbmax.shared.domain.usecase.ppdt

import kotlin.time.Clock

import com.ssbmax.shared.domain.model.Gender
import com.ssbmax.shared.domain.model.GenderTag
import com.ssbmax.shared.domain.model.PPDTPhase
import com.ssbmax.shared.domain.model.PPDTTestSession
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.repository.TestContentRepository
import com.ssbmax.shared.domain.repository.TestSessionRepository
import com.ssbmax.shared.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.first

/**
 * Resolves gender, creates the test session, and fetches the PPDT question.
 * Returns ProfileIncompleteException when the profile gate blocks the test.
 */
class LoadPPDTTestUseCase constructor(
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
            startTime = Clock.System.now().toEpochMilliseconds(),
            imageViewingStartTime = null,
            writingStartTime = null,
            currentPhase = PPDTPhase.INSTRUCTIONS,
            story = "",
            isCompleted = false,
            isPaused = false
        )
    }
}
