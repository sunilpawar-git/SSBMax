package com.ssbmax.ui.home.student

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.core.domain.model.PhaseProgress
import com.ssbmax.core.domain.model.SubTestProgress
import com.ssbmax.core.domain.model.TestPhase
import com.ssbmax.core.domain.model.TestStatus
import com.ssbmax.core.domain.model.TestType

/**
 * Phase Progress Ribbon Component
 * Two-column layout showing Phase 1 and Phase 2 progress
 */
@Composable
fun PhaseProgressRibbon(
    phase1Progress: PhaseProgress?,
    phase2Progress: PhaseProgress?,
    onPhaseClick: (TestPhase) -> Unit,
    onTestClick: (TestType) -> Unit,
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
            Text(
                "SSB Preparation Progress",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Phase 1 Card
                PhaseCard(
                    phase = TestPhase.PHASE_1,
                    progress = phase1Progress,
                    onPhaseClick = { onPhaseClick(TestPhase.PHASE_1) },
                    onTestClick = onTestClick,
                    modifier = Modifier.weight(1f)
                )
                
                // Phase 2 Card
                PhaseCard(
                    phase = TestPhase.PHASE_2,
                    progress = phase2Progress,
                    onPhaseClick = { onPhaseClick(TestPhase.PHASE_2) },
                    onTestClick = onTestClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PhaseCard(
    phase: TestPhase,
    progress: PhaseProgress?,
    onPhaseClick: () -> Unit,
    onTestClick: (TestType) -> Unit,
    modifier: Modifier = Modifier
) {
    val phaseColor = when (phase) {
        TestPhase.PHASE_1 -> Color(0xFF1976D2)
        TestPhase.PHASE_2 -> Color(0xFF388E3C)
    }
    
    val percentage = progress?.completionPercentage ?: 0f
    
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        when (phase) {
                            TestPhase.PHASE_1 -> "PHASE 1"
                            TestPhase.PHASE_2 -> "PHASE 2"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = phaseColor
                    )
                    Text(
                        when (phase) {
                            TestPhase.PHASE_1 -> "Screening"
                            TestPhase.PHASE_2 -> "Assessment"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Progress Circle
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(phaseColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${percentage.toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = phaseColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress Bar
            LinearProgressIndicator(
                progress = { percentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = phaseColor,
                trackColor = phaseColor.copy(alpha = 0.2f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Sub-tests List
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                progress?.subTests?.forEach { subTest ->
                    SubTestItem(
                        subTest = subTest,
                        phaseColor = phaseColor,
                        onClick = { onTestClick(subTest.testType) }
                    )
                } ?: run {
                    // Empty state
                    Text(
                        "No tests attempted yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // View Details Button
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
private fun SubTestItem(
    subTest: SubTestProgress,
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
        // Status Icon
        val (icon, iconColor) = when (subTest.status) {
            TestStatus.COMPLETED, TestStatus.GRADED -> 
                Icons.Default.CheckCircle to Color(0xFF4CAF50)
            TestStatus.IN_PROGRESS -> 
                Icons.Default.Schedule to Color(0xFFFFA726)
            TestStatus.SUBMITTED_PENDING_REVIEW -> 
                Icons.Default.HourglassEmpty to Color(0xFF2196F3)
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
                subTest.testType.displayName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            
            subTest.latestScore?.let { score ->
                Text(
                    "Score: ${score.toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = phaseColor
                )
            }
        }
        
        if (subTest.attemptsCount > 0) {
            Text(
                "${subTest.attemptsCount}×",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

