package com.ssbmax.ui.home.student.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ssbmax.R
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.model.dashboard.OLQDashboardData

/**
 * Psychology Tests Section
 */
@Composable
fun PsychologySection(
    results: OLQDashboardData.Phase2Results,
    onNavigateToResult: (TestType, String) -> Unit,
    isRefreshing: Boolean = false
) {
    DashboardSection(title = stringResource(R.string.dashboard_psychology)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PsychologyChip(stringResource(R.string.dashboard_test_tat), results.tatResult?.overallScore, TestType.TAT, results.tatResult?.submissionId, onNavigateToResult, isRefreshing)
            PsychologyChip(stringResource(R.string.dashboard_test_wat), results.watResult?.overallScore, TestType.WAT, results.watResult?.submissionId, onNavigateToResult, isRefreshing)
            PsychologyChip(stringResource(R.string.dashboard_test_srt), results.srtResult?.overallScore, TestType.SRT, results.srtResult?.submissionId, onNavigateToResult, isRefreshing)
            PsychologyChip(stringResource(R.string.dashboard_test_self_desc), results.sdResult?.overallScore, TestType.SD, results.sdResult?.submissionId, onNavigateToResult, isRefreshing)
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
