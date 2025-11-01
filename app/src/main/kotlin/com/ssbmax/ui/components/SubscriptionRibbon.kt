package com.ssbmax.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.core.domain.model.SubscriptionTier

/**
 * Beautiful subscription ribbon component
 * Displays current tier with gradient background and upgrade CTA
 */
@Composable
fun SubscriptionRibbon(
    currentTier: SubscriptionTier,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDismissible: Boolean = false,
    onDismiss: (() -> Unit)? = null
) {
    // Animation for scale effect
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.95f,
        animationSpec = tween(durationMillis = 300),
        label = "ribbon-scale"
    )
    
    val gradient = getTierGradient(currentTier)
    val shouldShowUpgrade = currentTier != SubscriptionTier.PREMIUM
    
    Card(
        onClick = if (shouldShowUpgrade) onUpgradeClick else { {} },
        modifier = modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(gradient)
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Tier Icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (currentTier == SubscriptionTier.PREMIUM) Icons.Default.Star else Icons.Default.Upgrade,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    Spacer(Modifier.width(12.dp))
                    
                    // Tier Info
                    Column {
                        Text(
                            if (shouldShowUpgrade) "Current Plan" else "Premium Member",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        
                        Text(
                            currentTier.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        
                        if (shouldShowUpgrade) {
                            Text(
                                getUpgradeMessage(currentTier),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
                
                // CTA or Badge
                if (shouldShowUpgrade) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "Upgrade",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.25f)
                    ) {
                        Text(
                            "Active",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
            
            // Dismiss button (optional)
            if (isDismissible && onDismiss != null && currentTier == SubscriptionTier.PREMIUM) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Compact version of subscription ribbon for use in smaller spaces
 */
@Composable
fun CompactSubscriptionRibbon(
    currentTier: SubscriptionTier,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradient = getTierGradient(currentTier)
    val shouldShowUpgrade = currentTier != SubscriptionTier.PREMIUM
    
    Card(
        onClick = if (shouldShowUpgrade) onUpgradeClick else { {} },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(gradient)
                )
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    if (currentTier == SubscriptionTier.PREMIUM) Icons.Default.Star else Icons.Default.Upgrade,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(Modifier.width(8.dp))
                
                Text(
                    "${currentTier.displayName} Plan",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
            
            if (shouldShowUpgrade) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Get gradient colors for tier
 */
private fun getTierGradient(tier: SubscriptionTier): List<Color> {
    return when (tier) {
        SubscriptionTier.FREE -> listOf(
            Color(0xFF6366f1),
            Color(0xFF8b5cf6)
        )
        SubscriptionTier.PRO -> listOf(
            Color(0xFF8b5cf6),
            Color(0xFFa855f7)
        )
        SubscriptionTier.PREMIUM -> listOf(
            Color(0xFFa855f7),
            Color(0xFFc026d3)
        )
        SubscriptionTier.PREMIUM -> listOf(
            Color(0xFFc026d3),
            Color(0xFFdb2777)
        )
    }
}

/**
 * Get upgrade message based on current tier
 */
private fun getUpgradeMessage(tier: SubscriptionTier): String {
    return when (tier) {
        SubscriptionTier.FREE -> "Unlock unlimited tests & more"
        SubscriptionTier.PRO -> "Get AI-powered insights"
        SubscriptionTier.PREMIUM -> "Access SSB Marketplace"
        SubscriptionTier.PREMIUM -> ""
    }
}

