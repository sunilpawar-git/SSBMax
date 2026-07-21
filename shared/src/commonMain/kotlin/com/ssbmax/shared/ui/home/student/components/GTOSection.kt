package com.ssbmax.shared.ui.home.student.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.model.dashboard.OLQDashboardData
import org.jetbrains.compose.resources.stringResource
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.dashboard_gto

/**
 * GTO Tests Section
 */
@Composable
fun GTOSection(
    results: OLQDashboardData.Phase2Results,
    onNavigateToResult: (TestType, String) -> Unit,
    isRefreshing: Boolean = false
) {
    DashboardSection(title = stringResource(Res.string.dashboard_gto)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            results.gtoResults.forEach { (type, result) ->
                TestScoreChip(
                    testName = type.displayName,
                    score = result.overallScore,
                    isRefreshing = isRefreshing,
                    onClick = { onNavigateToResult(result.testType, result.submissionId) }
                )
            }
        }
    }
}
