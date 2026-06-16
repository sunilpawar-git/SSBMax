package com.ssbmax.core.domain.usecase.tat

import com.ssbmax.core.domain.model.TATQuestion
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.repository.TestContentRepository
import com.ssbmax.core.domain.repository.TestSessionRepository
import javax.inject.Inject

class LoadTATTestUseCase @Inject constructor(
    private val testContentRepository: TestContentRepository,
    private val testSessionRepository: TestSessionRepository
) {
    suspend operator fun invoke(userId: String, testId: String): Result<List<TATQuestion>> = runCatching {
        testSessionRepository.createTestSession(userId, testId, TestType.TAT).getOrThrow()
        testContentRepository.getTATQuestions(testId).getOrThrow()
    }
}
