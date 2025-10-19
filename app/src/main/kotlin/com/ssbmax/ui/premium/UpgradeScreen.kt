package com.ssbmax.ui.premium

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.core.domain.model.BillingCycle
import com.ssbmax.core.domain.model.SubscriptionTier

/**
 * Upgrade Screen - Beautiful subscription tier comparison
 * Visual-only mockup without actual payment gateway integration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpgradeScreen(
    onNavigateBack: () -> Unit,
    viewModel: UpgradeViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Animation state
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choose Your Plan") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Unlock Your Potential",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Choose the perfect plan for your SSB journey",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            // Billing Cycle Toggle
            item {
                BillingCycleSelector(
                    selectedCycle = uiState.selectedBillingCycle,
                    onCycleSelected = { viewModel.selectBillingCycle(it) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Plan Cards
            items(uiState.availablePlans) { plan ->
                AnimatedPlanCard(
                    plan = plan,
                    currentTier = uiState.currentTier,
                    selectedBillingCycle = uiState.selectedBillingCycle,
                    isVisible = isVisible,
                    onUpgradeClick = { viewModel.upgradeToPlan(plan.tier) }
                )
            }
            
            // Footer Info
            item {
                InfoSection()
            }
        }
    }
    
    // Coming Soon Dialog
    if (uiState.showComingSoonDialog) {
        ComingSoonDialog(
            planName = uiState.selectedPlanForUpgrade?.displayName ?: "Premium",
            onDismiss = { viewModel.dismissComingSoonDialog() }
        )
    }
}

@Composable
private fun BillingCycleSelector(
    selectedCycle: BillingCycle,
    onCycleSelected: (BillingCycle) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BillingCycle.entries.forEach { cycle ->
                FilterChip(
                    selected = selectedCycle == cycle,
                    onClick = { onCycleSelected(cycle) },
                    label = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                when (cycle) {
                                    BillingCycle.MONTHLY -> "Monthly"
                                    BillingCycle.QUARTERLY -> "Quarterly"
                                    BillingCycle.ANNUALLY -> "Annually"
                                },
                                style = MaterialTheme.typography.labelLarge
                            )
                            if (cycle != BillingCycle.MONTHLY) {
                                Text(
                                    when (cycle) {
                                        BillingCycle.QUARTERLY -> "Save 13%"
                                        BillingCycle.ANNUALLY -> "Save 25%"
                                        else -> ""
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun AnimatedPlanCard(
    plan: SubscriptionPlan,
    currentTier: SubscriptionTier,
    selectedBillingCycle: BillingCycle,
    isVisible: Boolean,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = tween(durationMillis = 300),
        label = "scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "alpha"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .alpha(alpha),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (plan.isRecommended) 8.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (plan.tier == currentTier)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header with gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = plan.gradient.map { parseColor(it) }
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                plan.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                plan.tagline,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        
                        if (plan.isRecommended) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.3f)
                            ) {
                                Text(
                                    "POPULAR",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        
                        if (plan.tier == currentTier) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.3f)
                            ) {
                                Text(
                                    "CURRENT",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Price
                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        if (plan.tier == SubscriptionTier.BASIC) {
                            Text(
                                "Free",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        } else {
                            Text(
                                "₹${plan.getPriceForCycle(selectedBillingCycle).toInt()}",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                "/${when(selectedBillingCycle) {
                                    BillingCycle.MONTHLY -> "month"
                                    BillingCycle.QUARTERLY -> "quarter"
                                    BillingCycle.ANNUALLY -> "year"
                                }}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                    
                    plan.getSavingsForCycle(selectedBillingCycle)?.let { savings ->
                        Text(
                            savings,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Features
            plan.features.forEach { feature ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (feature.isIncluded) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = if (feature.isIncluded) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        feature.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (feature.isIncluded)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Action Button
            Button(
                onClick = onUpgradeClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = plan.tier != currentTier,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    when {
                        plan.tier == currentTier -> "Current Plan"
                        plan.tier < currentTier -> "Downgrade"
                        else -> "Upgrade Now"
                    },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun InfoSection(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Security, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text("Secure payment processing", style = MaterialTheme.typography.bodyMedium)
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text("Cancel anytime, no questions asked", style = MaterialTheme.typography.bodyMedium)
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.SupportAgent, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text("24/7 customer support", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ComingSoonDialog(
    planName: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Construction, contentDescription = null, modifier = Modifier.size(48.dp))
        },
        title = {
            Text("Coming Soon!", fontWeight = FontWeight.Bold)
        },
        text = {
            Text(
                "Payment integration for the $planName plan is coming soon! " +
                "We're working hard to bring you seamless subscription management.",
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it")
            }
        }
    )
}

/**
 * Parse hex color string to Compose Color
 */
private fun parseColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color.Gray
    }
}

