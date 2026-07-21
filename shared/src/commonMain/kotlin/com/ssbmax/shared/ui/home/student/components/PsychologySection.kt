package com.ssbmax.shared.ui.home.student.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.model.dashboard.OLQDashboardData
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.dashboard_psychology
import ssbmax.shared.generated.resources.dashboard_test_tat
import ssbmax.shared.generated.resources.dashboard_test_wat
import ssbmax.shared.generated.resources.dashboard_test_srt
import ssbmax.shared.generated.resources.dashboard_test_self_desc

/**
 * Psychology Tests Section
 */
@Composable
fun PsychologySection(
    results: OLQDashboardData.Phase2Results,
    onNavigateToResult: (TestType, String) -> Unit,
    isRefreshing: Boolean = false
) {
    DashboardSection(title = stringResource(Res.string.dashboard_psychology)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PsychologyChip(stringResource(Res.string.dashboard_test_tat), results.tatResult?.overallScore, TestType.TAT, results.tatResult?.submissionId, onNavigateToResult, isRefreshing)
            PsychologyChip(stringResource(Res.string.dashboard_test_wat), results.watResult?.overallScore, TestType.WAT, results.watResult?.submissionId, onNavigateToResult, isRefreshing)
            PsychologyChip(stringResource(Res.string.dashboard_test_srt), results.srtResult?.overallScore, TestType.SRT, results.srtResult?.submissionId, onNavigateToResult, isRefreshing)
            PsychologyChip(stringResource(Res.string.dashboard_test_self_desc), results.sdResult?.overallScore, TestType.SD, results.sdResult?.submissionId, onNavigateToResult, isRefreshing)
        }
    }
}

@Composable
private fun PsychologyChip(
    name: String,
    score: Float?,
    type: TestType,
    submissionId: String?,
    onNavigate: (TestType, String) -> Unit,
    isRefreshing: Boolean = false
) {
    TestScoreChip(
        testName = name,
        score = score,
        isRefreshing = isRefreshing,
        onClick = submissionId?.let { { onNavigate(type, it) } }
    )
}
