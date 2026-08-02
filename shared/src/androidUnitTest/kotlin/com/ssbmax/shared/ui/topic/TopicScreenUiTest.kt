package com.ssbmax.shared.ui.topic

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.ssbmax.shared.presentation.topic.StudyMaterialItem
import com.ssbmax.shared.presentation.topic.TopicUiState
import com.ssbmax.shared.presentation.topic.TopicViewModel
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
 * Port of the pre-KMP-convergence `app/src/androidTest/.../ui/topic/TopicScreenTest.kt`
 * onto shared's [TopicScreen] -- Phase 6a-2 of the KMP-convergence plan,
 * closing one of the five coverage gaps 6a named but deliberately deferred.
 *
 * Real structural drift from the pre-cutover test this replaces, verified by
 * reading [TopicScreen]/[TopicComponents]/[TopicUiState] rather than assumed:
 * - The Android original was a single scrollable page; this port is a
 *   3-tab screen (Overview / Study Material / Tests, see [TopicScreen]'s
 *   `selectedTab`) -- study-material assertions now need `initialTab = 1`.
 * - `topicScreen_showsErrorState`: [TopicUiState.error] was dead when this
 *   phase's test-porting pass first found it (a load failure set `error`
 *   but the UI showed nothing) -- flagged as a finding, then fixed the same
 *   phase (not left as tracked debt): [TopicScreen] now renders a
 *   `TopicErrorState` in place of the tabs with a retry button.
 * - `topicScreen_studyMaterialsAreClickable`'s old assertion ("materials
 *   render or *some* clickable element exists") is replaced with a precise
 *   click -> callback assertion, a stronger test than the one it replaces.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h1600dp")
class TopicScreenUiTest {

    private lateinit var mockViewModel: TopicViewModel
    private lateinit var uiStateFlow: MutableStateFlow<TopicUiState>

    @Before
    fun setup() {
        ensureComposeResourcesContextInitialized()
        mockViewModel = mockk(relaxed = true)
        uiStateFlow = MutableStateFlow(
            TopicUiState(testType = "TAT", topicTitle = "Thematic Apperception Test", isLoading = false)
        )
        every { mockViewModel.uiState } returns uiStateFlow
    }

    @Test
    fun topicScreen_displaysTopicTitle() = runComposeUiTest {
        setContent { TopicScreen(topicId = "TAT", onNavigateBack = {}, viewModel = mockViewModel) }

        onNodeWithText("TAT", substring = true).assertIsDisplayed()
    }

    @Test
    fun topicScreen_displaysIntroductionContent() = runComposeUiTest {
        uiStateFlow.value = uiStateFlow.value.copy(introduction = "TAT is a projective test of imagination.")
        setContent { TopicScreen(topicId = "TAT", onNavigateBack = {}, viewModel = mockViewModel) }

        onNodeWithText("TAT is a projective test", substring = true).assertIsDisplayed()
    }

    @Test
    fun topicScreen_studyMaterialsAreDisplayedAndClickable() = runComposeUiTest {
        uiStateFlow.value = uiStateFlow.value.copy(
            studyMaterials = listOf(
                StudyMaterialItem("mat1", "Introduction to TAT", "15 min", isPremium = false),
                StudyMaterialItem("mat2", "Advanced TAT", "30 min", isPremium = true)
            )
        )
        var clickedId: String? = null
        setContent {
            TopicScreen(
                topicId = "TAT",
                initialTab = 1,
                onNavigateBack = {},
                onNavigateToStudyMaterial = { id -> clickedId = id },
                viewModel = mockViewModel
            )
        }

        onNodeWithText("Introduction to TAT").assertIsDisplayed()
        onNodeWithText("Advanced TAT").assertIsDisplayed()

        onNodeWithText("Introduction to TAT").performClick()

        assert(clickedId == "mat1") { "Expected click callback with mat1, got $clickedId" }
    }

    @Test
    fun topicScreen_showsNoStudyMaterialsMessage() = runComposeUiTest {
        uiStateFlow.value = uiStateFlow.value.copy(studyMaterials = emptyList())
        setContent { TopicScreen(topicId = "TAT", initialTab = 1, onNavigateBack = {}, viewModel = mockViewModel) }

        onNodeWithText("No study materials available yet", substring = true).assertIsDisplayed()
    }

    @Test
    fun topicScreen_suppressesStudyMaterialsWhileLoading() = runComposeUiTest {
        uiStateFlow.value = uiStateFlow.value.copy(
            isLoading = true,
            studyMaterials = listOf(StudyMaterialItem("mat1", "Introduction to TAT", "15 min", isPremium = false))
        )
        setContent { TopicScreen(topicId = "TAT", initialTab = 1, onNavigateBack = {}, viewModel = mockViewModel) }

        onNodeWithText("Introduction to TAT").assertDoesNotExist()
    }

    @Test
    fun topicScreen_showsErrorStateWithRetry() = runComposeUiTest {
        uiStateFlow.value = uiStateFlow.value.copy(error = "Failed to load topic content")
        setContent { TopicScreen(topicId = "TAT", onNavigateBack = {}, viewModel = mockViewModel) }

        onNodeWithText("Failed to load topic content", substring = true).assertIsDisplayed()

        onNodeWithText("Retry").performClick()

        // Called once by the screen's initial LaunchedEffect(topicId), again by retry.
        verify(exactly = 2) { mockViewModel.loadTopic("TAT") }
    }
}
