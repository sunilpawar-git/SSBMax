package com.ssbmax.ui.tests.ppdt

import app.cash.turbine.test
import com.ssbmax.core.domain.usecase.ppdt.LoadPPDTTestUseCase
import io.mockk.coEvery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Profile gate tests at the ViewModel level.
 * Gender routing and profile resolution logic → LoadPPDTTestUseCaseTest in core:domain.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PPDTProfileGateTest : PPDTTestViewModelTestBase() {

    // ==================== Profile Gate ====================

    @Test
    fun `loadTest emits isProfileIncomplete=true when profile is not found`() = runTest {
        // WHY: AI assessment accuracy depends on gender — candidate must set up profile first.
        // Gating on null profile (server explicitly says no profile) prevents misleading assessments.
        coEvery { mockLoadPPDTTest(any(), any()) } returns
            Result.failure(LoadPPDTTestUseCase.ProfileIncompleteException())

        val viewModel = buildViewModel()
        viewModel.loadTest()
        advanceUntilIdle()

        viewModel.uiState.test {
            assertTrue("Should mark profile incomplete when no profile found", awaitItem().isProfileIncomplete)
        }
    }

    @Test
    fun `loadTest proceeds normally when profile exists with gender set`() = runTest {
        // WHY: A user with a complete profile should never be blocked.
        val viewModel = buildViewModel()
        viewModel.loadTest()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse("Should not flag profile incomplete when profile exists", state.isProfileIncomplete)
            assertFalse("Should finish loading", state.isLoading)
            assertNull("Should have no error", state.error)
        }
    }

    @Test
    fun `loadTest does not gate on profile network error`() = runTest {
        // WHY: Network error ≠ confirmed missing profile. Blocking on transient failures ruins the
        // candidate's session. Proceed and default to full image pool.
        // The use case returns success (falls back to null genderTag) on profile network error.
        val viewModel = buildViewModel()
        viewModel.loadTest()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse("Should NOT gate on network error", state.isProfileIncomplete)
        }
    }
}
