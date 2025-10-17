package com.ssbmax.ui.grading

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Test Detail Grading Screen - View submission and provide grade
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestDetailGradingScreen(
    submissionId: String,
    onNavigateBack: () -> Unit = {},
    onGradingComplete: () -> Unit = {},
    viewModel: TestDetailGradingViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSubmitDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(submissionId) {
        viewModel.loadSubmission(submissionId)
    }
    
    LaunchedEffect(uiState.isGraded) {
        if (uiState.isGraded) {
            onGradingComplete()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grade Submission") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (uiState.submission != null) {
                GradingBottomBar(
                    canSubmit = uiState.canSubmit,
                    onSubmit = { showSubmitDialog = true }
                )
            }
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
            uiState.submission != null -> {
                LazyColumn(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Student Info
                    item {
                        StudentInfoCard(
                            studentName = uiState.submission!!.userName,
                            testName = "PPDT Test",
                            submittedAt = uiState.submission!!.submittedAt
                        )
                    }
                    
                    // AI Preliminary Score
                    if (uiState.submission!!.aiPreliminaryScore != null) {
                        item {
                            AISuggestionsCard(
                                aiScore = uiState.submission!!.aiPreliminaryScore!!,
                                onAccept = { viewModel.acceptAISuggestions() }
                            )
                        }
                    }
                    
                    // Student's Story
                    item {
                        StudentStoryCard(story = uiState.submission!!.story)
                    }
                    
                    // Grading Form
                    item {
                        GradingFormCard(
                            perception = uiState.perception,
                            onPerceptionChange = { viewModel.updatePerception(it) },
                            imagination = uiState.imagination,
                            onImaginationChange = { viewModel.updateImagination(it) },
                            narration = uiState.narration,
                            onNarrationChange = { viewModel.updateNarration(it) },
                            characterDepiction = uiState.characterDepiction,
                            onCharacterDepictionChange = { viewModel.updateCharacterDepiction(it) },
                            positivity = uiState.positivity,
                            onPositivityChange = { viewModel.updatePositivity(it) },
                            feedback = uiState.feedback,
                            onFeedbackChange = { viewModel.updateFeedback(it) },
                            totalScore = uiState.totalScore
                        )
                    }
                }
            }
        }
    }
    
    // Submit confirmation dialog
    if (showSubmitDialog) {
        AlertDialog(
            onDismissRequest = { showSubmitDialog = false },
            title = { Text("Submit Grade?") },
            text = {
                Column {
                    Text("Final Score: ${uiState.totalScore}/100")
                    Text("This will notify the student.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(onClick = {
                    showSubmitDialog = false
                    viewModel.submitGrade()
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
private fun StudentInfoCard(
    studentName: String,
    testName: String,
    submittedAt: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Column {
                Text(
                    text = studentName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = testName,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun AISuggestionsCard(
    aiScore: com.ssbmax.core.domain.model.PPDTAIScore,
    onAccept: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "AI Suggestions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${aiScore.overallScore.toInt()}/100",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = "Click to use AI scores as a starting point",
                style = MaterialTheme.typography.bodySmall
            )
            
            OutlinedButton(
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AutoAwesome, null)
                Spacer(Modifier.width(8.dp))
                Text("Use AI Suggestions")
            }
        }
    }
}

@Composable
private fun StudentStoryCard(story: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Student's Story",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider()
            Text(
                text = story,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun GradingFormCard(
    perception: Float,
    onPerceptionChange: (Float) -> Unit,
    imagination: Float,
    onImaginationChange: (Float) -> Unit,
    narration: Float,
    onNarrationChange: (Float) -> Unit,
    characterDepiction: Float,
    onCharacterDepictionChange: (Float) -> Unit,
    positivity: Float,
    onPositivityChange: (Float) -> Unit,
    feedback: String,
    onFeedbackChange: (String) -> Unit,
    totalScore: Float
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Your Grade",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            ScoreSlider("Perception", perception, onPerceptionChange)
            ScoreSlider("Imagination", imagination, onImaginationChange)
            ScoreSlider("Narration", narration, onNarrationChange)
            ScoreSlider("Character Depiction", characterDepiction, onCharacterDepictionChange)
            ScoreSlider("Positivity", positivity, onPositivityChange)
            
            HorizontalDivider()
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Total Score",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${totalScore.toInt()}/100",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            OutlinedTextField(
                value = feedback,
                onValueChange = onFeedbackChange,
                label = { Text("Feedback for Student") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 8
            )
        }
    }
}

@Composable
private fun ScoreSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("${value.toInt()}/20", fontWeight = FontWeight.Medium)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..20f,
            steps = 19
        )
    }
}

@Composable
private fun GradingBottomBar(
    canSubmit: Boolean,
    onSubmit: () -> Unit
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = onSubmit,
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Done, null)
                Spacer(Modifier.width(8.dp))
                Text("Submit Grade")
            }
        }
    }
}

