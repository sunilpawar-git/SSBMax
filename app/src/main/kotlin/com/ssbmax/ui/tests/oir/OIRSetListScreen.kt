package com.ssbmax.ui.tests.oir

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.R
import com.ssbmax.core.domain.model.OIRSetProgress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OIRSetListScreen(
    onStartSet: (setNumber: Int) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: OIRSetListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.oir_set_list_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = uiState.error!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(
                        onClick = { viewModel.loadSets() },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text(stringResource(R.string.oir_retry))
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { /* top spacing */ }
                    items(uiState.sets) { setProgress ->
                        OIRSetRow(
                            setProgress = setProgress,
                            isNextSet = setProgress.setNumber == uiState.nextSetNumber,
                            onStartSet = onStartSet
                        )
                    }
                    item { /* bottom spacing */ }
                }
            }
        }
    }
}

@Composable
private fun OIRSetRow(
    setProgress: OIRSetProgress,
    isNextSet: Boolean,
    onStartSet: (Int) -> Unit
) {
    val isLocked = !setProgress.isCompleted && !isNextSet
    val alpha = if (isLocked) 0.4f else 1f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha),
        colors = CardDefaults.cardColors(
            containerColor = when {
                setProgress.isCompleted -> MaterialTheme.colorScheme.secondaryContainer
                isNextSet -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SetRowInfo(setProgress = setProgress, isNextSet = isNextSet)

            if (!isLocked) {
                SetRowButton(setProgress = setProgress, onStartSet = onStartSet)
            } else {
                Text(
                    text = stringResource(R.string.oir_set_list_locked),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SetRowInfo(setProgress: OIRSetProgress, isNextSet: Boolean) {
    Column {
        Text(
            text = stringResource(R.string.oir_set_list_set_number, setProgress.setNumber),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isNextSet) FontWeight.Bold else FontWeight.Normal
        )
        val subtitle = when {
            setProgress.isCompleted && setProgress.score != null -> {
                val score = setProgress.score ?: 0
                val pct = (score * 100) / setProgress.totalQuestions
                stringResource(
                    R.string.oir_set_list_score_format,
                    score,
                    setProgress.totalQuestions,
                    pct
                )
            }
            isNextSet -> stringResource(R.string.oir_set_list_not_started)
            else -> stringResource(R.string.oir_set_list_locked)
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SetRowButton(setProgress: OIRSetProgress, onStartSet: (Int) -> Unit) {
    if (setProgress.isCompleted) {
        OutlinedButton(onClick = { onStartSet(setProgress.setNumber) }) {
            Text(stringResource(R.string.oir_set_list_retake))
        }
    } else {
        Button(onClick = { onStartSet(setProgress.setNumber) }) {
            Text(stringResource(R.string.oir_set_list_start))
        }
    }
}
