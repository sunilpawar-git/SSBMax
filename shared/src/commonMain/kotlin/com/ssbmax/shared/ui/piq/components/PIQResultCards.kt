package com.ssbmax.shared.ui.piq.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.shared.domain.model.PIQAIScore
import com.ssbmax.shared.ui.util.formatOneDecimal
import org.jetbrains.compose.resources.stringResource
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.piq_ai_quality_assessment
import ssbmax.shared.generated.resources.piq_areas_improvement
import ssbmax.shared.generated.resources.piq_clarity
import ssbmax.shared.generated.resources.piq_completeness
import ssbmax.shared.generated.resources.piq_consistency
import ssbmax.shared.generated.resources.piq_family_info
import ssbmax.shared.generated.resources.piq_feedback
import ssbmax.shared.generated.resources.piq_motivation_goals
import ssbmax.shared.generated.resources.piq_overall_score
import ssbmax.shared.generated.resources.piq_personal_info
import ssbmax.shared.generated.resources.piq_score_decimal_format
import ssbmax.shared.generated.resources.piq_score_format
import ssbmax.shared.generated.resources.piq_self_assessment_label
import ssbmax.shared.generated.resources.piq_strengths

/**
 * Result-screen cards for [com.ssbmax.shared.ui.piq.PIQSubmissionResultScreen]
 * -- split out to keep both files under the 300-line limit (Android
 * original's single `PIQSubmissionResultScreen.kt` is 383 lines).
 * `"%.1f".format(...)` (JVM-only, no Kotlin/Native `actual`) replaced with
 * [formatOneDecimal], same precedent as this plan's other ported result
 * screens.
 */
@Composable
fun PIQAIScoreCard(score: PIQAIScore) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(stringResource(Res.string.piq_ai_quality_assessment), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(Res.string.piq_overall_score), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    stringResource(Res.string.piq_score_format, score.overallScore.toInt()),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            HorizontalDivider()
            ScoreBreakdown(stringResource(Res.string.piq_personal_info), score.personalInfoScore, 25f)
            ScoreBreakdown(stringResource(Res.string.piq_family_info), score.familyInfoScore, 25f)
            ScoreBreakdown(stringResource(Res.string.piq_motivation_goals), score.motivationScore, 25f)
            ScoreBreakdown(stringResource(Res.string.piq_self_assessment_label), score.selfAssessmentScore, 25f)
            HorizontalDivider()
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                QualityMetric("${score.completenessPercentage}%", stringResource(Res.string.piq_completeness), MaterialTheme.colorScheme.primary)
                QualityMetric(
                    stringResource(Res.string.piq_score_decimal_format, formatOneDecimal(score.clarityScore)),
                    stringResource(Res.string.piq_clarity), MaterialTheme.colorScheme.tertiary
                )
                QualityMetric(
                    stringResource(Res.string.piq_score_decimal_format, formatOneDecimal(score.consistencyScore)),
                    stringResource(Res.string.piq_consistency), MaterialTheme.colorScheme.secondary
                )
            }
            HorizontalDivider()
            Text(stringResource(Res.string.piq_feedback), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(score.feedback, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun PIQStrengthsCard(strengths: List<String>) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ThumbUp, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(Res.string.piq_strengths), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            strengths.forEach { BulletRow(it) }
        }
    }
}

@Composable
fun PIQAreasForImprovementCard(areas: List<String>) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TipsAndUpdates, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(Res.string.piq_areas_improvement), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            areas.forEach { BulletRow(it) }
        }
    }
}

@Composable
private fun BulletRow(text: String) {
    Row(modifier = Modifier.padding(start = 32.dp), verticalAlignment = Alignment.Top) {
        Text("• ", style = MaterialTheme.typography.bodyMedium)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun QualityMetric(value: String, label: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ScoreBreakdown(label: String, score: Float, maxScore: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("${score.toInt()}/${maxScore.toInt()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
        LinearProgressIndicator(progress = { score / maxScore }, modifier = Modifier.fillMaxWidth())
    }
}
