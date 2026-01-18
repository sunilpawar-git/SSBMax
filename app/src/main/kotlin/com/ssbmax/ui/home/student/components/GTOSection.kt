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
 * GTO Tests Section
 */
@Composable
fun GTOSection(
    results: OLQDashboardData.Phase2Results,
    onNavigateToResult: (TestType, String) -> Unit,
) {
    DashboardSection(title = stringResource(R.string.dashboard_gto)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            results.gtoResults.forEach { (type, result) ->
                TestScoreChip(
                    testName = type.displayName,
                    score = result.overallScore,
                    onClick = { onNavigateToResult(result.testType, result.submissionId) }
                )
            }
        }
    }
}
