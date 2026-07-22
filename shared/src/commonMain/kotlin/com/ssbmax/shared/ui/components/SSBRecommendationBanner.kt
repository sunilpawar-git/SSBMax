package com.ssbmax.shared.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ssbmax.shared.domain.validation.RecommendationOutcome
import com.ssbmax.shared.domain.validation.SSBRecommendationUIModel
import org.jetbrains.compose.resources.stringResource
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.ssb_banner_collapse_cd
import ssbmax.shared.generated.resources.ssb_banner_critical_label
import ssbmax.shared.generated.resources.ssb_banner_critical_weaknesses_label
import ssbmax.shared.generated.resources.ssb_banner_expand_cd
import ssbmax.shared.generated.resources.ssb_banner_factor_consistency_label
import ssbmax.shared.generated.resources.ssb_banner_factor_consistency_value
import ssbmax.shared.generated.resources.ssb_banner_factor_ii_label
import ssbmax.shared.generated.resources.ssb_banner_limitations_label
import ssbmax.shared.generated.resources.ssb_banner_scale_note

/**
 * KMP port of `app/.../ui/components/SSBRecommendationBanner.kt`, unchanged
 * layout/behavior. The Android original hardcoded its strings (a
 * pre-existing lint-rule violation, same class already ported-around
 * elsewhere this phase) -- ported here as real composeResources entries.
 */
@Composable
fun SSBRecommendationBanner(
    model: SSBRecommendationUIModel,
    modifier: Modifier = Modifier,
    showExpandedDetails: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(showExpandedDetails) }

    val (backgroundColor, gradientColors, iconTint, icon) = when (model.recommendation) {
        RecommendationOutcome.RECOMMENDED -> BannerStyle(Color(0xFF1B5E20), listOf(Color(0xFF2E7D32), Color(0xFF1B5E20)), Color.White, Icons.Default.CheckCircle)
        RecommendationOutcome.BORDERLINE -> BannerStyle(Color(0xFFF57F17), listOf(Color(0xFFFFA000), Color(0xFFF57F17)), Color.White, Icons.Default.Warning)
        RecommendationOutcome.NOT_RECOMMENDED -> BannerStyle(Color(0xFFB71C1C), listOf(Color(0xFFC62828), Color(0xFFB71C1C)), Color.White, Icons.Default.Cancel)
    }
    // backgroundColor unused (Android original also never applies it -- containerColor is Transparent
    // and the gradient Box paints over it); kept as a destructured value to match the original's shape.
    backgroundColor.let { }

    Card(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(modifier = Modifier.fillMaxWidth().background(brush = Brush.verticalGradient(gradientColors))) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = model.recommendationText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(text = model.subtitleText, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f), textAlign = TextAlign.Center)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    QuickStatChip(label = stringResource(Res.string.ssb_banner_limitations_label), value = "${model.limitationCount}/${model.maxLimitations}", isOk = model.limitationsOk, iconTint = iconTint)
                    QuickStatChip(label = stringResource(Res.string.ssb_banner_critical_label), value = if (model.hasCriticalWeakness) "⚠" else "✓", isOk = !model.hasCriticalWeakness, iconTint = iconTint)
                    QuickStatChip(label = stringResource(Res.string.ssb_banner_factor_ii_label), value = if (model.factorIIAutoReject) "⚠" else "✓", isOk = !model.factorIIAutoReject, iconTint = iconTint)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = stringResource(if (isExpanded) Res.string.ssb_banner_collapse_cd else Res.string.ssb_banner_expand_cd),
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
                AnimatedVisibility(visible = isExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 8.dp))
                        if (model.hasCriticalWeakness && model.criticalWeaknessNames.isNotEmpty()) {
                            DetailRow(icon = Icons.Default.ReportProblem, label = stringResource(Res.string.ssb_banner_critical_weaknesses_label), value = model.criticalWeaknessNames.joinToString(", "))
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        if (model.hasFactorInconsistency) {
                            DetailRow(icon = Icons.Default.Warning, label = stringResource(Res.string.ssb_banner_factor_consistency_label), value = stringResource(Res.string.ssb_banner_factor_consistency_value))
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(Res.string.ssb_banner_scale_note),
                            style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickStatChip(label: String, value: String, isOk: Boolean, iconTint: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (isOk) Color.White else Color.Yellow)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = iconTint.copy(alpha = 0.8f))
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "$label $value", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f))
    }
}

private data class BannerStyle(
    val backgroundColor: Color,
    val gradientColors: List<Color>,
    val iconTint: Color,
    val icon: ImageVector
)
