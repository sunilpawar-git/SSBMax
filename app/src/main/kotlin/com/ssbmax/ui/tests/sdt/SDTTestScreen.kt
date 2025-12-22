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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.R
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
            message = uiState.loadingMessage ?: stringResource(R.string.sdt_loading),
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
                    charCount = uiState.currentCharCount,
                    minChars = uiState.config?.minCharsPerQuestion ?: 50,
                    maxChars = uiState.config?.maxCharsPerQuestion ?: 1500,
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
            title = { Text(stringResource(R.string.sdt_submit_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.sdt_submit_message, uiState.validResponseCount, uiState.questions.size))
                    if (uiState.validResponseCount < 4) {
                        Text(stringResource(R.string.sdt_submit_warning), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showSubmitDialog = false
                    viewModel.submitTest()
                }) { Text(stringResource(R.string.sdt_action_submit)) }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitDialog = false }) { Text(stringResource(R.string.action_cancel)) }
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
                title = { Text(stringResource(R.string.sdt_full_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
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
            Text(stringResource(R.string.sdt_instructions_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    InstructionItem(stringResource(R.string.sdt_instruction_1))
                    InstructionItem(stringResource(R.string.sdt_instruction_2))
                    InstructionItem(stringResource(R.string.sdt_instruction_3))
                    InstructionItem(stringResource(R.string.sdt_instruction_4))
                    InstructionItem(stringResource(R.string.sdt_instruction_5))
                    InstructionItem(stringResource(R.string.sdt_instruction_6))
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.sdt_start_test))
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
    charCount: Int,
    minChars: Int,
    maxChars: Int,
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
                title = { Text(stringResource(R.string.sdt_question_header, questionNumber, totalQuestions)) },
                navigationIcon = {
                    IconButton(onClick = onShowExitDialog) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.sdt_action_exit))
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
                label = { Text(stringResource(R.string.sdt_answer_label)) },
                supportingText = {
                    val isError = charCount < minChars || charCount > maxChars
                    val color = if (isError) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    Text("Characters: $charCount / $maxChars (Min: $minChars)", color = color)
                },
                isError = charCount < minChars || charCount > maxChars,
                maxLines = 20
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onSkip, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.sdt_action_skip))
                }
                Button(
                    onClick = onNext,
                    enabled = canMoveNext,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(if (questionNumber < totalQuestions) R.string.sdt_action_next else R.string.sdt_action_review))
                }
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = onDismissExitDialog,
            title = { Text(stringResource(R.string.sdt_exit_title)) },
            text = { Text(stringResource(R.string.sdt_exit_message)) },
            confirmButton = {
                Button(onClick = onConfirmExit, colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )) { Text(stringResource(R.string.sdt_action_exit)) }
            },
            dismissButton = {
                TextButton(onClick = onDismissExitDialog) { Text(stringResource(R.string.action_cancel)) }
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
            TopAppBar(title = { Text(stringResource(R.string.sdt_review_title)) })
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
                                Text(stringResource(R.string.sdt_review_question_number, index + 1), style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                IconButton(onClick = { onEdit(index) }) {
                                    Icon(Icons.Default.Edit, stringResource(R.string.action_edit))
                                }
                            }
                            Text(question.question, style = MaterialTheme.typography.bodyMedium)
                            HorizontalDivider()
                            if (response != null && !response.isSkipped) {
                                Text(response.answer, style = MaterialTheme.typography.bodySmall)
                                Text("Characters: ${response.charCount}", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary)
                            } else {
                                Text(stringResource(R.string.sdt_review_skipped), style = MaterialTheme.typography.bodySmall,
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
                Text(stringResource(R.string.sdt_action_submit_test))
            }
        }
    }
}

