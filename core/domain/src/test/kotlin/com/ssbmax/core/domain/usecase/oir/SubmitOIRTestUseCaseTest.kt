package com.ssbmax.core.domain.usecase.oir

import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.SubmissionRepository
import com.ssbmax.core.domain.repository.TestSessionRepository
import com.ssbmax.core.domain.repository.TestUsageRecorder
import com.ssbmax.core.domain.usecase.dashboard.GetOLQDashboardUseCase
import com.ssbmax.core.domain.util.NoOpLogger
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * TDD tests for SubmitOIRTestUseCase.
 * RED phase: all tests fail until the use case is created (Phase 0-B GREEN).
 */
class SubmitOIRTestUseCaseTest {

    private val mockScoreCalculator = mockk<OIRTestScoreCalculator>()
    private val mockUsageRecorder   = mockk<TestUsageRecorder>(relaxed = true)
    private val mockDashboardUseCase = mockk<GetOLQDashboardUseCase>(relaxed = true)
    private val mockSubmissionRepo  = mockk<SubmissionRepository>()
    private val mockSessionRepo     = mockk<TestSessionRepository>(relaxed = true)

    private lateinit var useCase: SubmitOIRTestUseCase

    private val testSession = OIRTestSession(
        sessionId  = "session-001",
        userId     = "user-001",
        testId     = "oir_standard",
        questions  = emptyList(),
        answers    = emptyMap(),
        currentQuestionIndex = 0,
        startTime  = System.currentTimeMillis(),
        timeRemainingSeconds = 0
    )

    private val fakeResult = mockk<OIRTestResult>(relaxed = true)

    @Before
    fun setUp() {
        useCase = SubmitOIRTestUseCase(
            scoreCalculator   = mockScoreCalculator,
            usageRecorder     = mockUsageRecorder,
            dashboardUseCase  = mockDashboardUseCase,
            submissionRepository = mockSubmissionRepo,
            testSessionRepository = mockSessionRepo
        )
        every { mockScoreCalculator.calculate(any()) } returns fakeResult
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Successful orchestration
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `invoke successful orchestration runs all 5 steps in order`() = runTest {
        coEvery { mockSubmissionRepo.submitOIR(any(), null) } returns Result.success("session-001")

        val result = useCase(testSession)

        assertTrue(result.isSuccess)

        // Verify order: score → usage → dashboard → submit → endSession
        coVerifyOrder {
            mockScoreCalculator.calculate(testSession)
            mockUsageRecorder.recordTestUsage(TestType.OIR, testSession.userId)
            mockDashboardUseCase.invalidateCache(testSession.userId)
            mockSubmissionRepo.submitOIR(any(), null)
            mockSessionRepo.endTestSession(testSession.sessionId)
        }
    }

    @Test
    fun `invoke returns submissionId on success`() = runTest {
        coEvery { mockSubmissionRepo.submitOIR(any(), null) } returns Result.success("session-001")

        val result = useCase(testSession)

        assertEquals("session-001", result.getOrNull())
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Failure propagation
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `invoke failure at step 2 usageRecorder propagates and does NOT call step 3+`() = runTest {
        coEvery { mockUsageRecorder.recordTestUsage(any(), any()) } throws RuntimeException("quota error")

        val result = useCase(testSession)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { mockDashboardUseCase.invalidateCache(any()) }
        coVerify(exactly = 0) { mockSubmissionRepo.submitOIR(any(), any()) }
        coVerify(exactly = 0) { mockSessionRepo.endTestSession(any()) }
    }

    @Test
    fun `invoke failure at step 4 submitOIR propagates and endTestSession NOT called`() = runTest {
        coEvery { mockSubmissionRepo.submitOIR(any(), any()) } returns Result.failure(Exception("Firestore error"))

        val result = useCase(testSession)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { mockSessionRepo.endTestSession(any()) }
    }
}
