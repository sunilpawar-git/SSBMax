package com.ssbmax.ui.grading

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Test Detail Grading Screen for Assessors
 * Allows assessors to grade student submissions and send feedback via FCM notifications
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestDetailGradingScreen(
    submissionId: String,
    onNavigateBack: () -> Unit,
    viewModel: TestDetailGradingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Load submission on first composition
    LaunchedEffect(submissionId) {
        viewModel.loadSubmission(submissionId)
    }

    // Show error snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    // Navigate back on successful submission
    LaunchedEffect(uiState.gradingSubmitted) {
        if (uiState.gradingSubmitted) {
            snackbarHostState.showSnackbar("Grading submitted! Student notified.")
            viewModel.resetSubmittedState()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grade Submission") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                LoadingState(modifier = Modifier.padding(innerPadding))
            }
            uiState.submission != null -> {
                GradingContent(
                    uiState = uiState,
                    onGradeChange = viewModel::updateGrade,
                    onRemarksChange = viewModel::updateRemarks,
                    onSubmit = viewModel::submitGrading,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            else -> {
                ErrorState(
                    message = "Submission not found",
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun GradingContent(
    uiState: GradingUiState,
    onGradeChange: (Float) -> Unit,
    onRemarksChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val submission = uiState.submission ?: return

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Submission Info Card
        SubmissionInfoCard(
            testType = submission.testType.displayName,
            studentName = "Student", // TODO: Load from user repository
            submittedAt = submission.submittedAt
        )

        // Submission Content Card
        SubmissionContentCard(submission = submission)

        // Grade Input Section
        GradeInputSection(
            grade = uiState.grade,
            onGradeChange = onGradeChange
        )

        // Remarks Input Section
        RemarksInputSection(
            remarks = uiState.remarks,
            onRemarksChange = onRemarksChange
        )

        // Submit Button
        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isSubmitting,
            shape = RoundedCornerShape(8.dp)
        ) {
            if (uiState.isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Submit Grading & Notify Student")
            }
        }
    }
}

@Composable
private fun SubmissionInfoCard(
    testType: String,
    studentName: String,
    submittedAt: Long,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = testType,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Student: $studentName",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = formatDate(submittedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SubmissionContentCard(
    submission: com.ssbmax.core.domain.model.TestSubmission,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Submission Content",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Time Spent: ${submission.timeSpent / 1000 / 60} minutes",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Responses: ${submission.responses.size} questions",
                style = MaterialTheme.typography.bodyMedium
            )
            // TODO: Display actual submission content based on test type
            // For TAT: Show images and stories
            // For WAT/SRT: Show question-answer pairs
        }
    }
}

@Composable
private fun GradeInputSection(
    grade: Float,
    onGradeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Grade",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${grade.toInt()}/100",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = grade,
                onValueChange = onGradeChange,
                valueRange = 0f..100f,
                steps = 99,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun RemarksInputSection(
    remarks: String,
    onRemarksChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Remarks & Feedback",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = remarks,
                onValueChange = onRemarksChange,
                placeholder = { Text("Enter detailed feedback for the student...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                maxLines = 8
            )
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Grade,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
