package com.ssbmax.shared.ui.tat

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.ssbmax.shared.domain.model.SubscriptionTier
import com.ssbmax.shared.domain.model.TATPhase
import com.ssbmax.shared.domain.model.TATQuestion
import com.ssbmax.shared.domain.model.TATStoryResponse
import com.ssbmax.shared.presentation.tat.TATTestUiState
import com.ssbmax.shared.presentation.tat.TATTestViewModel
import com.ssbmax.shared.testing.ensureComposeResourcesContextInitialized
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Port of the pre-KMP-convergence `app/src/androidTest/.../ui/tests/tat/TATTestScreenTest.kt`
 * onto shared's [TATTestScreen] (Phase 6a of the KMP-convergence plan) --
 * the "one full test-taking flow, start to result" the plan calls out as
 * priority, alongside [TATSubmissionResultScreenUiTest].
 *
 * Unlike [com.ssbmax.shared.ui.auth.LoginScreenUiTest]/
 * [com.ssbmax.shared.ui.home.student.StudentHomeScreenUiTest], [TATTestScreen]
 * kept its `viewModel: TATTestViewModel = koinViewModel()` override
 * parameter (same precedent as OIR/SRT/WAT/Topic/Profile screens), so the
 * mock is passed directly, exactly like the pre-cutover test -- no Koin
 * scaffolding needed here.
 *
 * Real structural drift from the pre-cutover test this replaces, all
 * verified by reading [TATTestScreen]/`TATComponents.kt`/`TATDialogs.kt`
 * rather than assumed:
 * - Picture-progress copy changed from "Picture 1/12" to
 *   "Picture 1 of 12" (`tat_picture_number`/`tat_image_number`), and now
 *   renders in TWO places at once (the screen's top bar AND
 *   [com.ssbmax.shared.ui.tat.components.phases.TATImageViewingPhase]'s own
 *   header) -- assertions use `onAllNodesWithText(...).onFirst()`.
 * - The exit icon's content description is "Back" (`tat_back_cd`), not
 *   "Exit" -- the dialog it opens is still titled "Exit Test?".
 * - `TATBottomBar` no longer has a "Previous" button at all (only
 *   Next/Submit Test) -- the old `bottomBar_navigationButtonsWork` test's
 *   "Previous" assertion is dropped, not silently ignored.
 * - The old completed-count "3/12" text does not exist anywhere in the new
 *   UI (no separate completed-stories readout) -- `progressIndicator_showsCurrentProgress`
 *   asserts the top bar's "Picture N of 12" instead.
 * - On the last REVIEW-phase question, BOTH `TATBottomBar` (canSubmit=true)
 *   and `TATReviewPhase`'s own inline button show "Submit Test" -- two
 *   matches, not one.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h1600dp")
class TATTestScreenUiTest {

    private lateinit var mockViewModel: TATTestViewModel
    private lateinit var uiStateFlow: MutableStateFlow<TATTestUiState>
    private lateinit var testQuestions: List<TATQuestion>

    @Before
    fun setup() {
        ensureComposeResourcesContextInitialized()
        testQuestions = (1..12).map { position ->
            TATQuestion(id = "tat-$position", imageUrl = "https://example.com/tat$position.jpg", cardPosition = position)
        }
        mockViewModel = mockk(relaxed = true)
        uiStateFlow = MutableStateFlow(
            TATTestUiState(isLoading = false, testId = "test-123", questions = testQuestions, phase = TATPhase.INSTRUCTIONS)
        )
        every { mockViewModel.uiState } returns uiStateFlow
    }

    @Test
    fun instructionsScreen_displaysCorrectly() = runComposeUiTest {
        setContent { TATTestScreen(testId = "test-123", viewModel = mockViewModel) }

        onNodeWithText("TAT Test").assertIsDisplayed()
        onNodeWithText("TAT - Thematic Apperception Test", substring = true).assertIsDisplayed()
        onNodeWithText("Start Test").assertIsDisplayed()
    }

    @Test
    fun startButton_startsTest() = runComposeUiTest {
        setContent { TATTestScreen(testId = "test-123", viewModel = mockViewModel) }

        onNodeWithText("Start Test").performClick()

        verify { mockViewModel.startTest() }
    }

    @Test
    fun imageViewingPhase_displaysImageAndTimer() = runComposeUiTest {
        uiStateFlow.value = uiStateFlow.value.copy(
            phase = TATPhase.IMAGE_VIEWING,
            currentQuestionIndex = 0,
            viewingTimeRemaining = 30
        )
        setContent { TATTestScreen(testId = "test-123", viewModel = mockViewModel) }

        // Renders in both the top bar and TATImageViewingPhase's own header.
        onAllNodesWithText("Picture 1 of 12", substring = true).onFirst().assertIsDisplayed()
        onNodeWithText("30", substring = true).assertIsDisplayed()
    }

    @Test
    fun writingPhase_displaysStoryInputAndTimer() = runComposeUiTest {
        uiStateFlow.value = uiStateFlow.value.copy(
            phase = TATPhase.WRITING,
            currentQuestionIndex = 0,
            currentStory = ""
        )
        setContent { TATTestScreen(testId = "test-123", viewModel = mockViewModel) }

        onNodeWithText("Write Your Story", substring = true).assertIsDisplayed()
        // "0 / 1500 characters" -- TATQuestion.maxCharacters defaults to 1500.
        onNodeWithText("0 / 1500", substring = true).assertIsDisplayed()
    }

