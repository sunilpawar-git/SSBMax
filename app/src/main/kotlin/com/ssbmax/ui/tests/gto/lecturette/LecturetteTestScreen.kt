package com.ssbmax.ui.tests.gto.lecturette

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.ui.components.TestContentErrorState
import com.ssbmax.ui.components.TestContentLoadingState
import com.ssbmax.ui.tests.common.TestLimitReachedDialog
import com.ssbmax.ui.tests.gto.common.AnimatedWhiteNoiseOverlay
import com.ssbmax.ui.tests.gto.common.GTOWhiteNoisePlayer
import com.ssbmax.ui.tests.gto.common.rememberWhiteNoiseState

/**
 * Lecturette Test Screen
 * 
 * A 3-minute speech test where candidates:
 * 1. Choose 1 topic from 4 options
 * 2. Immediately deliver a speech (no preparation time)
 * 3. White noise plays during speech
 * 4. Submit for AI analysis
 * 
 * Phases:
 * 1. Instructions: Test format, rules, white noise warning
 * 2. Topic Selection: Choose 1 from 4 topics
 * 3. Speech: 3-minute timer + text input + white noise
 * 4. Review: Preview speech before submission
 * 5. Submitted: Navigate to result
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LecturetteTestScreen(
    testId: String,
    onTestComplete: (submissionId: String, subscriptionType: com.ssbmax.core.domain.model.SubscriptionType) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: LecturetteTestViewModel = hiltViewModel(),
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
    
    // Enable white noise during speech phase
    LaunchedEffect(uiState.phase) {
        when (uiState.phase) {
            LecturettePhase.SPEECH -> whiteNoiseState.enable()
            else -> whiteNoiseState.disable()
        }
    }
    
    // Navigate to result when completed
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
            text = { Text(uiState.upgradeMessage ?: "Upgrade to access Lecturette test") },
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
                    LecturettePhase.INSTRUCTIONS -> InstructionsPhase(
                        onStart = { viewModel.proceedToTopicSelection() },
                        onNavigateBack = onNavigateBack
                    )
                    LecturettePhase.TOPIC_SELECTION -> TopicSelectionPhase(
                        topics = uiState.topicChoices,
                        onTopicSelected = { viewModel.selectTopic(it) },
                        onNavigateBack = onNavigateBack
                    )
                    LecturettePhase.SPEECH -> SpeechPhase(
                        selectedTopic = uiState.selectedTopic,
                        speechTranscript = uiState.speechTranscript,
                        wordCount = uiState.wordCount,
                        timeRemaining = uiState.formattedTime,
                        isTimeLow = uiState.isTimeLow,
                        onTranscriptChanged = viewModel::onTranscriptChanged,
                        onProceedToReview = { viewModel.proceedToReview() },
                        onNavigateBack = { showExitDialog = true }
                    )
                    LecturettePhase.REVIEW -> ReviewPhase(
                        selectedTopic = uiState.selectedTopic,
                        speechTranscript = uiState.speechTranscript,
                        wordCount = uiState.wordCount,
                        isSubmitting = uiState.isSubmitting,
                        onBackToSpeech = { viewModel.backToSpeech() },
                        onSubmit = { viewModel.submitTest() }
                    )
                    LecturettePhase.SUBMITTED -> {
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
        
        // White noise overlay (only during speech)
        if (uiState.phase == LecturettePhase.SPEECH) {
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
                title = { Text("Lecturette") },
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
                imageVector = Icons.Default.RecordVoiceOver,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Lecturette Test",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Deliver a 3-minute speech on a topic of your choice",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    InstructionItem(
                        icon = Icons.Default.ListAlt,
                        title = "4 Topic Choices",
                        description = "Select 1 topic from 4 options"
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    InstructionItem(
                        icon = Icons.Default.Timer,
                        title = "3 Minutes",
                        description = "No preparation time - speak immediately"
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    InstructionItem(
                        icon = Icons.Default.Edit,
                        title = "Speech Input",
                        description = "Type your speech as you would speak it"
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    InstructionItem(
                        icon = Icons.Default.VolumeUp,
                        title = "White Noise",
                        description = "Background noise simulates real GTO environment"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Tip: Speak clearly, maintain eye contact (imagine your audience), and stay within the time limit.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
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
                Text("Continue to Topic Selection", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopicSelectionPhase(
    topics: List<String>,
    onTopicSelected: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Your Topic") },
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
                .padding(16.dp)
        ) {
            Text(
                text = "Choose 1 topic for your 3-minute speech:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            topics.forEachIndexed { index, topic ->
                TopicChoiceCard(
                    number = index + 1,
                    topic = topic,
                    onClick = { onTopicSelected(topic) }
                )
                
                if (index < topics.size - 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun TopicChoiceCard(
    number: Int,
    topic: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = "$number",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            
            Text(
                text = topic,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeechPhase(
    selectedTopic: String,
    speechTranscript: String,
    wordCount: Int,
    timeRemaining: String,
    isTimeLow: Boolean,
    onTranscriptChanged: (String) -> Unit,
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
                            color = if (wordCount < 100) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Min: 100 words",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = onProceedToReview,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = wordCount >= 100
                    ) {
                        Text("Review Speech")
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
                        text = "Your Topic",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = selectedTopic,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Speech input
            OutlinedTextField(
                value = speechTranscript,
                onValueChange = onTranscriptChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Deliver your speech here...") },
                textStyle = MaterialTheme.typography.bodyLarge,
                label = { Text("Speech Transcript") }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewPhase(
    selectedTopic: String,
    speechTranscript: String,
    wordCount: Int,
    isSubmitting: Boolean,
    onBackToSpeech: () -> Unit,
    onSubmit: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Speech") },
                navigationIcon = {
                    IconButton(onClick = onBackToSpeech, enabled = !isSubmitting) {
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
                        text = "Selected Topic",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = selectedTopic, style = MaterialTheme.typography.bodyMedium)
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
                            text = "Your Speech",
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
                        text = speechTranscript,
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
