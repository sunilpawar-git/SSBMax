package com.ssbmax.shared.presentation.splash

import com.ssbmax.shared.domain.model.UserRole
import com.ssbmax.shared.domain.util.NoOpLogger
import com.ssbmax.shared.presentation.testing.FakeAuthRepository
import com.ssbmax.shared.presentation.testing.FakeUserProfileRepository
import com.ssbmax.shared.presentation.testing.testUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Characterization test, written before converting [SplashViewModel] to
 * `androidx.lifecycle.ViewModel` (Phase 1 of the KMP-convergence plan). Must
 * pass unmodified against both the pre- and post-conversion class.
 */
class SplashViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var authRepository: FakeAuthRepository
    private lateinit var userProfileRepository: FakeUserProfileRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = FakeAuthRepository()
        userProfileRepository = FakeUserProfileRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = SplashViewModel(
        authRepository = authRepository,
        userProfileRepository = userProfileRepository,
        logger = NoOpLogger()
    )

    @Test
    fun `unauthenticated user navigates to login`() = runTest(testDispatcher) {
        authRepository.isAuthenticatedResult = false
        val viewModel = buildViewModel()

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(SplashNavigationEvent.NavigateToLogin, viewModel.navigationEvent.value)
    }

    @Test
    fun `authenticated user profile timeout still navigates to student home`() = runTest(testDispatcher) {
        authRepository.isAuthenticatedResult = true
        // currentUser stays null -> withTimeoutOrNull(5000) times out
        val viewModel = buildViewModel()

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(SplashNavigationEvent.NavigateToStudentHome, viewModel.navigationEvent.value)
    }

    @Test
    fun `incomplete profile navigates to profile onboarding`() = runTest(testDispatcher) {
        authRepository.isAuthenticatedResult = true
        authRepository.userFlow.value = testUser()
        userProfileRepository.hasCompletedProfileResult = false
        val viewModel = buildViewModel()

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(SplashNavigationEvent.NavigateToProfileOnboarding, viewModel.navigationEvent.value)
    }

    @Test
    fun `student role navigates to student home`() = runTest(testDispatcher) {
        authRepository.isAuthenticatedResult = true
        authRepository.userFlow.value = testUser(role = UserRole.STUDENT)
        userProfileRepository.hasCompletedProfileResult = true
        val viewModel = buildViewModel()

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(SplashNavigationEvent.NavigateToStudentHome, viewModel.navigationEvent.value)
    }

    @Test
    fun `instructor role navigates to instructor home`() = runTest(testDispatcher) {
        authRepository.isAuthenticatedResult = true
        authRepository.userFlow.value = testUser(role = UserRole.INSTRUCTOR)
        userProfileRepository.hasCompletedProfileResult = true
        val viewModel = buildViewModel()

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(SplashNavigationEvent.NavigateToInstructorHome, viewModel.navigationEvent.value)
    }

    @Test
    fun `both role navigates to role selection`() = runTest(testDispatcher) {
        authRepository.isAuthenticatedResult = true
        authRepository.userFlow.value = testUser(role = UserRole.BOTH)
        userProfileRepository.hasCompletedProfileResult = true
        val viewModel = buildViewModel()

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(SplashNavigationEvent.NavigateToRoleSelection, viewModel.navigationEvent.value)
    }
}
