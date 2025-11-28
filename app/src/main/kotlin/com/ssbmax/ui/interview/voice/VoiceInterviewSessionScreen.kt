package com.ssbmax.ui.interview.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.R

/** Voice Interview Session Screen - manages voice interview flow with recording/transcription */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceInterviewSessionScreen(
    sessionId: String,
    onNavigateBack: () -> Unit,
    onNavigateToResult: (String) -> Unit,
    viewModel: VoiceInterviewSessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showExitDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.updateRecordPermission(granted)
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.updateRecordPermission(hasPermission)
    }

    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted && uiState.resultId != null) {
            onNavigateToResult(uiState.resultId!!)
        }
    }

    // Exit Confirmation Dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
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
                        showExitDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text(stringResource(R.string.interview_exit_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExitDialog = false }
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
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showExitDialog = true }) {
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
                !uiState.hasRecordPermission -> PermissionContent(onRequest = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) })
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

@Composable
private fun PermissionContent(onRequest: () -> Unit) = Column(
    Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)
) {
    Spacer(Modifier.weight(1f))
    Icon(Icons.Default.Mic, stringResource(R.string.cd_microphone), Modifier.size(64.dp), MaterialTheme.colorScheme.primary)
    Text(stringResource(R.string.voice_interview_permission_required), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    Button(onClick = onRequest) { Text(stringResource(R.string.voice_interview_permission_button)) }
    Spacer(Modifier.weight(1f))
}

@Composable
private fun InterviewContent(uiState: VoiceInterviewSessionUiState, viewModel: VoiceInterviewSessionViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LinearProgressIndicator(
            progress = { uiState.getProgressPercentage() / 100f },
            modifier = Modifier.fillMaxWidth()
        )

        QuestionCard(questionText = uiState.currentQuestion!!.questionText)

        RecordingControlsCard(
            recordingState = uiState.recordingState,
            audioDurationMs = uiState.audioDurationMs,
            formattedDuration = uiState.getFormattedDuration(),
            canStart = uiState.canStartRecording(),
            canPlay = uiState.canPlayAudio(),
            canReRecord = uiState.canReRecord(),
            onStart = viewModel::startRecording,
            onStop = viewModel::stopRecording,
            onCancel = viewModel::cancelRecording
        )

        if (uiState.recordingState == RecordingState.RECORDING || uiState.recordingState == RecordingState.RECORDED) {
            LiveTranscriptionCard(
                state = uiState.transcriptionState,
                liveTranscription = uiState.liveTranscription,
                finalTranscription = uiState.finalTranscription,
                transcriptionError = uiState.transcriptionError,
                onEdit = viewModel::updateTranscription,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        SubmitButton(
            isSubmitting = uiState.isSubmittingResponse,
            canSubmit = uiState.canSubmitResponse(),
            hasMoreQuestions = uiState.hasMoreQuestions(),
            onSubmit = viewModel::submitResponse
        )
    }
}

@Composable
private fun QuestionCard(questionText: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = stringResource(R.string.voice_interview_question_label), style = MaterialTheme.typography.labelMedium)
            Text(text = questionText, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun RecordingControlsCard(
    recordingState: RecordingState,
    audioDurationMs: Long,
    formattedDuration: String,
    canStart: Boolean,
    canPlay: Boolean,
    canReRecord: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (recordingState == RecordingState.RECORDING) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = when (recordingState) {
                    RecordingState.IDLE -> stringResource(R.string.voice_interview_record_hint)
                    RecordingState.RECORDING -> stringResource(R.string.voice_interview_recording)
                    RecordingState.RECORDED -> stringResource(R.string.voice_interview_recorded)
                    RecordingState.PLAYING -> stringResource(R.string.voice_interview_playing)
                },
                style = MaterialTheme.typography.bodyMedium
            )

            if (audioDurationMs > 0) {
                Text(
                    text = stringResource(R.string.voice_interview_duration, formattedDuration),
                    style = MaterialTheme.typography.labelSmall
                )
            }

            RecordingButtons(
                recordingState = recordingState,
                canStart = canStart,
                canPlay = canPlay,
                canReRecord = canReRecord,
                onStart = onStart,
                onStop = onStop,
                onCancel = onCancel
            )
        }
    }
}

@Composable
private fun RecordingButtons(
    recordingState: RecordingState,
    canStart: Boolean,
    canPlay: Boolean,
    canReRecord: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onCancel: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        when (recordingState) {
            RecordingState.IDLE -> {
                FilledTonalButton(onClick = onStart, enabled = canStart) {
                    Icon(Icons.Default.Mic, stringResource(R.string.cd_microphone), Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.voice_interview_button_start_recording))
                }
            }
            RecordingState.RECORDING -> {
                FilledTonalButton(
                    onClick = onStop,
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Stop, stringResource(R.string.cd_stop), Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.voice_interview_button_stop_recording))
                }
            }
            RecordingState.RECORDED, RecordingState.PLAYING -> {
                OutlinedButton(onClick = { /* TODO: Playback */ }, enabled = canPlay) {
                    Icon(Icons.Default.PlayArrow, stringResource(R.string.cd_play), Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.voice_interview_button_play))
                }
                OutlinedButton(onClick = onCancel, enabled = canReRecord) {
                    Text(stringResource(R.string.voice_interview_button_re_record))
                }
            }
        }
    }
}

@Composable
private fun SubmitButton(isSubmitting: Boolean, canSubmit: Boolean, hasMoreQuestions: Boolean, onSubmit: () -> Unit) = Button(onClick = onSubmit, enabled = canSubmit, modifier = Modifier.fillMaxWidth()) {
    if (isSubmitting) CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
    else Text(stringResource(if (hasMoreQuestions) R.string.interview_button_submit else R.string.interview_button_complete))
}
