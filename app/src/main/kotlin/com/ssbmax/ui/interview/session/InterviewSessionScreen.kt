package com.ssbmax.ui.interview.session

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.R

private const val TAG = "InterviewSessionScreen"

/**
 * Interview Session Screen
 *
 * Simplified UX: User types response OR uses keyboard's voice input (mic button)
 * The Android keyboard's built-in voice-to-text is used instead of custom recording UI
 *
 * OPTIMIZATION: Uses background analysis - user navigates away instantly,
 * results arrive via notification.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterviewSessionScreen(
    sessionId: String,
    onNavigateBack: () -> Unit,
    onNavigateToResult: (String) -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: InterviewSessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showExitDialog by remember { mutableStateOf(false) }
    var showPendingDialog by remember { mutableStateOf(false) }
    
    // Navigation guard to prevent multiple navigations
    var hasNavigated by remember { mutableStateOf(false) }
    
    /** Safe navigation helper that prevents multiple calls */
    fun safeNavigateBack() {
        if (hasNavigated) {
            Log.d(TAG, "⚠️ safeNavigateBack: Already navigated, ignoring")
            return
        }
        Log.d(TAG, "🚪 safeNavigateBack: Navigating back")
        hasNavigated = true
        viewModel.stopAll()
        onNavigateBack()
    }

    // Handle completion - show pending dialog for background analysis
    LaunchedEffect(uiState.isCompleted, uiState.isResultPending) {
        when {
            // Background analysis mode - show dialog, then navigate to home
            uiState.isCompleted && uiState.isResultPending -> {
                showPendingDialog = true
            }
            // Instant result mode (legacy) - navigate directly to result
            uiState.isCompleted && uiState.resultId != null && !uiState.isResultPending -> {
                if (!hasNavigated) {
                    hasNavigated = true
                    viewModel.stopAll()
                    onNavigateToResult(uiState.resultId!!)
                }
            }
        }
    }

    // Results Pending Dialog
    if (showPendingDialog) {
        AlertDialog(
            onDismissRequest = { /* Can't dismiss - must acknowledge */ },
            title = {
                Text(stringResource(R.string.interview_results_pending_title))
            },
            text = {
                Text(
                    text = stringResource(R.string.interview_results_pending_message),
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPendingDialog = false
                        if (!hasNavigated) {
                            hasNavigated = true
                            viewModel.stopAll()
                            onNavigateToHome()
                        }
                    }
                ) {
                    Text(stringResource(R.string.button_ok))
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        Log.d(TAG, "🚀 Screen initialized for session: $sessionId")
    }
    
    // Cleanup effect - ensure ViewModel is stopped when screen is disposed
    DisposableEffect(sessionId) {
        Log.d(TAG, "📌 DisposableEffect: Screen entered composition for session: $sessionId")
        onDispose {
            Log.d(TAG, "🧹 DisposableEffect: Screen being disposed for session: $sessionId")
            // Only stop if we haven't already navigated (to avoid double-stop)
            if (!hasNavigated) {
                Log.d(TAG, "🛑 DisposableEffect: Stopping ViewModel on dispose")
                viewModel.stopAll()
            } else {
                Log.d(TAG, "⏭️ DisposableEffect: Already navigated, skipping stopAll()")
            }
        }
    }

    // Exit Confirmation Dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { 
                Log.d(TAG, "❌ Exit dialog dismissed")
                showExitDialog = false 
            },
            title = {
                Text(stringResource(R.string.interview_exit_title))
            },
            text = {
                Text(
                    text = stringResource(R.string.interview_exit_message),
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        Log.d(TAG, "✅ Exit confirmed via dialog")
                        showExitDialog = false
                        safeNavigateBack()
                    }
                ) {
                    Text(stringResource(R.string.interview_exit_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        Log.d(TAG, "❌ Exit cancelled via dialog")
                        showExitDialog = false 
                    }
                ) {
                    Text(stringResource(R.string.button_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.interview_question_number, uiState.currentQuestionIndex + 1, uiState.totalQuestions))
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        Log.d(TAG, "⬅️ Back arrow pressed")
                        safeNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    // Mute/Unmute toggle button (always enabled for silent text-based interview)
                    IconButton(onClick = { viewModel.toggleTTSMute() }) {
                        Icon(
                            imageVector = if (uiState.isTTSMuted)
                                Icons.Default.VolumeOff
                            else
                                Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (uiState.isTTSMuted)
                                stringResource(R.string.cd_unmute_tts)
                            else
                                stringResource(R.string.cd_mute_tts),
                            tint = if (uiState.isTTSMuted)
                                Color.Red // Red when muted
                            else
                                MaterialTheme.colorScheme.primary
                        )
                    }

                    // Close button
                    IconButton(onClick = {
                        Log.d(TAG, "❌ Close (X) button pressed")
                        viewModel.stopAll() // Stop TTS and recording before showing dialog
                        showExitDialog = true
                    }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.cd_exit_interview),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> LoadingContent()
                uiState.error != null -> ErrorContent(uiState.error)
                uiState.currentQuestion != null -> InterviewContent(uiState = uiState, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun LoadingContent() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

@Composable
private fun ErrorContent(error: String?) = Column(
    Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
) { Text(error ?: stringResource(R.string.interview_error_generic), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error) }

/**
 * Main interview content - Question + Response TextField + Submit
 * 
 * Simplified UX: Users type their response or use keyboard's voice input (mic button)
 */
@Composable
private fun InterviewContent(uiState: InterviewSessionUiState, viewModel: InterviewSessionViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Progress indicator
        LinearProgressIndicator(
            progress = { uiState.getProgressPercentage() / 100f },
            modifier = Modifier.fillMaxWidth()
        )

        // Question card
        QuestionCard(
            questionText = uiState.currentQuestion!!.questionText,
            isSpeaking = uiState.isTTSSpeaking,
            isMuted = uiState.isTTSMuted
        )

        // Response input card (TextField with keyboard voice support)
        ResponseInputCard(
            responseText = uiState.responseText,
            onResponseChange = viewModel::updateResponseText,
            enabled = uiState.canEditResponse()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Submit button
        SubmitButton(
            isSubmitting = uiState.isSubmittingResponse,
            canSubmit = uiState.canSubmitResponse(),
            hasMoreQuestions = uiState.hasMoreQuestions(),
            onSubmit = viewModel::submitResponse
        )
    }
}

/**
 * Question card showing the interviewer's question
 */
@Composable
private fun QuestionCard(questionText: String, isSpeaking: Boolean, isMuted: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.interview_question_label),
                    style = MaterialTheme.typography.labelMedium
                )
                // Show muted indicator OR speaking indicator
                if (isMuted) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.VolumeOff,
                            contentDescription = stringResource(R.string.cd_unmute_tts),
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.interview_audio_muted),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (isSpeaking) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = stringResource(R.string.cd_interviewer_speaking),
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.interview_interviewer_speaking),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Text(text = questionText, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/**
 * Response input card with TextField
 * 
 * Users can:
 * - Type their response directly
 * - Use the keyboard's microphone button for voice-to-text
 *   (native Android feature - no custom recording needed)
 */
@Composable
private fun ResponseInputCard(
    responseText: String,
    onResponseChange: (String) -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.interview_response_hint),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = responseText,
                onValueChange = onResponseChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp, max = 300.dp),
                enabled = enabled,
                placeholder = { 
                    Text(stringResource(R.string.interview_response_placeholder)) 
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Default
                ),
                maxLines = 10,
                supportingText = {
                    Text(
                        text = stringResource(R.string.interview_char_count, responseText.length),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            )
        }
    }
}

/**
 * Submit button - submits the current response
 */
@Composable
private fun SubmitButton(
    isSubmitting: Boolean,
    canSubmit: Boolean,
    hasMoreQuestions: Boolean,
    onSubmit: () -> Unit
) {
    Button(
        onClick = onSubmit,
        enabled = canSubmit,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isSubmitting) {
            CircularProgressIndicator(
                Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(
                stringResource(
                    if (hasMoreQuestions) R.string.interview_button_submit 
                    else R.string.interview_button_complete
                )
            )
        }
    }
}
