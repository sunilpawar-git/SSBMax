package com.ssbmax.ui.home.student.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ssbmax.R
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.model.dashboard.OLQDashboardData

/**
 * Phase 1 (Screening) Section
 */
@Composable
fun Phase1Section(
    results: OLQDashboardData.Phase1Results,
    onNavigateToResult: (TestType, String) -> Unit,
    isRefreshing: Boolean = false
) {
    DashboardSection(
        title = stringResource(R.string.dashboard_phase_1)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TestScoreChip(
                testName = stringResource(R.string.dashboard_test_oir),
                score = results.oirResult?.percentageScore,
                isOLQBased = false,
                isRefreshing = isRefreshing,
                onClick = { 
                    results.oirResult?.let { onNavigateToResult(TestType.OIR, it.sessionId) }
                }
            )
            TestScoreChip(
                testName = stringResource(R.string.dashboard_test_ppdt),
                score = results.ppdtOLQResult?.overallScore ?: results.ppdtResult?.finalScore,
                isOLQBased = true,
                isRefreshing = isRefreshing,
                onClick = results.ppdtResult?.let {
                    { onNavigateToResult(TestType.PPDT, it.submissionId) }
                }
            )
        }
    }
}
