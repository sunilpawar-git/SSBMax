package com.ssbmax.core.domain.usecase

import com.ssbmax.core.domain.model.SubscriptionTier
import com.ssbmax.core.domain.model.interview.OIRStatus
import com.ssbmax.core.domain.model.interview.PIQStatus
import com.ssbmax.core.domain.model.interview.PPDTStatus
import com.ssbmax.core.domain.model.interview.SubscriptionStatus
import com.ssbmax.core.domain.repository.SubmissionRepository
import com.ssbmax.core.domain.repository.SubscriptionRepository
import com.ssbmax.core.domain.repository.UsageInfo
import com.ssbmax.core.domain.model.PIQSubmission
import com.ssbmax.core.domain.model.OIRSubmission
import com.ssbmax.core.domain.model.OIRTestResult
import com.ssbmax.core.domain.model.PPDTSubmission
import com.ssbmax.core.domain.model.SubmissionStatus as ModelSubmissionStatus
import com.ssbmax.core.domain.model.TestType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class CheckInterviewPrerequisitesUseCaseTest {

    private val mockSubmissionRepo = mockk<SubmissionRepository>()
    private val mockSubscriptionRepo = mockk<SubscriptionRepository>()
    private val useCase = CheckInterviewPrerequisitesUseCase(
        mockSubmissionRepo,
        mockSubscriptionRepo
    )

    private val currentMonthStr = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))

    private fun mockPIQSubmission() {
        val mockSubmission = mockk<PIQSubmission>()
        coEvery { mockSubmission.id } returns "piq_sub_id"
        coEvery { mockSubmission.aiPreliminaryScore } returns null
        coEvery { mockSubmissionRepo.getLatestPIQSubmission(any()) } returns Result.success(mockSubmission)
    }

    private fun mockOIRSubmission(score: Float) {
        val mockSubmission = mockk<OIRSubmission>()
        val mockTestResult = mockk<OIRTestResult>()
        coEvery { mockSubmission.id } returns "oir_sub_id"
        coEvery { mockSubmission.testResult } returns mockTestResult
        coEvery { mockTestResult.percentageScore } returns score
        coEvery { mockSubmissionRepo.getLatestOIRSubmission(any()) } returns Result.success(mockSubmission)
    }

    private fun mockPPDTSubmission() {
        val mockSubmission = mockk<PPDTSubmission>()
        coEvery { mockSubmission.submissionId } returns "ppdt_sub_id"
        coEvery { mockSubmissionRepo.getLatestPPDTSubmission(any()) } returns Result.success(mockSubmission)
    }

    @Test
    fun `invoke when all prerequisites completed and under limit returns eligible`() = runTest {
        // Given
        mockPIQSubmission()
        mockOIRSubmission(80f)
        mockPPDTSubmission()
        
        coEvery { mockSubscriptionRepo.getSubscriptionTier(any()) } returns Result.success(SubscriptionTier.PRO)
        coEvery { mockSubscriptionRepo.getMonthlyUsage(any(), currentMonthStr) } returns Result.success(
            mapOf("Interview" to UsageInfo(used = 0, limit = 1))
        )

        // When
        val result = useCase("test_user_id")

        // Then
        assertTrue(result.isSuccess)
        val checkResult = result.getOrThrow()
        assertTrue(checkResult.isEligible)
        assertTrue(checkResult.piqStatus is PIQStatus.Completed)
        assertTrue(checkResult.oirStatus is OIRStatus.Completed)
        assertTrue(checkResult.ppdtStatus is PPDTStatus.Completed)
        assertTrue(checkResult.subscriptionStatus is SubscriptionStatus.Available)
        coVerify { mockSubscriptionRepo.getMonthlyUsage("test_user_id", currentMonthStr) }
    }

    @Test
    fun `invoke when OIR score below threshold returns not eligible`() = runTest {
        // Given
        mockPIQSubmission()
        mockOIRSubmission(45f)
        mockPPDTSubmission()
        
        coEvery { mockSubscriptionRepo.getSubscriptionTier(any()) } returns Result.success(SubscriptionTier.PRO)
        coEvery { mockSubscriptionRepo.getMonthlyUsage(any(), currentMonthStr) } returns Result.success(
            mapOf("Interview" to UsageInfo(used = 0, limit = 1))
        )

        // When
        val result = useCase("test_user_id")

        // Then
        assertTrue(result.isSuccess)
        val checkResult = result.getOrThrow()
        assertFalse(checkResult.isEligible)
        assertTrue(checkResult.oirStatus is OIRStatus.CompletedBelowThreshold)
        assertTrue(checkResult.failureReasons.contains("Score at least 50% in OIR test (current: 45%)"))
    }

    @Test
    fun `invoke when PIQ not started returns not eligible`() = runTest {
        // Given
        coEvery { mockSubmissionRepo.getLatestPIQSubmission(any()) } returns Result.success(null)
        mockOIRSubmission(80f)
        mockPPDTSubmission()
        
        coEvery { mockSubscriptionRepo.getSubscriptionTier(any()) } returns Result.success(SubscriptionTier.PRO)
        coEvery { mockSubscriptionRepo.getMonthlyUsage(any(), currentMonthStr) } returns Result.success(
            mapOf("Interview" to UsageInfo(used = 0, limit = 1))
        )

        // When
        val result = useCase("test_user_id")

        // Then
        assertTrue(result.isSuccess)
        val checkResult = result.getOrThrow()
        assertFalse(checkResult.isEligible)
        assertTrue(checkResult.piqStatus is PIQStatus.NotStarted)
        assertTrue(checkResult.failureReasons.contains("Complete Personal Information Questionnaire (PIQ)"))
    }

    @Test
    fun `invoke when subscription limit reached returns not eligible`() = runTest {
        // Given
        mockPIQSubmission()
        mockOIRSubmission(80f)
        mockPPDTSubmission()
        
        coEvery { mockSubscriptionRepo.getSubscriptionTier(any()) } returns Result.success(SubscriptionTier.PRO)
        coEvery { mockSubscriptionRepo.getMonthlyUsage(any(), currentMonthStr) } returns Result.success(
            mapOf("Interview" to UsageInfo(used = 1, limit = 1))
        )

        // When
        val result = useCase("test_user_id")

        // Then
        assertTrue(result.isSuccess)
        val checkResult = result.getOrThrow()
        assertFalse(checkResult.isEligible)
        assertTrue(checkResult.subscriptionStatus is SubscriptionStatus.LimitReached)
        assertTrue(checkResult.failureReasons.any { it.contains("Interview limit reached") })
        coVerify { mockSubscriptionRepo.getMonthlyUsage("test_user_id", currentMonthStr) }
    }
}
