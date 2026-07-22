package com.ssbmax.shared.ui.wat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ssbmax.shared.domain.model.SubscriptionType
import com.ssbmax.shared.domain.model.WATPhase
import com.ssbmax.shared.presentation.wat.WATTestViewModel
import com.ssbmax.shared.ui.common.TestLimitReachedDialog
import com.ssbmax.shared.ui.wat.components.WATExitDialog
import com.ssbmax.shared.ui.wat.components.WATInProgressPhase
import com.ssbmax.shared.ui.wat.components.WATInstructionsPhase
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.wat_loading
import ssbmax.shared.generated.resources.wat_retry_button

/**
 * KMP port of `app/.../ui/tests/wat/WATTestScreen.kt`.
 *
 * Uses `koinInject<WATTestViewModel>()` (not `koinViewModel()`), matching
 * this phase's plain-class ViewModel pattern -- a [DisposableEffect] calls
 * [WATTestViewModel.close] on leaving the screen, same as
 * [com.ssbmax.shared.ui.tat.TATTestScreen]/[com.ssbmax.shared.ui.ppdt.PPDTTestScreen].
 *
 * Navigation on submit follows the same precedent: a
 * `LaunchedEffect(uiState.isSubmitted)` watches the UiState field directly,
 * rather than the Android original's `Channel<TestNavigationEvent>`
 * (`BaseTestViewModel.navigationEvents`, dropped along with the rest of that
 * WorkManager-coupled base class).
 *
 * Simpler than TAT's screen: no bottom bar (WAT has no "next"/"confirm"
 * step -- the response is submitted directly from the in-progress phase),
 * no submit-confirmation dialog (the Android original auto-submits after the
 * last word), and no profile-required gate (WAT's `loadTest` doesn't check
 * gender/profile completeness, unlike TAT).
 */
@Composable
fun WATTestScreen(
    testId: String,
    onTestComplete: (submissionId: String, subscriptionType: SubscriptionType) -> Unit = { _, _ -> },
    onNavigateBack: () -> Unit = {},
    viewModel: WATTestViewModel = koinInject(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showExitDialog by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { viewModel.close() }
    }

    LaunchedEffect(testId) {
        viewModel.loadTest(testId)
    }

    LaunchedEffect(uiState.isSubmitted) {
        if (uiState.isSubmitted && uiState.submissionId != null && uiState.subscriptionType != null) {
            onTestComplete(uiState.submissionId!!, uiState.subscriptionType!!)
        }
    }

    if (uiState.isLimitReached) {
        TestLimitReachedDialog(
            tier = uiState.subscriptionTier,
            testsLimit = uiState.testsLimit,
            testsUsed = uiState.testsUsed,
            resetsAt = uiState.resetsAt,
            onUpgrade = { onNavigateBack() },
            onDismiss = onNavigateBack
        )
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> LoadingState(modifier = Modifier.fillMaxSize())
            uiState.error != null -> ErrorState(error = uiState.error!!, onRetry = { viewModel.loadTest(testId) }, modifier = Modifier.fillMaxSize())
            else -> when (uiState.phase) {
                WATPhase.INSTRUCTIONS -> WATInstructionsPhase(onStart = { viewModel.startTest() })
                WATPhase.IN_PROGRESS -> WATInProgressPhase(
                    word = uiState.currentWord?.word ?: "",
                    wordNumber = uiState.currentWordIndex + 1,
                    totalWords = uiState.words.size,
                    timeRemaining = uiState.timeRemaining,
                    response = uiState.currentResponse,
                    onResponseChange = { viewModel.updateResponse(it) },
                    onSubmit = { viewModel.submitResponse() },
                    onSkip = { viewModel.skipWord() },
                    onShowExitDialog = { showExitDialog = true }
                )
                WATPhase.COMPLETED -> Unit // auto-submits; brief transitional state
                WATPhase.SUBMITTED -> Unit // navigation happens in LaunchedEffect above
            }
        }
    }

    if (showExitDialog) {
        WATExitDialog(
            onDismiss = { showExitDialog = false },
            onExit = { showExitDialog = false; onNavigateBack() }
        )
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator()
            Text(text = stringResource(Res.string.wat_loading), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ErrorState(error: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = error, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onRetry) { Text(stringResource(Res.string.wat_retry_button)) }
        }
    }
}
