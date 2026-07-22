package com.ssbmax.shared.ui.piq

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.shared.presentation.piqresult.PIQSubmissionResultViewModel
import com.ssbmax.shared.ui.piq.components.PIQAIScoreCard
import com.ssbmax.shared.ui.piq.components.PIQAreasForImprovementCard
import com.ssbmax.shared.ui.piq.components.PIQStrengthsCard
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.piq_info_message
import ssbmax.shared.generated.resources.piq_quality_assessment
import ssbmax.shared.generated.resources.piq_return_home
import ssbmax.shared.generated.resources.piq_submission_id
import ssbmax.shared.generated.resources.piq_submitted_successfully

/**
 * KMP port of `app/.../ui/tests/piq/PIQSubmissionResultScreen.kt`. Fetches by
 * `submissionId` (ID-based navigation) via [PIQSubmissionResultViewModel].
 * See [com.ssbmax.shared.ui.piq.components.PIQAIScoreCard] and its siblings
 * for the score/strengths/areas cards, split out to respect the 300-line limit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PIQSubmissionResultScreen(
    submissionId: String,
    onNavigateHome: () -> Unit = {},
    viewModel: PIQSubmissionResultViewModel = koinInject(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    DisposableEffect(Unit) {
        onDispose { viewModel.close() }
    }

    LaunchedEffect(submissionId) {
        viewModel.loadSubmission(submissionId)
    }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.piq_quality_assessment)) }) }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                        Button(onClick = onNavigateHome) { Text(stringResource(Res.string.piq_return_home)) }
                    }
                }
            }
            uiState.submission != null -> {
                ResultContent(
                    submissionId = submissionId,
                    aiScore = uiState.submission!!.aiPreliminaryScore,
                    onNavigateHome = onNavigateHome,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun ResultContent(
    submissionId: String,
    aiScore: com.ssbmax.shared.domain.model.PIQAIScore?,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(stringResource(Res.string.piq_submitted_successfully), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(stringResource(Res.string.piq_submission_id, submissionId.take(8)), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        aiScore?.let { score ->
            item { PIQAIScoreCard(score) }
            if (score.strengths.isNotEmpty()) {
                item { PIQStrengthsCard(score.strengths) }
            }
            if (score.areasForImprovement.isNotEmpty()) {
                item { PIQAreasForImprovementCard(score.areasForImprovement) }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(Res.string.piq_info_message), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            Button(onClick = onNavigateHome, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.piq_return_home))
            }
        }
    }
}
