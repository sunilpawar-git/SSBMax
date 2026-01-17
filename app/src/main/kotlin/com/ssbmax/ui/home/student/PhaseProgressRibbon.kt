package com.ssbmax.ui.home.student

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.R
import com.ssbmax.core.designsystem.theme.SSBColors
import com.ssbmax.core.designsystem.theme.Spacing
import com.ssbmax.core.domain.model.Phase1Progress
import com.ssbmax.core.domain.model.Phase2Progress
import com.ssbmax.core.domain.model.TestPhase
import com.ssbmax.core.domain.model.TestProgress
import com.ssbmax.core.domain.model.TestStatus
import com.ssbmax.utils.DateFormatter

/**
 * Phase Progress Ribbon Component
 * Two-column layout showing Phase 1 and Phase 2 progress
 */
@Composable
fun PhaseProgressRibbon(
    phase1Progress: Phase1Progress?,
    phase2Progress: Phase2Progress?,
    onPhaseClick: (TestPhase) -> Unit,
    onTopicClick: (String) -> Unit,  // Navigate to topic screen with Tests tab
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Spacing.cardCornerRadiusLarge),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.cardPadding)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                Text(
                    text = stringResource(R.string.icon_trophy),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    stringResource(R.string.phase_progress_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.large))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                // Phase 1 Card
                Phase1Card(
                    progress = phase1Progress,
                    onPhaseClick = { onPhaseClick(TestPhase.PHASE_1) },
                    onTopicClick = onTopicClick,
                    modifier = Modifier.weight(1f)
                )
                
                // Phase 2 Card
                Phase2Card(
                    progress = phase2Progress,
                    onPhaseClick = { onPhaseClick(TestPhase.PHASE_2) },
                    onTopicClick = onTopicClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun Phase1Card(
    progress: Phase1Progress?,
    onPhaseClick: () -> Unit,
    onTopicClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val phaseColor = SSBColors.Info
    
    Card(
        modifier = modifier
            .height(Spacing.phaseCardHeight)
            .clickable(onClick = onPhaseClick),
        shape = RoundedCornerShape(Spacing.cardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = phaseColor.copy(alpha = 0.05f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = phaseColor.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.medium)
        ) {
            // Phase Header
            Column {
                Text(
                    stringResource(R.string.phase_1_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = phaseColor
                )
                Text(
                    stringResource(R.string.phase_1_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.large))
            
            // Tests List
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                if (progress != null) {
                    TestProgressItem(
                        testProgress = progress.oirProgress,
                        topicId = "oir",
                        phaseColor = phaseColor,
                        onClick = { onTopicClick("oir") }
                    )
                    TestProgressItem(
                        testProgress = progress.ppdtProgress,
                        topicId = "ppdt",
                        phaseColor = phaseColor,
                        onClick = { onTopicClick("ppdt") }
                    )
                } else {
                    Text(
                        stringResource(R.string.progress_no_tests),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            TextButton(
                onClick = onPhaseClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.progress_view_all), color = phaseColor)
                Spacer(modifier = Modifier.width(Spacing.extraSmall))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = stringResource(R.string.cd_phase_view_all),
                    tint = phaseColor,
                    modifier = Modifier.size(Spacing.iconSizeSmall)
                )
            }
        }
    }
}

@Composable
private fun Phase2Card(
    progress: Phase2Progress?,
    onPhaseClick: () -> Unit,
    onTopicClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val phaseColor = SSBColors.Success
    
    Card(
        modifier = modifier
            .height(Spacing.phaseCardHeight)
            .clickable(onClick = onPhaseClick),
        shape = RoundedCornerShape(Spacing.cardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = phaseColor.copy(alpha = 0.05f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = phaseColor.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.medium)
        ) {
            // Phase Header
            Column {
                Text(
                    stringResource(R.string.phase_2_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = phaseColor
                )
                Text(
                    stringResource(R.string.phase_2_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.large))
            
            // Tests List
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                if (progress != null) {
                    TestProgressItem(
                        testProgress = progress.psychologyProgress,
                        topicId = "psychology",
                        displayName = "Psychology Tests",
                        phaseColor = phaseColor,
                        onClick = { onTopicClick("psychology") }
                    )
                    TestProgressItem(
                        testProgress = progress.gtoProgress,
                        topicId = "gto",
                        displayName = "GTO Tasks",
                        phaseColor = phaseColor,
                        onClick = { onTopicClick("gto") }
                    )
                    TestProgressItem(
                        testProgress = progress.interviewProgress,
                        topicId = "interview",
                        displayName = "Interview",
                        phaseColor = phaseColor,
                        onClick = { onTopicClick("interview") }
                    )
                } else {
                    Text(
                        stringResource(R.string.progress_no_tests),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            TextButton(
                onClick = onPhaseClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.progress_view_all), color = phaseColor)
                Spacer(modifier = Modifier.width(Spacing.extraSmall))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = stringResource(R.string.cd_phase_view_all),
                    tint = phaseColor,
                    modifier = Modifier.size(Spacing.iconSizeSmall)
                )
            }
        }
    }
}

@Composable
private fun TestProgressItem(
    testProgress: TestProgress,
    topicId: String,
    displayName: String? = null,
    phaseColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status Icon - Checkmark for any completed/attempted test, empty circle for not attempted
        val (icon, iconColor) = when (testProgress.status) {
            TestStatus.COMPLETED, TestStatus.GRADED, TestStatus.SUBMITTED_PENDING_REVIEW ->
                Icons.Default.CheckCircle to SSBColors.Success
            TestStatus.IN_PROGRESS ->
                Icons.Default.Schedule to SSBColors.Warning
            TestStatus.NOT_ATTEMPTED ->
                Icons.Default.RadioButtonUnchecked to MaterialTheme.colorScheme.onSurfaceVariant
        }

        val contentDescription = when (testProgress.status) {
            TestStatus.COMPLETED, TestStatus.GRADED, TestStatus.SUBMITTED_PENDING_REVIEW ->
                stringResource(R.string.cd_test_status_completed)
            TestStatus.IN_PROGRESS ->
                stringResource(R.string.cd_test_status_in_progress)
            TestStatus.NOT_ATTEMPTED ->
                stringResource(R.string.cd_test_status_not_attempted)
        }

        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(Spacing.iconSizeExtraSmall)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                displayName ?: testProgress.testType.displayName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            
            // Simplified status text - just show "Completed on {date}" or "Not Attempted"
            Text(
                text = if (testProgress.lastAttemptDate != null) {
                    stringResource(R.string.progress_completed_on, DateFormatter.formatFullDate(testProgress.lastAttemptDate!!))
                } else {
                    stringResource(R.string.progress_not_attempted)
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (testProgress.lastAttemptDate != null) phaseColor
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
