package com.ssbmax.shared.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.shared.presentation.settings.SubscriptionTierModel
import org.jetbrains.compose.resources.stringResource
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.subscription_mgmt_current_plan_badge
import ssbmax.shared.generated.resources.subscription_mgmt_most_popular
import ssbmax.shared.generated.resources.subscription_mgmt_select_plan
import ssbmax.shared.generated.resources.subscription_mgmt_upgrade_to

/**
 * Plan-comparison cards (one per [SubscriptionTierModel]) for
 * [SubscriptionManagementScreen], split into their own file to keep
 * [SubscriptionPlanCards] under this repo's 300-line Quality Limit.
 */
@Composable
internal fun PlanComparisonCards(
    currentTier: SubscriptionTierModel,
    onUpgrade: (SubscriptionTierModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SubscriptionTierModel.entries.forEach { tier ->
            PlanCard(
                tier = tier,
                isCurrentPlan = tier == currentTier,
                onUpgrade = { onUpgrade(tier) }
            )
        }
    }
}

@Composable
private fun PlanCard(
    tier: SubscriptionTierModel,
    isCurrentPlan: Boolean,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        border = if (tier == SubscriptionTierModel.PRO) {
            CardDefaults.outlinedCardBorder().copy(width = 2.dp)
        } else {
            null
        },
        colors = if (isCurrentPlan) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = tier.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tier.price,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (tier == SubscriptionTierModel.PRO) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = stringResource(Res.string.subscription_mgmt_most_popular),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                if (isCurrentPlan) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.secondary
                    ) {
                        Text(
                            text = stringResource(Res.string.subscription_mgmt_current_plan_badge),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }
            }

            tier.features.forEach { feature ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(text = feature, style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (!isCurrentPlan) {
                Button(
                    onClick = onUpgrade,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (tier.ordinal > SubscriptionTierModel.FREE.ordinal) {
                            stringResource(Res.string.subscription_mgmt_upgrade_to, tier.displayName)
                        } else {
                            stringResource(Res.string.subscription_mgmt_select_plan)
                        }
                    )
                }
            }
        }
    }
}
