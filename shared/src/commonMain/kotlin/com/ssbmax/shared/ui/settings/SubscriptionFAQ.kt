package com.ssbmax.shared.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.subscription_mgmt_faq_a1
import ssbmax.shared.generated.resources.subscription_mgmt_faq_a2
import ssbmax.shared.generated.resources.subscription_mgmt_faq_a3
import ssbmax.shared.generated.resources.subscription_mgmt_faq_q1
import ssbmax.shared.generated.resources.subscription_mgmt_faq_q2
import ssbmax.shared.generated.resources.subscription_mgmt_faq_q3
import ssbmax.shared.generated.resources.subscription_mgmt_faq_title

/**
 * FAQ card for [SubscriptionManagementScreen], split into its own file to
 * keep [SubscriptionPlanCards] under this repo's 300-line Quality Limit.
 */
@Composable
internal fun SubscriptionFAQ(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(Res.string.subscription_mgmt_faq_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            FAQItem(
                question = stringResource(Res.string.subscription_mgmt_faq_q1),
                answer = stringResource(Res.string.subscription_mgmt_faq_a1)
            )

            FAQItem(
                question = stringResource(Res.string.subscription_mgmt_faq_q2),
                answer = stringResource(Res.string.subscription_mgmt_faq_a2)
            )

            FAQItem(
                question = stringResource(Res.string.subscription_mgmt_faq_q3),
                answer = stringResource(Res.string.subscription_mgmt_faq_a3)
            )
        }
    }
}

@Composable
private fun FAQItem(
    question: String,
    answer: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = question,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = answer,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
