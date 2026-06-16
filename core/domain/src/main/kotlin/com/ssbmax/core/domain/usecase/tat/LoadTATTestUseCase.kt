package com.ssbmax.core.domain.usecase.tat

import com.ssbmax.core.domain.model.Gender
import com.ssbmax.core.domain.model.GenderTag
import com.ssbmax.core.domain.model.TATQuestion
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.repository.TestContentRepository
import com.ssbmax.core.domain.repository.TestSessionRepository
import com.ssbmax.core.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class LoadTATTestUseCase @Inject constructor(
    private val testContentRepository: TestContentRepository,
    private val testSessionRepository: TestSessionRepository,
    private val userProfileRepository: UserProfileRepository
) {
    suspend operator fun invoke(userId: String, testId: String): Result<List<TATQuestion>> = runCatching {
        val genderTag = userProfileRepository.getUserProfile(userId).first()
            .getOrNull()
            ?.let { profile ->
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
