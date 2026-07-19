package com.ssbmax.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ssbmax.shared.presentation.oirresult.OirResultUiState
import org.koin.compose.koinInject

/**
 * Trivial Compose Multiplatform screen for the Phase 0 spike. Proves CMP
 * renders on both Android and iOS from one commonMain source set — NOT a
 * port of the real OIRTestResultScreen (that's Phase 5 scope). Displays a
 * single field (percentage score, or loading/error) from OirResultViewModel
 * to demonstrate the full stack (Koin DI -> ViewModel -> StateFlow -> CMP)
 * working end-to-end.
 *
 * No submissionId is wired to a real Firestore doc in this spike's UI demo
 * — see docs/PHASE0 notes for how to exercise it against a live document.
 */
@Composable
fun SpikeApp() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SSBMax KMP Spike (Phase 0)",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Koin + GitLive Firebase + SQLDelight + Compose Multiplatform",
                    style = MaterialTheme.typography.bodyMedium
                )
                OirResultStatusText()
            }
        }
    }
}

@Composable
private fun OirResultStatusText() {
    // koinInject resolves the ViewModel through the sharedModule Koin graph,
    // proving DI wiring works from a Composable in commonMain.
    val viewModel = koinInject<com.ssbmax.shared.presentation.oirresult.OirResultViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    val statusText = when {
        uiState.isLoading -> "OIR result: idle (no submissionId loaded in this spike demo)"
        uiState.error != null -> "OIR result error: ${uiState.error}"
        uiState.result != null -> "OIR score: ${uiState.result?.percentageScore}%"
        else -> "OIR result: no data"
    }
    Text(text = statusText, style = MaterialTheme.typography.bodySmall)
}
