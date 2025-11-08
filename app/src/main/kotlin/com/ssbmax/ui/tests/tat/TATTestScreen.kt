package com.ssbmax.ui.tests.tat

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.ssbmax.core.domain.model.TATPhase
import com.ssbmax.ui.components.TestContentErrorState
import com.ssbmax.ui.components.TestContentLoadingState

/**
 * TAT Test Screen - 12 pictures with story writing
 * Each picture: 30s viewing + 4min writing
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TATTestScreen(
    testId: String,
    onTestComplete: (String, com.ssbmax.core.domain.model.SubscriptionType) -> Unit = { _, _ -> }, // submissionId, subscriptionType
    onNavigateBack: () -> Unit = {},
    viewModel: TATTestViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showExitDialog by rememberSaveable { mutableStateOf(false) }
    var showSubmitDialog by rememberSaveable { mutableStateOf(false) }
    
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
    
    // Show subscription limit dialog if test limit reached
    if (uiState.isLimitReached) {
        com.ssbmax.ui.tests.common.TestLimitReachedDialog(
            tier = uiState.subscriptionTier,
            testsLimit = uiState.testsLimit,
            testsUsed = uiState.testsUsed,
            resetsAt = uiState.resetsAt,
            onDismiss = onNavigateBack,
            onUpgrade = onNavigateBack // Navigate back, user will handle upgrade from dashboard
        )
        return // Don't show the test UI
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("TAT Test")
                        if (uiState.currentQuestion != null) {
                            Text(
                                "Picture ${uiState.currentQuestionIndex + 1}/12",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showExitDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Exit")
                    }
                },
                actions = {
                    if (uiState.phase != TATPhase.INSTRUCTIONS) {
                        // Progress indicator
                        Text(
                            "${uiState.completedStories}/12",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (uiState.phase != TATPhase.INSTRUCTIONS && uiState.phase != TATPhase.SUBMITTED) {
                TATBottomBar(
                    phase = uiState.phase,
                    canMoveNext = uiState.canMoveToNextQuestion,
                    canMovePrevious = uiState.canMoveToPreviousQuestion,
                    canSubmit = uiState.canSubmitTest,
                    onPrevious = { viewModel.moveToPreviousQuestion() },
                    onNext = { viewModel.moveToNextQuestion() },
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
            when (uiState.phase) {
                TATPhase.INSTRUCTIONS -> {
                    InstructionsView(
                        onStart = { viewModel.startTest() }
                    )
                }
                TATPhase.IMAGE_VIEWING -> {
                    ImageViewingView(
                        imageUrl = uiState.currentQuestion?.imageUrl ?: "",
                        timeRemaining = uiState.viewingTimeRemaining,
                        sequenceNumber = uiState.currentQuestionIndex + 1
                    )
                }
                TATPhase.WRITING -> {
                    StoryWritingView(
                        story = uiState.currentStory,
                        onStoryChange = { viewModel.updateStory(it) },
                        timeRemaining = uiState.writingTimeRemaining,
                        minCharacters = uiState.currentQuestion?.minCharacters ?: 150,
                        maxCharacters = uiState.currentQuestion?.maxCharacters ?: 800,
                        charactersCount = uiState.currentStory.length,
                        sequenceNumber = uiState.currentQuestionIndex + 1
                    )
                }
                TATPhase.REVIEW_CURRENT -> {
                    ReviewCurrentStoryView(
                        story = uiState.currentStory,
                        charactersCount = uiState.currentStory.length,
                        sequenceNumber = uiState.currentQuestionIndex + 1,
                        onEdit = { viewModel.editCurrentStory() },
                        onConfirm = { viewModel.confirmCurrentStory() }
                    )
                }
                TATPhase.SUBMITTED -> {
                    // Handled by navigation
                }
            }
            
            // Loading overlay
            if (uiState.isLoading) {
                TestContentLoadingState(
                    message = "Loading TAT test images from cloud...",
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Error overlay
            if (uiState.error != null) {
                TestContentErrorState(
                    error = uiState.error!!,
                    onRetry = { viewModel.loadTest(testId) },
                    modifier = Modifier.fillMaxSize()
                )
            }
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
                    showExitDialog = false
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
    
    // Submit confirmation dialog
    if (showSubmitDialog) {
        AlertDialog(
            onDismissRequest = { showSubmitDialog = false },
            title = { Text("Submit Test?") },
            text = {
                Column {
                    Text("You have completed ${uiState.completedStories}/12 stories.")
                    if (uiState.completedStories < 12) {
                        Text(
                            "You can submit now or complete remaining stories.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showSubmitDialog = false
                    viewModel.submitTest()
                }) {
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

@Composable
private fun InstructionsView(
    onStart: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        "TAT - Thematic Apperception Test",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Write creative stories based on ambiguous pictures",
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
                        icon = Icons.Default.Image,
                        text = "You will see 12 pictures, one at a time"
                    )
                    InstructionItem(
                        icon = Icons.Default.Timer,
                        text = "View each picture for 30 seconds"
                    )
                    InstructionItem(
                        icon = Icons.Default.Edit,
                        text = "Write a story for each picture in 4 minutes"
                    )
                    InstructionItem(
                        icon = Icons.Default.Description,
                        text = "Write 150-800 characters per story"
                    )
                    InstructionItem(
                        icon = Icons.Default.Psychology,
                        text = "Be creative, positive, and describe characters"
                    )
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
                    Text("• Create complete stories with beginning, middle, and end")
                    Text("• Show leadership qualities and positive outlook")
                    Text("• Describe characters' emotions and motivations")
                    Text("• Focus on action and resolution")
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

@Composable
private fun ImageViewingView(
    imageUrl: String,
    timeRemaining: Int,
    sequenceNumber: Int
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
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Viewing Picture $sequenceNumber",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Observe carefully",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Timer, null)
                    Text(
                        "${timeRemaining}s",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (timeRemaining <= 10) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }
            }
        }
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Check if this is the 12th image (blank slide for imagination test)
            if (sequenceNumber == 12) {
                // Show blank white box for imagination test
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(androidx.compose.ui.graphics.Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Blank Slide\n(Use your imagination)",
                        style = MaterialTheme.typography.bodySmall,
                        color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.3f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                // Existing image loading code for regular TAT images (1-11)
                val context = androidx.compose.ui.platform.LocalContext.current
                
                // CRITICAL FIX: Create stable ImageRequest with remember(imageUrl)
                // This ensures the request is ONLY recreated when imageUrl changes
                val imageRequest = remember(imageUrl) {
                    android.util.Log.d("TATTestScreen", "🔄 Creating NEW ImageRequest for: $imageUrl")
                    coil.request.ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(true)
                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                        .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                        .listener(
                            onStart = {
                                android.util.Log.d("TATTestScreen", "🖼️ Coil: Loading started")
                            },
                            onSuccess = { _, _ ->
                                android.util.Log.d("TATTestScreen", "✅ Coil: Image loaded successfully!")
                            },
                            onError = { _, result ->
                                android.util.Log.e("TATTestScreen", "❌ Coil: Load failed: ${result.throwable.message}", result.throwable)
                            }
                        )
                        .build()
                }
                
                // AsyncImage with stable model - won't restart on recomposition
                AsyncImage(
                    model = imageRequest,
                    contentDescription = "TAT Picture $sequenceNumber",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
        
        // Full-width timer progress bar (matches PPDT implementation)
        LinearProgressIndicator(
            progress = { timeRemaining / 30f },
            modifier = Modifier.fillMaxWidth(),
            color = if (timeRemaining < 10) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            }
        )
    }
}

@Composable
private fun StoryWritingView(
    story: String,
    onStoryChange: (String) -> Unit,
    timeRemaining: Int,
    minCharacters: Int,
    maxCharacters: Int,
    charactersCount: Int,
    sequenceNumber: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Show blank slide reminder for 12th picture
        if (sequenceNumber == 12) {
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
                        Icon(Icons.Default.Lightbulb, contentDescription = null)
                        Text(
                            "Blank Slide (Picture 12/12)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        "Use your imagination to create a story. There was no picture shown - write what you visualize in your mind.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        if (sequenceNumber == 12) "Write Your Imagination" else "Write Your Story",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "$charactersCount / $maxCharacters characters",
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            charactersCount < minCharacters -> MaterialTheme.colorScheme.error
                            charactersCount > maxCharacters -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Timer, null)
                    Text(
                        "${timeRemaining / 60}:${(timeRemaining % 60).toString().padStart(2, '0')}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (timeRemaining <= 60) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }
            }
        }
        
        OutlinedTextField(
            value = story,
            onValueChange = onStoryChange,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            placeholder = { Text("Write a complete story with characters, situation, action, and outcome...") },
            supportingText = {
                Text("Include: Who, What, Why, How the story unfolds, and What happens in the end")
            },
            isError = charactersCount > maxCharacters
        )
        
        // Full-width timer progress bar (matches PPDT implementation)
        LinearProgressIndicator(
            progress = { timeRemaining.toFloat() / 240f },
            modifier = Modifier.fillMaxWidth(),
            color = if (timeRemaining < 60) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            }
        )
    }
}

@Composable
private fun ReviewCurrentStoryView(
    story: String,
    charactersCount: Int,
    sequenceNumber: Int,
    onEdit: () -> Unit,
    onConfirm: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Review Story $sequenceNumber",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "$charactersCount characters",
                        style = MaterialTheme.typography.bodySmall
                    )
                    HorizontalDivider()
                    Text(
                        story,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Edit")
                }
                
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Check, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Confirm")
                }
            }
        }
    }
}

@Composable
private fun TATBottomBar(
    phase: TATPhase,
    canMoveNext: Boolean,
    canMovePrevious: Boolean,
    canSubmit: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSubmit: () -> Unit
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (canMovePrevious) {
                OutlinedButton(onClick = onPrevious) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Previous")
                }
            } else {
                Spacer(Modifier.width(1.dp))
            }
            
            if (canSubmit) {
                Button(onClick = onSubmit) {
                    Text("Submit Test")
                }
            } else if (canMoveNext) {
                Button(onClick = onNext) {
                    Text("Next")
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                }
            }
        }
    }
}

