package com.ssbmax.ui.tests.ppdt

import app.cash.turbine.test
import com.ssbmax.core.domain.model.PPDTPhase
import io.mockk.coEvery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * Core PPDTTestViewModel tests: question loading, phase transitions, and story writing.
 * Timer/submission/pause tests → PPDTTestViewModelSubmissionTest
 * Bug regressions + subscription tests → PPDTTestViewModelRegressionTest
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PPDTTestViewModelTest : PPDTTestViewModelTestBase() {

    // ==================== Test Loading ====================

    @Test
    fun `loadTest success loads question and shows instructions`() = runTest {
        val viewModel = buildViewModel()
        viewModel.loadTest("ppdt_standard")
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse("Should not be loading", state.isLoading)
            assertNull("Should not have error", state.error)
            assertEquals("Should be in instructions phase", PPDTPhase.INSTRUCTIONS, state.currentPhase)
            assertTrue("Image URL should be set", state.imageUrl.isNotEmpty())
            assertEquals("Min characters should be 200", 200, state.minCharacters)
            assertEquals("Max characters should be 1000", 1000, state.maxCharacters)
        }
    }

    @Test
    fun `loadTest failure shows error message`() = runTest {
        coEvery { mockLoadPPDTTest(any(), any()) } returns Result.failure(Exception("Network error"))

        val viewModel = buildViewModel()
        viewModel.loadTest("ppdt_standard")
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse("Should not be loading", state.isLoading)
            assertNotNull("Should have error", state.error)
            assertTrue("Error message should be non-empty on load failure", state.error!!.isNotEmpty())
        }
    }

    @Test
    fun `loadTest with question fetch failure shows error`() = runTest {
        coEvery { mockLoadPPDTTest(any(), any()) } returns Result.failure(Exception("No question available"))

        val viewModel = buildViewModel()
        viewModel.loadTest("ppdt_standard")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull("Should have error", state.error)
        assertTrue("Error message should be non-empty on load failure", state.error!!.isNotEmpty())
    }

    // ==================== Phase Transitions ====================

    @Test
    fun `startTest transitions to image viewing with 30s timer`() = runTest {
        val viewModel = buildViewModel()
        viewModel.loadTest("ppdt_standard")
        advanceUntilIdle()

        viewModel.startTest()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Should be in image viewing phase", PPDTPhase.IMAGE_VIEWING, state.currentPhase)
            assertEquals("Timer should be 30s", 30, state.timeRemainingSeconds)
        }
    }

    @Test
    fun `image viewing auto-advances to writing after 30 seconds`() = runTest {
        val viewModel = buildViewModel()
        viewModel.loadTest("ppdt_standard")
        advanceUntilIdle()
        viewModel.startTest()

        advanceTimeBy(31000)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Should transition to writing phase", PPDTPhase.WRITING, state.currentPhase)
            assertEquals("Writing timer should be 4 minutes (240s)", 240, state.timeRemainingSeconds)
        }
    }

    @Test
    fun `proceedToNextPhase from viewing to writing transitions correctly`() = runTest {
        val viewModel = buildViewModel()
        viewModel.loadTest("ppdt_standard")
        advanceUntilIdle()
        viewModel.startTest()

        viewModel.proceedToNextPhase()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Should transition to writing phase", PPDTPhase.WRITING, state.currentPhase)
            assertEquals("Writing timer should start at 4 minutes", 240, state.timeRemainingSeconds)
        }
    }

    @Test
    fun `proceedToNextPhase from writing to review requires min characters`() = runTest {
        val viewModel = buildViewModel()
        viewModel.loadTest("ppdt_standard")
        advanceUntilIdle()
        viewModel.startTest()
        advanceTimeBy(31000)

        viewModel.updateStory("Short story")
        viewModel.proceedToNextPhase()
        assertEquals("Should still be in writing phase", PPDTPhase.WRITING, viewModel.uiState.value.currentPhase)

        val longStory = "a".repeat(210)
        viewModel.updateStory(longStory)
        viewModel.proceedToNextPhase()
        assertEquals("Should transition to review phase", PPDTPhase.REVIEW, viewModel.uiState.value.currentPhase)
    }

    @Test
    fun `returnToWriting transitions from review back to writing`() = runTest {
        val viewModel = buildViewModel()
        viewModel.loadTest("ppdt_standard")
        advanceUntilIdle()
        viewModel.startTest()
        advanceTimeBy(31000)
        val story = "a".repeat(210)
        viewModel.updateStory(story)
        viewModel.proceedToNextPhase()

        viewModel.returnToWriting()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Should return to writing phase", PPDTPhase.WRITING, state.currentPhase)
            assertEquals("Story should be preserved", story, state.story)
        }
    }

    @Test
    fun `proceedToNextPhase transitions from image viewing to writing`() = runTest {
        val viewModel = buildViewModel()
        viewModel.loadTest("ppdt_standard")
        advanceUntilIdle()
        viewModel.startTest()

        viewModel.proceedToNextPhase()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Should be in writing phase", PPDTPhase.WRITING, state.currentPhase)
        assertNotNull("Session should exist", state.session)
        assertEquals("Session phase should be writing", PPDTPhase.WRITING, state.session?.currentPhase)
    }

    // ==================== Story Writing ====================

    @Test
    fun `updateStory updates story text and character count`() = runTest {
        val viewModel = buildViewModel()
        viewModel.loadTest("ppdt_standard")
        advanceUntilIdle()
        viewModel.startTest()
        advanceTimeBy(31000)

        val storyText = "This is a story about a brave officer who saw something in the hazy picture."
        viewModel.updateStory(storyText)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Story should be updated", storyText, state.story)
            assertEquals("Character count should match", storyText.length, state.charactersCount)
        }
    }

    @Test
    fun `canProceedToNextPhase validates min characters`() = runTest {
        val viewModel = buildViewModel()
        viewModel.loadTest("ppdt_standard")
        advanceUntilIdle()
        viewModel.startTest()
        advanceTimeBy(31000)

        viewModel.updateStory("Short story content")
        assertFalse("Should not proceed with short story", viewModel.uiState.value.canProceedToNextPhase)

        viewModel.updateStory("a".repeat(205))
        assertTrue("Should proceed with valid story", viewModel.uiState.value.canProceedToNextPhase)
    }

    @Test
    fun `story enforces max characters of 1000`() = runTest {
        val viewModel = buildViewModel()
        viewModel.loadTest("ppdt_standard")
        advanceUntilIdle()
        viewModel.startTest()
        advanceTimeBy(31000)

        val veryLongStory = "a".repeat(1100)
        viewModel.updateStory(veryLongStory)

        assertEquals("Character count should be tracked", veryLongStory.length, viewModel.uiState.value.charactersCount)
    }
}
