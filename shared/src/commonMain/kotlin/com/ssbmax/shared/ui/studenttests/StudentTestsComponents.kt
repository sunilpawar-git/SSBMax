package com.ssbmax.shared.ui.studenttests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.shared.domain.model.TestStatus
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.presentation.studenttests.TestOverviewItem
import org.jetbrains.compose.resources.stringResource
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.student_tests_action_resume
import ssbmax.shared.generated.resources.student_tests_action_results
import ssbmax.shared.generated.resources.student_tests_action_retake
import ssbmax.shared.generated.resources.student_tests_action_start
import ssbmax.shared.generated.resources.student_tests_action_view
import ssbmax.shared.generated.resources.student_tests_category_gto
import ssbmax.shared.generated.resources.student_tests_category_interview
import ssbmax.shared.generated.resources.student_tests_category_psychology
import ssbmax.shared.generated.resources.student_tests_duration_format
import ssbmax.shared.generated.resources.student_tests_latest_score
import ssbmax.shared.generated.resources.student_tests_phase1_description
import ssbmax.shared.generated.resources.student_tests_phase1_title
import ssbmax.shared.generated.resources.student_tests_phase2_description
import ssbmax.shared.generated.resources.student_tests_phase2_title
import ssbmax.shared.generated.resources.student_tests_progress_format
import ssbmax.shared.generated.resources.student_tests_progress_label
import ssbmax.shared.generated.resources.student_tests_question_plural
import ssbmax.shared.generated.resources.student_tests_question_single
import ssbmax.shared.generated.resources.student_tests_view_details

/**
 * Extracted private composables for [StudentTestsScreen], split out purely to
 * keep both files under the repo's 300-line Quality Limit -- no behavior
 * change from having them inline.
 */

@Composable
internal fun Phase1TestsList(
    tests: List<TestOverviewItem>,
    onNavigateToTest: (TestType) -> Unit,
    onViewPhaseDetail: () -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            PhaseOverviewBanner(
                phaseTitle = stringResource(Res.string.student_tests_phase1_title),
                phaseDescription = stringResource(Res.string.student_tests_phase1_description),
                completedTests = tests.count { it.status == TestStatus.COMPLETED },
                totalTests = tests.size,
                onViewDetail = onViewPhaseDetail
            )
        }

        items(tests) { test ->
            TestOverviewCard(test = test, onStartTest = { onNavigateToTest(test.type) })
        }
    }
}

@Composable
internal fun Phase2TestsList(
    tests: List<TestOverviewItem>,
    onNavigateToTest: (TestType) -> Unit,
    onViewPhaseDetail: () -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            PhaseOverviewBanner(
                phaseTitle = stringResource(Res.string.student_tests_phase2_title),
                phaseDescription = stringResource(Res.string.student_tests_phase2_description),
                completedTests = tests.count { it.status == TestStatus.COMPLETED },
                totalTests = tests.size,
                onViewDetail = onViewPhaseDetail
            )
        }

        item { CategoryHeader(stringResource(Res.string.student_tests_category_psychology)) }
        items(tests.filter { it.category == "Psychology" }) { test ->
            TestOverviewCard(test = test, onStartTest = { onNavigateToTest(test.type) })
        }

        item { CategoryHeader(stringResource(Res.string.student_tests_category_gto)) }
        items(tests.filter { it.category == "GTO" }) { test ->
            TestOverviewCard(test = test, onStartTest = { onNavigateToTest(test.type) })
        }

        item { CategoryHeader(stringResource(Res.string.student_tests_category_interview)) }
        items(tests.filter { it.category == "Interview" }) { test ->
            TestOverviewCard(test = test, onStartTest = { onNavigateToTest(test.type) })
        }
    }
}

@Composable
private fun CategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun PhaseOverviewBanner(
    phaseTitle: String,
    phaseDescription: String,
    completedTests: Int,
    totalTests: Int,
    onViewDetail: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = phaseTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = phaseDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(Res.string.student_tests_progress_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = stringResource(Res.string.student_tests_progress_format, completedTests, totalTests),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                TextButton(onClick = onViewDetail) {
                    Text(stringResource(Res.string.student_tests_view_details))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TestOverviewCard(test: TestOverviewItem, onStartTest: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = test.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )

                Column {
                    Text(text = test.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(Res.string.student_tests_duration_format, test.durationMinutes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("•", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val questionWord = if (test.questionCount == 1) {
                            stringResource(Res.string.student_tests_question_single)
                        } else {
                            stringResource(Res.string.student_tests_question_plural)
                        }
                        Text(
                            text = "${test.questionCount} $questionWord",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (test.status == TestStatus.COMPLETED && test.latestScore != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(Res.string.student_tests_latest_score, test.latestScore.toInt()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            FilledTonalButton(onClick = onStartTest, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    when (test.status) {
                        TestStatus.NOT_ATTEMPTED -> stringResource(Res.string.student_tests_action_start)
                        TestStatus.IN_PROGRESS -> stringResource(Res.string.student_tests_action_resume)
                        TestStatus.SUBMITTED_PENDING_REVIEW -> stringResource(Res.string.student_tests_action_view)
                        TestStatus.GRADED -> stringResource(Res.string.student_tests_action_results)
                        TestStatus.COMPLETED -> stringResource(Res.string.student_tests_action_retake)
                    }
                )
            }
        }
    }
}
