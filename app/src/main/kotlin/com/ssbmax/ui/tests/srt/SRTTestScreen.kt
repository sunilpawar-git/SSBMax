package com.ssbmax.ui.tests.srt

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.core.domain.model.SRTPhase
import com.ssbmax.ui.components.TestContentErrorState
import com.ssbmax.ui.components.TestContentLoadingState

/**
 * SRT Test Screen - 60 practical situations with response input
 * Students describe how they would react in each situation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SRTTestScreen(
    testId: String,
    onTestComplete: (String) -> Unit = {}, // submissionId
    onNavigateBack: () -> Unit = {},
    viewModel: SRTTestViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showExitDialog by remember { mutableStateOf(false) }
    var showSubmitDialog by remember { mutableStateOf(false) }
    
    // Initialize test
    LaunchedEffect(testId) {
        viewModel.loadTest(testId)
    }
    
    // Handle completion
    LaunchedEffect(uiState.isSubmitted) {
        if (uiState.isSubmitted && uiState.submissionId != null) {
            onTestComplete(uiState.submissionId!!)
        }
    }
    
    when {
        uiState.isLoading -> {
            TestContentLoadingState(
                message = "Loading SRT test situations from cloud...",
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
                SRTPhase.INSTRUCTIONS -> {
                    InstructionsView(
                        onStart = { viewModel.startTest() },
                        onNavigateBack = onNavigateBack
                    )
                }
                SRTPhase.IN_PROGRESS -> {
                    TestInProgressView(
                        situation = uiState.currentSituation?.situation ?: "",
                        situationNumber = uiState.currentSituationIndex + 1,
                        totalSituations = uiState.situations.size,
                        response = uiState.currentResponse,
                        onResponseChange = { viewModel.updateResponse(it) },
                        minChars = uiState.config?.minResponseLength ?: 20,
                        maxChars = uiState.config?.maxResponseLength ?: 200,
                        canMoveNext = uiState.canMoveToNext,
                        onNext = { viewModel.moveToNext() },
                        onSkip = { viewModel.skipSituation() },
                        showExitDialog = showExitDialog,
                        onShowExitDialog = { showExitDialog = true },
                        onDismissExitDialog = { showExitDialog = false },
                        onConfirmExit = onNavigateBack
                    )
                }
                SRTPhase.REVIEW -> {
                    ReviewScreen(
                        responses = uiState.responses,
                        totalSituations = uiState.situations.size,
                        onEdit = { index -> viewModel.editResponse(index) },
                        onSubmit = { showSubmitDialog = true }
                    )
                }
                SRTPhase.COMPLETED, SRTPhase.SUBMITTED -> {
                    // Handled by navigation
                }
            }
        }
    }
    
    // Submit confirmation dialog
    if (showSubmitDialog) {
        AlertDialog(
            onDismissRequest = { showSubmitDialog = false },
            title = { Text("Submit Test?") },
            text = {
                Column {
                    Text("You have completed ${uiState.validResponseCount}/${uiState.situations.size} situations.")
                    if (uiState.validResponseCount < 60) {
                        Text(
                            "Some situations are skipped or incomplete.",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InstructionsView(
    onStart: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SRT Test") },
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
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            "SRT - Situation Reaction Test",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Describe how you would react in practical situations",
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
                            icon = Icons.Default.FormatListNumbered,
                            text = "60 practical situations will be presented"
                        )
                        InstructionItem(
                            icon = Icons.Default.Edit,
                            text = "Describe YOUR reaction in 20-200 characters"
                        )
                        InstructionItem(
                            icon = Icons.Default.Timer,
                            text = "Total time: 30 minutes (~30 sec per situation)"
                        )
                        InstructionItem(
                            icon = Icons.Default.CheckCircle,
                            text = "You can review and edit responses before submitting"
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
                        Text("• Be practical and realistic")
                        Text("• Show initiative and leadership")
                        Text("• Focus on action, not just thinking")
                        Text("• Consider others' wellbeing")
                        Text("• Demonstrate responsibility")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TestInProgressView(
    situation: String,
    situationNumber: Int,
    totalSituations: Int,
    response: String,
    onResponseChange: (String) -> Unit,
    minChars: Int,
    maxChars: Int,
    canMoveNext: Boolean,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    showExitDialog: Boolean,
    onShowExitDialog: () -> Unit,
    onDismissExitDialog: () -> Unit,
    onConfirmExit: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SRT Test")
                        Text(
                            "Situation $situationNumber / $totalSituations",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onShowExitDialog) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Exit")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSkip,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Skip")
                    }
                    
                    Button(
                        onClick = onNext,
                        enabled = canMoveNext,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Next")
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Situation card
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
                    Text(
                        "Situation",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        situation,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            
            // Response input
            OutlinedTextField(
                value = response,
                onValueChange = onResponseChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = { Text("Your Response") },
                placeholder = { Text("Describe what YOU would do in this situation...") },
                supportingText = {
                    Text(
                        "${response.length} / $maxChars characters",
                        color = when {
                            response.length < minChars -> MaterialTheme.colorScheme.error
                            response.length > maxChars -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                },
                isError = response.length > maxChars,
                maxLines = 8
            )
        }
    }
    
    // Exit dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = onDismissExitDialog,
            title = { Text("Exit Test?") },
            text = { Text("Your progress will be lost. Are you sure?") },
            confirmButton = {
                TextButton(onClick = onConfirmExit) {
                    Text("Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissExitDialog) {
                    Text("Continue")
                }
            }
        )
    }
}

@Composable
private fun ReviewScreen(
    responses: List<com.ssbmax.core.domain.model.SRTSituationResponse>,
    totalSituations: Int,
    onEdit: (Int) -> Unit,
    onSubmit: () -> Unit
) {
    Scaffold(
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = onSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Submit Test")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Review Your Responses",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text("${responses.filter { it.isValidResponse }.size}/$totalSituations completed")
                    }
                }
            }
            
            items(responses.size) { index ->
                val response = responses[index]
                ResponseReviewCard(
                    number = index + 1,
                    situation = response.situation,
                    response = response.response,
                    isSkipped = response.isSkipped,
                    onEdit = { onEdit(index) }
                )
            }
        }
    }
}

@Composable
private fun ResponseReviewCard(
    number: Int,
    situation: String,
    response: String,
    isSkipped: Boolean,
    onEdit: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Situation $number",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                
                if (isSkipped) {
                    AssistChip(
                        onClick = { },
                        label = { Text("Skipped") },
                        leadingIcon = {
                            Icon(Icons.Default.Warning, null, modifier = Modifier.size(18.dp))
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    )
                }
            }
            
            Text(
                situation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (!isSkipped) {
                HorizontalDivider()
                Text(
                    response,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Edit")
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

