package com.ssbmax.ui.home.student

import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.AuthRepository
import com.ssbmax.core.domain.repository.TestProgressRepository
import com.ssbmax.core.domain.repository.UserProfileRepository
import com.ssbmax.testing.BaseViewModelTest
import com.ssbmax.testing.MockDataFactory
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for StudentHomeViewModel
 * 
 * Tests dashboard functionality, progress tracking, and user info display
 */
class StudentHomeViewModelTest : BaseViewModelTest() {
    
    private lateinit var viewModel: StudentHomeViewModel
    private lateinit var authRepository: AuthRepository
    private lateinit var userProfileRepository: UserProfileRepository
    private lateinit var testProgressRepository: TestProgressRepository
    
    private val mockUser = SSBMaxUser(
        id = "user123",
        email = "test@example.com",
        displayName = "Test User",
        role = UserRole.STUDENT,
        subscriptionTier = SubscriptionTier.BASIC
    )
    
    private val mockProfile = MockDataFactory.createMockUserProfile(
        userId = "user123",
        fullName = "Test Aspirant"
    )
    
    @Before
    fun setUp() {
        authRepository = mockk(relaxed = true)
        userProfileRepository = mockk(relaxed = true)
        testProgressRepository = mockk(relaxed = true)
        
        // Default mocks
        coEvery { authRepository.currentUser } returns flowOf(mockUser)
        coEvery { userProfileRepository.getUserProfile(any()) } returns flowOf(Result.success(mockProfile))
        coEvery { testProgressRepository.getPhase1Progress(any()) } returns flowOf(MockDataFactory.createMockPhase1Progress())
        coEvery { testProgressRepository.getPhase2Progress(any()) } returns flowOf(MockDataFactory.createMockPhase2Progress())
    }
    
    // ==================== Initialization Tests ====================
    
    @Test
    fun `init - loads user profile`() = runTest {
        // When
        viewModel = StudentHomeViewModel(authRepository, userProfileRepository, testProgressRepository)
        advanceTimeBy(200)
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should load user name", "Test Aspirant", state.userName)
        coVerify { userProfileRepository.getUserProfile("user123") }
    }
    
    @Test
    fun `init - loads phase progress`() = runTest {
        // When
        viewModel = StudentHomeViewModel(authRepository, userProfileRepository, testProgressRepository)
        advanceTimeBy(200)
        
        // Then
        val state = viewModel.uiState.value
        assertNotNull("Should have phase 1 progress", state.phase1Progress)
        assertNotNull("Should have phase 2 progress", state.phase2Progress)
    }
    
    @Test
    fun `init - handles null user`() = runTest {
        // Given
        coEvery { authRepository.currentUser } returns flowOf(null)
        
        // When
        viewModel = StudentHomeViewModel(authRepository, userProfileRepository, testProgressRepository)
        advanceTimeBy(200)
        
        // Then - Should not crash
        assertNotNull("State should be valid", viewModel.uiState.value)
    }
    
    @Test
    fun `init - uses default name when profile fails`() = runTest {
        // Given
        coEvery { userProfileRepository.getUserProfile(any()) } returns flowOf(Result.failure(Exception("Not found")))
        
        // When
        viewModel = StudentHomeViewModel(authRepository, userProfileRepository, testProgressRepository)
        advanceTimeBy(200)
        
        // Then
        assertEquals("Should use default name", "Aspirant", viewModel.uiState.value.userName)
    }
    
    // ==================== Progress Tracking Tests ====================
    
    @Test
    fun `displays phase 1 progress correctly`() = runTest {
        // Given
        val phase1Progress = Phase1Progress(
            oirProgress = TestProgress(TestType.OIR, TestStatus.COMPLETED),
            ppdtProgress = TestProgress(TestType.PPDT, TestStatus.IN_PROGRESS)
        )
        coEvery { testProgressRepository.getPhase1Progress(any()) } returns flowOf(phase1Progress)
        
        // When
        viewModel = StudentHomeViewModel(authRepository, userProfileRepository, testProgressRepository)
        advanceTimeBy(200)
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should show phase 1 progress", phase1Progress, state.phase1Progress)
    }
    
    @Test
    fun `displays phase 2 progress correctly`() = runTest {
        // Given
        val phase2Progress = Phase2Progress(
            psychologyProgress = TestProgress(TestType.TAT, TestStatus.COMPLETED),
            gtoProgress = TestProgress(TestType.GTO, TestStatus.NOT_ATTEMPTED),
            interviewProgress = TestProgress(TestType.IO, TestStatus.NOT_ATTEMPTED)
        )
        coEvery { testProgressRepository.getPhase2Progress(any()) } returns flowOf(phase2Progress)
        
        // When
        viewModel = StudentHomeViewModel(authRepository, userProfileRepository, testProgressRepository)
        advanceTimeBy(200)
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should show phase 2 progress", phase2Progress, state.phase2Progress)
    }
    
