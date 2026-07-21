package com.ssbmax.shared.ui.home.student.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.model.dashboard.OLQDashboardData
import org.jetbrains.compose.resources.stringResource
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.dashboard_phase_1
import ssbmax.shared.generated.resources.dashboard_test_oir
import ssbmax.shared.generated.resources.dashboard_test_ppdt

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
        title = stringResource(Res.string.dashboard_phase_1)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TestScoreChip(
                testName = stringResource(Res.string.dashboard_test_oir),
                score = results.oirResult?.percentageScore,
                isOLQBased = false,
                isRefreshing = isRefreshing,
                onClick = {
                    results.oirResult?.let { onNavigateToResult(TestType.OIR, it.sessionId) }
                }
            )
            TestScoreChip(
                testName = stringResource(Res.string.dashboard_test_ppdt),
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
