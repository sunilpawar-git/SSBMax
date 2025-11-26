package com.ssbmax.ui.interview.start

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.R
import com.ssbmax.core.domain.model.interview.InterviewMode
import com.ssbmax.core.domain.model.interview.InterviewResult
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Start Interview Screen - Mode selection, prerequisites check, session creation, and history */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartInterviewScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSession: (String) -> Unit,
    onNavigateToResult: (String) -> Unit,
    viewModel: StartInterviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var consentGiven by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSessionCreated) {
        if (uiState.isSessionCreated && uiState.sessionId != null) {
            onNavigateToSession(uiState.sessionId!!)
        }
    }

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
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isGeneratingQuestions -> GeneratingQuestionsContent(uiState.loadingMessage)
                uiState.isLoading -> LoadingContent(uiState.loadingMessage)
                uiState.error != null -> ErrorContent(uiState.error, onRetry = viewModel::checkEligibility)
                else -> MainContentWithHistory(
                    uiState = uiState,
                    consentGiven = consentGiven,
                    onConsentChange = { consentGiven = it },
                    onModeSelect = viewModel::selectMode,
                    onStartInterview = { viewModel.createSession(consentGiven) },
                    onResultClick = onNavigateToResult
                )
            }
        }
    }
}

@Composable
private fun GeneratingQuestionsContent(loadingMessage: String?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(24.dp))
        Text(
            text = loadingMessage ?: stringResource(R.string.interview_generating_questions),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.interview_generating_questions_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LoadingContent(message: String?) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        if (message != null) {
            Spacer(Modifier.height(16.dp))
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ErrorContent(error: String?, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = error ?: stringResource(R.string.interview_error_generic),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
    }
}

@Composable
private fun MainContentWithHistory(
    uiState: StartInterviewUiState,
    consentGiven: Boolean,
    onConsentChange: (Boolean) -> Unit,
    onModeSelect: (InterviewMode) -> Unit,
    onStartInterview: () -> Unit,
    onResultClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Past Interview Results Section
        if (uiState.hasPastInterviews() || uiState.isLoadingHistory) {
            item {
                InterviewHistorySection(
                    results = uiState.pastResults,
                    isLoading = uiState.isLoadingHistory,
                    onResultClick = onResultClick
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
        }

        // New Interview Section Header
        item {
            Text(
                text = stringResource(R.string.interview_new_interview_title),
                style = MaterialTheme.typography.titleLarge
            )
        }

        // Mode selection
        item {
            Text(text = stringResource(R.string.interview_select_mode), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = uiState.selectedMode == InterviewMode.TEXT_BASED,
                    onClick = { onModeSelect(InterviewMode.TEXT_BASED) },
                    label = { Text(stringResource(R.string.interview_mode_text)) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = uiState.selectedMode == InterviewMode.VOICE_BASED,
                    onClick = { /* Disabled until voice UI implemented */ },
                    label = { Text(stringResource(R.string.interview_mode_voice)) },
                    enabled = false,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Prerequisites status
        if (uiState.prerequisiteResult != null) {
            item {
                PrerequisitesCard(isEligible = uiState.isEligible, failureReasons = uiState.getFailureReasons())
            }
        }

        // Consent checkbox
        if (uiState.isEligible) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = consentGiven, onCheckedChange = onConsentChange)
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.interview_consent_message), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Start button
        if (uiState.isEligible) {
            item {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onStartInterview,
                    enabled = consentGiven && !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.interview_button_start))
                }
            }
        }
    }
}

@Composable
private fun InterviewHistorySection(
    results: List<InterviewResult>,
    isLoading: Boolean,
    onResultClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.interview_history_title),
            style = MaterialTheme.typography.titleLarge
        )

        if (isLoading) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(text = stringResource(R.string.interview_loading_history))
            }
        } else if (results.isEmpty()) {
            Text(
                text = stringResource(R.string.interview_no_past_results),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            results.take(5).forEach { result ->
                InterviewResultCard(result = result, onClick = { onResultClick(result.id) })
            }

            if (results.size > 5) {
                Text(
                    text = stringResource(R.string.interview_more_results, results.size - 5),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun InterviewResultCard(
    result: InterviewResult,
    onClick: () -> Unit
) {
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
            .withZone(ZoneId.systemDefault())
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = dateFormatter.format(result.completedAt),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(
                            R.string.interview_result_summary,
                            result.getAverageOLQScore(),
                            result.getDurationMinutes()
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Score badge
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${result.overallRating}",
                    style = MaterialTheme.typography.titleLarge,
                    color = getScoreColor(result.overallRating)
                )
                Text(
                    text = "/10",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun getScoreColor(score: Int) = when {
    score <= 5 -> MaterialTheme.colorScheme.primary      // Good (SSB: lower is better)
    score <= 7 -> MaterialTheme.colorScheme.tertiary     // Average
    else -> MaterialTheme.colorScheme.error               // Poor
}

@Composable
private fun PrerequisitesCard(isEligible: Boolean, failureReasons: List<String>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = stringResource(R.string.interview_prerequisites_title), style = MaterialTheme.typography.titleMedium)
            if (isEligible) {
                Text(
                    text = "✓ ${stringResource(R.string.interview_prerequisites_met)}",
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                failureReasons.forEach { reason ->
                    Text(text = "• $reason", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
