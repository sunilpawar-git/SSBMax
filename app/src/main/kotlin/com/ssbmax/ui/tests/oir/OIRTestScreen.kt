package com.ssbmax.ui.tests.oir

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.core.domain.model.OIROption
import com.ssbmax.core.domain.model.OIRQuestion
import com.ssbmax.ui.components.TestContentErrorState
import com.ssbmax.ui.components.TestContentLoadingState

/**
 * OIR Test Screen - Main test interface
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OIRTestScreen(
    onTestComplete: (String, com.ssbmax.core.domain.model.SubscriptionType) -> Unit = { _, _ -> },
    onNavigateBack: () -> Unit = {},
    viewModel: OIRTestViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showExitDialog by rememberSaveable { mutableStateOf(false) }
    
    // Handle test completion
    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted && uiState.sessionId != null && uiState.subscriptionType != null) {
            onTestComplete(uiState.sessionId!!, uiState.subscriptionType!!)
        }
    }
    
    Scaffold(
        topBar = {
            OIRTestTopBar(
                questionNumber = uiState.currentQuestionIndex + 1,
                totalQuestions = uiState.totalQuestions,
                timeRemainingSeconds = uiState.timeRemainingSeconds,
                difficulty = uiState.currentDifficulty,
                onExitClick = { showExitDialog = true }
            )
        },
        bottomBar = {
            OIRTestBottomBar(
                currentIndex = uiState.currentQuestionIndex,
                totalQuestions = uiState.totalQuestions,
                isAnswered = uiState.currentQuestionAnswered,
                onPrevious = { viewModel.previousQuestion() },
                onNext = { viewModel.nextQuestion() },
                onSubmit = { viewModel.submitTest() }
            )
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
                        message = "Loading OIR test questions from cloud...",
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
                uiState.currentQuestion != null -> {
                    QuestionView(
                        question = uiState.currentQuestion!!,
                        selectedOptionId = uiState.selectedOptionId,
                        onOptionSelected = { optionId ->
                            viewModel.selectOption(optionId)
                        },
                        showFeedback = uiState.showFeedback,
                        isCorrect = uiState.isCurrentAnswerCorrect
                    )
                }
            }
        }
    }
    
    // Exit confirmation dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit Test?") },
            text = { Text("Are you sure you want to exit? Your progress will be saved and you can resume later.") },
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OIRTestTopBar(
    questionNumber: Int,
    totalQuestions: Int,
    timeRemainingSeconds: Int,
    difficulty: String,
    onExitClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Difficulty Badge
                DifficultyBadge(difficulty = difficulty)
                
                Text(
                    text = "Q $questionNumber/$totalQuestions",
                    style = MaterialTheme.typography.titleMedium
                )
                
                // Timer
                TimerChip(timeRemainingSeconds = timeRemainingSeconds)
            }
        },
        actions = {
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
    val isLowTime = timeRemainingSeconds < 300 // Less than 5 minutes
    
    AssistChip(
        onClick = { },
        label = {
            Text(
                text = String.format("%02d:%02d", minutes, seconds),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge
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
            },
            labelColor = if (isLowTime) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onTertiaryContainer
            }
        )
    )
}

@Composable
private fun QuestionView(
    question: OIRQuestion,
    selectedOptionId: String?,
    onOptionSelected: (String) -> Unit,
    showFeedback: Boolean,
    isCorrect: Boolean
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Question Type Badge
        item {
            AssistChip(
                onClick = { },
                label = {
                    Text(
                        question.type.displayName,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            )
        }
        
        // Question Text
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = question.questionText,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(20.dp)
                )
            }
        }
        
        // Options
        items(question.options) { option ->
            OptionCard(
                option = option,
                isSelected = option.id == selectedOptionId,
                isCorrect = showFeedback && option.id == question.correctAnswerId,
                isWrong = showFeedback && option.id == selectedOptionId && option.id != question.correctAnswerId,
                onClick = { if (!showFeedback) onOptionSelected(option.id) }
            )
        }
        
        // Feedback Section (shown after answering)
        if (showFeedback) {
            item {
                FeedbackCard(
                    isCorrect = isCorrect,
                    explanation = question.explanation
                )
            }
        }
    }
}

@Composable
private fun OptionCard(
    option: OIROption,
    isSelected: Boolean,
    isCorrect: Boolean,
    isWrong: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isCorrect -> MaterialTheme.colorScheme.tertiaryContainer
        isWrong -> MaterialTheme.colorScheme.errorContainer
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    
    val borderColor = when {
        isCorrect -> MaterialTheme.colorScheme.tertiary
        isWrong -> MaterialTheme.colorScheme.error
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = option.text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            
            when {
                isCorrect -> Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Correct",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(24.dp)
                )
                isWrong -> Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = "Wrong",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                isSelected -> Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedbackCard(
    isCorrect: Boolean,
    explanation: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCorrect) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
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
                Icon(
                    imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (isCorrect) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                Text(
                    text = if (isCorrect) "Correct!" else "Incorrect",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun OIRTestBottomBar(
    currentIndex: Int,
    totalQuestions: Int,
    isAnswered: Boolean,
    onPrevious: () -> Unit,
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Button
            OutlinedButton(
                onClick = onPrevious,
                enabled = currentIndex > 0
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Previous")
            }
            
            // Progress Indicator
            Text(
                text = "${currentIndex + 1}/$totalQuestions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            // Next/Submit Button
            if (currentIndex < totalQuestions - 1) {
                Button(
                    onClick = onNext,
                    enabled = isAnswered
                ) {
                    Text("Next")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
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
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Submit Test")
                }
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
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry")
        }
    }
}

/**
 * Difficulty Badge - Shows current test difficulty level
 */
@Composable
private fun DifficultyBadge(difficulty: String) {
    val (color, icon, label) = when (difficulty.uppercase()) {
        "EASY" -> Triple(
            Color(0xFF4CAF50), // Green
            Icons.Default.Done,
            "Easy"
        )
        "MEDIUM" -> Triple(
            Color(0xFFFFA726), // Orange
            Icons.Default.Star,
            "Medium"
        )
        "HARD" -> Triple(
            Color(0xFFEF5350), // Red
            Icons.Default.Warning,
            "Hard"
        )
        else -> Triple(
            MaterialTheme.colorScheme.secondary,
            Icons.Default.Star,
            difficulty
        )
    }
    
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

