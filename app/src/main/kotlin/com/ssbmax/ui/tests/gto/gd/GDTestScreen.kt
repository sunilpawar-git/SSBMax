package com.ssbmax.ui.tests.gto.gd

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.R
import com.ssbmax.ui.components.TestContentErrorState
import com.ssbmax.ui.components.TestContentLoadingState
import com.ssbmax.ui.tests.common.TestLimitReachedDialog
import com.ssbmax.ui.tests.gto.common.AnimatedWhiteNoiseOverlay
import com.ssbmax.ui.tests.gto.common.GTOWhiteNoisePlayer
import com.ssbmax.ui.tests.gto.common.rememberWhiteNoiseState

/**
 * Group Discussion (GD) Test Screen
 * 
 * A 20-minute test where candidates write their thoughts on a given topic
 * with white noise simulation to match real SSB conditions.
 * 
 * Phases:
 * 1. Instructions: Test format, rules, white noise warning
 * 2. Discussion: Topic display + text input + timer + white noise
 * 3. Review: Show response, word count, allow editing
 * 4. Submitted: Navigate to result screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GDTestScreen(
    testId: String,
    onTestComplete: (submissionId: String, subscriptionType: com.ssbmax.core.domain.model.SubscriptionType) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: GDTestViewModel = hiltViewModel(),
    whiteNoisePlayer: GTOWhiteNoisePlayer = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val whiteNoiseState = rememberWhiteNoiseState(whiteNoisePlayer)
    var showExitDialog by rememberSaveable { mutableStateOf(false) }
    
    // Load test on mount
    LaunchedEffect(testId) {
        viewModel.loadTest(testId)
    }
    
    // Enable white noise during discussion phase
    LaunchedEffect(uiState.phase) {
        when (uiState.phase) {
            GDPhase.DISCUSSION -> whiteNoiseState.enable()
            else -> whiteNoiseState.disable()
        }
    }
    
    // Navigate to result when test is completed
    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted && uiState.submissionId != null) {
            onTestComplete(uiState.submissionId!!, uiState.subscriptionType)
        }
    }
    
    // Cleanup on exit
    DisposableEffect(Unit) {
        onDispose {
            whiteNoiseState.disable()
        }
    }
    
    // Exit confirmation dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit Test?") },
            text = { Text("Your progress will be lost. Are you sure you want to exit?") },
            confirmButton = {
                TextButton(onClick = {
                    whiteNoiseState.disable()
                    onNavigateBack()
                }) {
                    Text("Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Limit reached dialog
    if (uiState.showLimitDialog) {
        TestLimitReachedDialog(
            tier = uiState.subscriptionType,
            message = uiState.limitMessage,
            onUpgrade = { /* TODO: Navigate to upgrade */ },
            onDismiss = {
                viewModel.dismissLimitDialog()
                onNavigateBack()
            }
        )
    }
    
    // Upgrade required dialog
    if (uiState.showUpgradeDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissUpgradeDialog() },
            title = { Text("Upgrade Required") },
            text = { Text(uiState.upgradeMessage ?: "Upgrade to access GTO tests") },
            confirmButton = {
                TextButton(onClick = { /* TODO: Navigate to upgrade */ }) {
                    Text("Upgrade")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.dismissUpgradeDialog()
                    onNavigateBack()
                }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Error dialog
    if (uiState.error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissError() },
            title = { Text("Error") },
            text = { Text(uiState.error!!) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissError() }) {
                    Text("OK")
                }
            }
        )
    }
    
    // Main content
    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                TestContentLoadingState(
                    message = uiState.loadingMessage ?: "Loading test...",
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                when (uiState.phase) {
                    GDPhase.INSTRUCTIONS -> InstructionsPhase(
                        onStart = { viewModel.startDiscussion() },
                        onNavigateBack = onNavigateBack
                    )
                    GDPhase.DISCUSSION -> DiscussionPhase(
                        topic = uiState.topic,
                        response = uiState.response,
                        wordCount = uiState.wordCount,
                        timeRemaining = uiState.formattedTime,
                        isTimeLow = uiState.isTimeLow,
                        onResponseChanged = viewModel::onResponseChanged,
                        onProceedToReview = { viewModel.proceedToReview() },
                        onNavigateBack = { showExitDialog = true }
                    )
                    GDPhase.REVIEW -> ReviewPhase(
                        topic = uiState.topic,
                        response = uiState.response,
                        wordCount = uiState.wordCount,
                        isSubmitting = uiState.isSubmitting,
                        onBackToDiscussion = { viewModel.backToDiscussion() },
                        onSubmit = { viewModel.submitTest() }
                    )
                    GDPhase.SUBMITTED -> {
                        // Will auto-navigate via LaunchedEffect
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Submitting test...")
                            }
                        }
                    }
                }
            }
        }
        
        // White noise overlay (only during discussion)
        if (uiState.phase == GDPhase.DISCUSSION) {
            AnimatedWhiteNoiseOverlay(
                baseAlpha = 0.08f,
                intensityRange = 0.03f,
                isEnabled = whiteNoiseState.isEnabled.value
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InstructionsPhase(
    onStart: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Group Discussion") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Groups,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Group Discussion Test",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    InstructionItem(
                        icon = Icons.Default.Topic,
                        title = "Topic",
                        description = "You'll be given a topic to discuss"
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    InstructionItem(
                        icon = Icons.Default.Timer,
                        title = "Time Limit",
                        description = "20 minutes to write your thoughts"
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    InstructionItem(
                        icon = Icons.Default.Edit,
                        title = "Word Count",
                        description = "Minimum 300 words, maximum 1500 words"
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    InstructionItem(
                        icon = Icons.Default.VolumeUp,
                        title = "White Noise",
                        description = "Background noise will play to simulate real GTO conditions"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "White noise will play during the test. Please ensure your volume is at a comfortable level.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Start Test", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiscussionPhase(
    topic: String,
    response: String,
    wordCount: Int,
    timeRemaining: String,
    isTimeLow: Boolean,
    onResponseChanged: (String) -> Unit,
    onProceedToReview: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = timeRemaining,
                            color = if (isTimeLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, "Exit")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Words: $wordCount",
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                wordCount < 300 -> MaterialTheme.colorScheme.error
                                wordCount > 1500 -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                        Text(
                            text = "Min: 300 | Max: 1500",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = onProceedToReview,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = wordCount >= 300 && wordCount <= 1500
                    ) {
                        Text("Review Response")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Topic card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Topic",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = topic,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Response input
            OutlinedTextField(
                value = response,
                onValueChange = onResponseChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Write your thoughts on this topic...") },
                textStyle = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewPhase(
    topic: String,
    response: String,
    wordCount: Int,
    isSubmitting: Boolean,
    onBackToDiscussion: () -> Unit,
    onSubmit: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Response") },
                navigationIcon = {
                    IconButton(onClick = onBackToDiscussion, enabled = !isSubmitting) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = onSubmit,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSubmitting
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Submit Test")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Topic",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = topic, style = MaterialTheme.typography.bodyMedium)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Your Response",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "$wordCount words",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = response,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun InstructionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
