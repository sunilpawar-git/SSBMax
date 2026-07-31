package com.ssbmax.shared.ui.ppdt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.shared.presentation.ppdtresult.PPDTSubmissionResultUiState
import com.ssbmax.shared.presentation.ppdtresult.PPDTSubmissionResultViewModel
import com.ssbmax.shared.ui.components.result.SubmissionConfirmationCard
import com.ssbmax.shared.ui.components.result.UnifiedOLQResultTemplate
import com.ssbmax.shared.ui.ppdt.components.PPDTOLQReasoningCard
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.result_ppdt_character_count_label
import ssbmax.shared.generated.resources.result_ppdt_characters_written
import ssbmax.shared.generated.resources.result_ppdt_story_submitted_title
import ssbmax.shared.generated.resources.result_ppdt_title
import ssbmax.shared.generated.resources.result_ppdt_writing_time_label
import ssbmax.shared.generated.resources.result_ppdt_writing_time_value
import ssbmax.shared.generated.resources.result_ppdt_your_story_title

/**
 * KMP port of `app/.../ui/tests/ppdt/PPDTSubmissionResultScreen.kt`. Uses
 * [UnifiedOLQResultTemplate] (this session's shared port, see that file's
 * doc comment) exactly the way the Android original does.
 *
 * Uses `koinViewModel<PPDTSubmissionResultViewModel>()`; `viewModelScope` is
 * cancelled automatically on leaving the screen, cancelling the still-open
 * Firestore listener if the user navigates away before `analysisStatus`
 * reaches `COMPLETED`.
 */
@Composable
fun PPDTSubmissionResultScreen(
    submissionId: String,
    onNavigateHome: () -> Unit = {},
    viewModel: PPDTSubmissionResultViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(submissionId) {
        viewModel.loadSubmission(submissionId)
    }

    UnifiedOLQResultTemplate(
        uiState = uiState,
        testTitle = stringResource(Res.string.result_ppdt_title),
        submissionConfirmationContent = { state -> PPDTSubmissionCard(state) },
        testSpecificContent = { state ->
            PPDTStoryPreview(state)
            state.olqResult?.let { result -> PPDTOLQReasoningCard(olqResult = result) }
        },
        submissionStatus = uiState.submission?.status,
        onNavigateHome = onNavigateHome,
        modifier = modifier
    )
}

@Composable
private fun PPDTSubmissionCard(uiState: PPDTSubmissionResultUiState) {
    val submission = uiState.submission ?: return
    SubmissionConfirmationCard(
        title = stringResource(Res.string.result_ppdt_story_submitted_title),
        subtitle = stringResource(Res.string.result_ppdt_characters_written, submission.charactersCount)
    )
}

@Composable
private fun PPDTStoryPreview(uiState: PPDTSubmissionResultUiState) {
    val submission = uiState.submission ?: return
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(Res.string.result_ppdt_your_story_title),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider()
            Text(text = submission.story, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 8.dp))
            HorizontalDivider()
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = stringResource(Res.string.result_ppdt_character_count_label), style = MaterialTheme.typography.bodySmall)
                Text(text = "${submission.charactersCount}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = stringResource(Res.string.result_ppdt_writing_time_label), style = MaterialTheme.typography.bodySmall)
                Text(
                    text = stringResource(Res.string.result_ppdt_writing_time_value, submission.writingTimeTakenMinutes),
                    style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
