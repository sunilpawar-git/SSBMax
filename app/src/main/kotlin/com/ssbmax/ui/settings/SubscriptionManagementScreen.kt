package com.ssbmax.ui.settings

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Subscription Management Screen
 * Shows current plan, usage, and upgrade options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionManagementScreen(
    onNavigateBack: () -> Unit,
    onUpgrade: (SubscriptionTierModel) -> Unit,
    viewModel: SubscriptionManagementViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val TAG = "SubscriptionManagementScreen"
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        Log.d(TAG, "Screen launched, loading subscription data")
        viewModel.loadSubscriptionData()
    }
    
    // Log UI state changes
    LaunchedEffect(uiState) {
        Log.d(TAG, "UI State changed: isLoading=${uiState.isLoading}, " +
                "tier=${uiState.currentTier.displayName}, " +
                "usageItems=${uiState.monthlyUsage.size}, " +
                "error=${uiState.error}")
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subscription & Billing") },
                navigationIcon = {
                    IconButton(onClick = { 
                        Log.d(TAG, "Back button clicked, navigating back")
                        try {
                            onNavigateBack()
                        } catch (e: Exception) {
                            Log.e(TAG, "ERROR during navigation back", e)
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                ErrorState(
                    error = uiState.error!!,
                    onRetry = { viewModel.loadSubscriptionData() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            else -> {
                SubscriptionContent(
                    uiState = uiState,
                    onUpgrade = onUpgrade,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                )
            }
        }
    }
}

@Composable
private fun SubscriptionContent(
    uiState: SubscriptionManagementUiState,
    onUpgrade: (SubscriptionTierModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val TAG = "SubscriptionContent"
    
    Log.d(TAG, "Rendering content for tier=${uiState.currentTier.displayName}")
    
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Current Plan Card
        Log.d(TAG, "Rendering CurrentPlanCard")
        CurrentPlanCard(
            tier = uiState.currentTier,
            expiresAt = uiState.subscriptionExpiresAt
        )
        
        // Monthly Usage Card
        if (uiState.currentTier != SubscriptionTierModel.PREMIUM) {
            Log.d(TAG, "Rendering MonthlyUsageCard for non-PREMIUM user")
            MonthlyUsageCard(usage = uiState.monthlyUsage)
        } else {
            Log.d(TAG, "Skipping MonthlyUsageCard for PREMIUM user")
        }
        
        // Benefits Comparison
        Text(
            text = "Compare Plans",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        PlanComparisonCards(
            currentTier = uiState.currentTier,
            onUpgrade = onUpgrade
        )
        
        // FAQ Section
        Spacer(modifier = Modifier.height(8.dp))
        SubscriptionFAQ()
    }
}

@Composable
private fun CurrentPlanCard(
    tier: SubscriptionTierModel,
    expiresAt: String?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (tier) {
                SubscriptionTierModel.FREE -> MaterialTheme.colorScheme.surface
                SubscriptionTierModel.PRO -> MaterialTheme.colorScheme.primaryContainer
                SubscriptionTierModel.PREMIUM -> MaterialTheme.colorScheme.tertiaryContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (tier) {
                        SubscriptionTierModel.FREE -> Icons.Default.AccountCircle
                        SubscriptionTierModel.PRO -> Icons.Default.Star
                        SubscriptionTierModel.PREMIUM -> Icons.Default.Diamond
                    },
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Column {
                    Text(
                        text = "Current Plan",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = tier.displayName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (tier != SubscriptionTierModel.FREE) {
                        Text(
                            text = tier.price,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            if (expiresAt != null && tier != SubscriptionTierModel.FREE) {
                HorizontalDivider()
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Renews on $expiresAt",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthlyUsageCard(
    usage: Map<String, UsageInfo>,
    modifier: Modifier = Modifier
) {
    val TAG = "MonthlyUsageCard"
    var expanded by remember { mutableStateOf(false) }
    
    // Log when composable is created
    LaunchedEffect(Unit) {
        Log.d(TAG, "MonthlyUsageCard composed with ${usage.size} usage items")
        usage.forEach { (key, value) ->
            Log.d(TAG, "  $key: used=${value.used}, limit=${value.limit}")
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = { 
            Log.d(TAG, "MonthlyUsageCard clicked! Current expanded=$expanded, toggling to ${!expanded}")
            try {
                expanded = !expanded
                Log.d(TAG, "Successfully toggled expanded state to $expanded")
            } catch (e: Exception) {
                Log.e(TAG, "ERROR toggling expanded state", e)
                throw e
            }
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "This Month's Usage",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }
            
            AnimatedVisibility(visible = expanded) {
                Log.d(TAG, "AnimatedVisibility: expanded=$expanded, rendering ${usage.size} usage rows")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider()
                    usage.forEach { (testType, info) ->
                        Log.d(TAG, "  Rendering UsageRow for $testType")
                        UsageRow(testType = testType, info = info)
                    }
                }
            }
        }
    }
}

@Composable
private fun UsageRow(
    testType: String,
    info: UsageInfo,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = testType,
            style = MaterialTheme.typography.bodyMedium
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Progress bar
            LinearProgressIndicator(
                progress = { info.used.toFloat() / info.limit.toFloat() },
                modifier = Modifier.width(60.dp),
                color = if (info.used >= info.limit) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
            
            Text(
                text = if (info.limit == -1) {
                    "${info.used} used"
                } else {
                    "${info.used}/${info.limit}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (info.used >= info.limit && info.limit != -1) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.width(60.dp),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun PlanComparisonCards(
    currentTier: SubscriptionTierModel,
    onUpgrade: (SubscriptionTierModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SubscriptionTierModel.values().forEach { tier ->
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
        } else null,
        colors = if (isCurrentPlan) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
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
                            text = "Most Popular",
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
                            text = "Current Plan",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }
            }
            
            // Features
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
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // Action Button
            if (!isCurrentPlan) {
                Button(
                    onClick = onUpgrade,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (tier.ordinal > SubscriptionTierModel.FREE.ordinal) {
                            "Upgrade to ${tier.displayName}"
                        } else {
                            "Select Plan"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SubscriptionFAQ(modifier: Modifier = Modifier) {
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
                text = "Frequently Asked Questions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            FAQItem(
                question = "Can I cancel anytime?",
                answer = "Yes! You can cancel your subscription at any time. You'll continue to have access until the end of your billing period."
            )
            
            FAQItem(
                question = "What payment methods do you accept?",
                answer = "We accept UPI, credit/debit cards, net banking, and popular digital wallets."
            )
            
            FAQItem(
                question = "Do test limits reset monthly?",
                answer = "Yes! Your test limits reset on the 1st of every month based on your subscription tier."
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

@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = error,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

/**
 * Usage info for a specific test type
 */
data class UsageInfo(
    val used: Int,
    val limit: Int // -1 for unlimited
)

