package com.ssbmax.ui.home.student.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.R

/**
 * Individual test score chip for dashboard
 * Color-coded: Green (≤5), Amber (6-7), Red (≥8)
 * 
 * @param testName Display name of the test (e.g., "TAT", "OIR")
 * @param score Test score (OLQ 1-10 or percentage for OIR)
 * @param isOLQBased Whether score follows OLQ scale (lower=better)
 * @param isRefreshing Whether data is being refreshed (shows subtle pulse animation)
 * @param onClick Click handler for navigation (null = not clickable)
 */
@Composable
fun TestScoreChip(
    testName: String,
    score: Float?,
    isOLQBased: Boolean = true,
    isRefreshing: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    // Subtle alpha pulse animation when refreshing
    val alpha by animateFloatAsState(
        targetValue = if (isRefreshing) 0.6f else 1.0f,
        animationSpec = tween(durationMillis = 300),
        label = "refresh_alpha"
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)  // Subtle visual feedback during refresh
            .then(
                // Make clickable when onClick provided, regardless of score
                // This allows navigation even during analysis (PENDING/ANALYZING)
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else Modifier
            ),
        shape = RoundedCornerShape(8.dp),
        color = when {
            score == null -> MaterialTheme.colorScheme.surfaceVariant
            score <= 5f -> Color(0xFF4CAF50).copy(alpha = 0.2f)  // Green
            score <= 7f -> Color(0xFFFFC107).copy(alpha = 0.2f)  // Amber
            else -> Color(0xFFF44336).copy(alpha = 0.2f)  // Red
        },
        border = if (score != null) {
            BorderStroke(
                1.dp,
                when {
                    score <= 5f -> Color(0xFF4CAF50)
                    score <= 7f -> Color(0xFFFFC107)
                    else -> Color(0xFFF44336)
                }
            )
        } else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = testName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (score != null) {
                Text(
                    text = "%.1f".format(score),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        score <= 5f -> Color(0xFF4CAF50)
                        score <= 7f -> Color(0xFFFFC107)
                        else -> Color(0xFFF44336)
                    }
                )
            } else {
                Text(
                    text = stringResource(R.string.dashboard_no_score),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

