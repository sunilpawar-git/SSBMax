package com.ssbmax.ui.components.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.R
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQCategory
import com.ssbmax.core.domain.model.interview.OLQScore

/**
 * Section header text for result sections.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}

/**
 * Category sub-header for OLQ groups.
 */
@Composable
fun CategorySubHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}

/**
 * Individual strength item card.
 */
@Composable
fun StrengthItem(
    strength: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary
            )
            Text(
                text = strength,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

/**
 * Individual weakness/area for improvement item card.
 */
@Composable
fun WeaknessItem(
    weakness: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = weakness,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

/**
 * Individual recommendation item card.
 */
@Composable
fun RecommendationItem(
    recommendation: String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = recommendation,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

/**
 * Extension function to add strengths section to a LazyListScope.
 * Only adds the section if the list is not empty.
 *
 * @param strengths List of strength descriptions
 */
fun LazyListScope.strengthsSection(strengths: List<String>) {
    if (strengths.isNotEmpty()) {
        item {
            SectionHeader(title = stringResource(R.string.result_top_strengths_title))
        }
        items(strengths) { strength ->
            StrengthItem(strength = strength)
        }
    }
}

/**
 * Extension function to add weaknesses/areas for improvement section to a LazyListScope.
 * Only adds the section if the list is not empty.
 *
 * @param weaknesses List of weakness/improvement area descriptions
 */
fun LazyListScope.weaknessesSection(weaknesses: List<String>) {
    if (weaknesses.isNotEmpty()) {
        item {
            SectionHeader(title = stringResource(R.string.result_areas_improvement_title))
        }
        items(weaknesses) { weakness ->
            WeaknessItem(weakness = weakness)
        }
    }
}

/**
 * Extension function to add recommendations section to a LazyListScope.
 * Only adds the section if the list is not empty.
 *
 * @param recommendations List of recommendations
 */
fun LazyListScope.recommendationsSection(recommendations: List<String>) {
    if (recommendations.isNotEmpty()) {
        item {
            SectionHeader(title = stringResource(R.string.result_recommendations_title))
        }
        items(recommendations) { recommendation ->
            RecommendationItem(recommendation = recommendation)
        }
    }
}

/**
 * Extension function to add OLQ scores grouped by category to a LazyListScope.
 * Displays all 15 OLQs organized by their category (Intellectual, Social, Dynamic, Character).
 *
 * @param olqScores Map of OLQ to OLQScore
 */
fun LazyListScope.olqCategorySection(olqScores: Map<OLQ, OLQScore>) {
    if (olqScores.isNotEmpty()) {
        item {
            SectionHeader(title = stringResource(R.string.result_olq_assessment_title))
        }

        OLQCategory.entries.forEach { category ->
            val olqsInCategory = olqScores.filter { it.key.category == category }
            if (olqsInCategory.isNotEmpty()) {
                item {
                    CategorySubHeader(title = category.displayName)
                }
                items(olqsInCategory.entries.toList()) { (olq, score) ->
                    OLQScoreCard(olq = olq, score = score, isStrength = null)
                }
            }
        }
    }
}

/**
 * Composable section displaying all strengths.
 * Used in non-lazy contexts.
 */
@Composable
fun StrengthsSection(
    strengths: List<String>,
    modifier: Modifier = Modifier
) {
    if (strengths.isNotEmpty()) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionHeader(title = stringResource(R.string.result_top_strengths_title))
            Spacer(modifier = Modifier.height(8.dp))
            strengths.forEach { strength ->
                StrengthItem(strength = strength)
            }
        }
    }
}

/**
 * Composable section displaying all weaknesses/areas for improvement.
 * Used in non-lazy contexts.
 */
@Composable
fun WeaknessesSection(
    weaknesses: List<String>,
    modifier: Modifier = Modifier
) {
    if (weaknesses.isNotEmpty()) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionHeader(title = stringResource(R.string.result_areas_improvement_title))
            Spacer(modifier = Modifier.height(8.dp))
            weaknesses.forEach { weakness ->
                WeaknessItem(weakness = weakness)
            }
        }
    }
}

/**
 * Composable section displaying all recommendations.
 * Used in non-lazy contexts.
 */
@Composable
fun RecommendationsSection(
    recommendations: List<String>,
    modifier: Modifier = Modifier
) {
    if (recommendations.isNotEmpty()) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionHeader(title = stringResource(R.string.result_recommendations_title))
            Spacer(modifier = Modifier.height(8.dp))
            recommendations.forEach { recommendation ->
                RecommendationItem(recommendation = recommendation)
            }
        }
    }
}
