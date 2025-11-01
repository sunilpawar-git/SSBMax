package com.ssbmax.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.core.domain.model.SubscriptionType

/**
 * Displays a subscription tier badge with appropriate styling.
 * Used to show user's subscription level on their profile avatar.
 */
@Composable
fun SubscriptionBadge(
    subscriptionType: SubscriptionType,
    modifier: Modifier = Modifier
) {
    val (label, containerColor, contentColor) = when (subscriptionType) {
        SubscriptionType.FREE -> Triple(
            "Free",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        SubscriptionType.PRO -> Triple(
            "Pro",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        SubscriptionType.PREMIUM -> Triple(
            "Premium",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
    
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold
        ),
        color = contentColor,
        modifier = modifier
            .background(
                color = containerColor,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

