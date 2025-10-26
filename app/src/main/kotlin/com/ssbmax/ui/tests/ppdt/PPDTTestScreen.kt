package com.ssbmax.ui.tests.ppdt

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.ssbmax.core.domain.model.PPDTPhase
import com.ssbmax.ui.components.TestContentErrorState
import com.ssbmax.ui.components.TestContentLoadingState

/**
 * PPDT Test Screen - Image viewing + Story writing
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PPDTTestScreen(
    testId: String,
    onTestComplete: (String, com.ssbmax.core.domain.model.SubscriptionType) -> Unit = { _, _ -> },
    onNavigateBack: () -> Unit = {},
    viewModel: PPDTTestViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showExitDialog by remember { mutableStateOf(false) }
    var showSubmitDialog by remember { mutableStateOf(false) }
    
    // Handle test submission
    LaunchedEffect(uiState.isSubmitted) {
        if (uiState.isSubmitted && uiState.submissionId != null && uiState.subscriptionType != null) {
            onTestComplete(uiState.submissionId!!, uiState.subscriptionType!!)
        }
    }
    
    Scaffold(
        topBar = {
            PPDTTopBar(
                currentPhase = uiState.currentPhase,
                timeRemainingSeconds = uiState.timeRemainingSeconds,
                onExitClick = { showExitDialog = true }
            )
        },
        bottomBar = {
            if (uiState.currentPhase != PPDTPhase.INSTRUCTIONS) {
                PPDTBottomBar(
                    currentPhase = uiState.currentPhase,
                    canProceed = uiState.canProceedToNextPhase,
                    onNext = { viewModel.proceedToNextPhase() },
                    onSubmit = { showSubmitDialog = true }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    TestContentLoadingState(
                        message = "Loading PPDT test from cloud...",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                uiState.error != null -> {
                    TestContentErrorState(
                        error = uiState.error!!,
                        onRetry = { viewModel.loadTest() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    when (uiState.currentPhase) {
                        PPDTPhase.INSTRUCTIONS -> InstructionsPhase(
                            onStart = { viewModel.startTest() }
                        )
                        PPDTPhase.IMAGE_VIEWING -> ImageViewingPhase(
                            imageUrl = uiState.imageUrl,
                            timeRemainingSeconds = uiState.timeRemainingSeconds
                        )
                        PPDTPhase.WRITING -> WritingPhase(
                            story = uiState.story,
                            onStoryChange = { viewModel.updateStory(it) },
                            charactersCount = uiState.charactersCount,
                            minCharacters = uiState.minCharacters,
                            maxCharacters = uiState.maxCharacters,
                            timeRemainingSeconds = uiState.timeRemainingSeconds
                        )
                        PPDTPhase.REVIEW -> ReviewPhase(
                            imageUrl = uiState.imageUrl,
                            story = uiState.story,
                            charactersCount = uiState.charactersCount,
                            onEdit = { viewModel.returnToWriting() }
                        )
                        PPDTPhase.SUBMITTED -> {
                            // This shouldn't show, navigation happens in LaunchedEffect
                        }
                    }
                }
            }
        }
    }
    
    // Exit confirmation dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit Test?") },
            text = { Text("Your progress will be saved. You can continue later from where you left off.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.pauseTest()
                        onNavigateBack()
                    }
                ) {
                    Text("Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Continue Test")
                }
            }
        )
    }
    
    // Submit confirmation dialog
    if (showSubmitDialog) {
        AlertDialog(
            onDismissRequest = { showSubmitDialog = false },
            title = { Text("Submit Test?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Once submitted, you cannot make changes.")
                    Text(
                        "Your story will be reviewed by your instructor.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSubmitDialog = false
                        viewModel.submitTest()
                    }
                ) {
                    Text("Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PPDTTopBar(
    currentPhase: PPDTPhase,
    timeRemainingSeconds: Int,
    onExitClick: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "PPDT Test",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = currentPhase.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        actions = {
            if (currentPhase in listOf(PPDTPhase.IMAGE_VIEWING, PPDTPhase.WRITING)) {
                TimerChip(timeRemainingSeconds = timeRemainingSeconds)
                Spacer(modifier = Modifier.width(8.dp))
            }
            IconButton(onClick = onExitClick) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Exit Test"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
private fun TimerChip(timeRemainingSeconds: Int) {
    val minutes = timeRemainingSeconds / 60
    val seconds = timeRemainingSeconds % 60
    val isLowTime = timeRemainingSeconds < 30
    
    AssistChip(
        onClick = { },
        label = {
            Text(
                text = String.format("%02d:%02d", minutes, seconds),
                fontWeight = FontWeight.Bold
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (isLowTime) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.tertiaryContainer
            }
        )
    )
}

@Composable
private fun InstructionsPhase(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Image,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "PPDT Instructions",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InstructionItem(
                    number = "1",
                    text = "You will be shown a hazy picture for 30 seconds"
                )
                InstructionItem(
                    number = "2",
                    text = "Observe carefully: characters, their age, gender, mood"
                )
                InstructionItem(
                    number = "3",
                    text = "After viewing, you'll have 4 minutes to write a story"
                )
                InstructionItem(
                    number = "4",
                    text = "Your story should include:\n• What led to this situation?\n• What is happening now?\n• What will be the outcome?"
                )
                InstructionItem(
                    number = "5",
                    text = "Write a clear, positive, and imaginative story"
                )
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "Your story will be reviewed by your instructor. You'll receive AI-based preliminary feedback immediately.",
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
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Start Test")
        }
    }
}

@Composable
private fun InstructionItem(number: String, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ImageViewingPhase(
    imageUrl: String,
    timeRemainingSeconds: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Text(
                text = "Observe the image carefully. Try to identify characters, their mood, and what's happening.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Center
            )
        }
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // TODO: Load actual image from URL
                // For now, show placeholder
                Image(
                    painter = rememberAsyncImagePainter(imageUrl),
                    contentDescription = "PPDT Test Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
        
        LinearProgressIndicator(
            progress = { timeRemainingSeconds / 30f },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun WritingPhase(
    story: String,
    onStoryChange: (String) -> Unit,
    charactersCount: Int,
    minCharacters: Int,
    maxCharacters: Int,
    timeRemainingSeconds: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Write your story based on the image you saw",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Include: What led to this? What's happening? What will be the outcome?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        OutlinedTextField(
            value = story,
            onValueChange = {
                if (it.length <= maxCharacters) {
                    onStoryChange(it)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            placeholder = { Text("Start writing your story here...") },
            supportingText = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Minimum: $minCharacters characters",
                        color = if (charactersCount >= minCharacters) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                    Text(
                        text = "$charactersCount / $maxCharacters",
                        color = when {
                            charactersCount >= maxCharacters -> MaterialTheme.colorScheme.error
                            charactersCount >= minCharacters -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            },
            minLines = 10,
            maxLines = 15
        )
    }
}

@Composable
private fun ReviewPhase(
    imageUrl: String,
    story: String,
    charactersCount: Int,
    onEdit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Review Your Submission",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Your Story",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = story,
                    style = MaterialTheme.typography.bodyLarge
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    text = "$charactersCount characters",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        OutlinedButton(
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Edit Story")
        }
    }
}

@Composable
private fun PPDTBottomBar(
    currentPhase: PPDTPhase,
    canProceed: Boolean,
    onNext: () -> Unit,
    onSubmit: () -> Unit
) {
    Surface(
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            when (currentPhase) {
                PPDTPhase.IMAGE_VIEWING -> {
                    // Auto-proceeds, no button needed
                }
                PPDTPhase.WRITING -> {
                    Button(
                        onClick = onNext,
                        enabled = canProceed
                    ) {
                        Text("Review")
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                PPDTPhase.REVIEW -> {
                    Button(
                        onClick = onSubmit,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Submit Test")
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Text(
            text = "Error Loading Test",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Button(onClick = onRetry) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry")
        }
    }
}

