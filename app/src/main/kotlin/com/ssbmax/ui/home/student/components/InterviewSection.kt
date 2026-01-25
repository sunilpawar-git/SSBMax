package com.ssbmax.ui.home.student.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ssbmax.R
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.model.dashboard.OLQDashboardData

/**
 * Interview Section
 */
@Composable
fun InterviewSection(
    results: OLQDashboardData.Phase2Results,
    onNavigateToResult: (TestType, String) -> Unit,
    isRefreshing: Boolean = false
) {
    DashboardSection(title = stringResource(R.string.dashboard_interview)) {
        TestScoreChip(
            testName = stringResource(R.string.dashboard_test_interview),
            score = results.interviewResult?.getAverageOLQScore(),
            isRefreshing = isRefreshing,
            onClick = results.interviewResult?.let {
                { onNavigateToResult(TestType.IO, it.id) }
            }
        )
    }
}
