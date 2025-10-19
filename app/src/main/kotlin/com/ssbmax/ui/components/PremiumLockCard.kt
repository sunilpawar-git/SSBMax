package com.ssbmax.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ssbmax.core.domain.model.SubscriptionTier

/**
 * Premium Lock Card - Overlay for locked features
 * Shows lock icon with upgrade CTA
 */
@Composable
fun PremiumLockCard(
    requiredTier: SubscriptionTier,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    showBackground: Boolean = true
) {
    val defaultMessage = when (requiredTier) {
        SubscriptionTier.BASIC -> "This feature is available to all users"
        SubscriptionTier.PRO -> "Upgrade to Pro to unlock this feature"
        SubscriptionTier.PREMIUM_AI -> "Upgrade to AI Premium for AI-powered insights"
        SubscriptionTier.PREMIUM -> "Upgrade to Premium for full marketplace access"
    }
    
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "lock-alpha"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha),
        contentAlignment = Alignment.Center
    ) {
        if (showBackground) {
            // Background blur effect
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(24.dp)
            )
        }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Lock icon with gradient background
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF8b5cf6).copy(alpha = 0.2f),
                                Color(0xFFc026d3).copy(alpha = 0.1f)
                            )
                        ),
                        shape = RoundedCornerShape(40.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Locked",
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            // Title
            Text(
                "Premium Feature",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            // Message
            Text(
                message ?: defaultMessage,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Tier badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Stars,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Required: ${requiredTier.displayName}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            // Upgrade button
            Button(
                onClick = onUpgradeClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.Upgrade,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Upgrade Now",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Compact lock overlay for smaller spaces
 */
@Composable
fun CompactPremiumLock(
    requiredTier: SubscriptionTier,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onUpgradeClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                
                Column {
                    Text(
                        "Premium Feature",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Requires ${requiredTier.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Icon(
                Icons.Default.Upgrade,
                contentDescription = "Upgrade",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Lock overlay that can be placed over any content
 */
@Composable
fun LockedContentOverlay(
    isLocked: Boolean,
    requiredTier: SubscriptionTier,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        // Content (blurred if locked)
        Box(
            modifier = if (isLocked) Modifier.blur(8.dp) else Modifier
        ) {
            content()
        }
        
        // Lock overlay
        AnimatedVisibility(
            visible = isLocked,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                PremiumLockCard(
                    requiredTier = requiredTier,
                    onUpgradeClick = onUpgradeClick,
                    showBackground = false
                )
            }
        }
    }
}

/**
 * Inline lock badge for list items
 */
@Composable
fun PremiumBadge(
    requiredTier: SubscriptionTier,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(if (compact) 8.dp else 12.dp),
        color = Color(0xFFf59e0b).copy(alpha = 0.15f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 8.dp else 12.dp,
                vertical = if (compact) 4.dp else 6.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = "Premium",
                modifier = Modifier.size(if (compact) 12.dp else 16.dp),
                tint = Color(0xFFf59e0b)
            )
            if (!compact) {
                Text(
                    requiredTier.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFf59e0b)
                )
            }
        }
    }
}

/**
 * Helper to check if feature is accessible
 */
fun isFeatureAccessible(
    currentTier: SubscriptionTier,
    requiredTier: SubscriptionTier
): Boolean {
    return currentTier >= requiredTier
}