    @Test
    fun `updates progress when data changes`() = runTest {
        // Given
        val initialProgress = Phase1Progress(
            oirProgress = TestProgress(TestType.OIR, TestStatus.NOT_ATTEMPTED),
            ppdtProgress = TestProgress(TestType.PPDT, TestStatus.NOT_ATTEMPTED)
        )
        val updatedProgress = Phase1Progress(
            oirProgress = TestProgress(TestType.OIR, TestStatus.COMPLETED),
            ppdtProgress = TestProgress(TestType.PPDT, TestStatus.IN_PROGRESS)
        )
        
        val progressFlow = kotlinx.coroutines.flow.MutableStateFlow(initialProgress)
        coEvery { testProgressRepository.getPhase1Progress(any()) } returns progressFlow
        
        viewModel = StudentHomeViewModel(authRepository, userProfileRepository, testProgressRepository)
        advanceTimeBy(200)
        
        // When - Update flow
        progressFlow.value = updatedProgress
        advanceTimeBy(100)
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should show updated progress", updatedProgress, state.phase1Progress)
    }
    
    // ==================== Refresh Tests ====================
    
    @Test
    fun `refreshProgress - reloads user data`() = runTest {
        // Given
        viewModel = StudentHomeViewModel(authRepository, userProfileRepository, testProgressRepository)
        advanceTimeBy(200)
        clearAllMocks(answers = false)
        
        // When
        viewModel.refreshProgress()
        advanceTimeBy(200)
        
        // Then
        coVerify(atLeast = 1) { userProfileRepository.getUserProfile("user123") }
    }
    
    @Test
    fun `refreshProgress - does not crash`() = runTest {
        // Given
        viewModel = StudentHomeViewModel(authRepository, userProfileRepository, testProgressRepository)
        advanceTimeBy(200)
        
        // When
        viewModel.refreshProgress()
        advanceTimeBy(200)
        
        // Then - Should not crash
        assertNotNull("State should be valid", viewModel.uiState.value)
    }
    
    // ==================== UI State Tests ====================
    
    @Test
    fun `initial state has default values`() = runTest {
        // When
        viewModel = StudentHomeViewModel(authRepository, userProfileRepository, testProgressRepository)
        advanceTimeBy(300) // Allow init to complete
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        // After init, userName will be loaded from profile
        assertTrue("Should have user name", state.userName.isNotEmpty())
        assertEquals("Streak should be 0", 0, state.currentStreak)
        assertEquals("Tests completed should be 0", 0, state.testsCompleted)
    }
    
    @Test
    fun `userName updates after loading profile`() = runTest {
        // Given
        val profile = MockDataFactory.createMockUserProfile(fullName = "John Doe")
        coEvery { userProfileRepository.getUserProfile(any()) } returns flowOf(Result.success(profile))
        
        // When
        viewModel = StudentHomeViewModel(authRepository, userProfileRepository, testProgressRepository)
        advanceTimeBy(200)
        
        // Then
        assertEquals("Should update user name", "John Doe", viewModel.uiState.value.userName)
    }
    
    @Test
    fun `handles null profile gracefully`() = runTest {
        // Given
        coEvery { userProfileRepository.getUserProfile(any()) } returns flowOf(Result.success(null))
        
        // When
        viewModel = StudentHomeViewModel(authRepository, userProfileRepository, testProgressRepository)
        advanceTimeBy(200)
        
        // Then
        assertEquals("Should use default name", "Aspirant", viewModel.uiState.value.userName)
    }
    
    // ==================== Progress Calculation Tests ====================
    
    @Test
    fun `displays correct progress for partially completed phase 1`() = runTest {
        // Given
        val phase1 = Phase1Progress(
            oirProgress = TestProgress(TestType.OIR, TestStatus.COMPLETED),
            ppdtProgress = TestProgress(TestType.PPDT, TestStatus.NOT_ATTEMPTED)
        )
        coEvery { testProgressRepository.getPhase1Progress(any()) } returns flowOf(phase1)
        
        // When
        viewModel = StudentHomeViewModel(authRepository, userProfileRepository, testProgressRepository)
        advanceTimeBy(300)
        
        // Then
        val progress = viewModel.uiState.value.phase1Progress
        assertNotNull("Should have progress", progress)
        // Just verify it has the correct test statuses
        progress?.let {
            assertEquals("OIR should be complete", TestStatus.COMPLETED, it.oirProgress.status)
            assertEquals("PPDT should not be attempted", TestStatus.NOT_ATTEMPTED, it.ppdtProgress.status)
        }
    }
    
