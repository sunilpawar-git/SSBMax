package com.ssbmax.shared.ui.submissions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.shared.domain.model.SubmissionStatus
import com.ssbmax.shared.presentation.submissions.SubmissionDetailUiState
import com.ssbmax.shared.presentation.submissions.SubmissionDetailViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.submission_detail_action_back_home
import ssbmax.shared.generated.resources.submission_detail_label_status
import ssbmax.shared.generated.resources.submission_detail_label_submitted
import ssbmax.shared.generated.resources.submission_detail_title

/**
 * KMP port of the Android `app/.../ui/submissions/SubmissionDetailScreen.kt`.
 * `submissionId` is passed directly (no `SavedStateHandle`), matching this
 * migration's [com.ssbmax.shared.ui.oir.OIRTestResultScreen] precedent --
 * see [SubmissionDetailViewModel]'s own doc comment.
 *
 * Score/feedback/pending/info/loading/error composables live in
 * `SubmissionDetailComponents.kt` -- a purely structural split to stay under
 * this repo's 300-line Quality Limit (see that file's own doc comment).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmissionDetailScreen(
    submissionId: String,
    onNavigateHome: () -> Unit = {},
    viewModel: SubmissionDetailViewModel = koinInject(),
    modifier: Modifier = Modifier
) {
    DisposableEffect(Unit) {
        onDispose { viewModel.close() }
    }

    LaunchedEffect(submissionId) {
        viewModel.loadSubmission(submissionId)
    }

    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.submission_detail_title)) }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                LoadingContent(Modifier.padding(padding))
            }
            uiState.error != null -> {
                ErrorContent(
                    error = uiState.error!!,
                    onRetry = { viewModel.retry() },
                    modifier = Modifier.padding(padding)
                )
            }
            else -> {
                DetailContent(
                    uiState = uiState,
                    onNavigateHome = onNavigateHome,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun DetailContent(
    uiState: SubmissionDetailUiState,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { HeaderCard(uiState) }

        if (uiState.hasScore) {
            item {
                ScoreCard(
                    aiScore = uiState.aiScore,
                    instructorScore = uiState.instructorScore
                )
            }
        }

        if (uiState.instructorScore != null || uiState.aiScore != null) {
            item {
                FeedbackCard(
                    score = uiState.instructorScore ?: uiState.aiScore!!
                )
            }
        }

        if (uiState.status == SubmissionStatus.SUBMITTED_PENDING_REVIEW) {
            item { PendingMessage() }
        }

        item {
            Button(
                onClick = onNavigateHome,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(Res.string.submission_detail_action_back_home))
            }
        }
    }
}

@Composable
private fun HeaderCard(uiState: SubmissionDetailUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = uiState.testName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                InfoItem(
                    icon = Icons.Default.Schedule,
                    label = stringResource(Res.string.submission_detail_label_submitted),
                    value = uiState.timeAgo
                )
                InfoItem(
                    icon = Icons.Default.Info,
                    label = stringResource(Res.string.submission_detail_label_status),
                    value = uiState.status.displayName
                )
            }
        }
    }
}
