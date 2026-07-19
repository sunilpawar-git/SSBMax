package com.ssbmax.shared.domain.usecase.tat

import com.ssbmax.shared.domain.model.Gender
import com.ssbmax.shared.domain.model.GenderTag
import com.ssbmax.shared.domain.model.TATQuestion
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.repository.TestContentRepository
import com.ssbmax.shared.domain.repository.TestSessionRepository
import com.ssbmax.shared.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.first

class LoadTATTestUseCase constructor(
    private val testContentRepository: TestContentRepository,
    private val testSessionRepository: TestSessionRepository,
    private val userProfileRepository: UserProfileRepository
) {
    class ProfileIncompleteException : Exception("User profile is incomplete or not found")

    suspend operator fun invoke(userId: String, testId: String): Result<List<TATQuestion>> = runCatching {
        val profileResult = userProfileRepository.getUserProfile(userId).first()
        if (profileResult.isSuccess && profileResult.getOrNull() == null) {
            throw ProfileIncompleteException()
        }
        val genderTag = profileResult.getOrNull()?.let { profile ->
            when (profile.gender) {
                Gender.MALE -> GenderTag.MALE
                Gender.FEMALE -> GenderTag.FEMALE
                else -> null
            }
        }
        testSessionRepository.createTestSession(userId, testId, TestType.TAT).getOrThrow()
        testContentRepository.getTATQuestions(testId, genderTag).getOrThrow()
    }
}
