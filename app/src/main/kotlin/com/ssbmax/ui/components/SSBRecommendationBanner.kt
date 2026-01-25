package com.ssbmax.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ssbmax.core.domain.validation.RecommendationOutcome
import com.ssbmax.core.domain.validation.SSBRecommendationUIModel

/**
 * SSB Recommendation Banner
 * 
 * Displays the SSB validation recommendation prominently at the top of result screens.
 * Shows RECOMMENDED (green), BORDERLINE (yellow), or NOT_RECOMMENDED (red) status
 * with expandable details.
 * 
 * Position: Above the Overall Performance card on all test result screens.
 * 
 * @param model The UI model containing recommendation data
 * @param modifier Optional modifier
 * @param showExpandedDetails Whether to show expanded details by default
 */
@Composable
fun SSBRecommendationBanner(
    model: SSBRecommendationUIModel,
    modifier: Modifier = Modifier,
    showExpandedDetails: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(showExpandedDetails) }
    
    val (backgroundColor, gradientColors, iconTint, icon) = when (model.recommendation) {
        RecommendationOutcome.RECOMMENDED -> Quadruple(
            Color(0xFF1B5E20), // Dark green
            listOf(Color(0xFF2E7D32), Color(0xFF1B5E20)),
            Color.White,
            Icons.Default.CheckCircle
        )
        RecommendationOutcome.BORDERLINE -> Quadruple(
            Color(0xFFF57F17), // Dark amber
            listOf(Color(0xFFFFA000), Color(0xFFF57F17)),
            Color.White,
            Icons.Default.Warning
        )
        RecommendationOutcome.NOT_RECOMMENDED -> Quadruple(
            Color(0xFFB71C1C), // Dark red
            listOf(Color(0xFFC62828), Color(0xFFB71C1C)),
            Color.White,
            Icons.Default.Cancel
        )
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(gradientColors)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Main Banner Content
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(36.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = model.recommendationText,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        
                        Text(
                            text = model.subtitleText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Quick Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    QuickStatChip(
                        label = "Limitations",
                        value = "${model.limitationCount}/${model.maxLimitations}",
                        isOk = model.limitationsOk,
                        iconTint = iconTint
                    )
                    
                    QuickStatChip(
                        label = "Critical",
                        value = if (model.hasCriticalWeakness) "⚠️" else "✓",
                        isOk = !model.hasCriticalWeakness,
                        iconTint = iconTint
                    )
                    
                    QuickStatChip(
                        label = "Factor II",
                        value = if (model.factorIIAutoReject) "⚠️" else "✓",
                        isOk = !model.factorIIAutoReject,
                        iconTint = iconTint
                    )
                }
                
                // Expand/Collapse indicator
                Spacer(modifier = Modifier.height(8.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
                
                // Expanded Details
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        
                        // Critical Weaknesses (if any)
                        if (model.hasCriticalWeakness && model.criticalWeaknessNames.isNotEmpty()) {
                            DetailRow(
                                icon = Icons.Default.ReportProblem,
                                label = "Critical Weaknesses:",
                                value = model.criticalWeaknessNames.joinToString(", ")
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        // Factor Inconsistency
                        if (model.hasFactorInconsistency) {
                            DetailRow(
                                icon = Icons.Default.Warning,
                                label = "Factor Consistency:",
                                value = "Score variation exceeds allowed range"
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        // SSB Note
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "💡 SSB uses 1-10 scale where LOWER scores indicate BETTER performance",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickStatChip(
    label: String,
    value: String,
    isOk: Boolean,
    iconTint: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isOk) Color.White else Color.Yellow
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = iconTint.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label $value",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

/**
 * Helper data class for banner styling.
 */
private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
