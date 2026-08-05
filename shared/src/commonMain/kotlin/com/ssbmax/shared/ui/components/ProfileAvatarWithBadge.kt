package com.ssbmax.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ssbmax.shared.domain.model.SubscriptionTier

/**
 * KMP port of `app/.../ui/components/ProfileAvatarWithBadge.kt` -- user
 * initials in a circular avatar with a [SubscriptionBadge] overlay at the
 * bottom-right corner. Closes the gap [DrawerHeader][com.ssbmax.shared.ui.components.drawer.DrawerHeader]
 * documented (KMP-convergence Phase 3b): the tier badge was silently dropped
 * from the drawer avatar because this composable hadn't been ported yet.
 */
@Composable
fun ProfileAvatarWithBadge(
    initials: String,
    subscriptionType: SubscriptionTier?,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape)
                .border(width = 2.dp, color = MaterialTheme.colorScheme.onPrimaryContainer, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center
            )
        }

        if (subscriptionType != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
            ) {
                SubscriptionBadge(subscriptionType = subscriptionType)
            }
        }
    }
}
