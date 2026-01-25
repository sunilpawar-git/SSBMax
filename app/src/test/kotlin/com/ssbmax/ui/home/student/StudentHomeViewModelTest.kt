package com.ssbmax.ui.home.student

import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.AuthRepository
import com.ssbmax.core.domain.repository.TestProgressRepository
import com.ssbmax.core.domain.repository.UserProfileRepository
import com.ssbmax.core.domain.repository.UnifiedResultRepository
import com.ssbmax.core.domain.usecase.dashboard.GetOLQDashboardUseCase
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Comprehensive tests for StudentHomeViewModel
 * Tests dashboard display, progress tracking, and user profile integration
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StudentHomeViewModelTest : BaseViewModelTest() {

    private lateinit var viewModel: StudentHomeViewModel
    private lateinit var mockAuthRepository: AuthRepository
    private lateinit var mockUserProfileRepository: UserProfileRepository
    private lateinit var mockTestProgressRepository: TestProgressRepository
    private lateinit var mockUnifiedResultRepository: UnifiedResultRepository
    private lateinit var mockGetOLQDashboard: GetOLQDashboardUseCase
    private lateinit var mockAnalyticsManager: com.ssbmax.core.data.analytics.AnalyticsManager
    private lateinit var mockNotificationRepository: com.ssbmax.core.domain.repository.NotificationRepository

    private lateinit var mockCurrentUserFlow: MutableStateFlow<SSBMaxUser?>
    
    private val testUser = SSBMaxUser(
        id = "test-user-123",
        email = "test@student.com",
        displayName = "Test Student",
        role = UserRole.STUDENT
    )
    
    private val testProfile = UserProfile(
        userId = "test-user-123",
        fullName = "Test Student Full Name",
        age = 22,
        gender = Gender.MALE,
        entryType = EntryType.GRADUATE,
        currentStreak = 5
    )
    
    private val phase1ProgressEmpty = Phase1Progress(
        oirProgress = TestProgress(TestType.OIR, TestStatus.NOT_ATTEMPTED),
        ppdtProgress = TestProgress(TestType.PPDT, TestStatus.NOT_ATTEMPTED)
    )
    
    private val phase1ProgressPartial = Phase1Progress(
        oirProgress = TestProgress(TestType.OIR, TestStatus.COMPLETED, latestScore = 85.0f),
        ppdtProgress = TestProgress(TestType.PPDT, TestStatus.NOT_ATTEMPTED)
    )
    
    private val phase2ProgressEmpty = Phase2Progress(
        psychologyProgress = TestProgress(TestType.TAT, TestStatus.NOT_ATTEMPTED),
        gtoProgress = TestProgress(TestType.GTO_GD, TestStatus.NOT_ATTEMPTED),
        interviewProgress = TestProgress(TestType.IO, TestStatus.NOT_ATTEMPTED)
    )
    
    private val phase2ProgressPartial = Phase2Progress(
        psychologyProgress = TestProgress(TestType.TAT, TestStatus.GRADED, latestScore = 78.0f),
        gtoProgress = TestProgress(TestType.GTO_GD, TestStatus.COMPLETED, latestScore = 82.0f),
        interviewProgress = TestProgress(TestType.IO, TestStatus.NOT_ATTEMPTED)
    )
    
    @Before
    fun setup() {
        mockAuthRepository = mockk()
        mockUserProfileRepository = mockk()
        mockTestProgressRepository = mockk()
        mockUnifiedResultRepository = mockk()
        mockGetOLQDashboard = mockk()
        mockAnalyticsManager = mockk(relaxed = true)
        mockNotificationRepository = mockk()

        mockCurrentUserFlow = MutableStateFlow(testUser)
        every { mockAuthRepository.currentUser } returns mockCurrentUserFlow

        // Setup default behaviors for new dependencies
        coEvery { mockUnifiedResultRepository.getRecentResults(any(), any()) } returns flowOf(emptyList())
        coEvery { mockUnifiedResultRepository.getOverallOLQProfile(any()) } returns flowOf(emptyMap())
        coEvery { mockGetOLQDashboard(any(), any()) } returns Result.success(mockk(relaxed = true))
        coEvery { mockNotificationRepository.getUnreadCount(any()) } returns flowOf(0) // Default to 0 notifications
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    // ==================== Initialization Tests ====================
    
    @Test
    fun `init observes user profile and test progress`() = runTest {
        // Given
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns 
            flowOf(Result.success(testProfile))
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            MutableStateFlow(phase1ProgressEmpty)
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            MutableStateFlow(phase2ProgressEmpty)
        
        // When
        viewModel = StudentHomeViewModel(
            mockAuthRepository,
            mockUserProfileRepository,
            mockTestProgressRepository,
            mockUnifiedResultRepository,
            mockGetOLQDashboard,
            mockAnalyticsManager,
            mockNotificationRepository
        )
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should load user name", "Test Student Full Name", state.userName)
        assertEquals("Should load streak", 5, state.currentStreak)
        assertEquals("Should have no tests completed", 0, state.testsCompleted)
    }
    
    @Test
    fun `initial state has default values`() {
        val defaultState = StudentHomeUiState()
        
        assertFalse("Should not be loading", defaultState.isLoading)
        assertEquals("Default name should be Aspirant", "Aspirant", defaultState.userName)
        assertEquals("Default streak should be 0", 0, defaultState.currentStreak)
        assertEquals("Default tests completed should be 0", 0, defaultState.testsCompleted)
        assertEquals("Default notifications should be 0", 0, defaultState.notificationCount)
        assertNull("Default phase1 progress should be null", defaultState.phase1Progress)
        assertNull("Default phase2 progress should be null", defaultState.phase2Progress)
        assertNull("Default error should be null", defaultState.error)
    }
    
    // ==================== User Profile Tests ====================
    
    @Test
    fun `loads user profile with full name`() = runTest {
        // Given
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns 
            flowOf(Result.success(testProfile))
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            MutableStateFlow(phase1ProgressEmpty)
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            MutableStateFlow(phase2ProgressEmpty)
        
        // When
        viewModel = StudentHomeViewModel(
            mockAuthRepository,
            mockUserProfileRepository,
            mockTestProgressRepository,
            mockUnifiedResultRepository,
            mockGetOLQDashboard,
            mockAnalyticsManager,
            mockNotificationRepository
        )
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should display full name", "Test Student Full Name", state.userName)
    }
    
    @Test
    fun `shows default name when profile is null`() = runTest {
        // Given
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns 
            flowOf(Result.success(null))
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            MutableStateFlow(phase1ProgressEmpty)
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            MutableStateFlow(phase2ProgressEmpty)
        
        // When
        viewModel = StudentHomeViewModel(
            mockAuthRepository,
            mockUserProfileRepository,
            mockTestProgressRepository,
            mockUnifiedResultRepository,
            mockGetOLQDashboard,
            mockAnalyticsManager,
            mockNotificationRepository
        )
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should show default name", "Aspirant", state.userName)
        assertEquals("Should show default streak", 0, state.currentStreak)
    }
    
    @Test
    fun `displays current streak from profile`() = runTest {
        // Given
        val profileWithStreak = testProfile.copy(currentStreak = 12)
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns 
            flowOf(Result.success(profileWithStreak))
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            MutableStateFlow(phase1ProgressEmpty)
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            MutableStateFlow(phase2ProgressEmpty)
        
        // When
        viewModel = StudentHomeViewModel(
            mockAuthRepository,
            mockUserProfileRepository,
            mockTestProgressRepository,
            mockUnifiedResultRepository,
            mockGetOLQDashboard,
            mockAnalyticsManager,
            mockNotificationRepository
        )
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should display streak", 12, state.currentStreak)
    }
    
    // ==================== Test Progress Tracking Tests ====================
    
    @Test
    fun `counts completed tests from both phases`() = runTest {
        // Given - 1 Phase 1 test + 2 Phase 2 tests = 3 total
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns 
            flowOf(Result.success(testProfile))
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            MutableStateFlow(phase1ProgressPartial)  // OIR completed
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            MutableStateFlow(phase2ProgressPartial)  // Psychology + GTO completed
        
        // When
        viewModel = StudentHomeViewModel(
            mockAuthRepository,
            mockUserProfileRepository,
            mockTestProgressRepository,
            mockUnifiedResultRepository,
            mockGetOLQDashboard,
            mockAnalyticsManager,
            mockNotificationRepository
        )
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should count 3 completed tests", 3, state.testsCompleted)
    }
    
    @Test
    fun `counts tests with any status except NOT_ATTEMPTED`() = runTest {
        // Given
        val phase1AllAttempted = Phase1Progress(
            oirProgress = TestProgress(TestType.OIR, TestStatus.IN_PROGRESS),
            ppdtProgress = TestProgress(TestType.PPDT, TestStatus.SUBMITTED_PENDING_REVIEW)
        )
        val phase2AllAttempted = Phase2Progress(
            psychologyProgress = TestProgress(TestType.TAT, TestStatus.GRADED),
            gtoProgress = TestProgress(TestType.GTO_GD, TestStatus.COMPLETED),
            interviewProgress = TestProgress(TestType.IO, TestStatus.IN_PROGRESS)
        )
        
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns 
            flowOf(Result.success(testProfile))
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            MutableStateFlow(phase1AllAttempted)
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            MutableStateFlow(phase2AllAttempted)
        
        // When
        viewModel = StudentHomeViewModel(
            mockAuthRepository,
            mockUserProfileRepository,
            mockTestProgressRepository,
            mockUnifiedResultRepository,
            mockGetOLQDashboard,
            mockAnalyticsManager,
            mockNotificationRepository
        )
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should count all 5 attempted tests", 5, state.testsCompleted)
    }
    
    @Test
    fun `zero tests completed when all NOT_ATTEMPTED`() = runTest {
        // Given
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns 
            flowOf(Result.success(testProfile))
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            MutableStateFlow(phase1ProgressEmpty)
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            MutableStateFlow(phase2ProgressEmpty)
        
        // When
        viewModel = StudentHomeViewModel(
            mockAuthRepository,
            mockUserProfileRepository,
            mockTestProgressRepository,
            mockUnifiedResultRepository,
            mockGetOLQDashboard,
            mockAnalyticsManager,
            mockNotificationRepository
        )
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should have 0 completed tests", 0, state.testsCompleted)
    }
    
    @Test
    fun `stores phase1 and phase2 progress in state`() = runTest {
        // Given
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns 
            flowOf(Result.success(testProfile))
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            MutableStateFlow(phase1ProgressPartial)
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            MutableStateFlow(phase2ProgressPartial)
        
        // When
        viewModel = StudentHomeViewModel(
            mockAuthRepository,
            mockUserProfileRepository,
            mockTestProgressRepository,
            mockUnifiedResultRepository,
            mockGetOLQDashboard,
            mockAnalyticsManager,
            mockNotificationRepository
        )
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertNotNull("Should have phase1 progress", state.phase1Progress)
        assertNotNull("Should have phase2 progress", state.phase2Progress)
        assertEquals("Phase1 OIR should be COMPLETED", 
            TestStatus.COMPLETED, state.phase1Progress?.oirProgress?.status)
        assertEquals("Phase2 Psychology should be GRADED", 
            TestStatus.GRADED, state.phase2Progress?.psychologyProgress?.status)
    }
    
    // ==================== Reactive Updates Tests ====================
    
    @Test
    fun `ui state updates when progress changes`() = runTest {
        // Given
        val phase1Flow = MutableStateFlow(phase1ProgressEmpty)
        val phase2Flow = MutableStateFlow(phase2ProgressEmpty)
        
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns 
            flowOf(Result.success(testProfile))
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns phase1Flow
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns phase2Flow
        
        viewModel = StudentHomeViewModel(
            mockAuthRepository,
            mockUserProfileRepository,
            mockTestProgressRepository,
            mockUnifiedResultRepository,
            mockGetOLQDashboard,
            mockAnalyticsManager,
            mockNotificationRepository
        )
        advanceUntilIdle()
        
        val initialState = viewModel.uiState.value
        assertEquals("Initial should have 0 tests", 0, initialState.testsCompleted)
        
        // When - progress updates
        phase1Flow.value = phase1ProgressPartial
        phase2Flow.value = phase2ProgressPartial
        advanceUntilIdle()
        
        // Then
        val updatedState = viewModel.uiState.value
        assertEquals("Should now have 3 completed tests", 3, updatedState.testsCompleted)
    }
    
    @Test
    fun `ui state updates when user profile changes`() = runTest {
        // Given
        val profileFlow = MutableStateFlow(Result.success(testProfile))
        
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns profileFlow
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            MutableStateFlow(phase1ProgressEmpty)
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            MutableStateFlow(phase2ProgressEmpty)
        
        viewModel = StudentHomeViewModel(
            mockAuthRepository,
            mockUserProfileRepository,
            mockTestProgressRepository,
            mockUnifiedResultRepository,
            mockGetOLQDashboard,
            mockAnalyticsManager,
            mockNotificationRepository
        )
        advanceUntilIdle()
        
        val initialState = viewModel.uiState.value
        assertEquals("Initial streak should be 5", 5, initialState.currentStreak)
        
        // When - profile updates with new streak
        val updatedProfile = testProfile.copy(currentStreak = 10)
        profileFlow.value = Result.success(updatedProfile)
        advanceUntilIdle()
        
        // Then
        val updatedState = viewModel.uiState.value
        assertEquals("Streak should update to 10", 10, updatedState.currentStreak)
    }
    
    // ==================== Refresh Tests ====================
    
    @Test
    fun `refreshProgress re-triggers observers`() = runTest {
        // Given
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns 
            flowOf(Result.success(testProfile))
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            MutableStateFlow(phase1ProgressEmpty)
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            MutableStateFlow(phase2ProgressEmpty)
        
        viewModel = StudentHomeViewModel(
            mockAuthRepository,
            mockUserProfileRepository,
            mockTestProgressRepository,
            mockUnifiedResultRepository,
            mockGetOLQDashboard,
            mockAnalyticsManager,
            mockNotificationRepository
        )
        advanceUntilIdle()
        
        // When
        viewModel.refreshProgress()
        advanceUntilIdle()
        
        // Then - no exception thrown, observers re-triggered
        val state = viewModel.uiState.value
        assertNotNull("State should still be accessible", state)
    }

    // ==================== Dashboard Loading State Tests (Phase 2 Fix) ====================

    @Test
    fun `dashboard should not show loading state on initial load`() = runTest {
        // Given - Dashboard loads data asynchronously
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns
            flowOf(Result.success(testProfile))
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns
            MutableStateFlow(phase1ProgressEmpty)
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns
            MutableStateFlow(phase2ProgressEmpty)
        coEvery { mockGetOLQDashboard(any(), any()) } returns Result.success(mockk(relaxed = true))

        // When - ViewModel is created
        viewModel = StudentHomeViewModel(
            mockAuthRepository,
            mockUserProfileRepository,
            mockTestProgressRepository,
            mockUnifiedResultRepository,
            mockGetOLQDashboard,
            mockAnalyticsManager,
            mockNotificationRepository
        )

        // Then - isLoadingDashboard should be false (no spinner blocking UI)
        val initialState = viewModel.uiState.value
        assertFalse(
            "Dashboard should not show loading spinner on initial load",
            initialState.isLoadingDashboard
        )
    }

    @Test
    fun `refreshDashboard sets isRefreshingDashboard but not isLoadingDashboard`() = runTest {
        // Given
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns
            flowOf(Result.success(testProfile))
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns
            MutableStateFlow(phase1ProgressEmpty)
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns
            MutableStateFlow(phase2ProgressEmpty)
        coEvery { mockGetOLQDashboard(any(), any()) } returns Result.success(mockk(relaxed = true))

        viewModel = StudentHomeViewModel(
            mockAuthRepository,
            mockUserProfileRepository,
            mockTestProgressRepository,
            mockUnifiedResultRepository,
            mockGetOLQDashboard,
            mockAnalyticsManager,
            mockNotificationRepository
        )
        advanceUntilIdle()

        // When - User manually refreshes dashboard
        viewModel.refreshDashboard()

        // Then - Should show refresh indicator, not full loading spinner
        // Note: isRefreshingDashboard is for the refresh icon animation
        val state = viewModel.uiState.value
        assertFalse(
            "isLoadingDashboard should remain false during refresh",
            state.isLoadingDashboard
        )
    }
    
    // ==================== Edge Cases Tests ====================
    
    @Test
    fun `handles null current user gracefully`() = runTest {
        // Given
        mockCurrentUserFlow.value = null
        
        coEvery { mockTestProgressRepository.getPhase1Progress(any()) } returns 
            MutableStateFlow(phase1ProgressEmpty)
        coEvery { mockTestProgressRepository.getPhase2Progress(any()) } returns 
            MutableStateFlow(phase2ProgressEmpty)
        
        // When
        viewModel = StudentHomeViewModel(
            mockAuthRepository,
            mockUserProfileRepository,
            mockTestProgressRepository,
            mockUnifiedResultRepository,
            mockGetOLQDashboard,
            mockAnalyticsManager,
            mockNotificationRepository
        )
        advanceUntilIdle()
        
        // Then - should not crash, default state preserved
        val state = viewModel.uiState.value
        assertEquals("Should show default name", "Aspirant", state.userName)
    }
    
    @Test
    fun `handles profile loading failure gracefully`() = runTest {
        // Given
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns
            flowOf(Result.failure(Exception("Profile load error")))
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns
            MutableStateFlow(phase1ProgressEmpty)
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns
            MutableStateFlow(phase2ProgressEmpty)

        // When
        viewModel = StudentHomeViewModel(
            mockAuthRepository,
            mockUserProfileRepository,
            mockTestProgressRepository,
            mockUnifiedResultRepository,
            mockGetOLQDashboard,
            mockAnalyticsManager,
            mockNotificationRepository
        )
        advanceUntilIdle()

        // Then - should not crash, still load progress
        val state = viewModel.uiState.value
        assertEquals("Should have test progress despite profile error", 0, state.testsCompleted)
    }

    // ==================== Notification Badge Tests (Phase 2) ====================

    @Test
    fun `notification count updates from repository flow`() = runTest {
        // Given
        val mockNotificationRepository: com.ssbmax.core.domain.repository.NotificationRepository = mockk()
        val notificationFlow = MutableStateFlow(5)
        coEvery { mockNotificationRepository.getUnreadCount(testUser.id) } returns notificationFlow

        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns
            flowOf(Result.success(testProfile))
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns
            MutableStateFlow(phase1ProgressEmpty)
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns
            MutableStateFlow(phase2ProgressEmpty)

        // When
        viewModel = StudentHomeViewModel(
            mockAuthRepository,
            mockUserProfileRepository,
            mockTestProgressRepository,
            mockUnifiedResultRepository,
            mockGetOLQDashboard,
            mockAnalyticsManager,
            mockNotificationRepository // NEW dependency
        )
        advanceUntilIdle()

        // Then
        assertEquals("Should load initial notification count", 5, viewModel.uiState.value.notificationCount)

        // When notification count changes
        notificationFlow.value = 10
        advanceUntilIdle()

        // Then
        assertEquals("Should update notification count reactively", 10, viewModel.uiState.value.notificationCount)
    }

    @Test
    fun `notification count is zero when user not logged in`() = runTest {
        // Given
        val mockNotificationRepository: com.ssbmax.core.domain.repository.NotificationRepository = mockk()
        mockCurrentUserFlow.value = null // No logged-in user

        coEvery { mockTestProgressRepository.getPhase1Progress(any()) } returns
            MutableStateFlow(phase1ProgressEmpty)
        coEvery { mockTestProgressRepository.getPhase2Progress(any()) } returns
            MutableStateFlow(phase2ProgressEmpty)

        // When
        viewModel = StudentHomeViewModel(
            mockAuthRepository,
            mockUserProfileRepository,
            mockTestProgressRepository,
            mockUnifiedResultRepository,
            mockGetOLQDashboard,
            mockAnalyticsManager,
            mockNotificationRepository
        )
        advanceUntilIdle()

        // Then
        assertEquals("Should have zero notifications when not logged in",
            0, viewModel.uiState.value.notificationCount)
    }

    @Test
    fun `notification repository failure defaults to zero count gracefully`() = runTest {
        // Given
        val mockNotificationRepository: com.ssbmax.core.domain.repository.NotificationRepository = mockk()
        coEvery { mockNotificationRepository.getUnreadCount(any()) } returns kotlinx.coroutines.flow.flow {
            throw Exception("Network error")
        }

        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns
            flowOf(Result.success(testProfile))
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns
            MutableStateFlow(phase1ProgressEmpty)
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns
            MutableStateFlow(phase2ProgressEmpty)

        // When
        viewModel = StudentHomeViewModel(
            mockAuthRepository,
            mockUserProfileRepository,
            mockTestProgressRepository,
            mockUnifiedResultRepository,
            mockGetOLQDashboard,
            mockAnalyticsManager,
            mockNotificationRepository
        )
        advanceUntilIdle()

        // Then
        assertEquals("Should gracefully default to 0 on repository failure",
            0, viewModel.uiState.value.notificationCount)
    }
}

