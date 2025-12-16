package com.ssbmax.ui.home.student.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.ssbmax.R
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.model.scoring.OLQAnalysisResult
import com.ssbmax.utils.DateFormatter

/**
 * Card component to display a single test result
 *
 * Shows:
 * - Test type with icon
 * - Overall rating/score
 * - Date
 * - Top strength (OLQ with best score)
 */
@Composable
fun ResultCard(
    result: OLQAnalysisResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, color) = getTestIconAndColor(result.testType)
    val dateText = DateFormatter.formatRelativeDate(result.analyzedAt)
    val topStrength = result.strengths.firstOrNull() ?: stringResource(R.string.result_not_available)

    Card(
        onClick = onClick,
        modifier = modifier
            .width(280.dp)
            .height(140.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Icon + Test Type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = result.testType.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Date
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Rating Badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = getRatingColor(result.overallRating)
            ) {
                Text(
                    text = result.overallRating,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // Bottom: Top Strength
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.result_top_strength),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = topStrength,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Get icon and color for test type
 */
private fun getTestIconAndColor(testType: TestType): Pair<ImageVector, Color> {
    return when (testType) {
        TestType.IO -> Icons.Default.RecordVoiceOver to Color(0xFF1976D2)
        TestType.GTO_GD -> Icons.Default.Forum to Color(0xFF388E3C)
        TestType.GTO_GPE -> Icons.Default.Map to Color(0xFFD84315)
        TestType.GTO_LECTURETTE -> Icons.Default.Mic to Color(0xFF7B1FA2)
        TestType.GTO_PGT -> Icons.AutoMirrored.Filled.TrendingUp to Color(0xFF00897B)
        TestType.GTO_HGT -> Icons.Default.People to Color(0xFFF57C00)
        TestType.GTO_GOR -> Icons.AutoMirrored.Filled.DirectionsRun to Color(0xFFC62828)
        TestType.GTO_IO -> Icons.Default.Person to Color(0xFF5E35B1)
        TestType.GTO_CT -> Icons.Default.MilitaryTech to Color(0xFF6D4C41)
        TestType.TAT -> Icons.Default.Image to Color(0xFF0288D1)
        TestType.WAT -> Icons.Default.EditNote to Color(0xFF689F38)
        TestType.SRT -> Icons.Default.Speed to Color(0xFFE64A19)
        TestType.SD -> Icons.Default.Psychology to Color(0xFF512DA8)
        else -> Icons.AutoMirrored.Filled.Assignment to Color(0xFF455A64)
    }
}

/**
 * Get color for rating badge
 */
private fun getRatingColor(rating: String): Color {
    return when (rating.lowercase()) {
        "excellent", "exceptional" -> Color(0xFF4CAF50)
        "very good", "good" -> Color(0xFF66BB6A)
        "average" -> Color(0xFFFFA726)
        "below average" -> Color(0xFFFF7043)
        "poor" -> Color(0xFFE53935)
        else -> Color(0xFF757575)
    }
}
