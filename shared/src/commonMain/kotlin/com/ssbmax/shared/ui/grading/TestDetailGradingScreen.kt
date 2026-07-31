package com.ssbmax.shared.ui.grading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.shared.presentation.grading.GradingUiState
import com.ssbmax.shared.presentation.grading.TestDetailGradingViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.grading_back
import ssbmax.shared.generated.resources.grading_submission_not_found
import ssbmax.shared.generated.resources.grading_submit_notify
import ssbmax.shared.generated.resources.grading_title
import ssbmax.shared.generated.resources.success_grading_submitted

/**
 * KMP port of the Android `app/.../ui/grading/TestDetailGradingScreen.kt`
 * (Assessor grading screen). `submissionId` passed directly (no
 * `SavedStateHandle`), matching this session's `SubmissionDetailScreen`
 * precedent.
 *
 * Info/content/grade-input/remarks-input/loading/error composables live in
 * `TestDetailGradingComponents.kt` -- a purely structural split to stay under
 * this repo's 300-line Quality Limit (see that file's own doc comment).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestDetailGradingScreen(
    submissionId: String,
    onNavigateBack: () -> Unit,
    viewModel: TestDetailGradingViewModel = koinViewModel()
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(submissionId) {
        viewModel.loadSubmission(submissionId)
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    val gradingSubmittedMessage = stringResource(Res.string.success_grading_submitted)

    LaunchedEffect(uiState.gradingSubmitted) {
        if (uiState.gradingSubmitted) {
            snackbarHostState.showSnackbar(gradingSubmittedMessage)
            viewModel.resetSubmittedState()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.grading_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.grading_back))
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
                    message = stringResource(Res.string.grading_submission_not_found),
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
        SubmissionInfoCard(
            testType = submission.testType.displayName,
            studentName = uiState.studentName,
            submittedAt = submission.submittedAt
        )

        SubmissionContentCard(submission = submission)

        GradeInputSection(
            grade = uiState.grade,
            onGradeChange = onGradeChange
        )

        RemarksInputSection(
            remarks = uiState.remarks,
            onRemarksChange = onRemarksChange
        )

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
                Text(stringResource(Res.string.grading_submit_notify))
            }
        }
    }
}
