package com.ssbmax.ui.interview.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.R

/**
 * Voice Interview Session Screen
 *
 * Manages the voice interview flow:
 * - Displays questions one by one
 * - Records audio responses
 * - Allows transcription review and editing
 * - Submits to AI for analysis
 * - Progresses through all questions
 * - Completes interview and navigates to results
 */
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

    // Handle RECORD_AUDIO permission
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.updateRecordPermission(granted)
    }

    // Check permission status on launch
    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.updateRecordPermission(hasPermission)
    }

    // Navigate to result when completed
    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted && uiState.resultId != null) {
            onNavigateToResult(uiState.resultId!!)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(
                        R.string.interview_question_number,
                        uiState.currentQuestionIndex + 1,
                        uiState.totalQuestions
                    ))
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = uiState.error ?: stringResource(R.string.interview_error_generic),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                !uiState.hasRecordPermission -> {
                    // Permission not granted
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Spacer(modifier = Modifier.weight(1f))

                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = stringResource(R.string.cd_microphone),
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.voice_interview_permission_required),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                            Text(stringResource(R.string.voice_interview_permission_button))
                        }

                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                uiState.currentQuestion != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Progress indicator
                        LinearProgressIndicator(
                            progress = { uiState.getProgressPercentage() / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Question card
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Question",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = uiState.currentQuestion!!.questionText,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }

                        // Recording controls
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (uiState.recordingState == RecordingState.RECORDING) {
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
                                // Status text
                                Text(
                                    text = when (uiState.recordingState) {
                                        RecordingState.IDLE -> stringResource(R.string.voice_interview_record_hint)
                                        RecordingState.RECORDING -> stringResource(R.string.voice_interview_recording)
                                        RecordingState.RECORDED -> stringResource(R.string.voice_interview_recorded)
                                        RecordingState.PLAYING -> "Playing..."
                                    },
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                // Duration display
                                if (uiState.audioDurationMs > 0) {
                                    Text(
                                        text = stringResource(
                                            R.string.voice_interview_duration,
                                            uiState.getFormattedDuration()
                                        ),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }

                                // Recording button row
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    when (uiState.recordingState) {
                                        RecordingState.IDLE -> {
                                            // Start recording button
                                            FilledTonalButton(
                                                onClick = { viewModel.startRecording() },
                                                enabled = uiState.canStartRecording()
                                            ) {
                                                Icon(
                                                    Icons.Default.Mic,
                                                    contentDescription = stringResource(R.string.cd_microphone),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(stringResource(R.string.voice_interview_button_start_recording))
                                            }
                                        }
                                        RecordingState.RECORDING -> {
                                            // Stop recording button
                                            FilledTonalButton(
                                                onClick = { viewModel.stopRecording() },
                                                colors = ButtonDefaults.filledTonalButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.error
                                                )
                                            ) {
                                                Icon(
                                                    Icons.Default.Stop,
                                                    contentDescription = stringResource(R.string.cd_stop),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(stringResource(R.string.voice_interview_button_stop_recording))
                                            }
                                        }
                                        RecordingState.RECORDED, RecordingState.PLAYING -> {
                                            // Play button (note: playback not implemented in MVP)
                                            OutlinedButton(
                                                onClick = { /* TODO: Phase 6 - implement playback */ },
                                                enabled = uiState.canPlayAudio()
                                            ) {
                                                Icon(
                                                    Icons.Default.PlayArrow,
                                                    contentDescription = stringResource(R.string.cd_play),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(stringResource(R.string.voice_interview_button_play))
                                            }

                                            // Re-record button
                                            OutlinedButton(
                                                onClick = { viewModel.cancelRecording() },
                                                enabled = uiState.canReRecord()
                                            ) {
                                                Text(stringResource(R.string.voice_interview_button_re_record))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Transcription editor
                        if (uiState.recordingState == RecordingState.RECORDED) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.voice_interview_transcription_label),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                OutlinedTextField(
                                    value = uiState.transcriptionText,
                                    onValueChange = { viewModel.updateTranscription(it) },
                                    placeholder = { Text(stringResource(R.string.voice_interview_transcription_hint)) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    minLines = 6
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Submit button
                        Button(
                            onClick = { viewModel.submitResponse() },
                            enabled = uiState.canSubmitResponse(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (uiState.isSubmittingResponse) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(
                                    if (uiState.hasMoreQuestions()) {
                                        stringResource(R.string.interview_button_submit)
                                    } else {
                                        stringResource(R.string.interview_button_complete)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
