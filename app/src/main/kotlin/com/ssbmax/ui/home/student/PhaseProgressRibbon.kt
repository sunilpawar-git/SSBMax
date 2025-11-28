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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.core.domain.model.Phase1Progress
import com.ssbmax.core.domain.model.Phase2Progress
import com.ssbmax.core.domain.model.TestPhase
import com.ssbmax.core.domain.model.TestProgress
import com.ssbmax.core.domain.model.TestStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "🏆",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    "SSB Preparation Progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
    val phaseColor = Color(0xFF1976D2)
    
    Card(
        modifier = modifier
            .height(280.dp)
            .clickable(onClick = onPhaseClick),
        shape = RoundedCornerShape(12.dp),
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
                .padding(12.dp)
        ) {
            // Phase Header
            Column {
                Text(
                    "PHASE 1",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = phaseColor
                )
                Text(
                    "Screening",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Tests List
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        "No tests attempted yet",
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
                Text("View All Tests", color = phaseColor)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = phaseColor,
                    modifier = Modifier.size(18.dp)
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
    val phaseColor = Color(0xFF388E3C)
    
    Card(
        modifier = modifier
            .height(280.dp)
            .clickable(onClick = onPhaseClick),
        shape = RoundedCornerShape(12.dp),
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
                .padding(12.dp)
        ) {
            // Phase Header
            Column {
                Text(
                    "PHASE 2",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = phaseColor
                )
                Text(
                    "Assessment",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Tests List
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        "No tests attempted yet",
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
                Text("View All Tests", color = phaseColor)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = phaseColor,
                    modifier = Modifier.size(18.dp)
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
                Icons.Default.CheckCircle to Color(0xFF4CAF50)
            TestStatus.IN_PROGRESS -> 
                Icons.Default.Schedule to Color(0xFFFFA726)
            TestStatus.NOT_ATTEMPTED -> 
                Icons.Default.RadioButtonUnchecked to Color(0xFF9E9E9E)
        }
        
        Icon(
            icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(16.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                displayName ?: testProgress.testType.displayName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            
            // Simplified status text - just show "Completed on {date}" or "Not Attempted"
            val statusText = testProgress.lastAttemptDate?.let { lastAttemptDate ->
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val dateStr = dateFormat.format(Date(lastAttemptDate))
                "Completed on $dateStr"
            } ?: "Not Attempted"
            
            Text(
                statusText,
                style = MaterialTheme.typography.labelSmall,
                color = if (testProgress.lastAttemptDate != null) phaseColor
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