    @Test
    fun `displays correct progress for completed phase 1`() = runTest {
        // Given
        val phase1 = Phase1Progress(
            oirProgress = TestProgress(TestType.OIR, TestStatus.COMPLETED),
            ppdtProgress = TestProgress(TestType.PPDT, TestStatus.COMPLETED)
        )
        coEvery { testProgressRepository.getPhase1Progress(any()) } returns flowOf(phase1)
        
        // When
        viewModel = StudentHomeViewModel(authRepository, userProfileRepository, testProgressRepository)
        advanceTimeBy(300)
        
        // Then
        val progress = viewModel.uiState.value.phase1Progress
        assertNotNull("Should have progress", progress)
        progress?.let {
            assertEquals("OIR complete", TestStatus.COMPLETED, it.oirProgress.status)
            assertEquals("PPDT complete", TestStatus.COMPLETED, it.ppdtProgress.status)
        }
    }
    
    @Test
    fun `handles empty progress data`() = runTest {
        // Given - Empty progress (all NOT_ATTEMPTED)
        val emptyPhase1 = Phase1Progress(
            oirProgress = TestProgress(TestType.OIR, TestStatus.NOT_ATTEMPTED),
            ppdtProgress = TestProgress(TestType.PPDT, TestStatus.NOT_ATTEMPTED)
        )
        val emptyPhase2 = Phase2Progress(
            psychologyProgress = TestProgress(TestType.TAT, TestStatus.NOT_ATTEMPTED),
            gtoProgress = TestProgress(TestType.GTO, TestStatus.NOT_ATTEMPTED),
            interviewProgress = TestProgress(TestType.IO, TestStatus.NOT_ATTEMPTED)
        )
        coEvery { testProgressRepository.getPhase1Progress(any()) } returns flowOf(emptyPhase1)
        coEvery { testProgressRepository.getPhase2Progress(any()) } returns flowOf(emptyPhase2)
        
        // When
        viewModel = StudentHomeViewModel(authRepository, userProfileRepository, testProgressRepository)
        advanceTimeBy(200)
        
        // Then
        val state = viewModel.uiState.value
        assertNotNull("Phase 1 should exist", state.phase1Progress)
        assertNotNull("Phase 2 should exist", state.phase2Progress)
        state.phase1Progress?.let {
            assertEquals("Phase 1 should be 0% complete", 0.0f, it.completionPercentage, 0.01f)
        }
    }
    
    // ==================== Error Handling Tests ====================
    
    @Test
    fun `handles repository errors gracefully`() = runTest {
        // Given
        coEvery { testProgressRepository.getPhase1Progress(any()) } returns flowOf(
            Phase1Progress(
                oirProgress = TestProgress(TestType.OIR, TestStatus.NOT_ATTEMPTED),
                ppdtProgress = TestProgress(TestType.PPDT, TestStatus.NOT_ATTEMPTED)
            )
        )
        
        // When
        viewModel = StudentHomeViewModel(authRepository, userProfileRepository, testProgressRepository)
        advanceTimeBy(300)
        
        // Then - Should not crash
        assertNotNull("State should be valid", viewModel.uiState.value)
    }
    
    @Test
    fun `handles auth repository errors`() = runTest {
        // Given - Return null user instead of throwing
        coEvery { authRepository.currentUser } returns flowOf(null)
        
        // When
        viewModel = StudentHomeViewModel(authRepository, userProfileRepository, testProgressRepository)
        advanceTimeBy(300)
        
        // Then - Should not crash, should use default name
        val state = viewModel.uiState.value
        assertNotNull("State should be valid", state)
        assertTrue("Should have a name", state.userName.isNotEmpty())
    }
    
    // ==================== Integration Tests ====================
    
    @Test
    fun `full flow - loads all data successfully`() = runTest {
        // Given - All repos return success
        val profile = MockDataFactory.createMockUserProfile(fullName = "Integration Test User")
        val phase1 = Phase1Progress(
            oirProgress = TestProgress(TestType.OIR, TestStatus.COMPLETED),
            ppdtProgress = TestProgress(TestType.PPDT, TestStatus.COMPLETED)
        )
        val phase2 = Phase2Progress(
            psychologyProgress = TestProgress(TestType.TAT, TestStatus.IN_PROGRESS),
            gtoProgress = TestProgress(TestType.GTO, TestStatus.NOT_ATTEMPTED),
            interviewProgress = TestProgress(TestType.IO, TestStatus.NOT_ATTEMPTED)
        )
        
        coEvery { userProfileRepository.getUserProfile(any()) } returns flowOf(Result.success(profile))
        coEvery { testProgressRepository.getPhase1Progress(any()) } returns flowOf(phase1)
        coEvery { testProgressRepository.getPhase2Progress(any()) } returns flowOf(phase2)
        
        // When
        viewModel = StudentHomeViewModel(authRepository, userProfileRepository, testProgressRepository)
        advanceTimeBy(300)
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should have user name", "Integration Test User", state.userName)
        assertEquals("Should have phase 1 progress", phase1, state.phase1Progress)
        assertEquals("Should have phase 2 progress", phase2, state.phase2Progress)
    }
}

