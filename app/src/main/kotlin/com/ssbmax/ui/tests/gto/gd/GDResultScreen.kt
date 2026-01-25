package com.ssbmax.ui.tests.gto.gd

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
import com.ssbmax.ui.components.SSBRecommendationBanner
import com.ssbmax.ui.tests.gto.common.*
import com.ssbmax.ui.tests.gto.gd.*

/**
 * Group Discussion Result Screen
 * 
 * Displays:
 * - Submission confirmation
 * - Analysis status (real-time updates via Flow)
 * - Topic and response preview
 * - 15 OLQ scores grouped by category
 * - Overall rating and score
 * - Top strengths and improvement areas
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GDResultScreen(
    submissionId: String,
    onNavigateHome: () -> Unit,
    viewModel: GDResultViewModel = hiltViewModel(),
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
                title = { Text("Group Discussion Results") },
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
                        SubmissionConfirmationCard(
                            topic = uiState.submission!!.topic,
                            charCount = uiState.submission!!.charCount,
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
                        // SSB Recommendation Banner (above Overall Performance)
                        uiState.ssbRecommendation?.let { recommendation ->
                            item {
                                SSBRecommendationBanner(model = recommendation)
                            }
                        }
                        
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
                                text = "Your Response",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        item {
                            ResponsePreviewCard(
                                topic = uiState.submission!!.topic,
                                response = uiState.submission!!.response,
                                charCount = uiState.submission!!.charCount
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

