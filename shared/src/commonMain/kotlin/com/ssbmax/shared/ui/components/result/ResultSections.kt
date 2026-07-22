package com.ssbmax.shared.ui.components.result

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.shared.domain.model.interview.OLQ
import com.ssbmax.shared.domain.model.interview.OLQCategory
import com.ssbmax.shared.domain.model.interview.OLQScore

/**
 * KMP port of `app/.../ui/components/result/ResultSections.kt`. Only the
 * `LazyListScope` extension functions actually used by
 * [UnifiedOLQResultTemplate] are ported (the non-lazy `StrengthsSection`/
 * `WeaknessesSection`/`RecommendationsSection` composables from the Android
 * original have zero callers anywhere in this repo -- grep-confirmed -- so
 * porting them now would manufacture new dead code, against this plan's own
 * precedent).
 */
@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = modifier)
}

@Composable
private fun CategorySubHeader(title: String, modifier: Modifier = Modifier) {
    Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, modifier = modifier)
}

@Composable
private fun StrengthItem(strength: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            Text(text = strength, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun WeaknessItem(weakness: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Text(text = weakness, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun RecommendationItem(recommendation: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(imageVector = Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(text = recommendation, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

fun LazyListScope.strengthsSection(strengths: List<String>, strengthsTitle: String) {
    if (strengths.isNotEmpty()) {
        item { SectionHeader(title = strengthsTitle) }
        items(strengths) { strength -> StrengthItem(strength = strength) }
    }
}

fun LazyListScope.weaknessesSection(weaknesses: List<String>, weaknessesTitle: String) {
    if (weaknesses.isNotEmpty()) {
        item { SectionHeader(title = weaknessesTitle) }
        items(weaknesses) { weakness -> WeaknessItem(weakness = weakness) }
    }
}

fun LazyListScope.recommendationsSection(recommendations: List<String>, recommendationsTitle: String) {
    if (recommendations.isNotEmpty()) {
        item { SectionHeader(title = recommendationsTitle) }
        items(recommendations) { recommendation -> RecommendationItem(recommendation = recommendation) }
    }
}

fun LazyListScope.olqCategorySection(olqScores: Map<OLQ, OLQScore>, assessmentTitle: String) {
    if (olqScores.isNotEmpty()) {
        item { SectionHeader(title = assessmentTitle) }
        OLQCategory.entries.forEach { category ->
            val olqsInCategory = OLQ.entries.filter { it.category == category && it in olqScores }
            if (olqsInCategory.isNotEmpty()) {
                item { CategorySubHeader(title = category.displayName) }
                items(olqsInCategory) { olq -> OLQScoreCard(olq = olq, score = olqScores.getValue(olq), isStrength = null) }
            }
        }
    }
}
