package com.ssbmax.ui.home.student.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Large score badge for overall average display
 * Color-coded: Green (≤5), Amber (6-7), Red (≥8)
 * 
 * @param score Overall OLQ score (1-10 scale, lower is better)
 */
@Composable
fun ScoreBadge(score: Float) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = when {
            score <= 5f -> Color(0xFF4CAF50)
            score <= 7f -> Color(0xFFFFC107)
            else -> Color(0xFFF44336)
        }
    ) {
        Text(
            text = "%.1f".format(score),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        )
    }
}

