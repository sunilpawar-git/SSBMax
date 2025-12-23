package com.ssbmax.ui.home.student.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.core.domain.model.interview.OLQ

/**
 * Section displaying OLQ strengths or improvement areas
 * 
 * @param title Section title (e.g., "🌟 Your Strengths")
 * @param olqs List of OLQ qualities with scores (sorted by score)
 * @param color Color scheme for score badges (green for strengths, red for weaknesses)
 */
@Composable
fun OLQStrengthsSection(
    title: String,
    olqs: List<Pair<OLQ, Float>>,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        olqs.forEach { (olq, score) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = olq.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = color.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "%.1f".format(score),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