    @Test
    fun writingPhase_storyInputWorks() = runComposeUiTest {
        uiStateFlow.value = uiStateFlow.value.copy(
            phase = TATPhase.WRITING,
            currentQuestionIndex = 0,
            currentStory = ""
        )
        setContent { TATTestScreen(testId = "test-123", viewModel = mockViewModel) }

        onNodeWithText(
            "Write a complete story with characters, situation, action, and outcome...",
            substring = true
        ).assertIsDisplayed()
    }

    @Test
    fun progressIndicator_showsCurrentProgress() = runComposeUiTest {
        // Writing phase, on the 4th picture (index 3) with 3 stories completed.
        uiStateFlow.value = uiStateFlow.value.copy(
            phase = TATPhase.WRITING,
            currentQuestionIndex = 3,
            responses = List(3) { i ->
                TATStoryResponse(
                    questionId = testQuestions[i].id,
                    story = "Story $i",
                    charactersCount = 200,
                    viewingTimeTakenSeconds = 30,
                    writingTimeTakenSeconds = 240,
                    submittedAt = i.toLong()
                )
            }
        )
        setContent { TATTestScreen(testId = "test-123", viewModel = mockViewModel) }

        onAllNodesWithText("Picture 4 of 12", substring = true).onFirst().assertIsDisplayed()
    }

    @Test
    fun bottomBar_nextButton_visibleAndEnabled_whenStoryIsValid() = runComposeUiTest {
        uiStateFlow.value = uiStateFlow.value.copy(
            phase = TATPhase.WRITING,
            currentQuestionIndex = 1,
            currentStory = "A".repeat(200) // within 150-1500 -> canMoveToNextQuestion true
        )
        setContent { TATTestScreen(testId = "test-123", viewModel = mockViewModel) }

        onNodeWithText("Next").assertIsDisplayed()
    }

    @Test
    fun completedTest_triggersCallback() = runComposeUiTest {
        val submissionId = "sub-123"
        var completedId: String? = null
        var completedType: SubscriptionTier? = null
        uiStateFlow.value = uiStateFlow.value.copy(
            phase = TATPhase.SUBMITTED,
            isSubmitted = true,
            submissionId = submissionId,
            subscriptionType = SubscriptionTier.FREE
        )

        setContent {
            TATTestScreen(
                testId = "test-123",
                viewModel = mockViewModel,
                onTestComplete = { id, type -> completedId = id; completedType = type }
            )
        }
        waitForIdle()

        assert(completedId == submissionId) { "Expected callback with $submissionId, got $completedId" }
        assert(completedType == SubscriptionTier.FREE)
    }

    @Test
    fun backButton_showsExitDialog() = runComposeUiTest {
        uiStateFlow.value = uiStateFlow.value.copy(phase = TATPhase.WRITING, currentQuestionIndex = 0)
        setContent { TATTestScreen(testId = "test-123", viewModel = mockViewModel) }

        // Content description is "Back" (tat_back_cd), not "Exit" -- verified
        // by reading TATTestScreen.kt's IconButton.
        onNodeWithContentDescription("Back").performClick()

        onNodeWithText("Exit Test", substring = true).assertIsDisplayed()
    }

    @Test
    fun loadingState_showsLoadingIndicator() = runComposeUiTest {
        uiStateFlow.value = uiStateFlow.value.copy(isLoading = true)
        setContent { TATTestScreen(testId = "test-123", viewModel = mockViewModel) }

        onNodeWithText("Loading", substring = true).assertIsDisplayed()
    }

    @Test
    fun errorState_displaysErrorMessage() = runComposeUiTest {
        uiStateFlow.value = uiStateFlow.value.copy(isLoading = false, error = "Failed to load test questions")
        setContent { TATTestScreen(testId = "test-123", viewModel = mockViewModel) }

        onNodeWithText("Failed to load test questions", substring = true).assertIsDisplayed()
    }

    @Test
    fun reviewPhase_displaysStoryReview() = runComposeUiTest {
        val testStory = "This is a test story with sufficient length to meet the minimum character requirement."
        uiStateFlow.value = uiStateFlow.value.copy(
            phase = TATPhase.REVIEW,
            currentQuestionIndex = 0,
            currentStory = testStory
        )
        setContent { TATTestScreen(testId = "test-123", viewModel = mockViewModel) }

        onNodeWithText(testStory, substring = true).assertIsDisplayed()
    }

    @Test
    fun lastQuestion_reviewShowsSubmitInsteadOfNext() = runComposeUiTest {
        val testStory = "A".repeat(200)
        uiStateFlow.value = uiStateFlow.value.copy(
            phase = TATPhase.REVIEW,
            currentQuestionIndex = testQuestions.lastIndex,
            currentStory = testStory,
            responses = testQuestions.dropLast(1).mapIndexed { index, question ->
                TATStoryResponse(
                    questionId = question.id,
                    story = "Story $index",
                    charactersCount = 200,
                    viewingTimeTakenSeconds = 30,
                    writingTimeTakenSeconds = 240,
                    submittedAt = index.toLong()
                )
            }
        )
        setContent { TATTestScreen(testId = "test-123", viewModel = mockViewModel) }

        // Both TATBottomBar (canSubmit=true on the last question) and
        // TATReviewPhase's own inline button show "Submit Test" here.
        onAllNodesWithText("Submit Test").onFirst().assertIsDisplayed()
        onNodeWithText("Next").assertDoesNotExist()
    }
}
