package com.ssbmax.ui.tests.sdt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SDTSubmissionResultScreen(
    submissionId: String,
    onNavigateHome: () -> Unit = {},
    viewModel: SDTSubmissionResultViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(submissionId) {
        viewModel.loadSubmission(submissionId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sdt_result_title)) }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                }
            }
            uiState.submission != null -> {
                ResultContent(
                    submission = uiState.submission!!, 
                    onNavigateHome = onNavigateHome,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun ResultContent(
    submission: com.ssbmax.core.domain.model.SDTSubmission, 
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.sdt_result_summary_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.sdt_result_completed, submission.totalResponses))
                    Text(stringResource(R.string.sdt_result_time_taken, submission.totalTimeTakenMinutes))
                    Text(stringResource(R.string.sdt_result_valid_responses, submission.validResponses))
                }
            }
        }

        submission.aiPreliminaryScore?.let { aiScore ->
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.sdt_result_ai_score_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.sdt_result_overall_score, aiScore.overallScore.toInt()), style = MaterialTheme.typography.titleLarge)
                        Divider()
                        ScoreRow(stringResource(R.string.sdt_result_self_awareness), aiScore.selfAwarenessScore)
                        ScoreRow(stringResource(R.string.sdt_result_emotional_maturity), aiScore.emotionalMaturityScore)
                        ScoreRow(stringResource(R.string.sdt_result_social_perception), aiScore.socialPerceptionScore)
                        ScoreRow(stringResource(R.string.sdt_result_introspection), aiScore.introspectionScore)
                        aiScore.feedback?.let {
                            Divider()
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            if (aiScore.strengths.isNotEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.sdt_result_strengths_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            aiScore.strengths.forEach { strength ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(stringResource(R.string.sdt_result_bullet))
                                    Text(strength)
                                }
                            }
                        }
                    }
                }
            }

            if (aiScore.areasForImprovement.isNotEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.sdt_result_improvement_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            aiScore.areasForImprovement.forEach { area ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(stringResource(R.string.sdt_result_bullet))
                                    Text(area)
                                }
                            }
                        }
                    }
                }
            }
        }

        items(submission.responses) { response ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(response.question, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (!response.isSkipped) {
                        Text(response.answer, style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(R.string.sdt_result_word_count, response.wordCount), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                    } else {
                        Text(stringResource(R.string.sdt_result_skipped), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        
        // Actions - Back to Home button
        item {
            Button(
                onClick = onNavigateHome,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.sdt_result_action_home))
            }
        }
    }
}

@Composable
private fun ScoreRow(label: String, score: Float) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(stringResource(R.string.sdt_result_score_format, score.toInt()), fontWeight = FontWeight.Bold)
    }
}

