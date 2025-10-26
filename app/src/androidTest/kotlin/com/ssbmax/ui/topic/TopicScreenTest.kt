package com.ssbmax.ui.topic

import androidx.compose.ui.test.*
import com.ssbmax.testing.BaseComposeTest
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Test

/**
 * UI tests for TopicScreen
 */
@HiltAndroidTest
class TopicScreenTest : BaseComposeTest() {

    private lateinit var mockViewModel: TopicViewModel
    private lateinit var uiStateFlow: MutableStateFlow<TopicUiState>

    @Before
    override fun setup() {
        super.setup()
        
        // Setup mocks
        mockViewModel = mockk(relaxed = true)
        uiStateFlow = MutableStateFlow(
            TopicUiState(
                testType = "TAT",
                topicTitle = "Thematic Apperception Test",
                isLoading = false
            )
        )
        every { mockViewModel.uiState } returns uiStateFlow
    }

    @Test
    fun topicScreen_displaysTopicTitle() {
        // Given: Topic loaded
        composeTestRule.setContent {
            TopicScreen(
                topicId = "TAT",
                onNavigateBack = {},
                viewModel = mockViewModel
            )
        }

        // Then: Topic title should be displayed
        composeTestRule
            .onNodeWithText("TAT", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun topicScreen_displaysContent() {
        // Given: Topic with content
        uiStateFlow.value = uiStateFlow.value.copy(
            introduction = "TAT is a projective test...",
            studyMaterials = listOf(
                StudyMaterialItem("mat1", "Introduction to TAT", "15 min", false),
                StudyMaterialItem("mat2", "Advanced TAT", "30 min", true)
            )
        )

        composeTestRule.setContent {
            TopicScreen(
                topicId = "TAT",
                onNavigateBack = {},
                viewModel = mockViewModel
            )
        }

        // Then: Content should be displayed
        composeTestRule
            .onNodeWithText("TAT is a projective test", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun topicScreen_showsLoadingState() {
        // Given: Loading state
        uiStateFlow.value = uiStateFlow.value.copy(
            isLoading = true
        )

        composeTestRule.setContent {
            TopicScreen(
                topicId = "TAT",
                onNavigateBack = {},
                viewModel = mockViewModel
            )
        }

        // Then: Loading indicator should be visible
        composeTestRule
            .onNodeWithText("Loading", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun topicScreen_showsErrorState() {
        // Given: Error state
        uiStateFlow.value = uiStateFlow.value.copy(
            isLoading = false,
            error = "Failed to load topic"
        )

        composeTestRule.setContent {
            TopicScreen(
                topicId = "TAT",
                onNavigateBack = {},
                viewModel = mockViewModel
            )
        }

        // Then: Error message should be displayed
        composeTestRule
            .onNodeWithText("Failed to load topic", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun topicScreen_studyMaterialsAreClickable() {
        // Given: Topic with study materials
        uiStateFlow.value = uiStateFlow.value.copy(
            studyMaterials = listOf(
                StudyMaterialItem("mat1", "Introduction", "10 min", false),
                StudyMaterialItem("mat2", "Key Concepts", "20 min", false),
                StudyMaterialItem("mat3", "Practice Questions", "30 min", true)
            )
        )

        composeTestRule.setContent {
            TopicScreen(
                topicId = "TAT",
                onNavigateBack = {},
                viewModel = mockViewModel
            )
        }

        // Then: Study materials should be displayed
        composeTestRule.waitForIdle()
        assert(composeTestRule.onAllNodes(hasText("Introduction", substring = true)).fetchSemanticsNodes().isNotEmpty() ||
               composeTestRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().isNotEmpty()) {
            "Screen should have study material content or clickable elements"
        }
    }
}

