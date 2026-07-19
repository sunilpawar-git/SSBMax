package com.ssbmax.ui.tests.ppdt

import app.cash.turbine.test
import com.ssbmax.shared.domain.model.PPDTPhase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PPDTTestViewModel tests for submission flow, timer management, pause, and cleanup.
 * Loading/phase/story tests → PPDTTestViewModelTest
 * Bug regressions + subscription tests → PPDTTestViewModelRegressionTest
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PPDTTestViewModelSubmissionTest : PPDTTestViewModelTestBase() {

    // ==================== Test Submission ====================

    @Test
    fun `submitTest creates submission with story data`() = runTest {
        val viewModel = buildViewModel()
        viewModel.loadTest("ppdt_standard")
        advanceUntilIdle()
        viewModel.startTest()
        advanceTimeBy(31000)

        val oneSentence = "This is a detailed story about what I perceived in the hazy image. " +
            "The characters showed leadership, courage, and determination in facing the challenge. "
        val story = oneSentence.repeat(2)
        viewModel.updateStory(story)
        viewModel.proceedToNextPhase()

        viewModel.submitTest()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse("Should not be loading", state.isLoading)
            assertTrue("Should be submitted", state.isSubmitted)
            assertNotNull("Should have submission ID", state.submissionId)
            assertNotNull("Should have subscription type", state.subscriptionType)
            assertNotNull("Should have submission", state.submission)
            val submission = state.submission!!
            assertEquals("Submission should have story", story, submission.story)
            assertTrue("Submission should have user ID", submission.userId.isNotEmpty())
        }
    }

    @Test
    fun `submission includes complete story data`() = runTest {
        val viewModel = buildViewModel()
        viewModel.loadTest("ppdt_standard")
        advanceUntilIdle()
        viewModel.startTest()
        advanceTimeBy(31000)

        val story = "a".repeat(300)
        viewModel.updateStory(story)
        viewModel.proceedToNextPhase()
        viewModel.submitTest()
        advanceUntilIdle()

        val submission = viewModel.uiState.value.submission!!
        assertEquals("Story should match", story, submission.story)
        assertEquals("Character count should match", 300, submission.charactersCount)
        assertTrue("Should have user ID", submission.userId.isNotEmpty())
        assertTrue("Submission ID should be set", submission.submissionId.isNotEmpty())
    }

    // ==================== Timer Management ====================

    @Test
    fun `viewing timer decrements correctly`() = runTest {
        val viewModel = buildViewModel()
        viewModel.loadTest("ppdt_standard")
        advanceUntilIdle()
        viewModel.startTest()

        val initialTime = viewModel.uiState.value.timeRemainingSeconds
        advanceTimeBy(5000)
        advanceUntilIdle()

        val newTime = viewModel.uiState.value.timeRemainingSeconds
        assertTrue("Time should decrease (initial=$initialTime, new=$newTime)", newTime < initialTime)
        assertTrue("Time should decrease by at least 4 seconds", initialTime - newTime >= 4)
    }

    @Test
    fun `writing timer expiry auto-advances to review`() = runTest {
        val viewModel = buildViewModel()
        viewModel.loadTest("ppdt_standard")
        advanceUntilIdle()
        viewModel.startTest()
        advanceTimeBy(31000)
        viewModel.updateStory("a".repeat(250))

        advanceTimeBy(241000)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Should auto-advance to review", PPDTPhase.REVIEW, state.currentPhase)
        }
    }

    // ==================== Pause/Resume ====================

    @Test
    fun `pauseTest cancels timer`() = runTest {
        val viewModel = buildViewModel()
        viewModel.loadTest("ppdt_standard")
        advanceUntilIdle()
        viewModel.startTest()

        val timeBeforePause = viewModel.uiState.value.timeRemainingSeconds
        viewModel.pauseTest()
        advanceTimeBy(5000)

        val timeAfterPause = viewModel.uiState.value.timeRemainingSeconds
        assertEquals("Timer should be paused", timeBeforePause, timeAfterPause)
    }

    // ==================== Cleanup ====================

    @Test
    fun `onCleared cancels timer job`() = runTest {
        val viewModel = buildViewModel()
        viewModel.loadTest("ppdt_standard")
        advanceUntilIdle()
        viewModel.startTest()

        val initialTime = viewModel.uiState.value.timeRemainingSeconds
        assertTrue("Timer was active before cleared", initialTime > 0)
    }
}
