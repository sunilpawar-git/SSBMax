package com.ssbmax.shared.ui.interviewstart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.shared.presentation.interviewstart.StartInterviewUiState
import com.ssbmax.shared.presentation.interviewstart.StartInterviewViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.interview_action_retry
import ssbmax.shared.generated.resources.interview_button_start
import ssbmax.shared.generated.resources.interview_cd_back
import ssbmax.shared.generated.resources.interview_consent_message
import ssbmax.shared.generated.resources.interview_error_generic
import ssbmax.shared.generated.resources.interview_generating_questions
import ssbmax.shared.generated.resources.interview_generating_questions_hint
import ssbmax.shared.generated.resources.interview_new_interview_title
import ssbmax.shared.generated.resources.interview_prerequisites_met
import ssbmax.shared.generated.resources.interview_prerequisites_title
import ssbmax.shared.generated.resources.interview_start_title

/**
 * KMP port of `app/.../ui/interview/start/StartInterviewScreen.kt`.
 *
 * Uses `koinViewModel<StartInterviewViewModel>()` -- `StartInterviewViewModel`
 * is a real `androidx.lifecycle.ViewModel`, cancelled automatically in its own
 * `onCleared`; no manual `DisposableEffect` teardown needed (Phase 1 of the
 * KMP-convergence plan).
 *
 * Documented deviation from the Android original: the notification-
 * permission request (`LocalNotificationPermissionController.current.request()`)
 * that the Android screen fires before starting is NOT reproduced here --
 * [com.ssbmax.shared.platform.permissions] Phase 4 shim is Koin-bound on
 * Android but the Android original's own version is bound via a
 * `CompositionLocal` provided from `MainActivity` (its own doc comment
 * explains why: the permission launcher must register before `STARTED`,
 * which Koin can't guarantee). This `shared`-only screen doesn't have an
 * equivalent `CompositionLocal` provider wired in yet, and interview results
 * still arrive correctly without the notification (the session/result
 * screens below don't depend on it) -- named here rather than silently
 * dropped.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartInterviewScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSession: (sessionId: String) -> Unit,
    viewModel: StartInterviewViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var consentGiven by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSessionCreated) {
        if (uiState.isSessionCreated && uiState.sessionId != null) {
            onNavigateToSession(uiState.sessionId!!)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.checkEligibility()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.interview_start_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.interview_cd_back))
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isGeneratingQuestions -> GeneratingQuestionsContent(uiState.loadingMessage)
                uiState.isLoading -> LoadingContent(uiState.loadingMessage)
                uiState.error != null -> ErrorContent(uiState.error, onRetry = viewModel::checkEligibility)
                else -> MainContent(
                    uiState = uiState,
                    consentGiven = consentGiven,
                    onConsentChange = { consentGiven = it },
                    onStartInterview = { viewModel.createSession(consentGiven) }
                )
            }
        }
    }
}

@Composable
private fun GeneratingQuestionsContent(loadingMessage: String?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(24.dp))
        Text(
            text = loadingMessage ?: stringResource(Res.string.interview_generating_questions),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.interview_generating_questions_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LoadingContent(message: String?) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        if (message != null) {
            Spacer(Modifier.height(16.dp))
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ErrorContent(error: String?, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = error ?: stringResource(Res.string.interview_error_generic),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text(stringResource(Res.string.interview_action_retry)) }
    }
}

@Composable
private fun MainContent(
    uiState: StartInterviewUiState,
    consentGiven: Boolean,
    onConsentChange: (Boolean) -> Unit,
    onStartInterview: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = stringResource(Res.string.interview_new_interview_title),
                style = MaterialTheme.typography.titleLarge
            )
        }

        if (uiState.prerequisiteResult != null) {
            item {
                PrerequisitesCard(isEligible = uiState.isEligible, failureReasons = uiState.getFailureReasons())
            }
        }

        if (uiState.isEligible) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = consentGiven, onCheckedChange = onConsentChange)
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(Res.string.interview_consent_message), style = MaterialTheme.typography.bodyMedium)
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onStartInterview,
                    enabled = consentGiven && !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(Res.string.interview_button_start))
                }
            }
        }
    }
}

@Composable
private fun PrerequisitesCard(isEligible: Boolean, failureReasons: List<String>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = stringResource(Res.string.interview_prerequisites_title), style = MaterialTheme.typography.titleMedium)
            if (isEligible) {
                Text(
                    text = "✓ ${stringResource(Res.string.interview_prerequisites_met)}",
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                failureReasons.forEach { reason ->
                    Text(text = "• $reason", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
