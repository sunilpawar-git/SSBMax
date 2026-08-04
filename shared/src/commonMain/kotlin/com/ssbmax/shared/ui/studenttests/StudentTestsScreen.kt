package com.ssbmax.shared.ui.studenttests

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.shared.domain.model.TestPhase
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.presentation.studenttests.StudentTestsViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.student_tests_tab_phase1
import ssbmax.shared.generated.resources.student_tests_tab_phase2
import ssbmax.shared.generated.resources.student_tests_title

/**
 * KMP port of the Android `app/.../ui/tests/StudentTestsScreen.kt` -- overview
 * of all tests across Phase 1/2, tabbed. Private composables (banner/card/
 * lists) extracted into [StudentTestsComponents] to stay under the 300-line
 * Quality Limit. See [StudentTestsViewModel] for the ViewModel-level porting
 * notes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentTestsScreen(
    onNavigateToPhase: (TestPhase) -> Unit = {},
    onNavigateToTest: (TestType) -> Unit = {},
    viewModel: StudentTestsViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.student_tests_title)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(modifier = modifier.fillMaxSize().padding(paddingValues)) {
            PrimaryTabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.surface) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(Res.string.student_tests_tab_phase1)) },
                    icon = { Icon(imageVector = Icons.Default.CheckCircleOutline, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(Res.string.student_tests_tab_phase2)) },
                    icon = { Icon(imageVector = Icons.Default.Psychology, contentDescription = null) }
                )
            }

            when (selectedTab) {
                0 -> Phase1TestsList(
                    tests = uiState.phase1Tests,
                    onNavigateToTest = onNavigateToTest,
                    onViewPhaseDetail = { onNavigateToPhase(TestPhase.PHASE_1) }
                )
                1 -> Phase2TestsList(
                    tests = uiState.phase2Tests,
                    onNavigateToTest = onNavigateToTest,
                    onViewPhaseDetail = { onNavigateToPhase(TestPhase.PHASE_2) }
                )
            }
        }
    }
}
