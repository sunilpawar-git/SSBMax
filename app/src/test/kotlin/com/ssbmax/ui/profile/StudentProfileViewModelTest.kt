package com.ssbmax.ui.profile

import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.TestProgressRepository
import com.ssbmax.core.domain.repository.UserProfileRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Comprehensive tests for StudentProfileViewModel
 * Tests profile loading, data aggregation, error handling
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StudentProfileViewModelTest : BaseViewModelTest() {
    
    private lateinit var viewModel: StudentProfileViewModel
    private lateinit var mockUserProfileRepository: UserProfileRepository
    private lateinit var mockTestProgressRepository: TestProgressRepository
    private lateinit var mockObserveCurrentUser: ObserveCurrentUserUseCase
    private lateinit var mockCurrentUserFlow: MutableStateFlow<SSBMaxUser?>
    
    private val testUser = SSBMaxUser(
        id = "user-123",
        email = "test@student.com",
        displayName = "Test Student",
        photoUrl = "https://example.com/photo.jpg",
        role = UserRole.STUDENT,
        subscriptionTier = SubscriptionTier.FREE,
        createdAt = System.currentTimeMillis(),
        lastLoginAt = System.currentTimeMillis()
    )
    
    private val testUserProfile = UserProfile(
        userId = "user-123",
        fullName = "Test Student Full Name",
        age = 24,
        gender = Gender.MALE,
        entryType = EntryType.GRADUATE,
        profilePictureUrl = "https://example.com/profile.jpg",
        subscriptionType = SubscriptionType.FREE
    )
    
    private val testPhase1Progress = Phase1Progress(
        oirProgress = TestProgress(
            testType = TestType.OIR,
            status = TestStatus.GRADED,
            lastAttemptDate = System.currentTimeMillis(),
            latestScore = 85f
        ),
        ppdtProgress = TestProgress(
            testType = TestType.PPDT,
            status = TestStatus.GRADED,
            lastAttemptDate = System.currentTimeMillis(),
            latestScore = 78f
        )
    )
    
    private val testPhase2Progress = Phase2Progress(
        psychologyProgress = TestProgress(
            testType = TestType.TAT,
            status = TestStatus.GRADED,
            lastAttemptDate = System.currentTimeMillis(),
            latestScore = 82f
        ),
        gtoProgress = TestProgress(
            testType = TestType.GTO_GD,
            status = TestStatus.COMPLETED,
            lastAttemptDate = System.currentTimeMillis(),
            latestScore = 90f
        ),
        interviewProgress = TestProgress(
            testType = TestType.IO,
            status = TestStatus.GRADED,
            lastAttemptDate = System.currentTimeMillis(),
            latestScore = 88f
        )
    )
    
    @Before
    fun setup() {
        mockUserProfileRepository = mockk(relaxed = true)
        mockTestProgressRepository = mockk(relaxed = true)
        mockObserveCurrentUser = mockk(relaxed = true)
        mockCurrentUserFlow = MutableStateFlow(null)
        
        // Mock current user flow
        every { mockObserveCurrentUser() } returns mockCurrentUserFlow
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    // ==================== Load Profile Successfully Tests ====================
    
    @Test
    fun `init loads profile successfully with all data`() = runTest {
        // Given
        mockCurrentUserFlow.value = testUser
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns 
            MutableStateFlow(Result.success(testUserProfile))
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            MutableStateFlow(testPhase1Progress)
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            MutableStateFlow(testPhase2Progress)
        
        // When
        viewModel = StudentProfileViewModel(
            mockUserProfileRepository,
            mockTestProgressRepository,
            mockObserveCurrentUser
        )
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertNull("Should have no error", state.error)
        assertEquals("Should have correct user name", "Test Student Full Name", state.userName)
        assertEquals("Should have correct email", "user-123", state.userEmail)
        assertEquals("Should have correct photo URL", "https://example.com/profile.jpg", state.photoUrl)
        assertFalse("Should not be premium (FREE tier)", state.isPremium)
    }
    
    @Test
    fun `profile calculates total tests attempted correctly`() = runTest {
        // Given
        mockCurrentUserFlow.value = testUser
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns 
            MutableStateFlow(Result.success(testUserProfile))
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            MutableStateFlow(testPhase1Progress)
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            MutableStateFlow(testPhase2Progress)
        
        // When
        viewModel = StudentProfileViewModel(
            mockUserProfileRepository,
            mockTestProgressRepository,
            mockObserveCurrentUser
        )
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        // OIR (85), PPDT (78), Psychology (82), GTO (90), Interview (88) = 5 tests
        assertEquals("Should count 5 tests with scores", 5, state.totalTestsAttempted)
    }
    
    @Test
    fun `profile calculates average score correctly`() = runTest {
        // Given
        mockCurrentUserFlow.value = testUser
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns 
            MutableStateFlow(Result.success(testUserProfile))
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            MutableStateFlow(testPhase1Progress)
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            MutableStateFlow(testPhase2Progress)
        
        // When
        viewModel = StudentProfileViewModel(
            mockUserProfileRepository,
            mockTestProgressRepository,
            mockObserveCurrentUser
        )
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        // Average: (85 + 78 + 82 + 90 + 88) / 5 = 84.6
        assertEquals("Should calculate average score", 84.6f, state.averageScore, 0.1f)
    }
    
    @Test
    fun `profile calculates phase1 completion correctly`() = runTest {
        // Given
        mockCurrentUserFlow.value = testUser
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns 
            MutableStateFlow(Result.success(testUserProfile))
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            MutableStateFlow(testPhase1Progress)
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            MutableStateFlow(testPhase2Progress)
        
        // When
        viewModel = StudentProfileViewModel(
            mockUserProfileRepository,
            mockTestProgressRepository,
            mockObserveCurrentUser
        )
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        // Both OIR and PPDT have scores: 50 + 50 = 100%
        assertEquals("Should have 100% phase1 completion", 100, state.phase1Completion)
    }
    
    @Test
    fun `profile calculates phase2 completion correctly`() = runTest {
        // Given
        mockCurrentUserFlow.value = testUser
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns 
            MutableStateFlow(Result.success(testUserProfile))
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            MutableStateFlow(testPhase1Progress)
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            MutableStateFlow(testPhase2Progress)
        
        // When
        viewModel = StudentProfileViewModel(
            mockUserProfileRepository,
            mockTestProgressRepository,
            mockObserveCurrentUser
        )
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        // All 3 tests have scores: 3 * 33 = 99% (coerced to max 100)
        assertEquals("Should have 99% phase2 completion", 99, state.phase2Completion)
    }
    
    // ==================== Handle Missing Data Tests ====================
    
    @Test
    fun `profile with no test scores calculates zero average`() = runTest {
        // Given
        mockCurrentUserFlow.value = testUser
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns 
            MutableStateFlow(Result.success(testUserProfile))
        
        val emptyPhase1 = Phase1Progress(
            oirProgress = TestProgress(testType = TestType.OIR),
            ppdtProgress = TestProgress(testType = TestType.PPDT)
        )
        val emptyPhase2 = Phase2Progress(
            psychologyProgress = TestProgress(testType = TestType.TAT),
            gtoProgress = TestProgress(testType = TestType.GTO_GD),
            interviewProgress = TestProgress(testType = TestType.IO)
        )
        
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            MutableStateFlow(emptyPhase1)
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            MutableStateFlow(emptyPhase2)
        
        // When
        viewModel = StudentProfileViewModel(
            mockUserProfileRepository,
            mockTestProgressRepository,
            mockObserveCurrentUser
        )
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should have 0 tests attempted", 0, state.totalTestsAttempted)
        assertEquals("Should have 0 average score", 0f, state.averageScore)
        assertEquals("Should have 0% phase1 completion", 0, state.phase1Completion)
        assertEquals("Should have 0% phase2 completion", 0, state.phase2Completion)
    }
    
    @Test
    fun `profile loads with null user profile uses fallback data`() = runTest {
        // Given
        mockCurrentUserFlow.value = testUser
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns 
            MutableStateFlow(Result.success(null)) // No profile
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            MutableStateFlow(testPhase1Progress)
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            MutableStateFlow(testPhase2Progress)
        
        // When
        viewModel = StudentProfileViewModel(
            mockUserProfileRepository,
            mockTestProgressRepository,
            mockObserveCurrentUser
        )
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertNull("Should have no error", state.error)
        // Should fallback to SSBMaxUser data
        assertEquals("Should use displayName as fallback", "Test Student", state.userName)
        assertEquals("Should use email as fallback", "test@student.com", state.userEmail)
    }
    
    @Test
    fun `profile with premium subscription shows premium status`() = runTest {
        // Given
        val premiumProfile = testUserProfile.copy(subscriptionType = SubscriptionType.PREMIUM)
        mockCurrentUserFlow.value = testUser
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns 
            MutableStateFlow(Result.success(premiumProfile))
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } returns 
            MutableStateFlow(testPhase1Progress)
        coEvery { mockTestProgressRepository.getPhase2Progress(testUser.id) } returns 
            MutableStateFlow(testPhase2Progress)
        
        // When
        viewModel = StudentProfileViewModel(
            mockUserProfileRepository,
            mockTestProgressRepository,
            mockObserveCurrentUser
        )
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertTrue("Should be premium user", state.isPremium)
    }
    
    // ==================== Error Handling Tests ====================
    
    @Test
    fun `profile with no authenticated user shows error`() = runTest {
        // Given - no user logged in
        mockCurrentUserFlow.value = null
        
        // When
        viewModel = StudentProfileViewModel(
            mockUserProfileRepository,
            mockTestProgressRepository,
            mockObserveCurrentUser
        )
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertNotNull("Should have error", state.error)
        assertTrue(
            "Error should indicate login required",
            state.error?.contains("login") == true
        )
    }
    
    @Test
    fun `profile handles repository exception gracefully`() = runTest {
        // Given
        mockCurrentUserFlow.value = testUser
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } throws 
            RuntimeException("Network error")
        
        // When
        viewModel = StudentProfileViewModel(
            mockUserProfileRepository,
            mockTestProgressRepository,
            mockObserveCurrentUser
        )
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertNotNull("Should have error", state.error)
        assertTrue(
            "Error message should mention network error",
            state.error?.contains("Network error") == true ||
            state.error?.contains("Failed to load profile") == true
        )
    }
    
    @Test
    fun `profile handles test progress failure gracefully`() = runTest {
        // Given
        mockCurrentUserFlow.value = testUser
        coEvery { mockUserProfileRepository.getUserProfile(testUser.id) } returns 
            MutableStateFlow(Result.success(testUserProfile))
        coEvery { mockTestProgressRepository.getPhase1Progress(testUser.id) } throws 
            Exception("Progress load failed")
        
        // When
        viewModel = StudentProfileViewModel(
            mockUserProfileRepository,
            mockTestProgressRepository,
            mockObserveCurrentUser
        )
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertNotNull("Should have error", state.error)
    }
    
    // ==================== UI State Tests ====================
    
    @Test
    fun `ui state has correct default values`() {
        // Test the UI state data class defaults
        val defaultState = StudentProfileUiState()
        
        assertEquals("", defaultState.userName)
        assertEquals("", defaultState.userEmail)
        assertNull(defaultState.photoUrl)
        assertFalse(defaultState.isPremium)
        assertEquals(0, defaultState.totalTestsAttempted)
        assertEquals(0, defaultState.totalStudyHours)
        assertEquals(0, defaultState.streakDays)
        assertEquals(0f, defaultState.averageScore)
        assertEquals(0, defaultState.phase1Completion)
        assertEquals(0, defaultState.phase2Completion)
        assertTrue(defaultState.recentAchievements.isEmpty())
        assertTrue(defaultState.recentTests.isEmpty())
        assertTrue(defaultState.isLoading)
        assertNull(defaultState.error)
    }
}

