package com.ssbmax.ui.instructor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.ui.submissions.SubmissionDetailScreen

/**
 * Grading Detail Screen for Instructors
 * Shows submission with grading interface
 * 
 * For now, we reuse SubmissionDetailScreen and add grading controls
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradingDetailScreen(
    submissionId: String,
    onNavigateBack: () -> Unit = {},
    onSubmitGrade: (Float, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var showGradingDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grade Submission") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = { showGradingDialog = true },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Grade, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Grade")
                    }
                }
            )
        }
    ) { padding ->
        // Show submission details
        SubmissionDetailScreen(
            submissionId = submissionId,
            onNavigateHome = { /* Already handled by topBar */ },
            modifier = Modifier.padding(padding)
        )
        
        // Grading Dialog
        if (showGradingDialog) {
            GradingDialog(
                onDismiss = { showGradingDialog = false },
                onSubmit = { score, feedback ->
                    onSubmitGrade(score, feedback)
                    showGradingDialog = false
                }
            )
        }
    }
}

@Composable
private fun GradingDialog(
    onDismiss: () -> Unit,
    onSubmit: (Float, String) -> Unit
) {
    var score by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Grade Submission") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Score Input
                OutlinedTextField(
                    value = score,
                    onValueChange = { 
                        if (it.isEmpty() || it.toFloatOrNull() != null) {
                            score = it
                        }
                    },
                    label = { Text("Score (0-100)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Feedback Input
                OutlinedTextField(
                    value = feedback,
                    onValueChange = { feedback = it },
                    label = { Text("Feedback") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val scoreValue = score.toFloatOrNull()
                    if (scoreValue != null && scoreValue in 0f..100f && feedback.isNotBlank()) {
                        onSubmit(scoreValue, feedback)
                    }
                },
                enabled = score.toFloatOrNull()?.let { it in 0f..100f } == true && feedback.isNotBlank()
            ) {
                Text("Submit Grade")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Instructor Grading Form - Simplified version
 * In a full implementation, this would have detailed scoring for each test type
 */
@Composable
fun InstructorGradingForm(
    onSubmitGrade: (Float, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var overallScore by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("") }
    
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Instructor Grading",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            OutlinedTextField(
                value = overallScore,
                onValueChange = { 
                    if (it.isEmpty() || it.toFloatOrNull() != null) {
                        overallScore = it
                    }
                },
                label = { Text("Overall Score (0-100)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = feedback,
                onValueChange = { feedback = it },
                label = { Text("Detailed Feedback") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                maxLines = 8
            )
            
            Button(
                onClick = {
                    val score = overallScore.toFloatOrNull()
                    if (score != null && score in 0f..100f && feedback.isNotBlank()) {
                        onSubmitGrade(score, feedback)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = overallScore.toFloatOrNull()?.let { it in 0f..100f } == true && feedback.isNotBlank()
            ) {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Submit Grade")
            }
        }
    }
}

