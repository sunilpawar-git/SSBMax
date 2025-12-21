package com.ssbmax.ui.tests.srt.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.R

/**
 * SRT Test In-Progress Screen
 * Displays current situation, response input, and navigation controls
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SRTInProgressView(
    situation: String,
    situationNumber: Int,
    totalSituations: Int,
    response: String,
    onResponseChange: (String) -> Unit,
    minChars: Int,
    maxChars: Int,
    canMoveNext: Boolean,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    showExitDialog: Boolean,
    onShowExitDialog: () -> Unit,
    onDismissExitDialog: () -> Unit,
    onConfirmExit: () -> Unit
) {
    Scaffold(
        topBar = {
            SRTHeader(
                situationNumber = situationNumber,
                totalSituations = totalSituations,
                onShowExitDialog = onShowExitDialog
            )
        },
        bottomBar = {
            SRTBottomBar(
                canMoveNext = canMoveNext,
                onNext = onNext,
                onSkip = onSkip
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SRTSituationCard(situation = situation)
            
            SRTResponseInput(
                response = response,
                onResponseChange = onResponseChange,
                minChars = minChars,
                maxChars = maxChars
            )
        }
    }
    
    // Exit dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = onDismissExitDialog,
            title = { Text(stringResource(R.string.srt_exit_title)) },
            text = { Text(stringResource(R.string.srt_exit_message)) },
            confirmButton = {
                TextButton(onClick = onConfirmExit) {
                    Text(stringResource(R.string.srt_exit))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissExitDialog) {
                    Text(stringResource(R.string.srt_continue))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SRTHeader(
    situationNumber: Int,
    totalSituations: Int,
    onShowExitDialog: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(stringResource(R.string.srt_test))
                Text(
                    stringResource(R.string.srt_situation_number, situationNumber, totalSituations),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onShowExitDialog) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.srt_exit))
            }
        }
    )
}

@Composable
private fun SRTSituationCard(situation: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(R.string.srt_situation),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                situation,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun SRTResponseInput(
    response: String,
    onResponseChange: (String) -> Unit,
    minChars: Int,
    maxChars: Int
) {
    OutlinedTextField(
        value = response,
        onValueChange = onResponseChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.srt_your_response)) },
        placeholder = { Text(stringResource(R.string.srt_response_placeholder)) },
        supportingText = {
            Text(
                stringResource(R.string.srt_char_count, response.length, maxChars),
                color = when {
                    response.length < minChars -> MaterialTheme.colorScheme.error
                    response.length > maxChars -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        },
        isError = response.length > maxChars,
        maxLines = 8
    )
}

@Composable
private fun SRTBottomBar(
    canMoveNext: Boolean,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.srt_skip))
            }

            Button(
                onClick = onNext,
                enabled = canMoveNext,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.srt_next))
                Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
            }
        }
    }
}
