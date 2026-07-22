package com.ssbmax.shared.ui.sdt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.shared.presentation.sdtresult.SDTSubmissionResultUiState
import com.ssbmax.shared.presentation.sdtresult.SDTSubmissionResultViewModel
import com.ssbmax.shared.ui.components.result.OLQResultContent
import com.ssbmax.shared.ui.components.result.SubmissionConfirmationCard
import com.ssbmax.shared.ui.components.result.UnifiedOLQResultTemplate
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.result_sdt_characters_label
import ssbmax.shared.generated.resources.result_sdt_title
import ssbmax.shared.generated.resources.sdt_result_completed
import ssbmax.shared.generated.resources.sdt_result_skipped
import ssbmax.shared.generated.resources.sdt_result_summary_title
import ssbmax.shared.generated.resources.sdt_result_time_taken
import ssbmax.shared.generated.resources.sdt_result_valid_responses

/**
 * KMP port of `app/.../ui/tests/sdt/SDTSubmissionResultScreen.kt`. Uses
 * [UnifiedOLQResultTemplate] and [OLQResultContent] exactly the way the
 * Android original does (SDT uses the generic score-breakdown card, same as
 * TAT/WAT/SRT), both already ported into `shared` during this phase's
 * earlier sessions, reused unchanged here.
 *
 * Uses `koinInject<SDTSubmissionResultViewModel>()` (not `koinViewModel()`);
 * a [DisposableEffect] calls [SDTSubmissionResultViewModel.close] on leaving
 * the screen, cancelling the still-open Firestore listener.
 */
@Composable
fun SDTSubmissionResultScreen(
    submissionId: String,
    onNavigateHome: () -> Unit = {},
    viewModel: SDTSubmissionResultViewModel = koinInject(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    DisposableEffect(Unit) {
        onDispose { viewModel.close() }
    }

    LaunchedEffect(submissionId) {
        viewModel.loadSubmission(submissionId)
    }

    UnifiedOLQResultTemplate(
        uiState = uiState,
        testTitle = stringResource(Res.string.result_sdt_title),
        submissionConfirmationContent = { state -> SDTSubmissionCard(state) },
        testSpecificContent = { state ->
            SDTResponsesList(state)
            state.olqResult?.let { OLQResultContent(olqResult = it) }
        },
        submissionStatus = uiState.submission?.status,
        onNavigateHome = onNavigateHome,
        modifier = modifier
    )
}

@Composable
private fun SDTSubmissionCard(uiState: SDTSubmissionResultUiState) {
    val submission = uiState.submission ?: return
    SubmissionConfirmationCard(
        subtitle = stringResource(Res.string.sdt_result_completed, submission.totalResponses)
    )
}

/** SDT-specific responses list showing each question and answer, mirroring
 *  the Android original's `SDTResponsesList`. */
@Composable
private fun SDTResponsesList(uiState: SDTSubmissionResultUiState) {
    val submission = uiState.submission ?: return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(Res.string.sdt_result_summary_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(stringResource(Res.string.sdt_result_time_taken, submission.totalTimeTakenMinutes))
                Text(stringResource(Res.string.sdt_result_valid_responses, submission.validResponses))
            }
        }
        submission.responses.forEach { response ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(response.question, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (!response.isSkipped) {
                        Text(response.answer, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            stringResource(Res.string.result_sdt_characters_label, response.charCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            stringResource(Res.string.sdt_result_skipped),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
