package com.ssbmax.shared.ui.topic

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ssbmax.shared.presentation.topic.TopicViewModel
import com.ssbmax.shared.ui.common.TabSwipeableContent
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.topic_tab_overview
import ssbmax.shared.generated.resources.topic_tab_study_material

/**
 * KMP port of the Android `app/.../ui/topic/TopicScreen.kt` -- 3-tab topic
 * detail screen (Overview / Study Material / Tests) for a specific SSB test
 * topic. Tab content composables extracted into [TopicComponents] to stay
 * under the 300-line Quality Limit. See [TopicViewModel] for the
 * ViewModel-level porting notes.
 */
@Composable
fun TopicScreen(
    topicId: String,
    initialTab: Int = 0,
    onNavigateBack: () -> Unit,
    onNavigateToStudyMaterial: (String) -> Unit = {},
    onNavigateToTest: (String) -> Unit = {},
    onNavigateToInterviewResult: (String) -> Unit = {},
    viewModel: TopicViewModel = koinInject(),
    modifier: Modifier = Modifier
) {
    DisposableEffect(Unit) {
        onDispose { viewModel.close() }
    }

    LaunchedEffect(topicId) {
        viewModel.loadTopic(topicId)
    }

    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab) }

    Scaffold(
        topBar = {
            TopicTopBar(title = uiState.topicTitle, testType = topicId, onNavigateBack = onNavigateBack)
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    label = { Text(stringResource(Res.string.topic_tab_overview)) },
                    alwaysShowLabel = true
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null) },
                    label = { Text(stringResource(Res.string.topic_tab_study_material)) },
                    alwaysShowLabel = true
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null) },
                    label = {
                        Text(
                            if (topicId.equals("PIQ", ignoreCase = true) || topicId.equals("PIQ_FORM", ignoreCase = true)) {
                                "Fill PIQ"
                            } else {
                                "Tests"
                            }
                        )
                    },
                    alwaysShowLabel = true
                )
            }
        }
    ) { paddingValues ->
        TabSwipeableContent(
            currentIndex = selectedTab,
            totalTabs = 3,
            onTabChange = { newTab -> selectedTab = newTab },
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> IntroductionTab(introduction = uiState.introduction, isLoading = uiState.isLoading)
                1 -> StudyMaterialTab(
                    materials = uiState.studyMaterials,
                    isLoading = uiState.isLoading,
                    onMaterialClick = onNavigateToStudyMaterial
                )
                2 -> TestsTab(
                    tests = uiState.availableTests,
                    topicId = topicId,
                    isLoading = uiState.isLoading,
                    pastInterviewResults = uiState.pastInterviewResults,
                    isLoadingInterviewHistory = uiState.isLoadingInterviewHistory,
                    onTestClick = { testType -> onNavigateToTest("${testType.name.lowercase()}_standard") },
                    onInterviewResultClick = onNavigateToInterviewResult
                )
            }
        }
    }
}
