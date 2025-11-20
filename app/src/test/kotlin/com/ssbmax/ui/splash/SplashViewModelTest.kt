package com.ssbmax.ui.splash

import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.model.SubscriptionTier
import com.ssbmax.core.domain.model.UserRole
import com.ssbmax.core.domain.repository.AuthRepository
import com.ssbmax.core.domain.repository.UserProfileRepository
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Comprehensive tests for SplashViewModel
 * Tests authentication routing logic and splash screen navigation
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest : BaseViewModelTest() {
    
    private lateinit var viewModel: SplashViewModel
    private lateinit var mockAuthRepository: AuthRepository
    private lateinit var mockUserProfileRepository: UserProfileRepository
    private lateinit var mockCurrentUserFlow: MutableStateFlow<SSBMaxUser?>
    
    private val testStudent = SSBMaxUser(
        id = "student-123",
        email = "student@test.com",
        displayName = "Test Student",
        photoUrl = null,
        role = UserRole.STUDENT,
        subscriptionTier = SubscriptionTier.FREE,
        createdAt = System.currentTimeMillis(),
        lastLoginAt = System.currentTimeMillis()
    )
    
    private val testInstructor = SSBMaxUser(
        id = "instructor-123",
        email = "instructor@test.com",
        displayName = "Test Instructor",
        photoUrl = null,
        role = UserRole.INSTRUCTOR,
        subscriptionTier = SubscriptionTier.PRO,
        createdAt = System.currentTimeMillis(),
        lastLoginAt = System.currentTimeMillis()
    )
    
    private val testBothRoles = SSBMaxUser(
        id = "both-123",
        email = "both@test.com",
        displayName = "Test Both",
        photoUrl = null,
        role = UserRole.BOTH,
        subscriptionTier = SubscriptionTier.PREMIUM,
        createdAt = System.currentTimeMillis(),
        lastLoginAt = System.currentTimeMillis()
    )
    
    @Before
    fun setup() {
        mockAuthRepository = mockk(relaxed = true)
        mockUserProfileRepository = mockk(relaxed = true)
        mockCurrentUserFlow = MutableStateFlow(null)
        
        // Mock currentUser as StateFlow
        every { mockAuthRepository.currentUser } returns mockCurrentUserFlow
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    // ==================== Not Authenticated Tests ====================
    
    @Test
    fun `init with no user navigates to login after 2 second delay`() = runTest {
        // Given - no user (currentUser = null)
        mockCurrentUserFlow.value = null
        
        // When
        viewModel = SplashViewModel(mockAuthRepository, mockUserProfileRepository)
        
        // Advance time by 2 seconds (splash delay)
        advanceTimeBy(2000)
        advanceUntilIdle()
        
        // Then
        val navigationEvent = viewModel.navigationEvent.value
        assertEquals(
            "Should navigate to login when no user",
            SplashNavigationEvent.NavigateToLogin,
            navigationEvent
        )
    }
    
    @Test
    fun `splash delay is minimum 2 seconds before navigation`() = runTest {
        // Given
        mockCurrentUserFlow.value = null
        
        // When
        viewModel = SplashViewModel(mockAuthRepository, mockUserProfileRepository)
        
        // Advance by 1 second (less than 2)
        advanceTimeBy(1000)
        
        // Then - should still be null
        assertNull(
            "Navigation should not happen before 2 seconds",
            viewModel.navigationEvent.value
        )
        
        // Advance remaining 1 second + processing
        advanceTimeBy(1000)
        advanceUntilIdle()
        
        // Then - should navigate now
        assertNotNull(
            "Navigation should happen after 2 seconds",
            viewModel.navigationEvent.value
        )
    }
    
    // ==================== Authenticated Without Profile Tests ====================
    
    @Test
    fun `user without completed profile navigates to onboarding`() = runTest {
        // Given - user exists but no profile
        mockCurrentUserFlow.value = testStudent
        coEvery { mockUserProfileRepository.hasCompletedProfile(testStudent.id) } returns 
            MutableStateFlow(false)
        
        // When
        viewModel = SplashViewModel(mockAuthRepository, mockUserProfileRepository)
        advanceTimeBy(2000)
        advanceUntilIdle()
        
        // Then
        val navigationEvent = viewModel.navigationEvent.value
        assertEquals(
            "Should navigate to profile onboarding when profile incomplete",
            SplashNavigationEvent.NavigateToProfileOnboarding,
            navigationEvent
        )
    }
    
    // ==================== Student Role Tests ====================
    
    @Test
    fun `student with completed profile navigates to student home`() = runTest {
        // Given
        mockCurrentUserFlow.value = testStudent
        coEvery { mockUserProfileRepository.hasCompletedProfile(testStudent.id) } returns 
            MutableStateFlow(true)
        
        // When
        viewModel = SplashViewModel(mockAuthRepository, mockUserProfileRepository)
        advanceTimeBy(2000)
        advanceUntilIdle()
        
        // Then
        val navigationEvent = viewModel.navigationEvent.value
        assertEquals(
            "Should navigate to student home for student role",
            SplashNavigationEvent.NavigateToStudentHome,
            navigationEvent
        )
    }
    
    // ==================== Instructor Role Tests ====================
    
    @Test
    fun `instructor with completed profile navigates to instructor home`() = runTest {
        // Given
        mockCurrentUserFlow.value = testInstructor
        coEvery { mockUserProfileRepository.hasCompletedProfile(testInstructor.id) } returns 
            MutableStateFlow(true)
        
        // When
        viewModel = SplashViewModel(mockAuthRepository, mockUserProfileRepository)
        advanceTimeBy(2000)
        advanceUntilIdle()
        
        // Then
        val navigationEvent = viewModel.navigationEvent.value
        assertEquals(
            "Should navigate to instructor home for instructor role",
            SplashNavigationEvent.NavigateToInstructorHome,
            navigationEvent
        )
    }
    
    // ==================== Both Roles Tests ====================
    
    @Test
    fun `user with both roles navigates to role selection`() = runTest {
        // Given
        mockCurrentUserFlow.value = testBothRoles
        coEvery { mockUserProfileRepository.hasCompletedProfile(testBothRoles.id) } returns 
            MutableStateFlow(true)
        
        // When
        viewModel = SplashViewModel(mockAuthRepository, mockUserProfileRepository)
        advanceTimeBy(2000)
        advanceUntilIdle()
        
        // Then
        val navigationEvent = viewModel.navigationEvent.value
        assertEquals(
            "Should navigate to role selection for user with both roles",
            SplashNavigationEvent.NavigateToRoleSelection,
            navigationEvent
        )
    }
    
    // ==================== State Tests ====================
    
    @Test
    fun `navigation event is initially null before init completes`() = runTest {
        // Given
        mockCurrentUserFlow.value = testStudent
        coEvery { mockUserProfileRepository.hasCompletedProfile(testStudent.id) } returns 
            MutableStateFlow(true)
        
        // When
        viewModel = SplashViewModel(mockAuthRepository, mockUserProfileRepository)
        
        // Then - before time advance
        assertNull(
            "Navigation event should be null before splash delay",
            viewModel.navigationEvent.value
        )
    }
    
    @Test
    fun `all navigation events are sealed class instances`() {
        // Test that all navigation events are properly defined
        val events = listOf(
            SplashNavigationEvent.NavigateToLogin,
            SplashNavigationEvent.NavigateToStudentHome,
            SplashNavigationEvent.NavigateToInstructorHome,
            SplashNavigationEvent.NavigateToRoleSelection,
            SplashNavigationEvent.NavigateToProfileOnboarding
        )
        
        events.forEach { event ->
            assertTrue(
                "All events should be instances of SplashNavigationEvent",
                event is SplashNavigationEvent
            )
        }
    }
}

