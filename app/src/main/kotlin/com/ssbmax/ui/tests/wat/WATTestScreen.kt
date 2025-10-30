package com.ssbmax.ui.tests.wat

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.core.domain.model.WATPhase
import com.ssbmax.ui.components.TestContentErrorState
import com.ssbmax.ui.components.TestContentLoadingState

/**
 * WAT Test Screen - 60 words with rapid 15-second responses
 * Minimalist fullscreen design for focused responses
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WATTestScreen(
    testId: String,
    onTestComplete: (String, com.ssbmax.core.domain.model.SubscriptionType) -> Unit = { _, _ -> },
    onNavigateBack: () -> Unit = {},
    viewModel: WATTestViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showExitDialog by rememberSaveable { mutableStateOf(false) }
    
    // Initialize test
    LaunchedEffect(testId) {
        viewModel.loadTest(testId)
    }
    
    // Handle completion
    LaunchedEffect(uiState.isSubmitted) {
        if (uiState.isSubmitted && uiState.submissionId != null && uiState.subscriptionType != null) {
            onTestComplete(uiState.submissionId!!, uiState.subscriptionType!!)
        }
    }
    
    when {
        uiState.isLoading -> {
            TestContentLoadingState(
                message = "Loading WAT test words from cloud...",
                modifier = Modifier.fillMaxSize()
            )
        }
        uiState.error != null -> {
            TestContentErrorState(
                error = uiState.error!!,
                onRetry = { viewModel.loadTest(testId) },
                modifier = Modifier.fillMaxSize()
            )
        }
        else -> {
            when (uiState.phase) {
                WATPhase.INSTRUCTIONS -> {
                    InstructionsView(
                        onStart = { viewModel.startTest() },
                        onNavigateBack = onNavigateBack
                    )
                }
                WATPhase.IN_PROGRESS -> {
                    TestInProgressView(
                        word = uiState.currentWord?.word ?: "",
                        wordNumber = uiState.currentWordIndex + 1,
                        totalWords = uiState.words.size,
                        timeRemaining = uiState.timeRemaining,
                        response = uiState.currentResponse,
                        onResponseChange = { viewModel.updateResponse(it) },
                        onSubmit = { viewModel.submitResponse() },
                        onSkip = { viewModel.skipWord() },
                        showExitDialog = showExitDialog,
                        onShowExitDialog = { showExitDialog = true },
                        onDismissExitDialog = { showExitDialog = false },
                        onConfirmExit = onNavigateBack
                    )
                }
                WATPhase.COMPLETED -> {
                    // Handled by auto-submit
                }
                WATPhase.SUBMITTED -> {
                    // Handled by navigation
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InstructionsView(
    onStart: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WAT Test") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            "WAT - Word Association Test",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Write the first word that comes to mind",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Instructions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        InstructionItem(
                            icon = Icons.Default.Numbers,
                            text = "60 words will be shown one at a time"
                        )
                        InstructionItem(
                            icon = Icons.Default.Timer,
                            text = "15 seconds per word"
                        )
                        InstructionItem(
                            icon = Icons.Default.Psychology,
                            text = "Write the FIRST word/phrase that comes to mind"
                        )
                        InstructionItem(
                            icon = Icons.Default.Speed,
                            text = "Auto-advances to next word after timeout"
                        )
                        InstructionItem(
                            icon = Icons.Default.Warning,
                            text = "Don't overthink - be spontaneous!"
                        )
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Info, null)
                        Column {
                            Text(
                                "Important",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Test cannot be paused once started. Total time: ~15 minutes",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lightbulb, null)
                            Text(
                                "Tips for Success",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text("• Be honest and spontaneous")
                        Text("• Positive associations are better")
                        Text("• Don't repeat same words")
                        Text("• One word or short phrase only")
                    }
                }
            }
            
            item {
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start Test")
                }
            }
        }
    }
}

@Composable
private fun TestInProgressView(
    word: String,
    wordNumber: Int,
    totalWords: Int,
    timeRemaining: Int,
    response: String,
    onResponseChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onSkip: () -> Unit,
    showExitDialog: Boolean,
    onShowExitDialog: () -> Unit,
    onDismissExitDialog: () -> Unit,
    onConfirmExit: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background with gradient based on time remaining
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = when {
                timeRemaining <= 5 -> MaterialTheme.colorScheme.errorContainer
                timeRemaining <= 10 -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top bar with progress and exit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onShowExitDialog) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Exit")
                    }
                    
                    Text(
                        "$wordNumber / $totalWords",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Timer
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (timeRemaining <= 5) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    ) {
                        Text(
                            "${timeRemaining}s",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                
                Spacer(Modifier.weight(1f))
                
                // Word display (Large, centered)
                AnimatedContent(
                    targetState = word,
                    transitionSpec = {
                        fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut()
                    },
                    label = "word_animation"
                ) { currentWord ->
                    Text(
                        text = currentWord,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 56.sp
                        ),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp)
                    )
                }
                
                Spacer(Modifier.weight(1f))
                
                // Response input
                OutlinedTextField(
                    value = response,
                    onValueChange = onResponseChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Your response...") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        textAlign = TextAlign.Center
                    )
                )
                
                Spacer(Modifier.height(16.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSkip,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Skip")
                    }
                    
                    Button(
                        onClick = onSubmit,
                        modifier = Modifier.weight(1f),
                        enabled = response.isNotBlank()
                    ) {
                        Text("Submit")
                    }
                }
            }
        }
    }
    
    // Exit dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = onDismissExitDialog,
            title = { Text("Exit Test?") },
            text = {
                Column {
                    Text("Test is in progress and cannot be paused.")
                    Text("All progress will be lost if you exit now.")
                }
            },
            confirmButton = {
                TextButton(onClick = onConfirmExit) {
                    Text("Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissExitDialog) {
                    Text("Continue Test")
                }
            }
        )
    }
}

@Composable
private fun InstructionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

