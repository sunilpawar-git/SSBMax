package com.ssbmax.ui.instructor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.R
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
                title = { Text(stringResource(R.string.grading_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    Button(
                        onClick = { showGradingDialog = true },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Grade, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.grading_detail_action_grade))
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
        title = { Text(stringResource(R.string.grading_dialog_title)) },
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
                    label = { Text(stringResource(R.string.grading_dialog_score_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Feedback Input
                OutlinedTextField(
                    value = feedback,
                    onValueChange = { feedback = it },
                    label = { Text(stringResource(R.string.grading_dialog_feedback_label)) },
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
                Text(stringResource(R.string.grading_dialog_action_submit))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
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
                text = stringResource(R.string.grading_form_title),
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
                label = { Text(stringResource(R.string.grading_form_score_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = feedback,
                onValueChange = { feedback = it },
                label = { Text(stringResource(R.string.grading_form_feedback_label)) },
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
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.grading_form_action_submit))
            }
        }
    }

}
