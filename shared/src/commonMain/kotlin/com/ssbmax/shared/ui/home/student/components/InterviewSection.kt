package com.ssbmax.shared.ui.home.student.components

import androidx.compose.runtime.Composable
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.model.dashboard.OLQDashboardData
import org.jetbrains.compose.resources.stringResource
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.dashboard_interview
import ssbmax.shared.generated.resources.dashboard_test_interview

/**
 * Interview Section
 */
@Composable
fun InterviewSection(
    results: OLQDashboardData.Phase2Results,
    onNavigateToResult: (TestType, String) -> Unit,
    isRefreshing: Boolean = false
) {
    DashboardSection(title = stringResource(Res.string.dashboard_interview)) {
        TestScoreChip(
            testName = stringResource(Res.string.dashboard_test_interview),
            score = results.interviewResult?.getAverageOLQScore(),
            isRefreshing = isRefreshing,
            onClick = results.interviewResult?.let {
                { onNavigateToResult(TestType.IO, it.id) }
            }
        )
    }
}
