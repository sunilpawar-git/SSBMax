package com.ssbmax.ui.tests.sdt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.ssbmax.core.domain.model.SDTPhase
import com.ssbmax.ui.components.TestContentErrorState
import com.ssbmax.ui.components.TestContentLoadingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SDTTestScreen(
    testId: String,
    onTestComplete: (String, com.ssbmax.core.domain.model.SubscriptionType) -> Unit = { _, _ -> },
    onNavigateBack: () -> Unit = {},
    viewModel: SDTTestViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showExitDialog by rememberSaveable { mutableStateOf(false) }
    var showSubmitDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(testId) { viewModel.loadTest(testId) }

    LaunchedEffect(uiState.isSubmitted) {
        if (uiState.isSubmitted && uiState.submissionId != null && uiState.subscriptionType != null) {
            onTestComplete(uiState.submissionId!!, uiState.subscriptionType!!)
        }
    }

    if (uiState.isLimitReached) {
        com.ssbmax.ui.tests.common.TestLimitReachedDialog(
            tier = uiState.subscriptionTier,
            testsLimit = uiState.testsLimit,
            testsUsed = uiState.testsUsed,
            resetsAt = uiState.resetsAt,
            onUpgrade = { onNavigateBack() },
            onDismiss = onNavigateBack
        )
        return
    }

    when {
        uiState.isLoading -> TestContentLoadingState(
            message = uiState.loadingMessage ?: "Loading SDT questions...",
            modifier = Modifier.fillMaxSize()
        )
        uiState.error != null -> TestContentErrorState(
            error = uiState.error!!,
            onRetry = { viewModel.loadTest(testId) },
            modifier = Modifier.fillMaxSize()
        )
        else -> {
            when (uiState.phase) {
                SDTPhase.INSTRUCTIONS -> InstructionsView(
                    onStart = { viewModel.startTest() },
                    onNavigateBack = onNavigateBack
                )
                SDTPhase.IN_PROGRESS -> QuestionInProgressView(
                    question = uiState.currentQuestion?.question ?: "",
                    questionNumber = uiState.currentQuestionIndex + 1,
                    totalQuestions = uiState.questions.size,
                    answer = uiState.currentAnswer,
                    onAnswerChange = { viewModel.updateAnswer(it) },
                    wordCount = uiState.currentWordCount,
                    maxWords = uiState.config?.maxWordsPerQuestion ?: 1000,
                    timeRemaining = uiState.totalTimeRemaining,
                    canMoveNext = uiState.canMoveToNext,
                    onNext = { viewModel.moveToNext() },
                    onSkip = { viewModel.skipQuestion() },
                    showExitDialog = showExitDialog,
                    onShowExitDialog = { showExitDialog = true },
                    onDismissExitDialog = { showExitDialog = false },
                    onConfirmExit = onNavigateBack
                )
                SDTPhase.REVIEW -> ReviewScreen(
                    questions = uiState.questions,
                    responses = uiState.responses,
                    onEdit = { index -> viewModel.editQuestion(index) },
                    onSubmit = { showSubmitDialog = true }
                )
                SDTPhase.COMPLETED, SDTPhase.SUBMITTED -> Unit
            }
        }
    }

    if (showSubmitDialog) {
        AlertDialog(
            onDismissRequest = { showSubmitDialog = false },
            title = { Text("Submit Test?") },
            text = {
                Column {
                    Text("You have answered ${uiState.validResponseCount}/${uiState.questions.size} questions.")
                    if (uiState.validResponseCount < 4) {
                        Text("Some questions are skipped.", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showSubmitDialog = false
                    viewModel.submitTest()
                }) { Text("Submit") }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitDialog = false }) { Text("Cancel") }
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
                title = { Text("SDT - Self Description Test") },
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Instructions", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    InstructionItem("You will be presented with 4 questions")
                    InstructionItem("Describe how others perceive you and how you see yourself")
                    InstructionItem("Total time: 15 minutes for all questions")
                    InstructionItem("Maximum 1000 words per question")
                    InstructionItem("You can skip and return to questions during the test")
                    InstructionItem("Be honest and specific in your responses")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                Text("Start Test")
            }
        }
    }
}

@Composable
private fun InstructionItem(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
        Text(text)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestionInProgressView(
    question: String,
    questionNumber: Int,
    totalQuestions: Int,
    answer: String,
    onAnswerChange: (String) -> Unit,
    wordCount: Int,
    maxWords: Int,
    timeRemaining: Int,
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
                title = { Text("Question $questionNumber/$totalQuestions") },
                navigationIcon = {
                    IconButton(onClick = onShowExitDialog) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Exit")
                    }
                },
                actions = {
                    TimerDisplay(timeRemaining)
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LinearProgressIndicator(
                progress = { questionNumber.toFloat() / totalQuestions },
                modifier = Modifier.fillMaxWidth()
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(question, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }

            OutlinedTextField(
                value = answer,
                onValueChange = onAnswerChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = { Text("Your Answer") },
                supportingText = {
                    val color = if (wordCount > maxWords) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                    Text("$wordCount / $maxWords words", color = color)
                },
                isError = wordCount > maxWords,
                maxLines = 20
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onSkip, modifier = Modifier.weight(1f)) {
                    Text("Skip")
                }
                Button(
                    onClick = onNext,
                    enabled = canMoveNext,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (questionNumber < totalQuestions) "Next" else "Review")
                }
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = onDismissExitDialog,
            title = { Text("Exit Test?") },
            text = { Text("Your progress will be lost. Are you sure you want to exit?") },
            confirmButton = {
                Button(onClick = onConfirmExit, colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )) { Text("Exit") }
            },
            dismissButton = {
                TextButton(onClick = onDismissExitDialog) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun TimerDisplay(timeRemaining: Int) {
    val minutes = timeRemaining / 60
    val seconds = timeRemaining % 60
    val color = when {
        timeRemaining > 300 -> MaterialTheme.colorScheme.primary
        timeRemaining > 60 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    Text("%02d:%02d".format(minutes, seconds), color = color, fontWeight = FontWeight.Bold)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewScreen(
    questions: List<com.ssbmax.core.domain.model.SDTQuestion>,
    responses: List<com.ssbmax.core.domain.model.SDTQuestionResponse>,
    onEdit: (Int) -> Unit,
    onSubmit: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Review Your Answers") })
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.weight(1f).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(questions) { index, question ->
                    val response = responses.getOrNull(index)
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Question ${index + 1}", style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                IconButton(onClick = { onEdit(index) }) {
                                    Icon(Icons.Default.Edit, "Edit")
                                }
                            }
                            Text(question.question, style = MaterialTheme.typography.bodyMedium)
                            Divider()
                            if (response != null && !response.isSkipped) {
                                Text(response.answer, style = MaterialTheme.typography.bodySmall)
                                Text("${response.wordCount} words", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary)
                            } else {
                                Text("(Skipped)", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Text("Submit Test")
            }
        }
    }
}

