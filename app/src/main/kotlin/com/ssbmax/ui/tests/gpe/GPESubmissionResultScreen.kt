package com.ssbmax.ui.tests.gpe

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQCategory
import com.ssbmax.ui.tests.gto.common.*

/**
 * GPE Submission Result Screen
 * 
 * Displays:
 * - Submission confirmation
 * - Analysis status (real-time updates via Flow)
 * - Scenario and planning response preview
 * - 15 OLQ scores grouped by category
 * - Overall rating and score
 * - Top strengths and improvement areas
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GPESubmissionResultScreen(
    submissionId: String,
    onNavigateHome: () -> Unit,
    viewModel: GPESubmissionResultViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Load submission on mount
    LaunchedEffect(submissionId) {
        viewModel.loadSubmission(submissionId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GPE Test Results") },
                actions = {
                    IconButton(onClick = onNavigateHome) {
                        Icon(Icons.Default.Home, "Home")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading results...")
                    }
                }
            }
            
            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = uiState.error!!,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.retry(submissionId) }) {
                            Text("Retry")
                        }
                    }
                }
            }
            
            uiState.submission != null -> {
                LazyColumn(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        GPESubmissionConfirmationCard(
                            scenario = uiState.submission!!.scenario,
                            plan = uiState.submission!!.plan,
                            characterCount = uiState.submission!!.characterCount,
                            timeSpent = uiState.formattedTimeSpent,
                            status = uiState.submission!!.status
                        )
                    }
                    
                    item {
                        when {
                            uiState.isAnalyzing -> AnalyzingCard()
                            uiState.isFailed -> AnalysisFailedCard()
                            uiState.isCompleted && uiState.result != null -> {
                                // Show results - handled by items below
                            }
                        }
                    }
                    
                    if (uiState.isCompleted && uiState.result != null) {
                        item {
                            OverallScoreCard(
                                overallScore = uiState.result!!.overallScore,
                                overallRating = uiState.result!!.overallRating,
                                aiConfidence = uiState.result!!.aiConfidence
                            )
                        }
                        
                        item {
                            Text(
                                text = "Top Strengths",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        items(uiState.result!!.topOLQs) { (olq, score) ->
                            OLQScoreCard(olq = olq, score = score, isStrength = true)
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Areas for Improvement",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        items(uiState.result!!.improvementOLQs) { (olq, score) ->
                            OLQScoreCard(olq = olq, score = score, isStrength = false)
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Detailed OLQ Analysis",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        OLQCategory.entries.forEach { category ->
                            item {
                                Text(
                                    text = category.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                )
                            }
                            
                            val categoryOLQs = OLQ.getByCategory(category)
                            items(categoryOLQs) { olq ->
                                val score = uiState.result!!.olqScores[olq]
                                if (score != null) {
                                    OLQScoreCard(olq = olq, score = score, isStrength = null)
                                }
                            }
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Your Planning Response",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        item {
                            GPEResponsePreviewCard(
                                scenario = uiState.submission!!.scenario,
                                plan = uiState.submission!!.plan,
                                characterCount = uiState.submission!!.characterCount
                            )
                        }
                    }
                    
                    // Home button
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onNavigateHome,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Back to Home")
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun GPESubmissionConfirmationCard(
    scenario: String,
    plan: String,
    characterCount: Int,
    timeSpent: String,
    status: com.ssbmax.core.domain.model.gto.GTOSubmissionStatus
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        "Planning Exercise Submitted!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Status: ${status.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            HorizontalDivider()
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Character Count:", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "$characterCount",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Time Spent:", style = MaterialTheme.typography.bodyMedium)
                Text(
                    timeSpent,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun GPEResponsePreviewCard(
    scenario: String,
    plan: String,
    characterCount: Int
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Scenario",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                scenario,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            HorizontalDivider()
            
            Text(
                "Your Plan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                plan,
                style = MaterialTheme.typography.bodyMedium
            )
            
            Text(
                "$characterCount characters",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
