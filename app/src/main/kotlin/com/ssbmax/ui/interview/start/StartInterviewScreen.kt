package com.ssbmax.ui.interview.start

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.R
import com.ssbmax.core.domain.model.interview.InterviewMode

/**
 * Start Interview Screen
 *
 * Allows user to:
 * - Select interview mode (text or voice)
 * - Check prerequisites
 * - Give consent for transcript storage
 * - Start interview session
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartInterviewScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSession: (String) -> Unit,
    viewModel: StartInterviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var consentGiven by remember { mutableStateOf(false) }

    // Navigate to session when created
    LaunchedEffect(uiState.isSessionCreated) {
        if (uiState.isSessionCreated && uiState.sessionId != null) {
            onNavigateToSession(uiState.sessionId!!)
        }
    }

    // Check eligibility on start
    LaunchedEffect(Unit) {
        viewModel.checkEligibility()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.interview_start_title)) },
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
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.checkEligibility() }) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Mode selection
                        Text(
                            text = stringResource(R.string.interview_select_mode),
                            style = MaterialTheme.typography.titleMedium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = uiState.selectedMode == InterviewMode.TEXT_BASED,
                                onClick = { viewModel.selectMode(InterviewMode.TEXT_BASED) },
                                label = { Text(stringResource(R.string.interview_mode_text)) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = uiState.selectedMode == InterviewMode.VOICE_BASED,
                                onClick = { /* Disabled until voice UI implemented */ },
                                label = { Text(stringResource(R.string.interview_mode_voice)) },
                                enabled = false,  // Disable voice mode until UI is implemented
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Prerequisites status
                        if (uiState.prerequisiteResult != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.interview_prerequisites_title),
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    if (uiState.isEligible) {
                                        Text(
                                            text = "✓ All prerequisites met",
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        uiState.getFailureReasons().forEach { reason ->
                                            Text(
                                                text = "• $reason",
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Consent checkbox
                        if (uiState.isEligible) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = consentGiven,
                                    onCheckedChange = { consentGiven = it }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.interview_consent_message),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Start button
                        if (uiState.isEligible) {
                            Button(
                                onClick = {
                                    viewModel.createSession(consentGiven = consentGiven)
                                },
                                enabled = consentGiven && !uiState.isLoading,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.interview_button_start))
                            }
                        }
                    }
                }
            }
        }
    }
}
