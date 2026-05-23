package com.ssbmax.ui

import android.content.ContentResolver
import android.content.Context
import android.provider.Settings
import app.cash.turbine.test
import com.ssbmax.core.domain.model.FCMToken
import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.model.SubscriptionTier
import com.ssbmax.core.domain.model.UserRole
import com.ssbmax.core.domain.repository.NotificationRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.core.domain.usecase.auth.SignOutUseCase
import com.ssbmax.testing.TestDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests for AppViewModel.
 * Covers:
 * - Global authentication state management
 * - Sign-out delegation
 * - FCM token synchronization on first login (first-install race condition fix)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var viewModel: AppViewModel
    private val mockObserveCurrentUser = mockk<ObserveCurrentUserUseCase>()
    private val mockSignOutUseCase = mockk<SignOutUseCase>()
    private val mockNotificationRepository = mockk<NotificationRepository>(relaxed = true)
    private val mockContext = mockk<Context>(relaxed = true)
    private val mockContentResolver = mockk<ContentResolver>(relaxed = true)
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

        // Mock Settings.Secure.getString for ANDROID_ID
        mockkStatic(Settings.Secure::class)
        every { Settings.Secure.getString(any(), Settings.Secure.ANDROID_ID) } returns "test-device-id"
        every { mockContext.contentResolver } returns mockContentResolver

        // Mock current user flow
        every { mockObserveCurrentUser() } returns mockCurrentUserFlow

        // Mock sign out use case
        coEvery { mockSignOutUseCase() } returns Result.success(Unit)

        // Mock notification repository defaults
        coEvery { mockNotificationRepository.getCurrentFCMToken() } returns Result.success("test-fcm-token")
        coEvery { mockNotificationRepository.saveFCMToken(any()) } returns Result.success(Unit)
    }

    private fun buildViewModel(): AppViewModel =
        AppViewModel(mockObserveCurrentUser, mockSignOutUseCase, mockNotificationRepository, mockContext)

    // ─────────────────────────────────────────────────────────────────────────
    // Auth State Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `initial currentUser is null`() = runTest {
        viewModel = buildViewModel()

        viewModel.currentUser.test {
            assertNull("Initial user should be null", awaitItem())
        }
    }

    @Test
    fun `currentUser emits when user signs in`() = runTest {
        viewModel = buildViewModel()

        viewModel.currentUser.test {
            assertNull("Initial user should be null", awaitItem())

            mockCurrentUserFlow.value = mockStudent

            val user = awaitItem()
            assertNotNull("User should not be null after sign in", user)
            assertEquals("student-123", user?.id)
            assertEquals("student@example.com", user?.email)
            assertEquals(UserRole.STUDENT, user?.role)
            assertEquals(SubscriptionTier.FREE, user?.subscriptionTier)
        }
    }

    @Test
    fun `currentUser emits null when user signs out`() = runTest {
        mockCurrentUserFlow.value = mockStudent
        viewModel = buildViewModel()

        viewModel.currentUser.test {
            assertNotNull("Initial user should not be null", awaitItem())

            mockCurrentUserFlow.value = null

            assertNull("User should be null after sign out", awaitItem())
        }
    }

    @Test
    fun `currentUser updates when user changes role`() = runTest {
        mockCurrentUserFlow.value = mockStudent
        viewModel = buildViewModel()

        viewModel.currentUser.test {
            assertEquals(UserRole.STUDENT, awaitItem()?.role)

            mockCurrentUserFlow.value = mockStudent.copy(role = UserRole.INSTRUCTOR)

            val updatedUser = awaitItem()
            assertEquals(UserRole.INSTRUCTOR, updatedUser?.role)
            assertEquals("student-123", updatedUser?.id)
        }
    }

    @Test
    fun `currentUser updates when user upgrades subscription`() = runTest {
        mockCurrentUserFlow.value = mockStudent
        viewModel = buildViewModel()

        viewModel.currentUser.test {
            assertEquals(SubscriptionTier.FREE, awaitItem()?.subscriptionTier)

            mockCurrentUserFlow.value = mockStudent.copy(subscriptionTier = SubscriptionTier.PRO)

            assertEquals(SubscriptionTier.PRO, awaitItem()?.subscriptionTier)
        }
    }

    @Test
    fun `currentUser switches between different users`() = runTest {
        viewModel = buildViewModel()

        viewModel.currentUser.test {
            assertNull(awaitItem())

            mockCurrentUserFlow.value = mockStudent
            assertEquals("student-123", awaitItem()?.id)

            mockCurrentUserFlow.value = mockInstructor
            assertEquals("instructor-456", awaitItem()?.id)
        }
    }

    @Test
    fun `currentUser StateFlow retains last value for new collectors`() = runTest {
        mockCurrentUserFlow.value = mockStudent
        viewModel = buildViewModel()

        viewModel.currentUser.test {
            val user = awaitItem()
            assertNotNull(user)
            assertEquals("student-123", user?.id)
        }
    }

    @Test
    fun `viewModel handles rapid user changes`() = runTest {
        viewModel = buildViewModel()

        viewModel.currentUser.test {
            assertNull(awaitItem())

            mockCurrentUserFlow.value = mockStudent
            assertEquals("student-123", awaitItem()?.id)

            mockCurrentUserFlow.value = null
            assertNull(awaitItem())

            mockCurrentUserFlow.value = mockInstructor
            assertEquals("instructor-456", awaitItem()?.id)

            mockCurrentUserFlow.value = mockStudent
            assertEquals("student-123", awaitItem()?.id)

            expectNoEvents()
        }
    }

    @Test
    fun `signOut calls SignOutUseCase`() = runTest {
        viewModel = buildViewModel()

        viewModel.signOut()
        advanceUntilIdle()

        coVerify { mockSignOutUseCase() }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FCM Token Sync Tests (First-Install Race Condition Fix)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `FCM token is synced when user logs in`() = runTest {
        viewModel = buildViewModel()
        advanceUntilIdle()

        mockCurrentUserFlow.value = mockStudent
        advanceUntilIdle()

        val tokenSlot = slot<FCMToken>()
        coVerify { mockNotificationRepository.getCurrentFCMToken() }
        coVerify { mockNotificationRepository.saveFCMToken(capture(tokenSlot)) }

        assertEquals("student-123", tokenSlot.captured.userId)
        assertEquals("test-fcm-token", tokenSlot.captured.token)
        assertEquals("test-device-id", tokenSlot.captured.deviceId)
        assertEquals("android", tokenSlot.captured.platform)
    }

    @Test
    fun `FCM token sync is not duplicated when same user re-emits`() = runTest {
        mockCurrentUserFlow.value = mockStudent
        viewModel = buildViewModel()
        advanceUntilIdle()

        // Re-emit same user (e.g., profile update, subscription change)
        mockCurrentUserFlow.value = mockStudent.copy(subscriptionTier = SubscriptionTier.PRO)
        advanceUntilIdle()

        // Token should only be synced once (distinctUntilChanged on user ID)
        coVerify(exactly = 1) { mockNotificationRepository.saveFCMToken(any()) }
    }

    @Test
    fun `FCM token is re-synced when a different user logs in`() = runTest {
        viewModel = buildViewModel()
        advanceUntilIdle()

        mockCurrentUserFlow.value = mockStudent
        advanceUntilIdle()

        mockCurrentUserFlow.value = null
        advanceUntilIdle()

        mockCurrentUserFlow.value = mockInstructor
        advanceUntilIdle()

        // Should have been called twice — once per distinct user ID
        coVerify(exactly = 2) { mockNotificationRepository.saveFCMToken(any()) }
    }

    @Test
    fun `FCM token sync is skipped when getCurrentFCMToken fails`() = runTest {
        coEvery { mockNotificationRepository.getCurrentFCMToken() } returns
                Result.failure(Exception("FCM unavailable"))

        viewModel = buildViewModel()
        mockCurrentUserFlow.value = mockStudent
        advanceUntilIdle()

        // saveFCMToken must NOT be called when token retrieval fails
        coVerify(exactly = 0) { mockNotificationRepository.saveFCMToken(any()) }
    }
}
