package com.ssbmax.ui.tests.gto.lecturette

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.core.domain.model.gto.GTOResult
import com.ssbmax.core.domain.model.gto.GTOSubmission
import com.ssbmax.ui.tests.gto.common.*
import com.ssbmax.ui.tests.gto.lecturette.*

@Composable
fun LecturetteResultContent(
    submission: GTOSubmission.LecturetteSubmission,
    result: GTOResult?,
    isAnalyzing: Boolean,
    isFailed: Boolean,
    formattedTimeSpent: String,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            LecturetteSubmissionCard(
                selectedTopic = submission.selectedTopic,
                allTopics = submission.topicChoices,
                charCount = submission.charCount,
                timeSpent = formattedTimeSpent
            )
        }
        
        item {
            when {
                isAnalyzing -> AnalyzingCard("Analyzing Your Speech...")
                isFailed -> AnalysisFailedCard()
            }
        }
        
        if (result != null) {
            item {
                OverallScoreCard(
                    overallScore = result.overallScore,
                    overallRating = result.overallRating,
                    aiConfidence = result.aiConfidence
                )
            }
            
            item {
                Text(
                    text = "Top Strengths",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            items(result.topOLQs) { (olq, score) ->
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
            
            items(result.improvementOLQs) { (olq, score) ->
                OLQScoreCard(olq = olq, score = score, isStrength = false)
            }
            
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Your Speech",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                SpeechPreviewCard(
                    selectedTopic = submission.selectedTopic,
                    speechTranscript = submission.speechTranscript,
                    charCount = submission.charCount
                )
            }
        }
        
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
