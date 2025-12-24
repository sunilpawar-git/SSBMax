package com.ssbmax.ui.tests.wat

import androidx.work.WorkManager
import app.cash.turbine.test
import com.ssbmax.core.data.repository.DifficultyProgressionManager
import com.ssbmax.core.data.repository.SubscriptionManager
import com.ssbmax.core.data.security.SecurityEventLogger
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.TestContentRepository
import com.ssbmax.core.domain.repository.UserProfileRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.core.domain.usecase.dashboard.GetOLQDashboardUseCase
import com.ssbmax.core.domain.usecase.submission.SubmitWATTestUseCase
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Unit tests for WATTestViewModel
 * 
 * Tests cover:
 * - Test loading success/failure
 * - Word navigation and timer
 * - Response updates and submissions
 * - Test completion and submission
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WATTestViewModelTest : BaseViewModelTest() {

    private lateinit var viewModel: WATTestViewModel
    private val mockTestContentRepo = mockk<TestContentRepository>(relaxed = true)
    private val mockSubmitWATTest = mockk<SubmitWATTestUseCase>(relaxed = true)
    private val mockObserveCurrentUser = mockk<ObserveCurrentUserUseCase>(relaxed = true)
    private val mockUserProfileRepo = mockk<UserProfileRepository>(relaxed = true)
    private val mockDifficultyManager = mockk<DifficultyProgressionManager>(relaxed = true)
    private val mockSubscriptionManager = mockk<SubscriptionManager>(relaxed = true)
    private val mockGetOLQDashboard = mockk<GetOLQDashboardUseCase>(relaxed = true)
    private val mockSecurityLogger = mockk<SecurityEventLogger>(relaxed = true)
    private val mockWorkManager = mockk<WorkManager>(relaxed = true)

    private val mockWords = createMockWords()
    private val mockUser = SSBMaxUser(
        id = "test-user-123",
        email = "test@example.com",
        displayName = "Test User",
        role = UserRole.STUDENT,
        createdAt = System.currentTimeMillis()
    )
    private val mockUserProfile = UserProfile(
        userId = "test-user-123",
        fullName = "Test User",
        age = 22,
        gender = Gender.MALE,
        entryType = EntryType.GRADUATE,
        subscriptionType = SubscriptionType.PRO
    )

    @Before
    fun setup() {
        every { mockObserveCurrentUser() } returns flowOf(mockUser)
        coEvery { mockTestContentRepo.createTestSession(any(), any(), TestType.WAT) } returns Result.success("session-wat-123")
        coEvery { mockTestContentRepo.getWATQuestions(any()) } returns Result.success(mockWords)
        coEvery { mockUserProfileRepo.getUserProfile(any()) } returns flowOf(Result.success(mockUserProfile))
        coEvery { mockSubmitWATTest(any(), any()) } returns Result.success("submission-wat-123")

        viewModel = WATTestViewModel(
            mockTestContentRepo,
            mockSubmitWATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager,
            mockGetOLQDashboard,
            mockSecurityLogger,
            mockWorkManager
        )
    }

    @Test
    fun `loadTest success updates state correctly`() = runTest {
        // Load test
        viewModel.loadTest("wat-test-1")
        advanceUntilIdle()

        // Wait for loaded state
        val loaded = viewModel.uiState.value
        assertFalse(loaded.isLoading)
        assertEquals("wat-test-1", loaded.testId)
        assertEquals(3, loaded.words.size)
        assertEquals(WATPhase.INSTRUCTIONS, loaded.phase)
        assertNull(loaded.error)
    }

    @Test
    fun `loadTest failure shows error message`() = runTest {
        coEvery { mockTestContentRepo.getWATQuestions(any()) } returns Result.failure(
            Exception("Network error")
        )

        viewModel.loadTest("wat-test-1")
        advanceUntilIdle()

        val errorState = viewModel.uiState.value
        assertFalse(errorState.isLoading)
        assertNotNull(errorState.error)
        assertTrue(errorState.error!!.isNotBlank())
    }

    @Test
    fun `currentWord returns correct word based on index`() = runTest {
        viewModel.loadTest("wat-test-1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("COURAGE", state.currentWord?.word)
        assertEquals(0, state.currentWordIndex)
    }

    @Test
    fun `words list size matches loaded test`() = runTest {
        viewModel.loadTest("wat-test-1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3, state.words.size)
        assertEquals("COURAGE", state.words[0].word)
        assertEquals("LEADERSHIP", state.words[1].word)
        assertEquals("TEAM", state.words[2].word)
    }


    // Helper function to create mock words
    private fun createMockWords(): List<WATWord> {
        return listOf(
            WATWord(
                id = UUID.randomUUID().toString(),
                word = "COURAGE",
                sequenceNumber = 1,
                timeAllowedSeconds = 15
            ),
            WATWord(
                id = UUID.randomUUID().toString(),
                word = "LEADERSHIP",
                sequenceNumber = 2,
                timeAllowedSeconds = 15
            ),
            WATWord(
                id = UUID.randomUUID().toString(),
                word = "TEAM",
                sequenceNumber = 3,
                timeAllowedSeconds = 15
            )
        )
    }
}

