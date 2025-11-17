package com.ssbmax.ui

import app.cash.turbine.test
import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.model.SubscriptionTier
import com.ssbmax.core.domain.model.UserRole
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.core.domain.usecase.auth.SignOutUseCase
import com.ssbmax.testing.TestDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests for AppViewModel
 * Tests global authentication state management and sign out
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var viewModel: AppViewModel
    private val mockObserveCurrentUser = mockk<ObserveCurrentUserUseCase>()
    private val mockSignOutUseCase = mockk<SignOutUseCase>()
    private val mockCurrentUserFlow = MutableStateFlow<SSBMaxUser?>(null)

    private val mockStudent = SSBMaxUser(
        id = "student-123",
        email = "student@example.com",
        displayName = "Test Student",
        photoUrl = null,
        role = UserRole.STUDENT,
        subscriptionTier = SubscriptionTier.FREE,
        subscription = null,
        studentProfile = null,
        instructorProfile = null,
        createdAt = System.currentTimeMillis(),
        lastLoginAt = System.currentTimeMillis()
    )

    private val mockInstructor = SSBMaxUser(
        id = "instructor-456",
        email = "instructor@example.com",
        displayName = "Test Instructor",
        photoUrl = null,
        role = UserRole.INSTRUCTOR,
        subscriptionTier = SubscriptionTier.PRO,
        subscription = null,
        studentProfile = null,
        instructorProfile = null,
        createdAt = System.currentTimeMillis(),
        lastLoginAt = System.currentTimeMillis()
    )

    @Before
    fun setup() {
        // Mock Android Log
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.e(any(), any()) } returns 0
        every { android.util.Log.e(any(), any(), any()) } returns 0

        // Mock current user flow
        every { mockObserveCurrentUser() } returns mockCurrentUserFlow

        // Mock sign out use case
        coEvery { mockSignOutUseCase() } returns Result.success(Unit)
    }

    @Test
    fun `initial currentUser is null`() = runTest {
        // When
        viewModel = AppViewModel(mockObserveCurrentUser, mockSignOutUseCase)

        // Then
        viewModel.currentUser.test {
            val user = awaitItem()
            assertNull("Initial user should be null", user)
        }
    }

    @Test
    fun `currentUser emits when user signs in`() = runTest {
        // Given
        viewModel = AppViewModel(mockObserveCurrentUser, mockSignOutUseCase)

        viewModel.currentUser.test {
            // Initial emission
            assertNull("Initial user should be null", awaitItem())

            // When - user signs in
            mockCurrentUserFlow.value = mockStudent

            // Then
            val user = awaitItem()
            assertNotNull("User should not be null after sign in", user)
            assertEquals("User ID should match", "student-123", user?.id)
            assertEquals("User email should match", "student@example.com", user?.email)
            assertEquals("User role should be STUDENT", UserRole.STUDENT, user?.role)
            assertEquals("User tier should be FREE", SubscriptionTier.FREE, user?.subscriptionTier)
        }
    }

    @Test
    fun `currentUser emits null when user signs out`() = runTest {
        // Given - user is already signed in
        mockCurrentUserFlow.value = mockStudent
        viewModel = AppViewModel(mockObserveCurrentUser, mockSignOutUseCase)

        viewModel.currentUser.test {
            // Initial emission with user
            assertNotNull("Initial user should not be null", awaitItem())

            // When - user signs out
            mockCurrentUserFlow.value = null

            // Then
            val user = awaitItem()
            assertNull("User should be null after sign out", user)
        }
    }

    @Test
    fun `currentUser updates when user changes role`() = runTest {
        // Given - student is signed in
        mockCurrentUserFlow.value = mockStudent
        viewModel = AppViewModel(mockObserveCurrentUser, mockSignOutUseCase)

        viewModel.currentUser.test {
            // Initial emission
            val initialUser = awaitItem()
            assertEquals("Initial role should be STUDENT", UserRole.STUDENT, initialUser?.role)

            // When - user role is updated to INSTRUCTOR
            val updatedStudent = mockStudent.copy(role = UserRole.INSTRUCTOR)
            mockCurrentUserFlow.value = updatedStudent

            // Then
            val updatedUser = awaitItem()
            assertNotNull("Updated user should not be null", updatedUser)
            assertEquals("Updated role should be INSTRUCTOR", UserRole.INSTRUCTOR, updatedUser?.role)
            assertEquals("User ID should remain the same", "student-123", updatedUser?.id)
        }
    }

    @Test
    fun `currentUser updates when user upgrades subscription`() = runTest {
        // Given - user with FREE tier
        mockCurrentUserFlow.value = mockStudent
        viewModel = AppViewModel(mockObserveCurrentUser, mockSignOutUseCase)

        viewModel.currentUser.test {
            // Initial emission
            val initialUser = awaitItem()
            assertEquals("Initial tier should be FREE", SubscriptionTier.FREE, initialUser?.subscriptionTier)

            // When - user upgrades to PRO
            val upgradedStudent = mockStudent.copy(subscriptionTier = SubscriptionTier.PRO)
            mockCurrentUserFlow.value = upgradedStudent

            // Then
            val updatedUser = awaitItem()
            assertNotNull("Updated user should not be null", updatedUser)
            assertEquals("Updated tier should be PRO", SubscriptionTier.PRO, updatedUser?.subscriptionTier)
        }
    }

    @Test
    fun `currentUser switches between different users`() = runTest {
        // Given
        viewModel = AppViewModel(mockObserveCurrentUser, mockSignOutUseCase)

        viewModel.currentUser.test {
            // Initial emission
            assertNull("Initial user should be null", awaitItem())

            // When - student signs in
            mockCurrentUserFlow.value = mockStudent

            // Then
            val student = awaitItem()
            assertEquals("First user should be student", "student-123", student?.id)
            assertEquals("First user role should be STUDENT", UserRole.STUDENT, student?.role)

            // When - switch to instructor (e.g., different account on same device)
            mockCurrentUserFlow.value = mockInstructor

            // Then
            val instructor = awaitItem()
            assertEquals("Second user should be instructor", "instructor-456", instructor?.id)
            assertEquals("Second user role should be INSTRUCTOR", UserRole.INSTRUCTOR, instructor?.role)
        }
    }

    @Test
    fun `currentUser StateFlow retains last value for new collectors`() = runTest {
        // Given - user already signed in before ViewModel creation
        mockCurrentUserFlow.value = mockStudent
        viewModel = AppViewModel(mockObserveCurrentUser, mockSignOutUseCase)

        // When - new collector subscribes
        viewModel.currentUser.test {
            // Then - should immediately receive the current user
            val user = awaitItem()
            assertNotNull("New collector should immediately get current user", user)
            assertEquals("User should be the student", "student-123", user?.id)
        }
    }

    @Test
    fun `currentUser StateFlow is properly configured`() = runTest {
        // This test verifies that the StateFlow is configured correctly with proper initial value

        // Given - user signed in before ViewModel creation
        mockCurrentUserFlow.value = mockStudent
        viewModel = AppViewModel(mockObserveCurrentUser, mockSignOutUseCase)

        // When - collect using test collector
        viewModel.currentUser.test {
            val user = awaitItem()
            // Then - should get the current user (may be null initially, then user)
            if (user == null) {
                // If null, wait for next emission
                val nextUser = awaitItem()
                assertNotNull("User should eventually be emitted", nextUser)
                assertEquals("User should match", "student-123", nextUser?.id)
            } else {
                assertEquals("User should match immediately", "student-123", user.id)
            }
        }
    }

    @Test
    fun `currentUser with complete user profile data`() = runTest {
        // Given - user with complete profile
        val completeUser = SSBMaxUser(
            id = "complete-user-789",
            email = "complete@example.com",
            displayName = "Complete User",
            photoUrl = "https://example.com/photo.jpg",
            role = UserRole.STUDENT,
            subscriptionTier = SubscriptionTier.PREMIUM,
            subscription = null,
            studentProfile = null,
            instructorProfile = null,
            createdAt = 1234567890L,
            lastLoginAt = 9876543210L
        )
        mockCurrentUserFlow.value = completeUser
        viewModel = AppViewModel(mockObserveCurrentUser, mockSignOutUseCase)

        // When/Then
        viewModel.currentUser.test {
            val user = awaitItem()
            assertNotNull("User should not be null", user)
            assertEquals("ID should match", "complete-user-789", user?.id)
            assertEquals("Email should match", "complete@example.com", user?.email)
            assertEquals("Display name should match", "Complete User", user?.displayName)
            assertEquals("Photo URL should match", "https://example.com/photo.jpg", user?.photoUrl)
            assertEquals("Role should be STUDENT", UserRole.STUDENT, user?.role)
            assertEquals("Tier should be PREMIUM", SubscriptionTier.PREMIUM, user?.subscriptionTier)
            assertEquals("Created at should match", 1234567890L, user?.createdAt)
            assertEquals("Last login at should match", 9876543210L, user?.lastLoginAt)
        }
    }

    @Test
    fun `viewModel handles rapid user changes`() = runTest {
        // Given
        viewModel = AppViewModel(mockObserveCurrentUser, mockSignOutUseCase)

        viewModel.currentUser.test {
            assertNull("Initial should be null", awaitItem())

            // When - rapid sign in/out
            mockCurrentUserFlow.value = mockStudent
            assertEquals("Should emit student", "student-123", awaitItem()?.id)

            mockCurrentUserFlow.value = null
            assertNull("Should emit null", awaitItem())

            mockCurrentUserFlow.value = mockInstructor
            assertEquals("Should emit instructor", "instructor-456", awaitItem()?.id)

            mockCurrentUserFlow.value = mockStudent
            assertEquals("Should emit student again", "student-123", awaitItem()?.id)

            // Then - all transitions should be captured
            expectNoEvents()
        }
    }

    @Test
    fun `signOut calls SignOutUseCase`() = runTest {
        // Given
        viewModel = AppViewModel(mockObserveCurrentUser, mockSignOutUseCase)

        // When
        viewModel.signOut()

        // Wait for async operation
        kotlinx.coroutines.delay(100)

        // Then
        coVerify { mockSignOutUseCase() }
    }
}
