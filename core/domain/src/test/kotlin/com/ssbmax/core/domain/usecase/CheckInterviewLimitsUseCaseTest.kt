package com.ssbmax.core.domain.usecase

import com.ssbmax.core.domain.model.SubscriptionTier
import com.ssbmax.core.domain.repository.InterviewRepository
import com.ssbmax.core.domain.repository.SubscriptionRepository
import com.ssbmax.core.domain.repository.UsageInfo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class CheckInterviewLimitsUseCaseTest {

    private val mockSubscriptionRepo = mockk<SubscriptionRepository>()
    private val mockInterviewRepo = mockk<InterviewRepository>()
    private val useCase = CheckInterviewLimitsUseCase(mockSubscriptionRepo, mockInterviewRepo)

    private val currentMonthStr = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))

    @Test
    fun `invoke when FREE tier under limit allows interview`() = runTest {
        // Given
        coEvery { mockSubscriptionRepo.getSubscriptionTier(any()) } returns Result.success(SubscriptionTier.FREE)
        coEvery { mockSubscriptionRepo.getMonthlyUsage(any(), currentMonthStr) } returns Result.success(
            mapOf("Interview" to UsageInfo(used = 0, limit = 1))
        )

        // When
        val result = useCase("test_user_id")

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow())
        coVerify { mockSubscriptionRepo.getMonthlyUsage("test_user_id", currentMonthStr) }
    }

    @Test
    fun `invoke when FREE tier over limit blocks interview`() = runTest {
        // Given
        coEvery { mockSubscriptionRepo.getSubscriptionTier(any()) } returns Result.success(SubscriptionTier.FREE)
        coEvery { mockSubscriptionRepo.getMonthlyUsage(any(), currentMonthStr) } returns Result.success(
            mapOf("Interview" to UsageInfo(used = 1, limit = 1))
        )

        // When
        val result = useCase("test_user_id")

        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrThrow())
        coVerify { mockSubscriptionRepo.getMonthlyUsage("test_user_id", currentMonthStr) }
    }

    @Test
    fun `invoke when PRO tier under limit allows interview`() = runTest {
        // Given
        coEvery { mockSubscriptionRepo.getSubscriptionTier(any()) } returns Result.success(SubscriptionTier.PRO)
        coEvery { mockSubscriptionRepo.getMonthlyUsage(any(), currentMonthStr) } returns Result.success(
            mapOf("Interview" to UsageInfo(used = 0, limit = 1))
        )

        // When
        val result = useCase("test_user_id")

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow())
        coVerify { mockSubscriptionRepo.getMonthlyUsage("test_user_id", currentMonthStr) }
    }

    @Test
    fun `invoke when PRO tier over limit blocks interview`() = runTest {
        // Given
        coEvery { mockSubscriptionRepo.getSubscriptionTier(any()) } returns Result.success(SubscriptionTier.PRO)
        coEvery { mockSubscriptionRepo.getMonthlyUsage(any(), currentMonthStr) } returns Result.success(
            mapOf("Interview" to UsageInfo(used = 1, limit = 1))
        )

        // When
        val result = useCase("test_user_id")

        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrThrow())
        coVerify { mockSubscriptionRepo.getMonthlyUsage("test_user_id", currentMonthStr) }
    }

    @Test
    fun `invoke when PREMIUM tier under limit allows interview`() = runTest {
        // Given
        coEvery { mockSubscriptionRepo.getSubscriptionTier(any()) } returns Result.success(SubscriptionTier.PREMIUM)
        coEvery { mockSubscriptionRepo.getMonthlyUsage(any(), currentMonthStr) } returns Result.success(
            mapOf("Interview" to UsageInfo(used = 2, limit = 3))
        )

        // When
        val result = useCase("test_user_id")

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow())
        coVerify { mockSubscriptionRepo.getMonthlyUsage("test_user_id", currentMonthStr) }
    }

    @Test
    fun `invoke when PREMIUM tier over limit blocks interview`() = runTest {
        // Given
        coEvery { mockSubscriptionRepo.getSubscriptionTier(any()) } returns Result.success(SubscriptionTier.PREMIUM)
        coEvery { mockSubscriptionRepo.getMonthlyUsage(any(), currentMonthStr) } returns Result.success(
            mapOf("Interview" to UsageInfo(used = 3, limit = 3))
        )

        // When
        val result = useCase("test_user_id")

        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrThrow())
        coVerify { mockSubscriptionRepo.getMonthlyUsage("test_user_id", currentMonthStr) }
    }

    @Test
    fun `getRemainingCount returns remaining count correctly`() = runTest {
        // Given
        coEvery { mockSubscriptionRepo.getSubscriptionTier(any()) } returns Result.success(SubscriptionTier.PREMIUM)
        coEvery { mockSubscriptionRepo.getMonthlyUsage(any(), currentMonthStr) } returns Result.success(
            mapOf("Interview" to UsageInfo(used = 1, limit = 3))
        )

        // When
        val result = useCase.getRemainingCount("test_user_id")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow())
        coVerify { mockSubscriptionRepo.getMonthlyUsage("test_user_id", currentMonthStr) }
    }
}
